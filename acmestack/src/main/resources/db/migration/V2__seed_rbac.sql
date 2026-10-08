-- ROLES
INSERT INTO roles (name, description, type, icon, color, updated_by) VALUES
                                                                         ('Admin',         'Full access to all features and settings.',  'SYSTEM', 'bi-shield',        'indigo', 'Alex Carter'),
                                                                         ('Manager',       'Manage team members, content and projects.', 'CUSTOM', 'bi-people',        'green',  'Sarah Mitchell'),
                                                                         ('Member',        'Standard access for team members.',          'SYSTEM', 'bi-person',        'blue',   'Alex Carter'),
                                                                         ('Support',       'Access to customer support tools and user data.', 'CUSTOM', 'bi-headset',  'orange', 'James Wilson'),
                                                                         ('Billing Admin', 'Manage billing, subscriptions and invoices.', 'CUSTOM', 'bi-credit-card',  'red',    'Emily Chen'),
                                                                         ('Viewer',        'Read-only access to most features and data.', 'CUSTOM', 'bi-eye',         'gray',   'Michael Rodriguez');

-- PERMISSIONS
INSERT INTO permissions (perm_key, name, description, category, resource, action, risk_level) VALUES
                                                                                                  ('user_management.view',   'View users',   'View the list of team members and their profiles.', 'User Management', 'Users', 'View',   'LOW'),
                                                                                                  ('user_management.create', 'Create users', 'Invite and create new users.',                      'User Management', 'Users', 'Create', 'MEDIUM'),
                                                                                                  ('user_management.edit',   'Edit users',   'Edit user details and roles.',                      'User Management', 'Users', 'Edit',   'MEDIUM'),
                                                                                                  ('user_management.suspend','Suspend users','Suspend and reactivate users.',                     'User Management', 'Users', 'Suspend','HIGH'),
                                                                                                  ('user_management.delete', 'Delete users', 'Permanently remove users.',                         'User Management', 'Users', 'Delete', 'HIGH'),

                                                                                                  ('role_management.view',   'View roles',   'View roles and their permissions.',                 'Role Management', 'Roles', 'View',   'LOW'),
                                                                                                  ('role_management.create', 'Create roles', 'Create custom roles.',                              'Role Management', 'Roles', 'Create', 'MEDIUM'),
                                                                                                  ('role_management.edit',   'Edit roles',   'Edit roles and assign users.',                      'Role Management', 'Roles', 'Edit',   'HIGH'),
                                                                                                  ('role_management.delete', 'Delete roles', 'Delete custom roles.',                              'Role Management', 'Roles', 'Delete', 'HIGH'),

                                                                                                  ('permission_management.view', 'View permissions', 'View the permission matrix.',                'Permission Management', 'Permissions', 'View', 'LOW'),
                                                                                                  ('permission_management.edit', 'Edit permissions', 'Change which roles hold which permissions.','Permission Management', 'Permissions', 'Edit', 'HIGH'),

                                                                                                  ('billing.view',   'View billing',   'View plan, invoices and subscription.',  'Billing', 'Billing', 'View',   'LOW'),
                                                                                                  ('billing.manage', 'Manage billing', 'Change plan and payment details.',       'Billing', 'Billing', 'Manage', 'HIGH'),

                                                                                                  ('audit.view',   'View activity logs',   'View the activity log.',             'Audit Logs', 'Activity', 'View',   'LOW'),
                                                                                                  ('audit.export', 'Export activity logs', 'Export the activity log to CSV.',    'Audit Logs', 'Activity', 'Export', 'MEDIUM'),

                                                                                                  ('settings.view', 'View settings',   'View workspace settings.',               'Security', 'Settings', 'View', 'LOW'),
                                                                                                  ('settings.edit', 'Modify settings', 'Change workspace and security settings.','Security', 'Settings', 'Edit', 'HIGH'),

                                                                                                  ('reports.view', 'View reports', 'Access the dashboard and reports.',          'Reports', 'Reports', 'View', 'LOW');

-- Admin: everything GRANTED
INSERT INTO role_permissions (role_id, permission_id, effect)
SELECT r.id, p.id, 'GRANTED' FROM roles r CROSS JOIN permissions p WHERE r.name = 'Admin';

-- Manager: everything GRANTED except deleting users, deleting roles, editing permissions, billing manage, editing settings
INSERT INTO role_permissions (role_id, permission_id, effect)
SELECT r.id, p.id,
       CASE WHEN p.perm_key IN ('user_management.delete','role_management.delete','role_management.edit',
                                'permission_management.edit','billing.manage','settings.edit')
                THEN 'DENIED' ELSE 'GRANTED' END
