package com.acmestack.dashboard;

import java.util.List;

public record SystemHealth(boolean operational, List<ServiceStatus> services, String uptime) {}