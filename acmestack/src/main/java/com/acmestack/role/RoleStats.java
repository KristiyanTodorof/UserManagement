package com.acmestack.role;

public record RoleStats(long total, long custom, long createdThisMonth,
                        long customCreatedThisMonth, long assigned, long reviewsDue) {

    public Integer totalTrend() {
        long before = total - createdThisMonth;
        return createdThisMonth == 0 || before <= 0 ? null : (int) Math.round(createdThisMonth * 100.0 / before);
    }
}