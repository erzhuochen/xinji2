package com.xinji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水实体类 - MySQL
 */
@Data
@TableName("t_payment_record")
public class PaymentRecord {
    
    /**
     * 流水号
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    /**
     * 订单ID
     */
    private String orderId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 支付方式: WECHAT-微信支付
     */
    private String paymentMethod;
    
    /**
     * 支付金额
     */
    private BigDecimal amount;
    
    /**
     * 第三方交易号
     */
    private String transactionId;
    
    /**
     * 支付状态: UNPAID-未支付, PAID-已支付, REFUNDING-退款中, REFUNDED-已退款
     */
    private String status;
    
    /**
     * 支付时间
     */
    private LocalDateTime paidAt;
    
    /**
     * 回调数据(JSON)
     */
    private String callbackData;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
