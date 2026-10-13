package com.acmestack.dashboard;

import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class HealthService {

    private final HealthEndpoint health;

    public HealthService(HealthEndpoint health) {
        this.health = health;
    }

    public SystemHealth check() {
        HealthComponent root = health.health();
        Map<String, HealthComponent> parts =
                root instanceof CompositeHealth c ? c.getComponents() : Map.of();

        boolean app = isUp(root);
        boolean db = isUp(parts.get("db"));
        boolean disk = parts.get("diskSpace") == null || isUp(parts.get("diskSpace"));

        List<ServiceStatus> services = List.of(
                new ServiceStatus("Authentication", db),   // sign-in needs the database
                new ServiceStatus("Database", db),
                new ServiceStatus("Disk space", disk),
                new ServiceStatus("Application", app));

        return new SystemHealth(app && db && disk, services, uptime());
    }

    private static boolean isUp(HealthComponent c) {
        return c != null && Status.UP.equals(c.getStatus());
    }

    private static String uptime() {
        Duration d = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
        if (d.toDays() > 0) return d.toDays() + "d " + d.toHoursPart() + "h";
        if (d.toHours() > 0) return d.toHours() + "h " + d.toMinutesPart() + "m";
        return Math.max(d.toMinutes(), 1) + "m";
    }
}