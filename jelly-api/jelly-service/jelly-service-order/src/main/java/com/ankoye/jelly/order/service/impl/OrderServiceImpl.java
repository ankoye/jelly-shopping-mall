package com.ankoye.jelly.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.ankoye.jelly.base.constant.OrderStatus;
import com.ankoye.jelly.goods.service.SkuService;
import com.ankoye.jelly.goods.service.SpuService;
import com.ankoye.jelly.order.dao.OrderItemMapper;
import com.ankoye.jelly.order.dao.OrderMapper;
import com.ankoye.jelly.order.domian.Order;
import com.ankoye.jelly.order.domian.OrderItem;
import com.ankoye.jelly.order.model.OrderDto;
import com.ankoye.jelly.order.service.OrderService;
import com.ankoye.jelly.pay.service.WXPayService;
import com.ankoye.jelly.util.IdUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.dromara.hmily.annotation.Hmily;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {
    @Value("${order-back-check-topic}")
    private String orderBackTopic;

    @Reference
    private SkuService skuService;
    @Reference
    private SpuService spuService;
    @Reference
    private WXPayService wxPayService;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderItemMapper orderItemMapper;

    @Override
    public Order getOrderById(String id) {
        return orderMapper.selectById(id);
    }

    @Override
    @Transactional // 换位全局事务
    public String createOrder(OrderDto orderDto) {
        // 1 - 处理订单信息
        String orderId = IdUtils.getOrderId();
        Order order = orderDto.convertToOrder();
        order.setId(orderId);
        Date now = new Date();
        order.setCreateTime(now);
        order.setUpdateTime(now);
        order.setStatus(OrderStatus.WAIT_PAY);
        orderMapper.insert(order);

        // 2 - 处理订单商品，预扣库存
        List<OrderItem> orderItem = orderDto.getOrderItem();
        for (OrderItem item : orderItem) {
            item.setOrderId(orderId);
            // 添加订单商品
            orderItemMapper.insert(item);
            // 冻结库存
            skuService.freezeScore(item.getSkuId(), item.getNum());
        }

        // 3 - 发送延迟消息，检查订单状态，发现超时未支付则取消这个订单
        try {
            DefaultMQProducer producer = rocketMQTemplate.getProducer();
            Message msg = new Message(orderBackTopic, "order", orderId.getBytes());
            msg.setDelayTimeLevel(3);  // 半小时
            producer.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orderId;
    }

    @Override
    @Transactional  // 换全局事务
    public int updateStatus(String id, String payTime, String transactionId){
        Order order = orderMapper.selectById(id);
        // 1- 设置订单新状态
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            order.setPayTime(format.parse(payTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        order.setStatus(OrderStatus.WAIT_SEND);
        order.setTransactionId(transactionId);
        orderMapper.updateById(order);

        // 2- 获取订单的商品，并扣减对应的库存
        List<OrderItem> orderItem = orderItemMapper.selectList(
                new QueryWrapper<OrderItem>().eq("order_id", id)
        );
        for (OrderItem item : orderItem) {
            skuService.decreaseScore(item.getSkuId(), item.getNum());
        }
        return 0;
    }

    @Override
    public int payFailStatus(String id) {
        Order order = orderMapper.selectById(id);
        // 设置订单状态
        order.setUpdateTime(new Date());
        order.setStatus(OrderStatus.PAY_FAIL);
        orderMapper.updateById(order);
        return 0;
    }

    @Override
    @Transactional
    public int deleteOrder(String id) {
        Order order = orderMapper.selectById(id);
        if(order.getStatus().equals(OrderStatus.WAIT_PAY)) {
            // 1 - 超时未支付，删除订单
            order.setStatus(OrderStatus.CLOSE);
            order.setUpdateTime(new Date());
            orderMapper.updateById(order);

            // 2 - 获取订单的商品，解冻库存
            List<OrderItem> orderItem = orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                    .eq("order_id", id)
            );
            for (OrderItem item : orderItem) {
                skuService.unfreezeScore(item.getSkuId(), item.getNum());
            }
            System.out.println("Ok");
            // 3 - 如果交易已经开启则关闭 - lose if
            Map<String, String> payResult = wxPayService.queryOrder(id);
            wxPayService.closeOrder(order.getId());
            System.out.println(payResult);
        }
        return 0;
    }

    @Override
    public List<OrderDto> getByUserId(String id) {
        List<OrderDto> result = new ArrayList<>();
        // 获取用户所有订单
        List<Order> orders = orderMapper.selectList(new QueryWrapper<Order>()
                .eq("user_id", id)
        );
        // 获取订单对应的商品
        for (Order order : orders) {
            OrderDto orderDto = new OrderDto().convertFor(order);
            List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                .eq("order_id", order.getId())
            );
            orderDto.setOrderItem(items);
            result.add(orderDto);
        }
        return result;
    }

    @Override
    @Transactional
    public int deleteById(String id) {
        // 删除订单项
        orderItemMapper.delete(new UpdateWrapper<OrderItem>()
            .eq("order_id", id)
        );
        // 删除订单
        orderMapper.deleteById(id);
        return 0;
    }

    @Override
    @Hmily(confirmMethod = "confirm", cancelMethod = "cancel")
    public void test() {
        skuService.abc();
        spuService.bdc();
    }

    public void confirm() {
        System.out.println("confirm");
    }
    public void cancel() {
        System.out.println("cancel");
    }

}
