package com.acmestack.user;

import com.acmestack.config.AppUserDetails;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class LoginListener {

    private final UserRepository users;

    public LoginListener(UserRepository users) {
        this.users = users;
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof AppUserDetails principal) {
            users.findById(principal.getId()).ifPresent(u -> {
                if (u.getStatus() == UserStatus.PENDING) u.setStatus(UserStatus.ACTIVE);
                u.setLastActiveAt(LocalDateTime.now());
                u.setFailedLoginCount(0);
            });
        }
    }
}