CREATE TABLE IF NOT EXISTS login_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50) NOT NULL,
    success TINYINT(1) NOT NULL DEFAULT 0,
    failure_reason VARCHAR(255),
    request_ip VARCHAR(64),
    user_agent VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_login_logs_user_id (user_id),
    KEY idx_login_logs_username_created_at (username, created_at),
    KEY idx_login_logs_success (success),
    CONSTRAINT fk_login_logs_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS token_blacklist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token_hash CHAR(64) NOT NULL,
    user_id BIGINT,
    username VARCHAR(50),
    expires_at DATETIME NOT NULL,
    blacklisted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_token_blacklist_hash (token_hash),
    KEY idx_token_blacklist_user_id (user_id),
    KEY idx_token_blacklist_expires_at (expires_at),
    CONSTRAINT fk_token_blacklist_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE operation_logs
    ADD COLUMN request_body TEXT AFTER description,
    ADD COLUMN response_status INT AFTER request_body,
    ADD COLUMN error_message VARCHAR(500) AFTER response_status,
    ADD COLUMN before_data TEXT AFTER error_message,
    ADD COLUMN after_data TEXT AFTER before_data,
    ADD COLUMN user_agent VARCHAR(255) AFTER after_data;

INSERT IGNORE INTO permissions (code, name, type, module, path, method, sort_order, status) VALUES
('login-log:view', '登录日志查看', 'MENU', 'LOGIN_LOG', '/login-logs', 'GET', 128, 'ACTIVE');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'login-log:view'
WHERE r.code = 'ADMIN';
