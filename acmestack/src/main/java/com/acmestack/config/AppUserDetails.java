package com.acmestack.config;

import com.acmestack.user.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final String roleName;
    private final UserStatus status;
    private final Collection<? extends GrantedAuthority> authorities;

    public AppUserDetails(Long id, String name, String email, String passwordHash,
                          String roleName, UserStatus status,
                          Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roleName = roleName;
        this.status = status;
        this.authorities = authorities;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getRoleName() { return roleName; }

    public String getInitials() {
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return status != UserStatus.SUSPENDED; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return status != UserStatus.PENDING; }
}