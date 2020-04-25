package com.ankoye.jelly.order.domian;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("tb_cart")
public class Cart implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)  // 数据库自增
    private Long id;

    private String skuId;

    private String userId;

    private String name;

    private Long price;

    private Integer num;

}