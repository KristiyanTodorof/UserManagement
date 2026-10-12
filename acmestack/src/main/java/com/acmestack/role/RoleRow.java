package com.acmestack.role;

import com.acmestack.user.User;
import java.util.List;

public record RoleRow(Role role, long users, long granted, long totalPermissions,
                      List<User> preview, long extra) {}