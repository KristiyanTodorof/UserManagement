package com.acmestack.settings;

import com.acmestack.audit.AuditService;
import com.acmestack.audit.EventType;
import com.acmestack.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SettingsService {

    public static final List<Integer> TIMEOUTS = List.of(15, 30, 60, 120, 480);
    public static final List<String> THEMES = List.of("LIGHT", "DARK", "SYSTEM");

    public static final Map<String, String> EMAIL_LEVELS = Map.of(
            "ALL", "All notifications", "IMPORTANT", "Important only");
    public static final Map<String, String> PRODUCT_LEVELS = Map.of(
            "ALL", "All updates", "MAJOR", "Only major updates");

    public static final Map<String, String> ZONES = zones();

    private static Map<String, String> zones() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Pacific/Honolulu", "Hawaii");
        m.put("America/Anchorage", "Alaska");
        m.put("America/Los_Angeles", "Pacific Time (US & Canada)");
        m.put("America/Denver", "Mountain Time (US & Canada)");
        m.put("America/Chicago", "Central Time (US & Canada)");
        m.put("America/New_York", "Eastern Time (US & Canada)");
        m.put("UTC", "UTC");
        m.put("Europe/London", "London");
        m.put("Europe/Berlin", "Berlin, Paris, Rome");
        m.put("Europe/Sofia", "Sofia, Athens, Helsinki");
        m.put("Asia/Dubai", "Dubai");
        m.put("Asia/Kolkata", "India");
        m.put("Asia/Tokyo", "Tokyo");
        m.put("Australia/Sydney", "Sydney");
        return m;
    }

    private final WorkspaceRepository workspaces;
    private final IntegrationRepository integrations;
    private final AuditService audit;

    private volatile WorkspaceInfo cached;

    public SettingsService(WorkspaceRepository workspaces, IntegrationRepository integrations,
                           AuditService audit) {
        this.workspaces = workspaces;
        this.integrations = integrations;
        this.audit = audit;
    }


    public WorkspaceInfo current() {
        WorkspaceInfo c = cached;
        if (c == null) {
            c = toInfo(load());
            cached = c;
        }
        return c;
    }

    public SettingsForm form() {
        WorkspaceInfo w = current();
        SettingsForm f = new SettingsForm();
        f.setName(w.name());
        f.setTimezone(w.timezone());
        f.setSessionTimeoutMinutes(w.sessionTimeoutMinutes());
        f.setTwoFactorRequired(w.twoFactorRequired());
        f.setEmailNotifications(w.emailNotifications());
        f.setEmailLevel(w.emailLevel());
        f.setProductUpdates(w.productUpdates());
        f.setProductLevel(w.productLevel());
        f.setTheme(w.theme());
        return f;
    }

    public List<SecurityCheck> securityChecks() {
        WorkspaceInfo w = current();
        return List.of(
                new SecurityCheck("Two-factor authentication required", w.twoFactorRequired()),
                new SecurityCheck("Session timeout is 60 minutes or less", w.sessionTimeoutMinutes() <= 60),
                new SecurityCheck("Accounts lock after 5 failed sign-ins", true),   // built in Phase 4
                new SecurityCheck("Email notifications enabled", w.emailNotifications()));
    }

    @Transactional(readOnly = true)
    public List<Integration> integrations() {
        return integrations.findAllByOrderByIdAsc();
    }

    @Transactional
    public boolean save(SettingsForm f, String actorName) {
        if (!ZONES.containsKey(f.getTimezone())) throw new BusinessException("Unknown time zone.");
        if (!TIMEOUTS.contains(f.getSessionTimeoutMinutes())) throw new BusinessException("Invalid session timeout.");
        if (!EMAIL_LEVELS.containsKey(f.getEmailLevel())) throw new BusinessException("Invalid email notification level.");
        if (!PRODUCT_LEVELS.containsKey(f.getProductLevel())) throw new BusinessException("Invalid product update level.");
        if (!THEMES.contains(f.getTheme())) throw new BusinessException("Invalid theme.");

        Workspace w = load();
        String name = f.getName().trim();

        Map<String, Object> changes = new LinkedHashMap<>();
        diff(changes, "workspace_name", w.getName(), name);
        diff(changes, "timezone", w.getTimezone(), f.getTimezone());
        diff(changes, "session_timeout_minutes", w.getSessionTimeoutMinutes(), f.getSessionTimeoutMinutes());
        diff(changes, "two_factor_required", w.isTwoFactorRequired(), f.isTwoFactorRequired());
        diff(changes, "email_notifications", w.isEmailNotifications(), f.isEmailNotifications());
        diff(changes, "email_level", w.getEmailLevel(), f.getEmailLevel());
        diff(changes, "product_updates", w.isProductUpdates(), f.isProductUpdates());
        diff(changes, "product_level", w.getProductLevel(), f.getProductLevel());
        diff(changes, "theme", w.getTheme(), f.getTheme());
        if (changes.isEmpty()) return false;

        w.setName(name);
        w.setTimezone(f.getTimezone());
        w.setSessionTimeoutMinutes(f.getSessionTimeoutMinutes());
        w.setTwoFactorRequired(f.isTwoFactorRequired());
        w.setEmailNotifications(f.isEmailNotifications());
        w.setEmailLevel(f.getEmailLevel());
        w.setProductUpdates(f.isProductUpdates());
        w.setProductLevel(f.getProductLevel());
        w.setTheme(f.getTheme());
        w.setUpdatedAt(LocalDateTime.now());
        w.setUpdatedBy(actorName);
        workspaces.save(w);

        audit.log(EventType.SETTINGS_MODIFIED,
                "Updated workspace settings (" + String.join(", ", changes.keySet()) + ")",
                "Settings", 1L, "Workspace settings", changes);

        evictCacheAfterCommit();
        return true;
    }

    @Transactional
    public void toggleIntegration(Long id) {
        Integration i = integrations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean connect = i.getStatus() == IntegrationStatus.AVAILABLE;
        i.setStatus(connect ? IntegrationStatus.CONNECTED : IntegrationStatus.AVAILABLE);
        audit.log(EventType.SETTINGS_MODIFIED,
                (connect ? "Connected " : "Disconnected ") + i.getName() + " (demo integration)",
                "Integration", i.getId(), i.getName(), Map.of("status", i.getStatus().name()));
    }

    // ---------- helpers ----------

    private Workspace load() {
        return workspaces.findById(1L).orElseThrow(() -> new IllegalStateException("Workspace row is missing"));
    }

    private static WorkspaceInfo toInfo(Workspace w) {
        return new WorkspaceInfo(w.getName(), w.getTimezone(), w.getSessionTimeoutMinutes(),
                w.isTwoFactorRequired(), w.isEmailNotifications(), w.getEmailLevel(),
                w.isProductUpdates(), w.getProductLevel(), w.getTheme(), w.getPlan());
    }

    private static void diff(Map<String, Object> changes, String label, Object before, Object after) {
        if (!Objects.equals(before, after)) changes.put(label, before + " → " + after);
    }

    private void evictCacheAfterCommit() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cached = null;
            }
        });
    }
}