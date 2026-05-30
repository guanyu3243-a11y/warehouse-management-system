CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_roles_code (code),
    KEY idx_roles_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    module VARCHAR(50) NOT NULL,
    path VARCHAR(255),
    method VARCHAR(10),
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_permissions_code (code),
    KEY idx_permissions_module (module),
    KEY idx_permissions_type (type),
    KEY idx_permissions_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permissions_role_permission (role_id, permission_id),
    KEY idx_role_permissions_permission_id (permission_id),
    CONSTRAINT fk_role_permissions_role_id FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission_id FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_roles_user_role (user_id, role_id),
    KEY idx_user_roles_role_id (role_id),
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_warehouse_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_warehouse_permissions_user_warehouse (user_id, warehouse_id),
    KEY idx_user_warehouse_permissions_warehouse_id (warehouse_id),
    CONSTRAINT fk_user_warehouse_permissions_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_warehouse_permissions_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO roles (code, name, description, status) VALUES
('ADMIN', '系统管理员', '拥有系统全部管理权限', 'ACTIVE'),
('MANAGER', '仓库主管', '管理基础资料、出入库、库存和报表', 'ACTIVE'),
('STAFF', '仓库操作员', '处理日常出入库和库存查询', 'ACTIVE'),
('VIEWER', '只读查看员', '仅查看业务数据和报表', 'ACTIVE');

