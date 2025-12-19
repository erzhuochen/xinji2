package com.xinji.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 支付请求DTO
 */
@Data
public class PaymentRequest {
    
    @NotBlank(message = "订单ID不能为空")
    private String orderId;
}
