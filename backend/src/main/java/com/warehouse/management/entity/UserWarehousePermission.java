package com.warehouse.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("user_warehouse_permissions")
public class UserWarehousePermission extends BaseEntity {

    private Long userId;

    private Long warehouseId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}
