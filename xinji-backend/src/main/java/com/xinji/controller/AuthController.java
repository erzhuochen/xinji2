package com.xinji.controller;

import com.xinji.dto.request.LoginRequest;
import com.xinji.dto.request.SendCodeRequest;
import com.xinji.dto.response.ApiResponse;
import com.xinji.dto.response.LoginResponse;
import com.xinji.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        userService.sendCode(request);
        return ApiResponse.success("验证码已发送");
    }
    
    /**
     * 登录/注册
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ApiResponse.success("登录成功", response);
    }
    
    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // JWT无状态，客户端删除Token即可
        return ApiResponse.success("退出登录成功");
    }
    
    /**
     * 刷新Token
     */
    @PostMapping("/refresh-token")
    public ApiResponse<LoginResponse> refreshToken(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        LoginResponse response = userService.refreshToken(token);
        return ApiResponse.success("Token刷新成功", response);
    }
}
