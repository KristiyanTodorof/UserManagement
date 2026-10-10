CREATE TABLE audit_logs (
                            id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                            actor_id      BIGINT,                          -- no FK on purpose: logs must outlive users
                            actor_name    VARCHAR(100) NOT NULL,
                            actor_email   VARCHAR(150),
                            event_type    VARCHAR(30)  NOT NULL,
                            description   VARCHAR(255) NOT NULL,
                            target_type   VARCHAR(30),
                            target_id     BIGINT,
                            target_label  VARCHAR(150),
                            severity      VARCHAR(10)  NOT NULL,
                            status        VARCHAR(10)  NOT NULL,
                            ip_address    VARCHAR(45),
                            location      VARCHAR(100),
                            device        VARCHAR(100),
                            metadata      VARCHAR(2000),
                            created_at    DATETIME     NOT NULL,
                            INDEX idx_audit_created (created_at),
                            INDEX idx_audit_type (event_type),
                            INDEX idx_audit_actor (actor_id),
                            INDEX idx_audit_target (target_type, target_id)
);