package com.acmestack.user;

import com.acmestack.audit.AuditService;
import com.acmestack.audit.AuditStatus;
import com.acmestack.audit.EventType;
import com.acmestack.config.AppUserDetails;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Component
public class LoginListener {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository users;
    private final AuditService audit;

    public LoginListener(UserRepository users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (!(event.getAuthentication().getPrincipal() instanceof AppUserDetails principal)) return;

        boolean first = users.findById(principal.getId()).map(u -> {
            boolean wasPending = u.getStatus() == UserStatus.PENDING;
            if (wasPending) u.setStatus(UserStatus.ACTIVE);
            u.setLastActiveAt(LocalDateTime.now());
            u.setFailedLoginCount(0);
            return wasPending;
        }).orElse(false);

        audit.logAs(principal.getId(), principal.getName(), principal.getUsername(),
                EventType.SIGN_IN, AuditStatus.SUCCESS,
                first ? "First sign-in (account activated)" : "Signed in",
                "User", principal.getId(), principal.getName(), null);
    }

    @EventListener
    @Transactional
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        String email = event.getAuthentication().getName();
        Optional<User> found = users.findByEmailAndDeletedAtIsNull(email);

        if (found.isEmpty()) {
            audit.logAs(null, "Unknown", email, EventType.FAILED_SIGN_IN, AuditStatus.FAILED,
                    "Sign-in attempt for an unknown account", "User", null, email, null);
            return;
        }

        User u = found.get();
        int attempts = u.getFailedLoginCount() + 1;
        u.setFailedLoginCount(attempts);
        audit.logAs(u.getId(), u.getName(), u.getEmail(), EventType.FAILED_SIGN_IN, AuditStatus.FAILED,
                "Invalid password attempt", "User", u.getId(), u.getEmail(), Map.of("attempt", attempts));

        if (attempts >= MAX_FAILED_ATTEMPTS && u.getStatus() != UserStatus.SUSPENDED && !isLastActiveAdmin(u)) {
            u.setStatus(UserStatus.SUSPENDED);
            audit.logAs(u.getId(), u.getName(), u.getEmail(), EventType.ACCOUNT_LOCKED, AuditStatus.SUCCESS,
                    "Account suspended after " + attempts + " failed sign-ins",
                    "User", u.getId(), u.getName(), null);
        }
    }

    @EventListener
    @Transactional
    public void onLocked(AuthenticationFailureLockedEvent event) {
        String email = event.getAuthentication().getName();
        Optional<User> found = users.findByEmailAndDeletedAtIsNull(email);
        Long id = found.map(User::getId).orElse(null);
        audit.logAs(id, found.map(User::getName).orElse("Unknown"), email,
                EventType.FAILED_SIGN_IN, AuditStatus.FAILED,
                "Sign-in blocked: account is suspended", "User", id, email, null);
    }

    /** Never auto-lock the only active Admin, or nobody could unlock anyone. */
    private boolean isLastActiveAdmin(User u) {
        return "Admin".equals(u.getRole().getName()) && u.getStatus() == UserStatus.ACTIVE
                && users.countByRoleNameAndStatusAndDeletedAtIsNull("Admin", UserStatus.ACTIVE) <= 1;
    }
}