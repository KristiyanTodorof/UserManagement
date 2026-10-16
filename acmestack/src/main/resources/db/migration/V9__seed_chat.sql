SET @alex    = (SELECT id FROM users WHERE email = 'alex.carter@acmestack.com');
SET @sarah   = (SELECT id FROM users WHERE email = 'sarah.mitchell@acmestack.com');
SET @james   = (SELECT id FROM users WHERE email = 'james.wilson@acmestack.com');
SET @emily   = (SELECT id FROM users WHERE email = 'emily.chen@acmestack.com');
SET @olivia  = (SELECT id FROM users WHERE email = 'olivia.bennett@acmestack.com');
SET @daniel  = (SELECT id FROM users WHERE email = 'daniel.kim@acmestack.com');
SET @michael = (SELECT id FROM users WHERE email = 'michael.rodriguez@acmestack.com');
SET @ava     = (SELECT id FROM users WHERE email = 'ava.thompson@acmestack.com');
SET @noah    = (SELECT id FROM users WHERE email = 'noah.patel@acmestack.com');

-- 1. Sarah (direct)
INSERT INTO conversations (type, direct_key, created_by, created_at)
VALUES ('DIRECT', CONCAT(LEAST(@alex, @sarah), ':', GREATEST(@alex, @sarah)), @alex, NOW() - INTERVAL 3 DAY);
SET @c1 = LAST_INSERT_ID();
INSERT INTO conversation_members (conversation_id, user_id, joined_at)
VALUES (@c1, @alex, NOW() - INTERVAL 3 DAY), (@c1, @sarah, NOW() - INTERVAL 3 DAY);
INSERT INTO messages (conversation_id, sender_id, body, created_at) VALUES
                                                                        (@c1, @sarah, 'Hey Alex! How''s the progress on the new dashboard coming along?', NOW() - INTERVAL 60 MINUTE),
                                                                        (@c1, @alex,  'It''s going really well! The main components are almost finished. I''ll share a preview with you in a few minutes.', NOW() - INTERVAL 58 MINUTE),
                                                                        (@c1, @sarah, 'That''s great to hear! Could you also take a look at the updated designs for the analytics section?', NOW() - INTERVAL 55 MINUTE),
                                                                        (@c1, @alex,  'Just reviewed them. The new designs look fantastic! I have a few minor suggestions, but overall it''s looking really solid.', NOW() - INTERVAL 40 MINUTE),
                                                                        (@c1, @sarah, 'Perfect! Let''s discuss them in our sync tomorrow. Thanks for the quick review!', NOW() - INTERVAL 5 MINUTE),
                                                                        (@c1, @sarah, 'Sounds good! I''ll also send the roadmap before the sync.', NOW() - INTERVAL 2 MINUTE);

-- 2. Product Team (group)
INSERT INTO conversations (type, name, icon, color, created_by, created_at)
VALUES ('GROUP', 'Product Team', 'bi-people', 'green', @alex, NOW() - INTERVAL 10 DAY);
SET @c2 = LAST_INSERT_ID();
INSERT INTO conversation_members (conversation_id, user_id, joined_at) VALUES
                                                                           (@c2, @alex, NOW() - INTERVAL 10 DAY), (@c2, @daniel, NOW() - INTERVAL 10 DAY), (@c2, @sarah, NOW() - INTERVAL 10 DAY),
                                                                           (@c2, @james, NOW() - INTERVAL 10 DAY), (@c2, @emily, NOW() - INTERVAL 10 DAY);
INSERT INTO messages (conversation_id, sender_id, body, created_at) VALUES
                                                                        (@c2, @alex,   'Kicking off the sprint review notes. Please add your updates.', NOW() - INTERVAL 4 HOUR),
                                                                        (@c2, @daniel, 'Backend changes are merged.', NOW() - INTERVAL 3 HOUR),
                                                                        (@c2, @sarah,  'Design handoff is ready for review.', NOW() - INTERVAL 2 HOUR),
                                                                        (@c2, @james,  'API docs updated, please check the examples.', NOW() - INTERVAL 90 MINUTE),
                                                                        (@c2, @emily,  'I''ll test the onboarding flow this afternoon.', NOW() - INTERVAL 40 MINUTE),
                                                                        (@c2, @daniel, 'Great progress this week!', NOW() - INTERVAL 10 MINUTE);

