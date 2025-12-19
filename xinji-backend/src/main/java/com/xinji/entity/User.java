package com.xinji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类 - MySQL
 */
@Data
@TableName("t_user")
public class User {
    
    /**
     * 用户ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    /**
     * 手机号(AES加密存储)
     */
    private String phone;
    
    /**
     * 手机号哈希(用于查询)
     */
    private String phoneHash;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 会员状态: FREE-免费用户, PRO-付费会员
     */
    private String memberStatus;
    
    /**
     * 会员到期时间
     */
    private LocalDateTime memberExpireTime;
    
    /**
     * 注册时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime registerTime;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
    
    /**
     * 今日AI分析已用次数
     */
    @TableField(exist = false)
    private Integer usedAiQuota;
    
    /**
     * 今日AI分析配额
     */
    @TableField(exist = false)
    private Integer todayAiQuota;
    
    /**
     * 日记数量
     */
    @TableField(exist = false)
    private Integer diaryCount;
    
    /**
     * 是否删除: 0-否, 1-是
     */
    @TableLogic
    private Integer deleted;
    
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
