package com.acmestack.audit;

public enum EventType {
    SIGN_IN("Sign-in", "bi-box-arrow-in-right", Severity.LOW),
    FAILED_SIGN_IN("Failed sign-in", "bi-exclamation-triangle", Severity.HIGH),
    ACCOUNT_LOCKED("Account locked", "bi-lock", Severity.HIGH),
    USER_INVITED("User invited", "bi-person-plus", Severity.LOW),
    USER_UPDATED("User updated", "bi-person-gear", Severity.LOW),
    ROLE_CHANGED("Role changed", "bi-people", Severity.MEDIUM),
    USER_SUSPENDED("User suspended", "bi-slash-circle", Severity.MEDIUM),
    USER_REACTIVATED("User reactivated", "bi-check-circle", Severity.LOW),
    USER_DEACTIVATED("User deactivated", "bi-trash", Severity.MEDIUM),
    PASSWORD_RESET("Password reset", "bi-key", Severity.LOW),
    REPORT_EXPORTED("Report exported", "bi-file-earmark-text", Severity.LOW),
    // used in later phases
    ROLE_CREATED("Role created", "bi-shield-plus", Severity.MEDIUM),
    ROLE_UPDATED("Role updated", "bi-shield", Severity.MEDIUM),
    ROLE_DELETED("Role deleted", "bi-shield-x", Severity.HIGH),
    PERMISSION_UPDATED("Permission updated", "bi-shield-check", Severity.MEDIUM),
    SETTINGS_MODIFIED("Settings modified", "bi-gear", Severity.LOW);

    private final String label;
    private final String icon;
    private final Severity severity;

    EventType(String label, String icon, Severity severity) {
        this.label = label;
        this.icon = icon;
        this.severity = severity;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public Severity getSeverity() { return severity; }
}