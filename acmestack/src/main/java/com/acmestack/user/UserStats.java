package com.acmestack.user;

public record UserStats(long total, long newThisWeek, long newPrevWeek, long active, long admins) {

    public int activePct() { return total == 0 ? 0 : (int) Math.round(active * 100.0 / total); }
    public int adminPct()  { return total == 0 ? 0 : (int) Math.round(admins * 100.0 / total); }

    public Integer totalTrend() {
        long before = total - newThisWeek;
        return before <= 0 ? null : (int) Math.round(newThisWeek * 100.0 / before);
    }

    public Integer newTrend() {
        return newPrevWeek == 0 ? null : (int) Math.round((newThisWeek - newPrevWeek) * 100.0 / newPrevWeek);
    }
}