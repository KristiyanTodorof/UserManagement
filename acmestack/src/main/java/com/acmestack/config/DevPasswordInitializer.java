package com.acmestack.config;

import com.acmestack.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DevPasswordInitializer implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public DevPasswordInitializer(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        users.findAll().stream()
                .filter(u -> u.getPasswordHash() == null)
                .forEach(u -> {
                    u.setPasswordHash(encoder.encode("Password123!"));
                    users.save(u);
                });
    }
}