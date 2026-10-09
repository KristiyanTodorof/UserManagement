package com.acmestack.common;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component("fmt")
public class Fmt {

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
        return t == null ? "—" : t.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH));
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
}