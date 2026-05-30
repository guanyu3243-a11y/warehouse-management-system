CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    movement_no VARCHAR(80) NOT NULL,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id BIGINT NOT NULL,
    source_no VARCHAR(80) NOT NULL,
    quantity_before INT NOT NULL DEFAULT 0,
    change_quantity INT NOT NULL,
    quantity_after INT NOT NULL DEFAULT 0,
    operator_id BIGINT,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_movements_movement_no (movement_no),
    KEY idx_stock_movements_product_id (product_id),
    KEY idx_stock_movements_warehouse_id (warehouse_id),
    KEY idx_stock_movements_movement_type (movement_type),
    KEY idx_stock_movements_source (source_type, source_id),
    KEY idx_stock_movements_source_no (source_no),
    KEY idx_stock_movements_created_at (created_at),
    CONSTRAINT fk_stock_movements_product_id FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_movements_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_movements_operator_id FOREIGN KEY (operator_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO permissions (code, name, type, module, path, method, sort_order, status) VALUES
('stock-movement:view', '库存流水查看', 'MENU', 'STOCK_MOVEMENT', '/stock-movements', 'GET', 95, 'ACTIVE');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'stock-movement:view'
WHERE r.code IN ('ADMIN', 'MANAGER', 'STAFF', 'VIEWER');
