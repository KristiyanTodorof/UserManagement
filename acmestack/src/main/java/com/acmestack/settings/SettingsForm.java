package com.acmestack.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SettingsForm {

    @NotBlank(message = "Workspace name is required")
    @Size(max = 100, message = "Maximum 100 characters")
    private String name;

    @NotBlank(message = "Choose a time zone")
    private String timezone;

    @NotNull(message = "Choose a timeout")
    private Integer sessionTimeoutMinutes;

    private boolean twoFactorRequired;
    private boolean emailNotifications;

    @NotBlank
    private String emailLevel;

    private boolean productUpdates;

    @NotBlank
    private String productLevel;

    @NotBlank
    private String theme;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Integer getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(Integer v) { this.sessionTimeoutMinutes = v; }
    public boolean isTwoFactorRequired() { return twoFactorRequired; }
    public void setTwoFactorRequired(boolean v) { this.twoFactorRequired = v; }
    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean v) { this.emailNotifications = v; }
    public String getEmailLevel() { return emailLevel; }
    public void setEmailLevel(String emailLevel) { this.emailLevel = emailLevel; }
    public boolean isProductUpdates() { return productUpdates; }
    public void setProductUpdates(boolean v) { this.productUpdates = v; }
    public String getProductLevel() { return productLevel; }
    public void setProductLevel(String productLevel) { this.productLevel = productLevel; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
}