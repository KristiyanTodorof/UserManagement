package com.acmestack.role;

import com.acmestack.permission.RolePermission;
import java.util.List;

public record RolePermissionGroup(String category, List<RolePermission> items, long granted) {}