-- 3. James (direct)
INSERT INTO conversations (type, direct_key, created_by, created_at)
VALUES ('DIRECT', CONCAT(LEAST(@alex, @james), ':', GREATEST(@alex, @james)), @alex, NOW() - INTERVAL 5 DAY);
SET @c3 = LAST_INSERT_ID();
INSERT INTO conversation_members (conversation_id, user_id, joined_at)
VALUES (@c3, @alex, NOW() - INTERVAL 5 DAY), (@c3, @james, NOW() - INTERVAL 5 DAY);
INSERT INTO messages (conversation_id, sender_id, body, created_at) VALUES
                                                                        (@c3, @alex,  'Morning James, did the staging deploy go through?', NOW() - INTERVAL 6 HOUR),
                                                                        (@c3, @james, 'Yes, all green.', NOW() - INTERVAL 5 HOUR),
                                                                        (@c3, @james, 'Can we sync on the API changes?', NOW() - INTERVAL 1 HOUR);

-- 4. Emily (direct)
INSERT INTO conversations (type, direct_key, created_by, created_at)
VALUES ('DIRECT', CONCAT(LEAST(@alex, @emily), ':', GREATEST(@alex, @emily)), @alex, NOW() - INTERVAL 4 DAY);
SET @c4 = LAST_INSERT_ID();
INSERT INTO conversation_members (conversation_id, user_id, joined_at)
VALUES (@c4, @alex, NOW() - INTERVAL 4 DAY), (@c4, @emily, NOW() - INTERVAL 4 DAY);
INSERT INTO messages (conversation_id, sender_id, body, created_at) VALUES
                                                                        (@c4, @alex,  'Could you update the onboarding documentation?', NOW() - INTERVAL 1 DAY),
                                                                        (@c4, @emily, 'I''ve updated the documentation.', NOW() - INTERVAL 2 HOUR);

-- 5. Support (group)
INSERT INTO conversations (type, name, icon, color, created_by, created_at)
VALUES ('GROUP', 'Support', 'bi-headset', 'orange', @alex, NOW() - INTERVAL 20 DAY);
SET @c5 = LAST_INSERT_ID();
INSERT INTO conversation_members (conversation_id, user_id, joined_at) VALUES
                                                                           (@c5, @alex, NOW() - INTERVAL 20 DAY), (@c5, @olivia, NOW() - INTERVAL 20 DAY), (@c5, @noah, NOW() - INTERVAL 20 DAY);
INSERT INTO messages (conversation_id, sender_id, body, created_at) VALUES
                                                                        (@c5, @alex,   'Please triage the open tickets today.', NOW() - INTERVAL 5 HOUR),
                                                                        (@c5, @noah,   'On it.', NOW() - INTERVAL 4 HOUR),
                                                                        (@c5, @olivia, 'Ticket 4821 looks urgent.', NOW() - INTERVAL 210 MINUTE),
                                                                        (@c5, @olivia, 'New ticket assigned to you', NOW() - INTERVAL 3 HOUR);

-- 6. Design Team (group)
INSERT INTO conversations (type, name, icon, color, created_by, created_at)
VALUES ('GROUP', 'Design Team', 'bi-palette', 'indigo', @alex, NOW() - INTERVAL 15 DAY);
SET @c6 = LAST_INSERT_ID();
INSERT INTO conversation_members (conversation_id, user_id, joined_at) VALUES
                                                                           (@c6, @alex, NOW() - INTERVAL 15 DAY), (@c6, @michael, NOW() - INTERVAL 15 DAY), (@c6, @sarah, NOW() - INTERVAL 15 DAY);
INSERT INTO messages (conversation_id, sender_id, body, created_at) VALUES
                                                                        (@c6, @alex,    'Any updates on the new component library?', NOW() - INTERVAL 6 HOUR),
                                                                        (@c6, @michael, 'Here are the latest mocks', NOW() - INTERVAL 5 HOUR);

-- 7. Marketing (group)
INSERT INTO conversations (type, name, icon, color, created_by, created_at)
VALUES ('GROUP', 'Marketing', 'bi-megaphone', 'red', @alex, NOW() - INTERVAL 12 DAY);
SET @c7 = LAST_INSERT_ID();
INSERT INTO conversation_members (conversation_id, user_id, joined_at) VALUES
                                                                           (@c7, @alex, NOW() - INTERVAL 12 DAY), (@c7, @ava, NOW() - INTERVAL 12 DAY), (@c7, @emily, NOW() - INTERVAL 12 DAY);
