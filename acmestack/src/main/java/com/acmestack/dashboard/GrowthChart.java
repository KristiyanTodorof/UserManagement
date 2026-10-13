package com.acmestack.dashboard;

import java.util.List;

public record GrowthChart(List<String> labels, List<Long> values) {}