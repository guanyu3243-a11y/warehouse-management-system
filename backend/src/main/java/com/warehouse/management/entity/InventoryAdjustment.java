package com.warehouse.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("inventory_adjustments")
public class InventoryAdjustment extends BaseEntity {

    private String adjustmentNo;

    private Long warehouseId;

    private Long operatorId;

    private String reason;

    private Integer totalAdjustQuantity;

    private String status;

    private String remark;

    public String getAdjustmentNo() {
        return adjustmentNo;
    }

    public void setAdjustmentNo(String adjustmentNo) {
        this.adjustmentNo = adjustmentNo;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getTotalAdjustQuantity() {
        return totalAdjustQuantity;
    }

    public void setTotalAdjustQuantity(Integer totalAdjustQuantity) {
        this.totalAdjustQuantity = totalAdjustQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
