package com.acmestack.permission;

public record PermissionStats(long total, long categories, long totalRules,
                              long granted, long denied, long limited, long reviewsDue) {

    public int grantedPct() { return pct(granted, totalRules); }
    public int deniedPct()  { return pct(denied, totalRules); }
    public int reviewPct()  { return pct(reviewsDue, total); }

    private static int pct(long part, long whole) {
        return whole == 0 ? 0 : (int) Math.round(part * 100.0 / whole);
    }
}