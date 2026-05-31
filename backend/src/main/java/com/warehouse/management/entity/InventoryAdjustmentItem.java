package com.warehouse.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("inventory_adjustment_items")
public class InventoryAdjustmentItem extends BaseEntity {

    private Long adjustmentId;

    private Long productId;

    private Integer quantityBefore;

    private Integer adjustQuantity;

    private Integer quantityAfter;

    private String remark;

    public Long getAdjustmentId() {
        return adjustmentId;
    }

    public void setAdjustmentId(Long adjustmentId) {
        this.adjustmentId = adjustmentId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantityBefore() {
        return quantityBefore;
    }

    public void setQuantityBefore(Integer quantityBefore) {
        this.quantityBefore = quantityBefore;
    }

    public Integer getAdjustQuantity() {
        return adjustQuantity;
    }

    public void setAdjustQuantity(Integer adjustQuantity) {
        this.adjustQuantity = adjustQuantity;
    }

    public Integer getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(Integer quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
