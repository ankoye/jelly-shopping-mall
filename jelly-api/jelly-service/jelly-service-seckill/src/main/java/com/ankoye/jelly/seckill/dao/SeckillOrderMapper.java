package com.ankoye.jelly.seckill.dao;

import com.ankoye.jelly.order.domian.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SeckillOrderMapper extends BaseMapper<Order> {
}
