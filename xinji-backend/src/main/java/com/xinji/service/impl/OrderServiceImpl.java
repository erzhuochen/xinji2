package com.xinji.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinji.dto.request.CreateOrderRequest;
import com.xinji.dto.response.OrderResponse;
import com.xinji.dto.response.PageResponse;
import com.xinji.dto.response.WechatPrepayResponse;
import com.xinji.entity.Order;
import com.xinji.entity.PaymentRecord;
import com.xinji.entity.User;
import com.xinji.exception.BusinessException;
import com.xinji.mapper.OrderRepository;
import com.xinji.mapper.PaymentRecordRepository;
import com.xinji.mapper.UserRepository;
import com.xinji.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final UserRepository userRepository;
    
    @Value("${membership.monthly-price:29}")
    private int monthlyPrice;
    
    @Value("${membership.quarterly-price:78}")
    private int quarterlyPrice;
    
    @Value("${membership.annual-price:288}")
    private int annualPrice;
    
    @Override
    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        // 计算订单金额
        BigDecimal amount = calculateAmount(request.getPlanType());
        
        // 生成订单号
        String orderId = generateOrderId();
        
        // 创建订单
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setPlanType(request.getPlanType());
        order.setAmount(amount);
        order.setStatus("PENDING");
        order.setAutoRenew(Boolean.TRUE.equals(request.getAutoRenew()) ? 1 : 0);
        order.setExpireAt(LocalDateTime.now().plusMinutes(15)); // 15分钟超时
        
        orderRepository.insert(order);
        
        log.info("订单创建成功: orderId={}, userId={}, amount={}", orderId, userId, amount);
        
        return convertToResponse(order);
    }
    
    @Override
    public PageResponse<OrderResponse> listOrders(String userId, Integer page, Integer pageSize, String status) {
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 10 : Math.min(pageSize, 100);
        
        Page<Order> pageParam = new Page<>(page, pageSize);
        IPage<Order> orderPage = orderRepository.findByUserIdPage(pageParam, userId, status);
        
        List<OrderResponse> list = orderPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return new PageResponse<>(orderPage.getTotal(), page, pageSize, list);
    }
    
    @Override
    public OrderResponse getOrder(String userId, String orderId) {
        Order order = orderRepository.selectById(orderId);
        
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        
        if (!order.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权查看该订单");
        }
        
        return convertToResponse(order);
    }
    
    @Override
    @Transactional
    public void cancelOrder(String userId, String orderId) {
        Order order = orderRepository.selectById(orderId);
        
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        
        if (!order.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权取消该订单");
        }
        
        if (!"PENDING".equals(order.getStatus())) {
            throw BusinessException.badRequest("订单状态不允许取消");
        }
        
        order.setStatus("CANCELLED");
        orderRepository.updateById(order);
        
        log.info("订单已取消: orderId={}", orderId);
    }
    
    @Override
    public WechatPrepayResponse wechatPrepay(String userId, String orderId) {
        Order order = orderRepository.selectById(orderId);
        
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        
        if (!order.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权支付该订单");
        }
        
        if (!"PENDING".equals(order.getStatus())) {
            throw BusinessException.badRequest("订单状态不允许支付");
        }
        
        if (order.getExpireAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.badRequest("订单已过期");
        }
        
        // TODO: 调用微信支付API获取prepay_id
        // 这里模拟返回
        WechatPrepayResponse response = new WechatPrepayResponse();
        response.setPrepayId("wx" + System.currentTimeMillis());
        response.setAppId("wx_app_id");
        response.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        response.setNonceStr(IdUtil.simpleUUID());
        response.setPackageValue("prepay_id=" + response.getPrepayId());
        response.setSignType("RSA");
        response.setPaySign("mock_sign");
        
        log.info("微信预下单成功: orderId={}, prepayId={}", orderId, response.getPrepayId());
        
        return response;
    }
    
    @Override
    @Transactional
    public void handleWechatNotify(String requestBody, String signature, String timestamp, String nonce, String serial) {
        // TODO: 验证签名
        log.info("收到微信支付回调");
        
        // TODO: 解析回调数据，获取订单号和交易号
        // 这里模拟处理
        String orderId = ""; // 从requestBody解析
        String transactionId = ""; // 从requestBody解析
        
        // 幂等性检查
        Order order = orderRepository.selectById(orderId);
        if (order == null) {
            log.warn("订单不存在: {}", orderId);
            return;
        }
        
        if ("PAID".equals(order.getStatus())) {
            log.info("订单已支付，忽略重复回调: {}", orderId);
            return;
        }
        
        // 更新订单状态
        order.setStatus("PAID");
        order.setPaymentMethod("WECHAT");
        order.setTransactionId(transactionId);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.updateById(order);
        
        // 创建支付流水
        PaymentRecord record = new PaymentRecord();
        record.setOrderId(orderId);
        record.setUserId(order.getUserId());
        record.setPaymentMethod("WECHAT");
        record.setAmount(order.getAmount());
        record.setTransactionId(transactionId);
        record.setStatus("PAID");
        record.setPaidAt(LocalDateTime.now());
        record.setCallbackData(requestBody);
        paymentRecordRepository.insert(record);
        
        // 激活会员
        activateMembership(order.getUserId(), order.getPlanType());
        
        log.info("订单支付成功: orderId={}, transactionId={}", orderId, transactionId);
    }
    
    @Override
    public OrderResponse queryPaymentStatus(String userId, String orderId) {
        return getOrder(userId, orderId);
    }
    
    @Override
    @Transactional
    public void cancelExpiredOrders() {
        log.info("开始取消超时订单...");
        
        List<Order> expiredOrders = orderRepository.findExpiredOrders(LocalDateTime.now());
        
        if (expiredOrders.isEmpty()) {
            log.info("没有超时订单需要处理");
            return;
        }
        
        List<String> orderIds = expiredOrders.stream()
                .map(Order::getId)
                .collect(Collectors.toList());
        
        int updated = orderRepository.batchUpdateExpired(orderIds);
        
        log.info("超时订单处理完成: 共{}个订单已标记为过期", updated);
    }
    
    /**
     * 计算订单金额
     */
    private BigDecimal calculateAmount(String planType) {
        int priceInCents = switch (planType) {
            case "MONTHLY" -> monthlyPrice;
            case "QUARTERLY" -> quarterlyPrice;
            case "ANNUAL" -> annualPrice;
            default -> throw BusinessException.badRequest("无效的套餐类型");
        };
        
        // 转换为元
        return BigDecimal.valueOf(priceInCents).divide(BigDecimal.valueOf(1000));
    }
    
    /**
     * 生成订单号
     */
    private String generateOrderId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%03d", (int) (Math.random() * 1000));
        return "o" + timestamp + random;
    }
    
    /**
     * 激活会员
     */
    private void activateMembership(String userId, String planType) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            log.error("用户不存在: {}", userId);
            return;
        }
        
        // 计算会员到期时间
        LocalDateTime expireTime = user.getMemberExpireTime();
        LocalDateTime now = LocalDateTime.now();
        
        // 如果已是会员且未过期，则延长；否则从当前时间开始计算
        LocalDateTime baseTime = (expireTime != null && expireTime.isAfter(now)) ? expireTime : now;
        
        LocalDateTime newExpireTime = switch (planType) {
            case "MONTHLY" -> baseTime.plusMonths(1);
            case "QUARTERLY" -> baseTime.plusMonths(3);
            case "ANNUAL" -> baseTime.plusYears(1);
            default -> baseTime.plusMonths(1);
        };
        
        user.setMemberStatus("PRO");
        user.setMemberExpireTime(newExpireTime);
        userRepository.updateById(user);
        
        log.info("会员激活成功: userId={}, expireTime={}", userId, newExpireTime);
    }
    
    /**
     * 转换为响应对象
     */
    private OrderResponse convertToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setUserId(order.getUserId());
        response.setPlanType(order.getPlanType());
        response.setAmount(order.getAmount());
        response.setStatus(order.getStatus());
        response.setAutoRenew(order.getAutoRenew() == 1);
        response.setPaymentMethod(order.getPaymentMethod());
        response.setTransactionId(order.getTransactionId());
        response.setCreatedAt(order.getCreateTime());
        response.setPaidAt(order.getPaidAt());
        response.setExpireAt(order.getExpireAt());
        return response;
    }
}
