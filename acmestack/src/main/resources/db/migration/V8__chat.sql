CREATE TABLE conversations (
                               id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
                               type                     VARCHAR(10)  NOT NULL,            -- DIRECT | GROUP
                               name                     VARCHAR(60),                      -- groups only
                               icon                     VARCHAR(40)  NOT NULL DEFAULT 'bi-people',
                               color                    VARCHAR(20)  NOT NULL DEFAULT 'indigo',
                               direct_key               VARCHAR(30)  UNIQUE,              -- "12:57": one direct chat per pair
                               created_by               BIGINT,
                               created_at               DATETIME     NOT NULL,
                               last_message_at          DATETIME,
                               last_message_preview     VARCHAR(120),
                               last_message_sender_id   BIGINT,
                               last_message_sender_name VARCHAR(100)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE conversation_members (
                                      id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      conversation_id       BIGINT   NOT NULL,
                                      user_id               BIGINT   NOT NULL,
                                      last_read_message_id  BIGINT   NOT NULL DEFAULT 0,
                                      muted                 BOOLEAN  NOT NULL DEFAULT FALSE,
                                      joined_at             DATETIME NOT NULL,
                                      CONSTRAINT fk_cm_conv FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
                                      CONSTRAINT fk_cm_user FOREIGN KEY (user_id) REFERENCES users(id),
                                      CONSTRAINT uq_cm UNIQUE (conversation_id, user_id),
                                      INDEX idx_cm_user (user_id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE messages (
                          id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                          conversation_id  BIGINT        NOT NULL,
                          sender_id        BIGINT        NOT NULL,
                          body             VARCHAR(2000) NOT NULL DEFAULT '',
                          attachment_name  VARCHAR(150),
                          attachment_path  VARCHAR(40),                              -- random file name on disk
                          attachment_size  BIGINT,
                          created_at       DATETIME      NOT NULL,
                          CONSTRAINT fk_msg_conv   FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
                          CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES users(id),
                          INDEX idx_msg_conv (conversation_id, id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- new permissions
INSERT INTO permissions (perm_key, name, description, category, resource, action, risk_level) VALUES
                                                                                                  ('chat.use',    'Use team chat',      'Read and send messages in conversations you belong to.', 'Chat', 'Chat', 'Use',    'LOW'),
                                                                                                  ('chat.groups', 'Create group chats', 'Create group conversations.',                            'Chat', 'Chat', 'Create', 'MEDIUM');

INSERT INTO role_permissions (role_id, permission_id, effect)
SELECT r.id, p.id,
       CASE WHEN p.perm_key = 'chat.use'    AND r.name IN ('Admin', 'Manager', 'Member', 'Support', 'Billing Admin') THEN 'GRANTED'
            WHEN p.perm_key = 'chat.groups' AND r.name IN ('Admin', 'Manager') THEN 'GRANTED'
            ELSE 'DENIED' END
FROM roles r CROSS JOIN permissions p
WHERE p.perm_key IN ('chat.use', 'chat.groups');