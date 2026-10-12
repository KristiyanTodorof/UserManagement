ALTER TABLE roles ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE roles SET created_at = NOW() - INTERVAL 400 DAY;

UPDATE roles SET updated_at = NOW() - INTERVAL 2 DAY  WHERE name = 'Admin';
UPDATE roles SET updated_at = NOW() - INTERVAL 4 DAY  WHERE name = 'Manager';
UPDATE roles SET updated_at = NOW() - INTERVAL 7 DAY  WHERE name IN ('Member', 'Support');
UPDATE roles SET updated_at = NOW() - INTERVAL 14 DAY WHERE name IN ('Billing Admin', 'Viewer');