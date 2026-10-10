SET @alex   = (SELECT id FROM users WHERE email = 'alex.carter@acmestack.com');
SET @sarah  = (SELECT id FROM users WHERE email = 'sarah.mitchell@acmestack.com');
SET @olivia = (SELECT id FROM users WHERE email = 'olivia.bennett@acmestack.com');
SET @emily  = (SELECT id FROM users WHERE email = 'emily.chen@acmestack.com');
SET @noah   = (SELECT id FROM users WHERE email = 'noah.patel@acmestack.com');

INSERT INTO audit_logs
(actor_id, actor_name, actor_email, event_type, description, target_type, target_id, target_label,
 severity, status, ip_address, location, device, metadata, created_at) VALUES
                                                                           (@sarah,  'Sarah Mitchell', 'sarah.mitchell@acmestack.com', 'PASSWORD_RESET', 'Reset their password',
                                                                            'User', @sarah, 'Sarah Mitchell', 'LOW', 'SUCCESS', '203.0.113.42', 'San Francisco, CA, United States', 'Chrome on macOS',
                                                                            '{"event_id":"evt_seed0001","method":"password_reset","mfa_enabled":true}', NOW() - INTERVAL 2 HOUR),
                                                                           (@alex,   'Alex Carter', 'alex.carter@acmestack.com', 'ROLE_CHANGED', 'Changed role from Member to Manager',
                                                                            'User', @emily, 'Emily Chen', 'MEDIUM', 'SUCCESS', '198.51.100.7', 'New York, NY, United States', 'Safari on macOS',
                                                                            '{"event_id":"evt_seed0002","from":"Member","to":"Manager"}', NOW() - INTERVAL 3 HOUR),
                                                                           (@sarah,  'Sarah Mitchell', 'sarah.mitchell@acmestack.com', 'SIGN_IN', 'Signed in',
                                                                            'User', @sarah, 'Sarah Mitchell', 'LOW', 'SUCCESS', '203.0.113.42', 'San Francisco, CA, United States', 'Chrome on macOS',
                                                                            '{"event_id":"evt_seed0003"}', NOW() - INTERVAL 3 HOUR),
                                                                           (@alex,   'Alex Carter', 'alex.carter@acmestack.com', 'SIGN_IN', 'Signed in',
                                                                            'User', @alex, 'Alex Carter', 'LOW', 'SUCCESS', '198.51.100.7', 'New York, NY, United States', 'Edge on Windows',
                                                                            '{"event_id":"evt_seed0004"}', NOW() - INTERVAL 5 HOUR),
                                                                           (NULL,    'Unknown', 'admin@acmestack.com', 'FAILED_SIGN_IN', 'Sign-in attempt for an unknown account',
                                                                            'User', NULL, 'admin@acmestack.com', 'HIGH', 'FAILED', '203.0.113.99', 'Unknown', 'Firefox on Linux',
                                                                            '{"event_id":"evt_seed0005"}', NOW() - INTERVAL 26 HOUR),
                                                                           (@alex,   'Alex Carter', 'alex.carter@acmestack.com', 'SIGN_IN', 'Signed in',
                                                                            'User', @alex, 'Alex Carter', 'LOW', 'SUCCESS', '198.51.100.7', 'New York, NY, United States', 'Edge on Windows',
                                                                            '{"event_id":"evt_seed0006"}', NOW() - INTERVAL 28 HOUR),
                                                                           (@olivia, 'Olivia Bennett', 'olivia.bennett@acmestack.com', 'FAILED_SIGN_IN', 'Invalid password attempt',
                                                                            'User', @olivia, 'olivia.bennett@acmestack.com', 'HIGH', 'FAILED', '192.0.2.15', 'Austin, TX, United States', 'Chrome on Windows',
                                                                            '{"event_id":"evt_seed0007","attempt":1}', NOW() - INTERVAL 30 HOUR),
                                                                           (@alex,   'Alex Carter', 'alex.carter@acmestack.com', 'USER_INVITED', 'Invited a new user to the workspace',
                                                                            'User', @noah, 'Noah Patel', 'LOW', 'SUCCESS', '198.51.100.7', 'New York, NY, United States', 'Edge on Windows',
                                                                            '{"event_id":"evt_seed0008","role":"Member"}', NOW() - INTERVAL 2 DAY),
                                                                           (@sarah,  'Sarah Mitchell', 'sarah.mitchell@acmestack.com', 'USER_SUSPENDED', 'Suspended a user account',
                                                                            'User', @olivia, 'Olivia Bennett', 'MEDIUM', 'SUCCESS', '203.0.113.42', 'San Francisco, CA, United States', 'Chrome on macOS',
                                                                            '{"event_id":"evt_seed0009"}', NOW() - INTERVAL 2 DAY),
                                                                           (@alex,   'Alex Carter', 'alex.carter@acmestack.com', 'REPORT_EXPORTED', 'Exported user activity report',
                                                                            'Report', NULL, 'Activity Logs', 'LOW', 'SUCCESS', '198.51.100.7', 'New York, NY, United States', 'Edge on Windows',
                                                                            '{"event_id":"evt_seed0010","format":"csv"}', NOW() - INTERVAL 2 DAY);