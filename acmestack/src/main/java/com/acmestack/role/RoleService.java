package com.acmestack.role;

import com.acmestack.audit.AuditService;
import com.acmestack.audit.EventType;
import com.acmestack.common.BusinessException;
import com.acmestack.permission.Effect;
import com.acmestack.permission.Permission;
import com.acmestack.permission.PermissionRepository;
import com.acmestack.permission.RolePermission;
import com.acmestack.permission.RolePermissionRepository;
import com.acmestack.user.User;
import com.acmestack.user.UserRepository;
import com.acmestack.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoleService {

    public static final List<String> ICONS = List.of("bi-shield", "bi-people", "bi-person", "bi-headset",
            "bi-credit-card", "bi-eye", "bi-briefcase", "bi-bar-chart", "bi-gear", "bi-star");
    public static final List<String> COLORS = List.of("indigo", "green", "blue", "orange", "red", "gray");

    private final RoleRepository roles;
    private final UserRepository users;
    private final PermissionRepository permissions;
    private final RolePermissionRepository rolePermissions;
    private final UserService userService;
    private final AuditService audit;

    public RoleService(RoleRepository roles, UserRepository users, PermissionRepository permissions,
                       RolePermissionRepository rolePermissions, UserService userService,
                       AuditService audit) {
        this.roles = roles;
        this.users = users;
        this.permissions = permissions;
        this.rolePermissions = rolePermissions;
        this.userService = userService;
        this.audit = audit;
    }

    // ---------- reading ----------

    /** There are only a handful of roles, so filtering and sorting happen in memory. */
    @Transactional(readOnly = true)
    public List<RoleRow> list(String q, String type, String sort) {
        Map<Long, Long> userCounts = toMap(users.countUsersPerRole());
        Map<Long, Long> grantedCounts = toMap(rolePermissions.countGrantedByRole());
        long totalPermissions = permissions.count();
        String needle = q == null ? "" : q.trim().toLowerCase();

        Comparator<RoleRow> order = switch (sort) {
            case "updated" -> Comparator.comparing((RoleRow r) -> r.role().getUpdatedAt()).reversed();
            case "users" -> Comparator.comparingLong(RoleRow::users).reversed();
            default -> Comparator.comparing((RoleRow r) -> r.role().getName(), String.CASE_INSENSITIVE_ORDER);
        };

        return roles.findAll().stream()
                .filter(r -> needle.isEmpty()
                        || r.getName().toLowerCase().contains(needle)
                        || (r.getDescription() != null && r.getDescription().toLowerCase().contains(needle)))
                .filter(r -> type.isBlank() || r.getType().name().equalsIgnoreCase(type))
                .map(r -> {
                    long n = userCounts.getOrDefault(r.getId(), 0L);
                    List<User> preview = users.findTop3ByRoleIdAndDeletedAtIsNullOrderByCreatedAtAsc(r.getId());
                    return new RoleRow(r, n, grantedCounts.getOrDefault(r.getId(), 0L),
                            totalPermissions, preview, Math.max(0, n - preview.size()));
                })
                .sorted(order)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleStats stats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthAgo = now.minusDays(30);
        LocalDateTime reviewLimit = now.minusDays(90);
        List<Role> all = roles.findAll();

        long custom = all.stream().filter(r -> r.getType() == RoleType.CUSTOM).count();
        long created = all.stream().filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(monthAgo)).count();
        long customCreated = all.stream()
                .filter(r -> r.getType() == RoleType.CUSTOM)
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(monthAgo)).count();
        long due = all.stream().filter(r -> r.getUpdatedAt() != null && r.getUpdatedAt().isBefore(reviewLimit)).count();

        return new RoleStats(all.size(), custom, created, customCreated, users.countByDeletedAtIsNull(), due);
    }

    @Transactional(readOnly = true)
    public Role get(Long id) {
        return roles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public long assignedCount(Long roleId) {
        return users.countByRoleIdAndDeletedAtIsNull(roleId);
    }

    @Transactional(readOnly = true)
    public List<User> assignedUsers(Long roleId) {
        return users.findTop20ByRoleIdAndDeletedAtIsNullOrderByNameAsc(roleId);
    }

    @Transactional(readOnly = true)
    public List<User> assignable(Long roleId) {
        return users.findAssignable(roleId);
    }

    @Transactional(readOnly = true)
    public List<RolePermissionGroup> permissionGroups(Long roleId) {
        Map<String, List<RolePermission>> byCategory = new LinkedHashMap<>();
        for (RolePermission rp : rolePermissions.findWithPermissionByRoleId(roleId)) {
            byCategory.computeIfAbsent(rp.getPermission().getCategory(), k -> new ArrayList<>()).add(rp);
        }
        return byCategory.entrySet().stream()
                .map(e -> new RolePermissionGroup(e.getKey(), e.getValue(),
                        e.getValue().stream().filter(rp -> rp.getEffect() == Effect.GRANTED).count()))
                .toList();
    }

    // ---------- writing ----------

    @Transactional
    public Role create(RoleForm f, String actorName) {
        String name = f.getName().trim();
        if (roles.existsByNameIgnoreCase(name)) {
            throw new BusinessException("A role with this name already exists.");
        }
        LocalDateTime now = LocalDateTime.now();
        Role r = new Role();
        r.setName(name);
        r.setDescription(clean(f.getDescription()));
        r.setType(RoleType.CUSTOM);
        r.setIcon(icon(f.getIcon()));
        r.setColor(color(f.getColor()));
        r.setCreatedAt(now);
        r.setUpdatedAt(now);
        r.setUpdatedBy(actorName);
        roles.save(r);

        // a new role starts with every permission DENIED, so the matrix has a full row for it
        for (Permission p : permissions.findAll()) {
            addRow(r, p, Effect.DENIED);
        }

        audit.log(EventType.ROLE_CREATED, "Created a custom role", "Role", r.getId(), r.getName(),
                Map.of("type", "CUSTOM"));
        return r;
    }

    @Transactional
    public void update(Long id, RoleForm f, String actorName) {
        Role r = get(id);
        String name = f.getName().trim();
        if (r.getType() == RoleType.SYSTEM && !r.getName().equals(name)) {
            throw new BusinessException("System roles can't be renamed.");
        }
        if (roles.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("A role with this name already exists.");
        }
        r.setName(name);
        r.setDescription(clean(f.getDescription()));
        r.setIcon(icon(f.getIcon()));
        r.setColor(color(f.getColor()));
        r.setUpdatedAt(LocalDateTime.now());
        r.setUpdatedBy(actorName);

        audit.log(EventType.ROLE_UPDATED, "Updated role details", "Role", r.getId(), r.getName(), null);
    }

    @Transactional
    public Role duplicate(Long id, String actorName) {
        Role src = get(id);
        String base = "Copy of " + src.getName();
        String name = base;
        int n = 2;
        while (roles.existsByNameIgnoreCase(name)) {
            name = base + " (" + n++ + ")";
        }
        if (name.length() > 50) {
            throw new BusinessException("The name is too long to duplicate. Rename the role first.");
        }

        LocalDateTime now = LocalDateTime.now();
        Role copy = new Role();
        copy.setName(name);
        copy.setDescription(src.getDescription());
        copy.setType(RoleType.CUSTOM);
        copy.setIcon(src.getIcon());
        copy.setColor(src.getColor());
        copy.setCreatedAt(now);
        copy.setUpdatedAt(now);
        copy.setUpdatedBy(actorName);
        roles.save(copy);

        for (RolePermission rp : rolePermissions.findWithPermissionByRoleId(src.getId())) {
            addRow(copy, rp.getPermission(), rp.getEffect());
        }

        audit.log(EventType.ROLE_CREATED, "Duplicated role " + src.getName(), "Role", copy.getId(),
                copy.getName(), Map.of("copied_from", src.getName()));
        return copy;
    }

    @Transactional
    public void delete(Long id) {
        Role r = get(id);
        if (r.getType() == RoleType.SYSTEM) {
            throw new BusinessException("System roles can't be deleted.");
        }
        if (users.countByRoleId(id) > 0) {
            throw new BusinessException(
                    "This role is still assigned to users (including deactivated ones). Reassign them first.");
        }
        rolePermissions.deleteByRoleId(id);
        roles.delete(r);
        audit.log(EventType.ROLE_DELETED, "Deleted a custom role", "Role", id, r.getName(), null);
    }

    /**
     * Deliberately NOT @Transactional: every user is changed in its own transaction,
     * so one refused user (yourself, the last Admin) doesn't roll back the others.
     */
    public AssignResult assign(Long roleId, List<Long> userIds, Long actingUserId) {
        get(roleId);
        int changed = 0;
        List<String> problems = new ArrayList<>();
        for (Long userId : userIds) {
            try {
                userService.changeRole(userId, roleId, actingUserId);
                changed++;
            } catch (BusinessException e) {
                problems.add(e.getMessage());
            }
        }
        return new AssignResult(changed, problems.stream().distinct().toList());
    }

    // ---------- helpers ----------

    private void addRow(Role role, Permission permission, Effect effect) {
        RolePermission rp = new RolePermission();
        rp.setRole(role);
        rp.setPermission(permission);
        rp.setEffect(effect);
        rolePermissions.save(rp);
    }

    private static Map<Long, Long> toMap(List<RoleCount> counts) {
        return counts.stream().collect(Collectors.toMap(RoleCount::roleId, RoleCount::count));
    }

    private static String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Only known values are accepted, since they end up in CSS class names. */
    private static String icon(String i) {
        return ICONS.contains(i) ? i : ICONS.get(0);
    }

    private static String color(String c) {
        return COLORS.contains(c) ? c : COLORS.get(0);
    }
}