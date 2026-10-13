package com.acmestack.dashboard;

public record DashboardStats(long total, long newThisWeek, long activeSessions,
                             long pending, long alerts, long alertsPrev) {

    /** Growth of the user base over the last 7 days, or null if it can't be computed. */
    public Integer totalTrend() {
        long before = total - newThisWeek;
        return newThisWeek == 0 || before <= 0 ? null : (int) Math.round(newThisWeek * 100.0 / before);
    }

    /** Alerts this week vs last week, or null if last week had none. */
    public Integer alertTrend() {
        return alertsPrev == 0 ? null : (int) Math.round((alerts - alertsPrev) * 100.0 / alertsPrev);
    }
}