package com.ankoye.jelly.seckill.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("tb_seckill_goods")
public class SeckillGoods implements Serializable {
    @TableId
    private String id;

    /**
     * spu ID
     */
    private String spuId;

    /**
     * sku ID
     */
    private String skuId;

    /**
     * 标题
     */
    private String title;

    /**
     * 商品图片
     */
    private String image;

    /**
     * 原价格
     */
    private BigDecimal price;

    /**
     * 秒杀价格
     */
    private BigDecimal costPrice;

    /**
     * 商家ID
     */
    private String merchantId;

    /**
     * 添加日期
     */
    private Date createTime;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yy/MM/dd")
    private Date startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yy/MM/dd")
    private Date endTime;

    /**
     * 秒杀时间段
     */
    private Integer region;

    /**
     * 秒杀商品数
     */
    private Integer num;

    /**
     * 剩余库存数
     */
    private Integer stockCount;

    /**
     * 是否开启秒杀
     */
    private Boolean isMarketable;

    /**
     * 审核日期
     */
    private Date checkTime;

    /**
     * 审核状态
     */
    private Integer status;

}
