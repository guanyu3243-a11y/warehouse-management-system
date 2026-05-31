package com.warehouse.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("stock_takes")
public class StockTake extends BaseEntity {

    private String stockTakeNo;

    private Long warehouseId;

    private Long operatorId;

    private String title;

    private Integer totalBookQuantity;

    private Integer totalActualQuantity;

    private Integer totalDifferenceQuantity;

    private String status;

    private String remark;

    public String getStockTakeNo() {
        return stockTakeNo;
    }

    public void setStockTakeNo(String stockTakeNo) {
        this.stockTakeNo = stockTakeNo;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getTotalBookQuantity() {
        return totalBookQuantity;
    }

    public void setTotalBookQuantity(Integer totalBookQuantity) {
        this.totalBookQuantity = totalBookQuantity;
    }

    public Integer getTotalActualQuantity() {
        return totalActualQuantity;
    }

    public void setTotalActualQuantity(Integer totalActualQuantity) {
        this.totalActualQuantity = totalActualQuantity;
    }

    public Integer getTotalDifferenceQuantity() {
        return totalDifferenceQuantity;
    }

    public void setTotalDifferenceQuantity(Integer totalDifferenceQuantity) {
        this.totalDifferenceQuantity = totalDifferenceQuantity;
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
