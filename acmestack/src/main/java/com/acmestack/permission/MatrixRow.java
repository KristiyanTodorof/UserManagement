package com.acmestack.permission;

import java.util.List;

public record MatrixRow(Permission permission, boolean reviewDue, List<MatrixCell> cells) {}