package com.acmestack.permission;

import java.util.List;

public record MatrixGroup(String category, List<MatrixRow> rows) {}