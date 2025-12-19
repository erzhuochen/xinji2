package com.xinji.security;

import com.xinji.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全上下文工具类
 * 获取当前登录用户信息
 */
@Component
public class SecurityContext {
    
    /**
     * 获取当前登录用户
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }
    
    /**
     * 获取当前登录用户ID
     */
    public String getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }
    
    /**
     * 判断当前用户是否为Pro会员
     */
    public boolean isProMember() {
        User user = getCurrentUser();
        return user != null && "PRO".equals(user.getMemberStatus());
    }
    
    /**
     * 判断是否已登录
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               authentication.getPrincipal() instanceof User;
    }
}
