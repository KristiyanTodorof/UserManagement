package com.acmestack.permission;

import com.acmestack.audit.AuditService;
import com.acmestack.audit.EventType;
import com.acmestack.common.BusinessException;
import com.acmestack.role.Role;
import com.acmestack.role.RoleRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private static final String ADMIN_ROLE = "Admin";
    private static final int REVIEW_DAYS = 90;

    private final PermissionRepository permissions;
    private final RoleRepository roles;
    private final RolePermissionRepository rolePermissions;
    private final AuditService audit;

    public PermissionService(PermissionRepository permissions, RoleRepository roles,
                             RolePermissionRepository rolePermissions, AuditService audit) {
        this.permissions = permissions;
        this.roles = roles;
        this.rolePermissions = rolePermissions;
        this.audit = audit;
    }

    // ---------- reading ----------

    @Transactional(readOnly = true)
    public Matrix matrix(String q, String category, Long roleId, String review) {
        List<Role> shown = roles.findAll(Sort.by("id")).stream()
                .filter(r -> roleId == null || r.getId().equals(roleId))
                .toList();

        Map<String, Effect> effects = new HashMap<>();
        for (RolePermission rp : rolePermissions.findAllWithRefs()) {
            effects.put(rp.getPermission().getId() + ":" + rp.getRole().getId(), rp.getEffect());
        }

        String needle = q == null ? "" : q.trim().toLowerCase();
        Map<String, List<MatrixRow>> byCategory = new LinkedHashMap<>();

        for (Permission p : permissions.findAllByOrderByIdAsc()) {
            boolean due = isReviewDue(p);
            if (!needle.isEmpty() && !matches(p, needle)) continue;
            if (!category.isBlank() && !p.getCategory().equals(category)) continue;
            if ("due".equals(review) && !due) continue;
            if ("ok".equals(review) && due) continue;

            List<MatrixCell> cells = shown.stream()
                    .map(r -> new MatrixCell(r.getId(), r.getName(),
                            effects.getOrDefault(p.getId() + ":" + r.getId(), Effect.DENIED),
                            ADMIN_ROLE.equals(r.getName())))
                    .toList();
            byCategory.computeIfAbsent(p.getCategory(), k -> new ArrayList<>())
                    .add(new MatrixRow(p, due, cells));
        }

        List<MatrixGroup> groups = byCategory.entrySet().stream()
                .map(e -> new MatrixGroup(e.getKey(), e.getValue()))
                .toList();
        return new Matrix(shown, groups);
    }

    @Transactional(readOnly = true)
    public PermissionStats stats() {
        List<Permission> all = permissions.findAll();
        long categories = all.stream().map(Permission::getCategory).distinct().count();
        return new PermissionStats(
                all.size(), categories, rolePermissions.count(),
                rolePermissions.countByEffect(Effect.GRANTED),
                rolePermissions.countByEffect(Effect.DENIED),
                rolePermissions.countByEffect(Effect.LIMITED),
                permissions.countByLastReviewedAtBefore(LocalDateTime.now().minusDays(REVIEW_DAYS)));
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return permissions.findAllByOrderByIdAsc().stream()
                .map(Permission::getCategory).distinct().toList();
    }

    @Transactional(readOnly = true)
    public List<Role> allRoles() {
        return roles.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Permission get(Long id) {
        return permissions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<RoleAccess> roleAccess(Long permissionId) {
        Map<Long, Effect> byRole = rolePermissions.findByPermissionIdWithRole(permissionId).stream()
                .collect(Collectors.toMap(rp -> rp.getRole().getId(), RolePermission::getEffect));
        return roles.findAll(Sort.by("id")).stream()
                .map(r -> new RoleAccess(r, byRole.getOrDefault(r.getId(), Effect.DENIED),
                        ADMIN_ROLE.equals(r.getName())))
                .toList();
    }

    public boolean isReviewDue(Permission p) {
        return p.getLastReviewedAt() != null
                && p.getLastReviewedAt().isBefore(LocalDateTime.now().minusDays(REVIEW_DAYS));
    }

    // ---------- writing ----------

    /** Sets the effect, or cycles to the next one when {@code requested} is null. */
    @Transactional
    public Effect change(Long permissionId, Long roleId, Effect requested, String actorName) {
        Permission p = get(permissionId);
        Role r = roles.findById(roleId).orElseThrow(() -> new BusinessException("Role not found."));
        if (ADMIN_ROLE.equals(r.getName())) {
            throw new BusinessException("The Admin role always holds every permission.");
        }

        RolePermission rp = rolePermissions.findByRoleIdAndPermissionId(roleId, permissionId)
                .orElseGet(() -> {
                    RolePermission created = new RolePermission();
                    created.setRole(r);
                    created.setPermission(p);
                    created.setEffect(Effect.DENIED);
                    return created;
                });

        Effect before = rp.getEffect();
        Effect after = requested != null ? requested : next(before);
        if (after == before) return before;

        rp.setEffect(after);
        rolePermissions.save(rp);

        r.setUpdatedAt(LocalDateTime.now());   // shows up as "Last updated" on the Roles page
        r.setUpdatedBy(actorName);

        audit.log(EventType.PERMISSION_UPDATED,
                "Changed \"" + p.getName() + "\" for " + r.getName() + ": " + label(before) + " → " + label(after),
                "Permission", p.getId(), p.getName(),
                Map.of("role", r.getName(), "permission", p.getKey(),
                        "from", before.name(), "to", after.name()));
        return after;
    }

    @Transactional
    public void markReviewed(Long permissionId) {
        Permission p = get(permissionId);
        p.setLastReviewedAt(LocalDateTime.now());
        audit.log(EventType.PERMISSION_UPDATED, "Marked permission as reviewed",
                "Permission", p.getId(), p.getName(), null);
    }

    // ---------- helpers ----------

    private static Effect next(Effect e) {
        return switch (e) {
            case DENIED -> Effect.GRANTED;
            case GRANTED -> Effect.LIMITED;
            case LIMITED -> Effect.DENIED;
        };
    }

    private static String label(Effect e) {
        return e.name().charAt(0) + e.name().substring(1).toLowerCase();
    }

    private static boolean matches(Permission p, String needle) {
        return p.getName().toLowerCase().contains(needle)
                || p.getKey().toLowerCase().contains(needle)
                || p.getCategory().toLowerCase().contains(needle)
                || (p.getDescription() != null && p.getDescription().toLowerCase().contains(needle));
    }
}