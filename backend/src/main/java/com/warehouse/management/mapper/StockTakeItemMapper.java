package com.warehouse.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehouse.management.entity.StockTakeItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockTakeItemMapper extends BaseMapper<StockTakeItem> {
}
