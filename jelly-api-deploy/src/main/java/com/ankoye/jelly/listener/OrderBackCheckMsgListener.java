package com.ankoye.jelly.listener;

import com.ankoye.jelly.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author ankoye@qq.com
 */
@Slf4j
@Component
@RocketMQMessageListener( // 用户订单支付状态回查
        topic = "${user-order-topic}",
        selectorExpression = "check",
        consumerGroup = "order-check-group",
        consumeMode = ConsumeMode.CONCURRENTLY, // 并行处理，默认
        messageModel = MessageModel.CLUSTERING  // 集群消费，默认
)
public class OrderBackCheckMsgListener implements RocketMQListener<String> {
    @Autowired
    private OrderService orderService;

    @Override
    public void onMessage(String orderId) {
        log.info("订单支付状态回查：{}", orderId);
        // 超时未支付，回差订单
        orderService.checkOrder(orderId);
    }
}
