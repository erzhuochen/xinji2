package com.xinji.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 */
@Data
public class UserProfileResponse {
    
    private String id;
    
    /**
     * 脱敏手机号
     */
    private String phone;
    
    private String nickname;
    
    private String avatar;
    
    /**
     * 会员状态
     */
    private String memberStatus;
    
    /**
     * 会员到期时间
     */
    private LocalDateTime memberExpireTime;
    
    /**
     * 注册时间
     */
    private LocalDateTime registerTime;
    
    /**
     * 累计日记数量
     */
    private Integer diaryCount;
    
    /**
     * 今日AI分析配额
     */
    private Integer todayAiQuota;
    
    /**
     * 今日已使用配额
     */
    private Integer usedAiQuota;
}
