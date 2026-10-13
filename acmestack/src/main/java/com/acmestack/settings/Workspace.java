package com.acmestack.settings;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workspace")
public class Workspace {

    @Id
    private Long id;

    private String name;
    private String timezone;

    @Column(name = "session_timeout_minutes")
    private int sessionTimeoutMinutes;

    @Column(name = "two_factor_required")
    private boolean twoFactorRequired;

    @Column(name = "email_notifications")
    private boolean emailNotifications;

    @Column(name = "email_level")
    private String emailLevel;

    @Column(name = "product_updates")
    private boolean productUpdates;

    @Column(name = "product_level")
    private String productLevel;

    private String theme;
    private String plan;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTimezone() {
        return timezone;
    }

    public int getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public boolean isTwoFactorRequired() {
        return twoFactorRequired;
    }

    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public String getEmailLevel() {
        return emailLevel;
    }

    public boolean isProductUpdates() {
        return productUpdates;
    }

    public String getProductLevel() {
        return productLevel;
    }

    public String getTheme() {
        return theme;
    }

    public String getPlan() {
        return plan;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public void setTwoFactorRequired(boolean twoFactorRequired) {
        this.twoFactorRequired = twoFactorRequired;
    }

    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public void setEmailLevel(String emailLevel) {
        this.emailLevel = emailLevel;
    }

    public void setProductUpdates(boolean productUpdates) {
        this.productUpdates = productUpdates;
    }

    public void setProductLevel(String productLevel) {
        this.productLevel = productLevel;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}