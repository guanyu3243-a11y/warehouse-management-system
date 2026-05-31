CREATE TABLE IF NOT EXISTS inventory_adjustments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    adjustment_no VARCHAR(80) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    total_adjust_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_inventory_adjustments_no (adjustment_no),
    KEY idx_inventory_adjustments_warehouse_id (warehouse_id),
    KEY idx_inventory_adjustments_operator_id (operator_id),
    KEY idx_inventory_adjustments_status (status),
    CONSTRAINT fk_inventory_adjustments_warehouse_id FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_inventory_adjustments_operator_id FOREIGN KEY (operator_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_adjustment_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    adjustment_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity_before INT NOT NULL DEFAULT 0,
    adjust_quantity INT NOT NULL,
    quantity_after INT NOT NULL DEFAULT 0,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_inventory_adjustment_items_adjustment_id (adjustment_id),
    KEY idx_inventory_adjustment_items_product_id (product_id),
    CONSTRAINT fk_inventory_adjustment_items_adjustment_id FOREIGN KEY (adjustment_id) REFERENCES inventory_adjustments (id),
    CONSTRAINT fk_inventory_adjustment_items_product_id FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO permissions (code, name, type, module, path, method, sort_order, status) VALUES
('inventory-adjustment:view', '库存调整查看', 'MENU', 'INVENTORY_ADJUSTMENT', '/inventory-adjustments', 'GET', 116, 'ACTIVE'),
('inventory-adjustment:create', '库存调整新增', 'BUTTON', 'INVENTORY_ADJUSTMENT', '/api/inventory-adjustments', 'POST', 117, 'ACTIVE'),
('inventory-adjustment:update', '库存调整编辑', 'BUTTON', 'INVENTORY_ADJUSTMENT', '/api/inventory-adjustments/{id}', 'PUT', 118, 'ACTIVE'),
('inventory-adjustment:confirm', '库存调整确认', 'BUTTON', 'INVENTORY_ADJUSTMENT', '/api/inventory-adjustments/{id}/confirm', 'POST', 119, 'ACTIVE'),
('inventory-adjustment:cancel', '库存调整取消', 'BUTTON', 'INVENTORY_ADJUSTMENT', '/api/inventory-adjustments/{id}/cancel', 'POST', 120, 'ACTIVE');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'inventory-adjustment:view',
    'inventory-adjustment:create',
    'inventory-adjustment:update',
    'inventory-adjustment:confirm',
    'inventory-adjustment:cancel'
)
WHERE r.code IN ('ADMIN', 'MANAGER');
