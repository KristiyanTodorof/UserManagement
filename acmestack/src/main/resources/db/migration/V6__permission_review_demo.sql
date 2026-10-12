UPDATE permissions SET last_reviewed_at = NOW() - INTERVAL 120 DAY
WHERE perm_key IN ('user_management.delete', 'role_management.delete',
    'permission_management.edit', 'billing.manage');