package com.acmestack.user;

public record PermissionGroup(String category, long granted, long total) {}