package com.xinji.controller;

import com.xinji.dto.request.DeleteAccountRequest;
import com.xinji.dto.request.UpdateProfileRequest;
import com.xinji.dto.response.ApiResponse;
import com.xinji.dto.response.UserProfileResponse;
import com.xinji.security.SecurityContext;
import com.xinji.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final SecurityContext securityContext;
    
    /**
     * 获取用户信息
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile() {
        String userId = securityContext.getCurrentUserId();
        UserProfileResponse response = userService.getProfile(userId);
        return ApiResponse.success(response);
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        String userId = securityContext.getCurrentUserId();
        userService.updateProfile(userId, request);
        return ApiResponse.success("更新成功");
    }
    
    /**
     * 导出用户数据
     */
    @PostMapping("/export")
    public ApiResponse<Map<String, Object>> exportData() {
        String userId = securityContext.getCurrentUserId();
        String exportUrl = userService.exportData(userId);
        return ApiResponse.success("导出成功", Map.of(
                "exportUrl", exportUrl,
                "expiresAt", java.time.LocalDateTime.now().plusDays(7)
        ));
    }
    
    /**
     * 注销账号
     */
    @PostMapping("/delete")
    public ApiResponse<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        String userId = securityContext.getCurrentUserId();
        userService.deleteAccount(userId, request);
        return ApiResponse.success("账号已删除");
    }
}
