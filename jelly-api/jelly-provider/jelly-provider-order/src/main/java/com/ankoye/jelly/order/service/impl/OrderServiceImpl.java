package com.ankoye.jelly.order.service.impl;

import com.ankoye.jelly.base.constant.OrderStatus;
import com.ankoye.jelly.goods.domain.Sku;
import com.ankoye.jelly.goods.domain.Spu;
import com.ankoye.jelly.goods.feign.SkuFeign;
import com.ankoye.jelly.goods.feign.SpuFeign;
import com.ankoye.jelly.order.common.constant.RedisKey;
import com.ankoye.jelly.order.dao.CartMapper;
import com.ankoye.jelly.order.dao.OrderItemMapper;
import com.ankoye.jelly.order.dao.OrderMapper;
import com.ankoye.jelly.order.domian.Order;
import com.ankoye.jelly.order.domian.OrderItem;
import com.ankoye.jelly.order.model.OrderModel;
import com.ankoye.jelly.order.reference.OrderReference;
import com.ankoye.jelly.order.service.OrderService;
import com.ankoye.jelly.seckill.feign.SeckillSkuFeign;
import com.ankoye.jelly.util.IdUtils;
import com.ankoye.jelly.web.exception.CastException;
import com.ankoye.jelly.web.support.BaseService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author ankoye@qq.com
 */
@Service
public class OrderServiceImpl extends BaseService<Order> implements OrderService, OrderReference {
    @Value("${user-order-topic}")
    private String orderTopic;
    @Value("${seckill-order-topic}")
    private String seckillTopic;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private CartMapper cartMapper;

    @Autowired
    private SkuFeign skuFeign;
    @Autowired
    private SpuFeign spuFeign;
    @Autowired
    private SeckillSkuFeign seckillSkuFeign;


