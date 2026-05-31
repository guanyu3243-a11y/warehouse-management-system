package com.warehouse.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("stock_take_items")
public class StockTakeItem extends BaseEntity {

    private Long stockTakeId;

    private Long productId;

    private Integer bookQuantity;

    private Integer actualQuantity;

    private Integer differenceQuantity;

    private String remark;

    public Long getStockTakeId() {
        return stockTakeId;
    }

    public void setStockTakeId(Long stockTakeId) {
        this.stockTakeId = stockTakeId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getBookQuantity() {
        return bookQuantity;
    }

    public void setBookQuantity(Integer bookQuantity) {
        this.bookQuantity = bookQuantity;
    }

    public Integer getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(Integer actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public Integer getDifferenceQuantity() {
        return differenceQuantity;
    }

    public void setDifferenceQuantity(Integer differenceQuantity) {
        this.differenceQuantity = differenceQuantity;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
