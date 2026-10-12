package com.acmestack.permission;

public record MatrixCell(Long roleId, String roleName, Effect effect, boolean locked) {}