    @Override
    public OrderModel getOrderById(String id) {
        Order order = orderMapper.selectById(id);
        OrderModel orderModel = new OrderModel().convertFor(order);
        List<OrderItem> orderItems = orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                .eq("order_id", id)
        );
        orderModel.setOrderItem(orderItems);
        return orderModel;
    }

    @Override
    public OrderModel getPrepareOrder(String id) {
        return (OrderModel) redisTemplate.boundHashOps(RedisKey.PREPARE_ORDER).get(id);
    }

    @Override
    public void checkPrepareOrder(String id) {
        OrderModel order = getPrepareOrder(id);
        if (order == null) {    // 为空，说明创建了订单
            return ;
        }
        // 删除预订单
        redisTemplate.boundHashOps(RedisKey.PREPARE_ORDER).delete(id);
        // 如果是秒杀订单，回滚库存
        if (order.getType() == 1) {
            seckillSkuFeign.rollback(order);
        }
    }

    @Override
    public String prepare(OrderModel form) {
        // form中仅包含userId、skuId和num
        List<OrderItem> formItem = form.getOrderItem();
        // 创建预订单
        OrderModel orderModel = new OrderModel();
        String orderId = IdUtils.getOrderId();
        BigDecimal money = BigDecimal.valueOf(0);
        // 设置订单项
        for (OrderItem item : formItem) {
            Sku sku = skuFeign.getById(item.getSkuId()).getData();
            Spu spu = spuFeign.getSpuById(sku.getSpuId()).getData();
            BigDecimal subtotal = sku.getPrice().multiply(BigDecimal.valueOf(item.getNum())); // 小计
            OrderItem orderItem = new OrderItem(
                    orderId, sku.getSpuId(), item.getSkuId(), spu.getMerchantId(), spu.getTitle(), sku.getImage(),
                    sku.getSku(), sku.getPrice(), sku.getPrice(), item.getNum(), subtotal
            );
            orderModel.getOrderItem().add(orderItem);
            // 统计总金额
            money = money.add(subtotal);
        }
        // 设置订单信息
        orderModel.setId(orderId);
        orderModel.setUserId(form.getUserId());
        orderModel.setCartIds(form.getCartIds());
        orderModel.setType(0);              // 普通订单
        orderModel.setMoney(money);         // 待修改
        orderModel.setPayMoney(money);
        orderModel.setWeight(0);            // 待修改

        // 将预订单存储到redis
        redisTemplate.boundHashOps(RedisKey.PREPARE_ORDER).put(orderId, orderModel);
        return orderId;
    }

    @Override
    @Transactional
    public String create(OrderModel form) {
        // 重新到redis中获取订单，防止被修改
        OrderModel orderModel = (OrderModel) redisTemplate.boundHashOps(RedisKey.PREPARE_ORDER).get(form.getId());
        if (orderModel == null) {
            CastException.cast("订单不存在或已过期");
        }
        // 1 - 将订单入库
        Order order = orderModel.convertToOrder();
        Date now = new Date();
        order.setAddressId(form.getAddressId());
        order.setCreateTime(now); // 待删除
        order.setUpdateTime(now); // 待删除
        order.setStatus(OrderStatus.WAIT_PAY);
        orderMapper.insert(order);
        // 2 - 处理订单商品，预扣库存
        List<OrderItem> orderItem = orderModel.getOrderItem();
        for (OrderItem item : orderItem) {
            // 添加订单商品
            orderItemMapper.insert(item);
            if (order.getType() == 0) {
                // 不是秒杀订单 - 冻结库存
                skuFeign.freezeScore(item.getSkuId(), item.getNum());
            }
        }
        // 3 - 发送延迟消息，检查订单状态，发现超时未支付则取消这个订单
        try {
            DefaultMQProducer producer = rocketMQTemplate.getProducer();
            String topic = order.getType() == 0 ? orderTopic : seckillTopic;
            Message msg = new Message(topic, "check", orderModel.getId().getBytes());
            msg.setDelayTimeLevel(16);  // 半小时
            producer.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 4 - 如果在购物车下单，则删除购物车商品
        List<String> cartIds = orderModel.getCartIds();
        if (cartIds != null && cartIds.size() != 0) {
            cartMapper.deleteBatchIds(cartIds);
        }
        // 5 - 删除预订单
        redisTemplate.boundHashOps(RedisKey.PREPARE_ORDER).delete(order.getId());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(String id, String payTime, String transactionId){
        Order order = orderMapper.selectById(id);
        // 1- 设置订单新状态
        DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            order.setPayTime(format.parse(payTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        order.setStatus(OrderStatus.WAIT_SEND);
        order.setTransactionId(transactionId);
        orderMapper.updateById(order);

        // 2 - 获取订单的商品，并扣减对应的库存，增加销售量
        List<OrderItem> orderItem = orderItemMapper.selectList(
                new QueryWrapper<OrderItem>().eq("order_id", id)
        );
        // 普通订单
        if (order.getType() == 0) {
            // 2 - 获取订单的商品，并扣减对应的库存，增加销售量
            for (OrderItem item : orderItem) {
                skuFeign.paySuccess(item.getSpuId(), item.getSkuId(), item.getNum());
            }
        } else if (order.getType() == 1) {
            for (OrderItem item : orderItem) {
                seckillSkuFeign.updateStock(item.getSkuId(), item.getNum());
            }
        }
        return true;
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
    @Transactional(rollbackFor = Exception.class)
    public int checkOrder(String id) {
        Order order = orderMapper.selectById(id);
        // 如果为超时未支付
        if(order.getStatus().equals(OrderStatus.WAIT_PAY)) {
            // 1 - 超时未支付，删除订单
            order.setStatus(OrderStatus.CLOSE);
            order.setUpdateTime(new Date()); // 待删除
            orderMapper.updateById(order);
            // 2 - 获取订单的商品，解冻库存
            List<OrderItem> orderItem = orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                    .eq("order_id", id)
            );
            for (OrderItem item : orderItem) {
                skuFeign.unfreezeScore(item.getSkuId(), item.getNum());
            }
            /** 3 - 如果交易已经开启则关闭 */
        }
        return 0;
    }

    @Override
    public PageInfo<OrderModel> getByUserId(String id, Integer page, Integer size) {
        List<OrderModel> result = new ArrayList<>();
        PageHelper.startPage(page, size);
        // 获取用户所有订单
        List<Order> orders = orderMapper.selectList(new QueryWrapper<Order>()
                .eq("user_id", id)
                .lt("status", OrderStatus.DELETE)
                .orderByDesc("create_time")
        );
        // 获取订单对应的商品
        for (Order order : orders) {
            OrderModel orderModel = new OrderModel().convertFor(order);
            List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                .eq("order_id", order.getId())
            );
            orderModel.setOrderItem(items);
            result.add(orderModel);
        }
        PageInfo pageInfo = new PageInfo(orders);
        pageInfo.setList(result);
        return pageInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteById(String id) {
        // 删除订单项
        //orderItemMapper.delete(new UpdateWrapper<OrderItem>()
        //   .eq("order_id", id)
        //);
        // 删除订单
        // orderMapper.deleteById(id);
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.DELETE);
        return orderMapper.updateById(order);
    }

}
