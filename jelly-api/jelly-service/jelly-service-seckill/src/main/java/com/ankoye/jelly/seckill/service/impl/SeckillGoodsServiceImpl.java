package com.ankoye.jelly.seckill.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ankoye.jelly.base.constant.GoodsStatus;
import com.ankoye.jelly.goods.domain.Sku;
import com.ankoye.jelly.goods.service.SkuService;
import com.ankoye.jelly.seckill.common.constant.RedisKey;
import com.ankoye.jelly.seckill.dao.SeckillGoodsMapper;
import com.ankoye.jelly.seckill.domain.SeckillSku;
import com.ankoye.jelly.seckill.model.SeckillGoods;
import com.ankoye.jelly.seckill.service.SeckillGoodsService;
import com.ankoye.jelly.util.IdUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {
    @Reference
    private SkuService skuService;
    @Resource
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public PageInfo<SeckillSku> list(Integer page, Integer size) {
        PageHelper.startPage(page, size);
        List<SeckillSku> seckillGoods = seckillGoodsMapper.selectList(null);
        return new PageInfo<>(seckillGoods);
    }

    @Override
    @Transactional // 全局事务
    public void add(SeckillSku goods) {
        // 查询商品信息
        Sku sku = skuService.getSkuById(goods.getSkuId());
        // 添加秒杀商品
        goods.setId(IdUtils.getSpuId());
        goods.setSpuId(sku.getSpuId());
        goods.setImage(sku.getImage());
        goods.setStockCount(goods.getNum());
        goods.setCreateTime(new Date());
        goods.setIsMarketable(true);
        goods.setStatus(GoodsStatus.SUCCESS);
        seckillGoodsMapper.insert(goods);
        // 冻结商品库存
        skuService.freezeScore(goods.getSkuId(), goods.getNum());
    }

    @Override
    public List<SeckillSku> timeList(String time) {
        return redisTemplate.boundHashOps(RedisKey.SECKILL_GOODS_KEY + time).values();
    }

    @Override
    public SeckillGoods detail(String time, String spuId) {
        return (SeckillGoods) redisTemplate.boundHashOps(RedisKey.SECKILL_GOODS_KEY + time).get(spuId);
    }
}