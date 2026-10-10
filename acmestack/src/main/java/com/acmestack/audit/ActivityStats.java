package com.acmestack.audit;

public record ActivityStats(DayStat events, DayStat failedSignIns, DayStat roleChanges, DayStat exports) {}