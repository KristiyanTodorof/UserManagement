package com.acmestack.audit;

public record DayStat(long today, long yesterday, boolean lowerIsBetter) {

    public long diff() { return today - yesterday; }

    public Integer pct() {
        return yesterday == 0 ? null : (int) Math.round((today - yesterday) * 100.0 / yesterday);
    }

    public boolean good() { return lowerIsBetter ? diff() <= 0 : diff() >= 0; }
}