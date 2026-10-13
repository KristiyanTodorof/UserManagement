package com.acmestack.settings;

import java.time.DateTimeException;
import java.time.ZoneId;

public record WorkspaceInfo(String name, String timezone, int sessionTimeoutMinutes,
                            boolean twoFactorRequired, boolean emailNotifications, String emailLevel,
                            boolean productUpdates, String productLevel, String theme, String plan) {

    public ZoneId zoneId() {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            return ZoneId.systemDefault();
        }
    }
}