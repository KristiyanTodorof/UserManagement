package com.acmestack.common;

import com.acmestack.settings.SettingsService;
import org.springframework.stereotype.Component;
import com.acmestack.chat.FileTypes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component("fmt")
public class Fmt {

    private final SettingsService settings;

    public Fmt(SettingsService settings) {
        this.settings = settings;
    }

    public String ago(LocalDateTime t) {
        if (t == null) return "Never";
        Duration d = Duration.between(t, LocalDateTime.now());
        long minutes = d.toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        long hours = d.toHours();
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        long days = d.toDays();
        if (days < 30) return days + (days == 1 ? " day ago" : " days ago");
        return date(t);
    }

    public String date(LocalDateTime t) {
        return t == null ? "—" : local(t).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH));
    }

    public String dateTime(LocalDateTime t) {
        return t == null ? "—" : local(t).format(DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH));
    }

    public String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    public int colorIndex(String name) {
        return Math.floorMod(name.hashCode(), 6);
    }

    public String categoryIcon(String category) {
        return switch (category) {
            case "User Management" -> "bi-people";
            case "Role Management" -> "bi-shield";
            case "Permission Management" -> "bi-file-earmark-text";
            case "Billing" -> "bi-credit-card";
            case "Audit Logs" -> "bi-clock-history";
            case "Security" -> "bi-lock";
            case "Reports" -> "bi-bar-chart";
            case "Chat" -> "bi-chat-dots";
            default -> "bi-shield-check";
        };
    }

    private ZonedDateTime local(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).withZoneSameInstant(settings.current().zoneId());
    }

    public String chatTime(LocalDateTime t) {
        if (t == null) return "";
        ZonedDateTime z = local(t);
        boolean today = z.toLocalDate().equals(ZonedDateTime.now(settings.current().zoneId()).toLocalDate());
        return z.format(DateTimeFormatter.ofPattern(today ? "h:mm a" : "MMM d, h:mm a", Locale.ENGLISH));
    }

    public String fileSize(Long bytes) {
        if (bytes == null) return "";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ENGLISH, "%.0f KB", bytes / 1024.0);
        return String.format(Locale.ENGLISH, "%.1f MB", bytes / (1024.0 * 1024));
    }

    public boolean isImage(String fileName) { return FileTypes.isImage(fileName); }
    public String fileIcon(String fileName)  { return FileTypes.icon(fileName); }
    public String fileColor(String fileName) { return FileTypes.color(fileName); }
}