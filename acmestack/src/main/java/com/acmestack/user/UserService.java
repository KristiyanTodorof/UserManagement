package com.acmestack.user;

import com.acmestack.common.BusinessException;
import com.acmestack.permission.Effect;
import com.acmestack.permission.RolePermission;
import com.acmestack.permission.RolePermissionRepository;
import com.acmestack.role.Role;
import com.acmestack.role.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    public static final int PAGE_SIZE = 8;
    private static final String ADMIN_ROLE = "Admin";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final UserRepository users;
    private final RoleRepository roles;
    private final RolePermissionRepository rolePermissions;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public UserService(UserRepository users, RoleRepository roles,
                       RolePermissionRepository rolePermissions, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.rolePermissions = rolePermissions;
        this.encoder = encoder;
    }


    @Transactional(readOnly = true)
    public Page<User> search(String q, Long roleId, UserStatus status, String period, int page) {
        LocalDateTime since = switch (period) {
            case "7" -> LocalDateTime.now().minusDays(7);
            case "30" -> LocalDateTime.now().minusDays(30);
            case "90" -> LocalDateTime.now().minusDays(90);
            default -> null;
        };
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));
        return users.findAll(UserSpecs.filter(q, roleId, status, since), pageable);
    }

    @Transactional(readOnly = true)
    public UserStats stats() {
        LocalDateTime now = LocalDateTime.now();
        return new UserStats(
                users.countByDeletedAtIsNull(),
                users.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(now.minusDays(7)),
                users.countByCreatedAtBetweenAndDeletedAtIsNull(now.minusDays(14), now.minusDays(7)),
                users.countByStatusAndDeletedAtIsNull(UserStatus.ACTIVE),
                users.countByRoleNameAndDeletedAtIsNull(ADMIN_ROLE));
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findActiveById(id).orElseThrow(() -> new BusinessException("User not found."));
    }

    @Transactional(readOnly = true)
    public List<PermissionGroup> permissionGroups(Long roleId) {
        Map<String, long[]> counts = new LinkedHashMap<>();
        for (RolePermission rp : rolePermissions.findWithPermissionByRoleId(roleId)) {
            long[] c = counts.computeIfAbsent(rp.getPermission().getCategory(), k -> new long[2]);
            c[1]++;
            if (rp.getEffect() == Effect.GRANTED) c[0]++;
        }
        return counts.entrySet().stream()
                .map(e -> new PermissionGroup(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    @Transactional
    public String invite(String name, String email, Long roleId) {
        String cleanEmail = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(cleanEmail)) {
            throw new BusinessException("A user with this email already exists.");
        }
        String temp = tempPassword();
        User u = new User();
        u.setName(name.trim());
        u.setEmail(cleanEmail);
        u.setRole(findRole(roleId));
        u.setStatus(UserStatus.PENDING);
        u.setCreatedAt(LocalDateTime.now());
        u.setPasswordHash(encoder.encode(temp));
        users.save(u);
        return temp;
    }

    @Transactional
    public void update(Long id, String name, String email, Long roleId, Long actingUserId) {
        User u = get(id);
        String cleanEmail = email.trim().toLowerCase();
        if (!u.getEmail().equalsIgnoreCase(cleanEmail) && users.existsByEmailIgnoreCase(cleanEmail)) {
            throw new BusinessException("A user with this email already exists.");
        }
        Role newRole = findRole(roleId);
        if (!u.getRole().getId().equals(newRole.getId())) {
            if (u.getId().equals(actingUserId)) throw new BusinessException("You can't change your own role.");
            guardLastAdmin(u);
        }
        u.setName(name.trim());
        u.setEmail(cleanEmail);
        u.setRole(newRole);
    }

    @Transactional
    public void suspend(Long id, Long actingUserId) {
        User u = get(id);
        if (u.getId().equals(actingUserId)) throw new BusinessException("You can't suspend yourself.");
        guardLastAdmin(u);
        u.setStatus(UserStatus.SUSPENDED);
    }

    @Transactional
    public void reactivate(Long id) {
        User u = get(id);
        u.setStatus(UserStatus.ACTIVE);
        u.setFailedLoginCount(0);
    }

    @Transactional
    public void delete(Long id, Long actingUserId) {
        User u = get(id);
        if (u.getId().equals(actingUserId)) throw new BusinessException("You can't delete yourself.");
        guardLastAdmin(u);
        u.setDeletedAt(LocalDateTime.now());   // soft delete
    }

    @Transactional
    public String resetPassword(Long id) {
        User u = get(id);
        String temp = tempPassword();
        u.setPasswordHash(encoder.encode(temp));
        u.setFailedLoginCount(0);
        return temp;
    }

    private Role findRole(Long roleId) {
        return roles.findById(roleId).orElseThrow(() -> new BusinessException("Role not found."));
    }

    private void guardLastAdmin(User u) {
        boolean isActiveAdmin = ADMIN_ROLE.equals(u.getRole().getName()) && u.getStatus() == UserStatus.ACTIVE;
        if (isActiveAdmin && users.countByRoleNameAndStatusAndDeletedAtIsNull(ADMIN_ROLE, UserStatus.ACTIVE) <= 1) {
            throw new BusinessException("This is the last active Admin. Promote someone else first.");
        }
    }

    private String tempPassword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}