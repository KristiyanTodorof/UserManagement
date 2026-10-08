package com.acmestack.config;

import com.acmestack.permission.RolePermissionRepository;
import com.acmestack.user.User;
import com.acmestack.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;
    private final RolePermissionRepository rolePermissions;

    public AppUserDetailsService(UserRepository users, RolePermissionRepository rolePermissions) {
        this.users = users;
        this.rolePermissions = rolePermissions;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = users.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));

        List<SimpleGrantedAuthority> authorities = rolePermissions
                .findGrantedKeys(user.getRole().getId()).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new AppUserDetails(user.getId(), user.getName(), user.getEmail(),
                user.getPasswordHash(), user.getRole().getName(),
                user.getStatus(), authorities);
    }
}