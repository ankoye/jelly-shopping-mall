package com.ankoye.jelly.order.listener;

import com.ankoye.jelly.order.service.OrderService;
import com.ankoye.jelly.pay.service.WXPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${order-back-check-topic}",
        consumerGroup = "order-check-group",
        consumeMode = ConsumeMode.ORDERLY
)
public class BackCheckOrderMsgListener implements RocketMQListener<String> {
    @Autowired
    private OrderService orderService;


    @Override
    public void onMessage(String orderId) {
        log.info("订单支付状态回查：{}", orderId);
        // 超时未支付，删除订单
        orderService.deleteOrder(orderId);
    }
}