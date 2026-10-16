package com.acmestack.config;

/** Picks the first page a user is allowed to see. */
public final class LandingPages {

    private static final String[][] ORDER = {
            {"reports.view", "/dashboard"},
            {"user_management.view", "/users"},
            {"role_management.view", "/roles"},
            {"permission_management.view", "/permissions"},
            {"audit.view", "/activity"},
            {"settings.view", "/settings"},
            {"chat.use", "/chat"}
    };

    private LandingPages() {}

    public static String forUser(AppUserDetails user) {
        for (String[] entry : ORDER) {
            if (user.can(entry[0])) return entry[1];
        }
        return "/no-access";
    }
}