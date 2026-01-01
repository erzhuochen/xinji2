package com.xinji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类 - MySQL
 */
@Data
@TableName("t_order")
public class Order {
    
    /**
     * 订单ID
     */
    @TableId(type = IdType.INPUT)
    private String id;

    private String orderNo;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 套餐类型: MONTHLY-月卡, QUARTERLY-季卡, ANNUAL-年卡
     */
    private String planType;
    
    /**
     * 订单金额(元)
     */
    private BigDecimal amount;
    
    /**
     * 订单状态: PENDING-待支付, PAID-已支付, CANCELLED-已取消, REFUNDED-已退款, EXPIRED-已过期
     */
    private String status;
    
    /**
     * 是否自动续费
     */
    private Integer autoRenew;
    
    /**
     * 支付方式
     */
    private String paymentMethod;
    
    /**
     * 第三方交易号
     */
    private String transactionId;
    
    /**
     * 支付时间
     */
    private LocalDateTime paidAt;
    
    /**
     * 订单过期时间(15分钟)
     */
    private LocalDateTime expireAt;

    private int deleted;
    
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
