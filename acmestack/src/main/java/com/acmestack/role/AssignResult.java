package com.acmestack.role;

import java.util.List;

public record AssignResult(int changed, List<String> problems) {}