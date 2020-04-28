package com.ankoye.jelly.goods.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("tb_sku")
public class Sku implements Serializable {
    private String id;

    private String spuId;

    private String sku;

    private Long price;

    private Float discount;

    private Integer num;

    private Integer alertNum;

    private String image;

    private Integer saleNum;

    private Date createTime;

    private Date updateTime;

    private Integer freezeNum;
}