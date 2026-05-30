package com.warehouse.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehouse.management.entity.Stock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface StockMapper extends BaseMapper<Stock> {

    @Select("""
            SELECT id, product_id, warehouse_id, quantity, locked_quantity, version, created_at, updated_at
            FROM stock
            WHERE product_id = #{productId}
              AND warehouse_id = #{warehouseId}
            FOR UPDATE
            """)
    Stock selectByProductAndWarehouseForUpdate(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId
    );

    @Update("""
            UPDATE stock
            SET quantity = #{quantity},
                version = version + 1
            WHERE id = #{id}
            """)
    int updateQuantityById(
            @Param("id") Long id,
            @Param("quantity") Integer quantity
    );

    @Update("""
            UPDATE stock
            SET quantity = quantity - #{quantity},
                version = version + 1
            WHERE id = #{id}
              AND quantity >= #{quantity}
            """)
    int decreaseQuantityByIdIfEnough(
            @Param("id") Long id,
            @Param("quantity") Integer quantity
    );
}
