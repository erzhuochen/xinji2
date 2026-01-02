package com.xinji.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应DTO
 */
@Data
public class OrderResponse {
    
    private String orderId;
    
    private String orderNo;
    
    private String userId;
    
    /**
     * 套餐类型
     */
    private String planType;
    
    /**
     * 订单金额
     */
    private BigDecimal amount;
    
    /**
     * 订单状态
     */
    private String status;
    
    /**
     * 是否自动续费
     */
    private Boolean autoRenew;
    
    /**
     * 支付方式
     */
    private String paymentMethod;
    
    /**
     * 第三方交易号
     */
    private String transactionId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 支付时间
     */
    private LocalDateTime paidAt;
    
    /**
     * 订单过期时间
     */
    private LocalDateTime expireAt;
}
