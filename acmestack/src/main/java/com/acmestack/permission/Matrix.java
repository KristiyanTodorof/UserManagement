package com.acmestack.permission;

import com.acmestack.role.Role;
import java.util.List;

public record Matrix(List<Role> roles, List<MatrixGroup> groups) {}