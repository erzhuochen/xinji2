package com.xinji.service;

import com.xinji.dto.request.CreateOrderRequest;
import com.xinji.dto.response.OrderResponse;
import com.xinji.dto.response.PageResponse;
import com.xinji.dto.response.WechatPrepayResponse;

/**
 * 订单服务接口
 */
public interface OrderService {
    
    /**
     * 创建订单
     */
    OrderResponse createOrder(String userId, CreateOrderRequest request);
    
    /**
     * 获取订单列表
     */
    PageResponse<OrderResponse> listOrders(String userId, Integer page, Integer pageSize, String status);
    
    /**
     * 获取订单详情
     */
    OrderResponse getOrder(String userId, String orderId);
    
    /**
     * 取消订单
     */
    void cancelOrder(String userId, String orderId);
    
    /**
     * 微信支付预下单
     */
    WechatPrepayResponse wechatPrepay(String userId, String orderId);
    
    /**
     * 微信支付回调
     */
    void handleWechatNotify(String requestBody, String signature, String timestamp, String nonce, String serial);
    
    /**
     * 查询支付状态
     */
    OrderResponse queryPaymentStatus(String userId, String orderId);
    
    /**
     * 模拟支付成功(测试用)
     */
    OrderResponse mockPaySuccess(String userId, String orderId);
    
    /**
     * 取消超时订单(定时任务)
     */
    void cancelExpiredOrders();
}
