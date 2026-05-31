CREATE TABLE IF NOT EXISTS stock_takes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_take_no VARCHAR(80) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    total_book_quantity INT NOT NULL DEFAULT 0,
    total_actual_quantity INT NOT NULL DEFAULT 0,
    total_difference_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_takes_no (stock_take_no),
    KEY idx_stock_takes_warehouse_id (warehouse_id),
    KEY idx_stock_takes_operator_id (operator_id),
    KEY idx_stock_takes_status (status),
    CONSTRAINT fk_stock_takes_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_takes_operator_id FOREIGN KEY (operator_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_take_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_take_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    book_quantity INT NOT NULL DEFAULT 0,
    actual_quantity INT NOT NULL DEFAULT 0,
    difference_quantity INT NOT NULL DEFAULT 0,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_stock_take_items_stock_take_id (stock_take_id),
    KEY idx_stock_take_items_product_id (product_id),
    CONSTRAINT fk_stock_take_items_stock_take_id FOREIGN KEY (stock_take_id) REFERENCES stock_takes (id),
    CONSTRAINT fk_stock_take_items_product_id FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO permissions (code, name, type, module, path, method, sort_order, status) VALUES
('stock-take:view', '库存盘点查看', 'MENU', 'STOCK_TAKE', '/stock-takes', 'GET', 121, 'ACTIVE'),
('stock-take:create', '库存盘点新增', 'BUTTON', 'STOCK_TAKE', '/api/stock-takes', 'POST', 122, 'ACTIVE'),
('stock-take:update', '库存盘点编辑', 'BUTTON', 'STOCK_TAKE', '/api/stock-takes/{id}', 'PUT', 123, 'ACTIVE'),
('stock-take:confirm', '库存盘点确认', 'BUTTON', 'STOCK_TAKE', '/api/stock-takes/{id}/confirm', 'POST', 124, 'ACTIVE'),
('stock-take:cancel', '库存盘点取消', 'BUTTON', 'STOCK_TAKE', '/api/stock-takes/{id}/cancel', 'POST', 125, 'ACTIVE'),
('stock-take:import', '库存盘点导入', 'BUTTON', 'STOCK_TAKE', '/api/stock-takes/{id}/import', 'POST', 126, 'ACTIVE'),
('stock-take:export', '库存盘点导出', 'BUTTON', 'STOCK_TAKE', '/api/stock-takes/{id}/export', 'GET', 127, 'ACTIVE');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'stock-take:view',
    'stock-take:create',
    'stock-take:update',
    'stock-take:confirm',
    'stock-take:cancel',
    'stock-take:import',
    'stock-take:export'
)
WHERE r.code IN ('ADMIN', 'MANAGER');
