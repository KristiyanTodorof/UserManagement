CREATE TABLE roles (
                       id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                       name         VARCHAR(50)  NOT NULL UNIQUE,
                       description  VARCHAR(255),
                       type         VARCHAR(10)  NOT NULL,           -- SYSTEM | CUSTOM
                       icon         VARCHAR(40)  NOT NULL DEFAULT 'bi-person',
                       color        VARCHAR(20)  NOT NULL DEFAULT 'blue',
                       updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_by   VARCHAR(100)
);

CREATE TABLE permissions (
                             id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                             perm_key         VARCHAR(80)  NOT NULL UNIQUE,   -- e.g. user_management.view
                             name             VARCHAR(80)  NOT NULL,          -- e.g. View users
                             description      VARCHAR(255),
                             category         VARCHAR(50)  NOT NULL,          -- e.g. User Management
                             resource         VARCHAR(50)  NOT NULL,
                             action           VARCHAR(30)  NOT NULL,
                             risk_level       VARCHAR(10)  NOT NULL,          -- LOW | MEDIUM | HIGH
                             last_reviewed_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions (
                                  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  role_id        BIGINT      NOT NULL,
                                  permission_id  BIGINT      NOT NULL,
                                  effect         VARCHAR(10) NOT NULL,             -- GRANTED | DENIED | LIMITED
                                  CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles(id),
                                  CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES permissions(id),
                                  CONSTRAINT uq_role_perm UNIQUE (role_id, permission_id)
);

CREATE TABLE users (
                       id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                       name                VARCHAR(100) NOT NULL,
                       email               VARCHAR(150) NOT NULL UNIQUE,
                       password_hash       VARCHAR(100),
                       status              VARCHAR(12)  NOT NULL,        -- PENDING | ACTIVE | SUSPENDED
                       role_id             BIGINT       NOT NULL,
                       created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       last_active_at      DATETIME,
                       deleted_at          DATETIME,
                       failed_login_count  INT          NOT NULL DEFAULT 0,
                       CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles(id)
);