INSERT INTO messages (conversation_id, sender_id, body, created_at) VALUES
                                                                        (@c7, @alex, 'Launch date is confirmed for next week.', NOW() - INTERVAL 1 DAY),
                                                                        (@c7, @ava,  'Campaign assets are ready', NOW() - INTERVAL 23 HOUR);

-- 8. Noah (direct)
INSERT INTO conversations (type, direct_key, created_by, created_at)
VALUES ('DIRECT', CONCAT(LEAST(@alex, @noah), ':', GREATEST(@alex, @noah)), @alex, NOW() - INTERVAL 2 DAY);
SET @c8 = LAST_INSERT_ID();
INSERT INTO conversation_members (conversation_id, user_id, joined_at)
VALUES (@c8, @alex, NOW() - INTERVAL 2 DAY), (@c8, @noah, NOW() - INTERVAL 2 DAY);
INSERT INTO messages (conversation_id, sender_id, body, created_at) VALUES
                                                                        (@c8, @alex, 'I''ve approved your access request.', NOW() - INTERVAL 26 HOUR),
                                                                        (@c8, @noah, 'Thanks for the help!', NOW() - INTERVAL 25 HOUR);

-- 9. Platform Team (group)
INSERT INTO conversations (type, name, icon, color, created_by, created_at)
VALUES ('GROUP', 'Platform Team', 'bi-layers', 'blue', @alex, NOW() - INTERVAL 30 DAY);
SET @c9 = LAST_INSERT_ID();
INSERT INTO conversation_members (conversation_id, user_id, joined_at) VALUES
                                                                           (@c9, @alex, NOW() - INTERVAL 30 DAY), (@c9, @daniel, NOW() - INTERVAL 30 DAY), (@c9, @michael, NOW() - INTERVAL 30 DAY);
INSERT INTO messages (conversation_id, sender_id, body, created_at) VALUES
                                                                        (@c9, @alex,   'Reminder: the release window is Thursday.', NOW() - INTERVAL 2 DAY),
                                                                        (@c9, @daniel, 'Deployment completed', NOW() - INTERVAL 47 HOUR);

-- everybody has read everything ...
UPDATE conversation_members cm
SET cm.last_read_message_id = COALESCE((SELECT MAX(m.id) FROM messages m WHERE m.conversation_id = cm.conversation_id), 0);

-- ... except Alex's unread messages (the newest N in each of these conversations)
UPDATE conversation_members SET last_read_message_id =
                                    COALESCE((SELECT id FROM messages WHERE conversation_id = @c1 ORDER BY id DESC LIMIT 1 OFFSET 2), 0)
WHERE conversation_id = @c1 AND user_id = @alex;
UPDATE conversation_members SET last_read_message_id =
                                    COALESCE((SELECT id FROM messages WHERE conversation_id = @c2 ORDER BY id DESC LIMIT 1 OFFSET 5), 0)
WHERE conversation_id = @c2 AND user_id = @alex;
UPDATE conversation_members SET last_read_message_id =
                                    COALESCE((SELECT id FROM messages WHERE conversation_id = @c3 ORDER BY id DESC LIMIT 1 OFFSET 1), 0)
WHERE conversation_id = @c3 AND user_id = @alex;
UPDATE conversation_members SET last_read_message_id =
                                    COALESCE((SELECT id FROM messages WHERE conversation_id = @c5 ORDER BY id DESC LIMIT 1 OFFSET 3), 0)
WHERE conversation_id = @c5 AND user_id = @alex;

-- list previews come from the newest message
UPDATE conversations c
    JOIN (SELECT conversation_id, MAX(id) AS mid FROM messages GROUP BY conversation_id) x ON x.conversation_id = c.id
    JOIN messages m ON m.id = x.mid
    JOIN users u ON u.id = m.sender_id
    SET c.last_message_at = m.created_at,
        c.last_message_preview = LEFT(m.body, 120),
        c.last_message_sender_id = u.id,
        c.last_message_sender_name = u.name;