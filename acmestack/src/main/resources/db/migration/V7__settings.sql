CREATE TABLE workspace (
                           id                       BIGINT       PRIMARY KEY,          -- always 1
                           name                     VARCHAR(100) NOT NULL,
                           timezone                 VARCHAR(60)  NOT NULL,
                           session_timeout_minutes  INT          NOT NULL,
                           two_factor_required      BOOLEAN      NOT NULL,
                           email_notifications      BOOLEAN      NOT NULL,
                           email_level              VARCHAR(20)  NOT NULL,
                           product_updates          BOOLEAN      NOT NULL,
                           product_level            VARCHAR(20)  NOT NULL,
                           theme                    VARCHAR(10)  NOT NULL,
                           plan                     VARCHAR(20)  NOT NULL,
                           updated_at               DATETIME     NOT NULL,
                           updated_by               VARCHAR(100)
);

INSERT INTO workspace
(id, name, timezone, session_timeout_minutes, two_factor_required, email_notifications,
 email_level, product_updates, product_level, theme, plan, updated_at, updated_by)
VALUES
    (1, 'AcmeStack', 'America/Los_Angeles', 30, TRUE, TRUE, 'ALL', FALSE, 'MAJOR', 'LIGHT', 'Pro', NOW(), 'System');

-- Mocked integrations: Phase 10 turns Slack into a real one
CREATE TABLE integrations (
                              id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                              name        VARCHAR(50)  NOT NULL,
                              description VARCHAR(120) NOT NULL,
                              icon        VARCHAR(40)  NOT NULL,
                              status      VARCHAR(12)  NOT NULL                              -- CONNECTED | AVAILABLE
);

INSERT INTO integrations (name, description, icon, status) VALUES
                                                               ('Slack',            'Team communication',   'bi-slack',  'CONNECTED'),
                                                               ('Google Workspace', 'Sync users and groups', 'bi-google', 'AVAILABLE'),
                                                               ('GitHub',           'Link repositories',    'bi-github', 'AVAILABLE');