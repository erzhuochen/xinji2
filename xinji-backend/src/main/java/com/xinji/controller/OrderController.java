package com.xinji.controller;

import com.xinji.dto.request.CreateOrderRequest;
import com.xinji.dto.request.PaymentRequest;
import com.xinji.dto.response.ApiResponse;
import com.xinji.dto.response.OrderResponse;
import com.xinji.dto.response.PageResponse;
import com.xinji.dto.response.WechatPrepayResponse;
import com.xinji.security.SecurityContext;
import com.xinji.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    private final SecurityContext securityContext;
    
    /**
     * 创建订单
     */
    @PostMapping("/create")
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        String userId = securityContext.getCurrentUserId();
        OrderResponse response = orderService.createOrder(userId, request);
        return ApiResponse.success("订单创建成功", response);
    }
    
    /**
     * 获取订单列表
     */
    @GetMapping("/list")
    public ApiResponse<PageResponse<OrderResponse>> listOrders(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        String userId = securityContext.getCurrentUserId();
        PageResponse<OrderResponse> response = orderService.listOrders(userId, page, pageSize, status);
        return ApiResponse.success(response);
    }
    
    /**
     * 获取订单详情
     */
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable String orderId) {
        String userId = securityContext.getCurrentUserId();
        OrderResponse response = orderService.getOrder(userId, orderId);
        return ApiResponse.success(response);
    }
    
    /**
     * 取消订单
     */
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancelOrder(@PathVariable String orderId) {
        String userId = securityContext.getCurrentUserId();
        orderService.cancelOrder(userId, orderId);
        return ApiResponse.success("订单已取消");
    }
}
