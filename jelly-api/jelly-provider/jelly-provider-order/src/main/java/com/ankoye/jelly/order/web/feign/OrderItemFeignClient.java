package com.ankoye.jelly.order.web.feign;

import com.ankoye.jelly.base.result.Wrapper;
import com.ankoye.jelly.base.result.Wrappers;
import com.ankoye.jelly.order.domian.OrderItem;
import com.ankoye.jelly.order.feign.OrderItemFeign;
import com.ankoye.jelly.order.reference.OrderItemReference;
import com.ankoye.jelly.order.service.OrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderItemFeignClient implements OrderItemFeign {
    @Autowired
    private OrderItemReference itemReference;

    @Override
    public Wrapper<List<OrderItem>> getOrderItem(String id) {
        return Wrappers.success(
                itemReference.selectList(new OrderItem().setOrderId(id))
        );
    }
}
