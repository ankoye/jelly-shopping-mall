package com.ankoye.jelly.seckill.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户排队抢单信息
 *
 * @author ankoye@qq.com
 */
@Data
public class OrderQueue implements Serializable {
    public static final int QUEUING = 1; // 排队中
    public static final int CREATED = 2;    // 订单已创建
    // 用户
    private String userId;

    // 排队时间
    private Date createTime;

    // 商品id
    private String skuId;

    // 秒杀数量
    private Integer num;

    // 商品spuId
    private String spuId;

    // 时间段
    private String time;

    // 排队状态
    private Integer status;

    // 订单号
    private String orderId;
}
