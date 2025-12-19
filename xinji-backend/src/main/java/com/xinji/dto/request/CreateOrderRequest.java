package com.xinji.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建订单请求DTO
 */
@Data
public class CreateOrderRequest {
    
    @NotBlank(message = "套餐类型不能为空")
    @Pattern(regexp = "^(MONTHLY|QUARTERLY|ANNUAL)$", message = "套餐类型无效")
    private String planType;
    
    /**
     * 是否自动续费
     */
    private Boolean autoRenew = false;
}
