package com.xinji.controller;

import com.xinji.dto.request.PaymentRequest;
import com.xinji.dto.response.ApiResponse;
import com.xinji.dto.response.OrderResponse;
import com.xinji.dto.response.WechatPrepayResponse;
import com.xinji.security.SecurityContext;
import com.xinji.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;
    private final SecurityContext securityContext;

    /**
     * 微信支付预下单
     */
    @PostMapping("/wechat/prepay")
    public ApiResponse<WechatPrepayResponse> wechatPrepay(@Valid @RequestBody PaymentRequest request) {
        String userId = securityContext.getCurrentUserId();
        WechatPrepayResponse response = orderService.wechatPrepay(userId, request.getOrderId());
        return ApiResponse.success(response);
    }

    /**
     * 模拟支付接口（前端测试用）
     */
    @PostMapping("/mock/pay")
    public ApiResponse<OrderResponse> mockPay(@Valid @RequestBody PaymentRequest request) {
        String userId = securityContext.getCurrentUserId();
        OrderResponse resp = orderService.mockPay(userId, request.getOrderId());
        return ApiResponse.success("支付成功", resp);
    }

    /**
     * 微信支付回调
     */
    @PostMapping("/wechat/notify")
    public Map<String, String> wechatNotify(
            @RequestBody String requestBody,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial) {

        try {
            orderService.handleWechatNotify(requestBody, signature, timestamp, nonce, serial);
            return Map.of("code", "SUCCESS", "message", "成功");
        } catch (Exception e) {
            log.error("微信支付回调处理失败", e);
            return Map.of("code", "FAIL", "message", "处理失败");
        }
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/order/{orderId}/status")
    public ApiResponse<OrderResponse> queryPaymentStatus(@PathVariable String orderId) {
        String userId = securityContext.getCurrentUserId();
        OrderResponse response = orderService.queryPaymentStatus(userId, orderId);
        return ApiResponse.success(response);
    }
}