FROM roles r CROSS JOIN permissions p WHERE r.name = 'Manager';

-- Member: view-type permissions only; the rest DENIED; billing view LIMITED
INSERT INTO role_permissions (role_id, permission_id, effect)
SELECT r.id, p.id,
       CASE WHEN p.perm_key IN ('user_management.view','audit.view','reports.view') THEN 'GRANTED'
            WHEN p.perm_key = 'billing.view' THEN 'LIMITED'
            ELSE 'DENIED' END
FROM roles r CROSS JOIN permissions p WHERE r.name = 'Member';

-- Support: user view, audit view, reports view, security view
INSERT INTO role_permissions (role_id, permission_id, effect)
SELECT r.id, p.id,
       CASE WHEN p.perm_key IN ('user_management.view','audit.view','reports.view','settings.view') THEN 'GRANTED'
            ELSE 'DENIED' END
FROM roles r CROSS JOIN permissions p WHERE r.name = 'Support';

-- Billing Admin: billing + reports + audit view
INSERT INTO role_permissions (role_id, permission_id, effect)
SELECT r.id, p.id,
       CASE WHEN p.perm_key IN ('billing.view','billing.manage','reports.view','audit.view') THEN 'GRANTED'
            ELSE 'DENIED' END
FROM roles r CROSS JOIN permissions p WHERE r.name = 'Billing Admin';

-- Viewer: audit view only; user view and reports LIMITED
INSERT INTO role_permissions (role_id, permission_id, effect)
SELECT r.id, p.id,
       CASE WHEN p.perm_key = 'audit.view' THEN 'GRANTED'
            WHEN p.perm_key IN ('user_management.view','reports.view') THEN 'LIMITED'
            ELSE 'DENIED' END
FROM roles r CROSS JOIN permissions p WHERE r.name = 'Viewer';

-- USERS (password_hash is filled in Phase 2, when we add login)
INSERT INTO users (name, email, status, role_id, created_at, last_active_at)
SELECT 'Alex Carter',      'alex.carter@acmestack.com',      'ACTIVE',    r.id, NOW() - INTERVAL 400 DAY, NOW() - INTERVAL 5 MINUTE  FROM roles r WHERE r.name = 'Admin'
UNION ALL SELECT 'Sarah Mitchell',   'sarah.mitchell@acmestack.com',   'ACTIVE',    r.id, NOW() - INTERVAL 300 DAY, NOW() - INTERVAL 2 HOUR    FROM roles r WHERE r.name = 'Admin'
          UNION ALL SELECT 'James Wilson',     'james.wilson@acmestack.com',     'ACTIVE',    r.id, NOW() - INTERVAL 200 DAY, NOW() - INTERVAL 5 HOUR    FROM roles r WHERE r.name = 'Member'
                    UNION ALL SELECT 'Emily Chen',       'emily.chen@acmestack.com',       'PENDING',   r.id, NOW() - INTERVAL 3 DAY,   NOW() - INTERVAL 1 DAY     FROM roles r WHERE r.name = 'Manager'
                              UNION ALL SELECT 'Michael Rodriguez','michael.rodriguez@acmestack.com','ACTIVE',    r.id, NOW() - INTERVAL 150 DAY, NOW() - INTERVAL 3 HOUR    FROM roles r WHERE r.name = 'Member'
                                        UNION ALL SELECT 'Olivia Bennett',   'olivia.bennett@acmestack.com',   'SUSPENDED', r.id, NOW() - INTERVAL 120 DAY, NOW() - INTERVAL 2 DAY     FROM roles r WHERE r.name = 'Admin'
                                                  UNION ALL SELECT 'Daniel Kim',       'daniel.kim@acmestack.com',       'ACTIVE',    r.id, NOW() - INTERVAL 90 DAY,  NOW() - INTERVAL 1 DAY     FROM roles r WHERE r.name = 'Member'
                                                            UNION ALL SELECT 'Ava Thompson',     'ava.thompson@acmestack.com',     'ACTIVE',    r.id, NOW() - INTERVAL 60 DAY,  NOW() - INTERVAL 4 HOUR    FROM roles r WHERE r.name = 'Manager'
                                                                      UNION ALL SELECT 'Noah Patel',       'noah.patel@acmestack.com',       'PENDING',   r.id, NOW() - INTERVAL 2 DAY,   NOW() - INTERVAL 2 DAY     FROM roles r WHERE r.name = 'Member';