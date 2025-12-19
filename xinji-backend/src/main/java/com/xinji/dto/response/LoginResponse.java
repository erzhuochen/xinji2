package com.xinji.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse {
    
    /**
     * JWT访问令牌
     */
    private String token;
    
    /**
     * 令牌有效期(秒)
     */
    private Long expiresIn;
    
    /**
     * 用户信息
     */
    private UserInfo user;
    
    @Data
    public static class UserInfo {
        private String id;
        private String phone;  // 脱敏后的手机号
        private String memberStatus;
        private LocalDateTime memberExpireTime;
        private LocalDateTime registerTime;
    }
}