INSERT IGNORE INTO permissions (code, name, type, module, path, method, sort_order, status) VALUES
('dashboard:view', 'Dashboard 查看', 'MENU', 'DASHBOARD', '/dashboard', 'GET', 10, 'ACTIVE'),
('category:view', '商品分类查看', 'MENU', 'CATEGORY', '/categories', 'GET', 20, 'ACTIVE'),
('category:create', '商品分类新增', 'BUTTON', 'CATEGORY', '/api/categories', 'POST', 21, 'ACTIVE'),
('category:update', '商品分类编辑', 'BUTTON', 'CATEGORY', '/api/categories/{id}', 'PUT', 22, 'ACTIVE'),
('category:delete', '商品分类删除', 'BUTTON', 'CATEGORY', '/api/categories/{id}', 'DELETE', 23, 'ACTIVE'),
('product:view', '服装商品查看', 'MENU', 'PRODUCT', '/products', 'GET', 30, 'ACTIVE'),
('product:create', '服装商品新增', 'BUTTON', 'PRODUCT', '/api/products', 'POST', 31, 'ACTIVE'),
('product:update', '服装商品编辑', 'BUTTON', 'PRODUCT', '/api/products/{id}', 'PUT', 32, 'ACTIVE'),
('product:delete', '服装商品删除', 'BUTTON', 'PRODUCT', '/api/products/{id}', 'DELETE', 33, 'ACTIVE'),
('warehouse:view', '仓库查看', 'MENU', 'WAREHOUSE', '/warehouses', 'GET', 40, 'ACTIVE'),
('warehouse:create', '仓库新增', 'BUTTON', 'WAREHOUSE', '/api/warehouses', 'POST', 41, 'ACTIVE'),
('warehouse:update', '仓库编辑', 'BUTTON', 'WAREHOUSE', '/api/warehouses/{id}', 'PUT', 42, 'ACTIVE'),
('warehouse:delete', '仓库删除', 'BUTTON', 'WAREHOUSE', '/api/warehouses/{id}', 'DELETE', 43, 'ACTIVE'),
('supplier:view', '供应商查看', 'MENU', 'SUPPLIER', '/suppliers', 'GET', 50, 'ACTIVE'),
('supplier:create', '供应商新增', 'BUTTON', 'SUPPLIER', '/api/suppliers', 'POST', 51, 'ACTIVE'),
('supplier:update', '供应商编辑', 'BUTTON', 'SUPPLIER', '/api/suppliers/{id}', 'PUT', 52, 'ACTIVE'),
('supplier:delete', '供应商删除', 'BUTTON', 'SUPPLIER', '/api/suppliers/{id}', 'DELETE', 53, 'ACTIVE'),
('user:view', '用户查看', 'MENU', 'USER', '/users', 'GET', 60, 'ACTIVE'),
('user:create', '用户新增', 'BUTTON', 'USER', '/api/users', 'POST', 61, 'ACTIVE'),
('user:update', '用户编辑', 'BUTTON', 'USER', '/api/users/{id}', 'PUT', 62, 'ACTIVE'),
('user:delete', '用户删除', 'BUTTON', 'USER', '/api/users/{id}', 'DELETE', 63, 'ACTIVE'),
('role:view', '角色查看', 'MENU', 'ROLE', '/roles', 'GET', 70, 'ACTIVE'),
('role:create', '角色新增', 'BUTTON', 'ROLE', '/api/roles', 'POST', 71, 'ACTIVE'),
('role:update', '角色编辑', 'BUTTON', 'ROLE', '/api/roles/{id}', 'PUT', 72, 'ACTIVE'),
('role:delete', '角色删除', 'BUTTON', 'ROLE', '/api/roles/{id}', 'DELETE', 73, 'ACTIVE'),
('permission:view', '权限查看', 'MENU', 'PERMISSION', '/permissions', 'GET', 80, 'ACTIVE'),
('permission:update', '权限维护', 'BUTTON', 'PERMISSION', '/api/permissions/{id}', 'PUT', 81, 'ACTIVE'),
('stock:view', '库存查看', 'MENU', 'STOCK', '/stock', 'GET', 90, 'ACTIVE'),
('stock:low:view', '低库存预警查看', 'MENU', 'STOCK', '/low-stock', 'GET', 91, 'ACTIVE'),
('stock-in:view', '入库查看', 'MENU', 'STOCK_IN', '/stock-in', 'GET', 100, 'ACTIVE'),
('stock-in:create', '入库新增', 'BUTTON', 'STOCK_IN', '/api/stock-in', 'POST', 101, 'ACTIVE'),
('stock-in:update', '入库编辑', 'BUTTON', 'STOCK_IN', '/api/stock-in/{id}', 'PUT', 102, 'ACTIVE'),
('stock-in:confirm', '入库确认', 'BUTTON', 'STOCK_IN', '/api/stock-in/{id}/confirm', 'POST', 103, 'ACTIVE'),
('stock-in:cancel', '入库取消', 'BUTTON', 'STOCK_IN', '/api/stock-in/{id}/cancel', 'POST', 104, 'ACTIVE'),
('stock-in:delete', '入库删除', 'BUTTON', 'STOCK_IN', '/api/stock-in/{id}', 'DELETE', 105, 'ACTIVE'),
('stock-out:view', '出库查看', 'MENU', 'STOCK_OUT', '/stock-out', 'GET', 110, 'ACTIVE'),
('stock-out:create', '出库新增', 'BUTTON', 'STOCK_OUT', '/api/stock-out', 'POST', 111, 'ACTIVE'),
('stock-out:update', '出库编辑', 'BUTTON', 'STOCK_OUT', '/api/stock-out/{id}', 'PUT', 112, 'ACTIVE'),
('stock-out:confirm', '出库确认', 'BUTTON', 'STOCK_OUT', '/api/stock-out/{id}/confirm', 'POST', 113, 'ACTIVE'),
('stock-out:cancel', '出库取消', 'BUTTON', 'STOCK_OUT', '/api/stock-out/{id}/cancel', 'POST', 114, 'ACTIVE'),
('stock-out:delete', '出库删除', 'BUTTON', 'STOCK_OUT', '/api/stock-out/{id}', 'DELETE', 115, 'ACTIVE'),
('operation-log:view', '操作日志查看', 'MENU', 'OPERATION_LOG', '/operation-logs', 'GET', 120, 'ACTIVE');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
WHERE r.code = 'ADMIN';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'dashboard:view',
    'category:view', 'category:create', 'category:update',
    'product:view', 'product:create', 'product:update',
    'warehouse:view', 'warehouse:create', 'warehouse:update',
    'supplier:view', 'supplier:create', 'supplier:update',
    'stock:view', 'stock:low:view',
    'stock-in:view', 'stock-in:create', 'stock-in:update', 'stock-in:confirm', 'stock-in:cancel',
    'stock-out:view', 'stock-out:create', 'stock-out:update', 'stock-out:confirm', 'stock-out:cancel',
    'operation-log:view'
)
WHERE r.code = 'MANAGER';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'dashboard:view',
    'category:view',
    'product:view',
    'warehouse:view',
    'supplier:view',
    'stock:view', 'stock:low:view',
    'stock-in:view', 'stock-in:create', 'stock-in:update',
    'stock-out:view', 'stock-out:create', 'stock-out:update'
)
WHERE r.code = 'STAFF';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'dashboard:view',
    'category:view',
    'product:view',
    'warehouse:view',
    'supplier:view',
    'stock:view', 'stock:low:view',
    'stock-in:view',
    'stock-out:view'
)
WHERE r.code = 'VIEWER';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = u.role;
