package com.acmestack.config;

import com.acmestack.user.UserRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Updates users.last_active_at, but at most once a minute per user. */
@Component
public class ActivityTracker {

    private static final Duration INTERVAL = Duration.ofMinutes(1);

    private final UserRepository users;
    private final ConcurrentMap<Long, Instant> lastWrite = new ConcurrentHashMap<>();

    public ActivityTracker(UserRepository users) {
        this.users = users;
    }

    public void touch(Long userId) {
        Instant now = Instant.now();
        Instant previous = lastWrite.get(userId);
        if (previous != null && Duration.between(previous, now).compareTo(INTERVAL) < 0) return;
        lastWrite.put(userId, now);
        users.touch(userId, LocalDateTime.now());
    }
}