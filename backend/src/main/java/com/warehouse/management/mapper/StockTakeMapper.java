package com.warehouse.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehouse.management.entity.StockTake;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockTakeMapper extends BaseMapper<StockTake> {
}
