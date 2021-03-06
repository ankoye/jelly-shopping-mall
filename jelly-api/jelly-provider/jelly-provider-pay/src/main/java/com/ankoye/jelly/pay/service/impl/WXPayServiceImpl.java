package com.ankoye.jelly.pay.service.impl;

import com.ankoye.jelly.pay.config.WXPayAppConfig;
import com.ankoye.jelly.pay.model.Order;
import com.ankoye.jelly.pay.service.WXPayService;
import com.github.wxpay.sdk.WXPay;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hmily.annotation.Hmily;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ankoye@qq.com
 */
@Slf4j
@Service
@Component("wxPayService")
public class WXPayServiceImpl implements WXPayService {
    @Autowired
    private WXPayAppConfig wxPayAppConfig;
    @Autowired
    private WXPay wxPay;

    @Override
    public Map<String, String> nativePay(Order order) {
        Map<String, String> requestMap = new HashMap<>();
        try {
            requestMap.put("body", order.getName());                            // 商品描述
            requestMap.put("out_trade_no", order.getId());                      // 商户订单号
            // BigDecimal fen = order.getMoney().multiply(new BigDecimal("100"));
            BigDecimal fen = new BigDecimal("0.01").multiply(new BigDecimal("100"));
            fen = fen.setScale(0, BigDecimal.ROUND_UP);                // 1
            requestMap.put("total_fee",String.valueOf(fen));                    // 总金额 1分
            requestMap.put("trade_type", "NATIVE");                             // 支付类型
            requestMap.put("notify_url", wxPayAppConfig.getPayNotifyUrl());     // 接收微信支付异步通知回调地址

            Map<String, String> resultMap = wxPay.unifiedOrder(requestMap);
            log.info("生成预付款状态：{}", requestMap);

            // 获取返回码
            String returnCode = resultMap.get("return_code");
            if ("SUCCESS".equals(returnCode)) {
                // ...
            }
            return resultMap;
        } catch (Exception e) {
            log.error("订单号：{}，错误信息：{}", order.getId(), e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, String> queryOrder(String orderId) {
        try{
            Map<String ,String> map = new HashMap<>();
            map.put("out_trade_no", orderId);
            return wxPay.orderQuery(map);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Map<String, String> closeOrder(String orderId) {
        try{
            Map<String,String> map = new HashMap<>();
            map.put("out_trade_no", orderId);
            return wxPay.closeOrder(map);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Map<String, String> refund(Order order) {
        try {
            Map<String, String> requestMap = new HashMap<>();
            System.out.println(wxPayAppConfig.toString());
            requestMap.put("out_trade_no", order.getId());
            requestMap.put("out_refund_no", "xxx");
            requestMap.put("total_fee", order.getMoney().toString());
            requestMap.put("refund_fee", order.getMoney().toString());
            requestMap.put("refund_desc", "退款原因");
            return wxPay.refund(requestMap);
        } catch (Exception e) {
            log.error("订单号：{}，错误信息：{}", order.getId(), e.getMessage());
            return null;
        }

//        String return_code = responseMap.get("return_code");   //返回状态码
//        String return_msg = responseMap.get("return_msg");     //返回信息
//        if ("SUCCESS".equals(return_code)) {
//            String result_code = responseMap.get("result_code");       //业务结果
//            String err_code_des = responseMap.get("err_code_des");     //错误代码描述
//            if ("SUCCESS".equals(result_code)) {
//                //表示退款申请接受成功，结果通过退款查询接口查询
//                //修改用户订单状态为退款申请中或已退款。退款异步通知根据需求，可选
//                log.info("退款申请成功");
//            } else {
//                log.info("错误信息:{}", err_code_des);
//            }
//        } else {
//            log.info("错误信息:{}", return_msg);
//        }
    }
}
