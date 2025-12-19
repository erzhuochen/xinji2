package com.xinji.service;

import com.xinji.dto.request.DeleteAccountRequest;
import com.xinji.dto.request.LoginRequest;
import com.xinji.dto.request.SendCodeRequest;
import com.xinji.dto.request.UpdateProfileRequest;
import com.xinji.dto.response.LoginResponse;
import com.xinji.dto.response.UserProfileResponse;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 发送验证码
     */
    void sendCode(SendCodeRequest request);
    
    /**
     * 登录/注册
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 刷新Token
     */
    LoginResponse refreshToken(String oldToken);
    
    /**
     * 获取用户信息
     */
    UserProfileResponse getProfile(String userId);
    
    /**
     * 更新用户信息
     */
    void updateProfile(String userId, UpdateProfileRequest request);
    
    /**
     * 导出用户数据
     */
    String exportData(String userId);
    
    /**
     * 注销账号
     */
    void deleteAccount(String userId, DeleteAccountRequest request);
}
