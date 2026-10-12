package com.acmestack.permission;

import com.acmestack.role.Role;

public record RoleAccess(Role role, Effect effect, boolean locked) {}