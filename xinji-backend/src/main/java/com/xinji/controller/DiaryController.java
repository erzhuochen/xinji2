package com.xinji.controller;

import com.xinji.dto.request.DiaryRequest;
import com.xinji.dto.response.ApiResponse;
import com.xinji.dto.response.DiaryResponse;
import com.xinji.dto.response.PageResponse;
import com.xinji.security.SecurityContext;
import com.xinji.service.DiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 日记控制器
 */
@Slf4j
@RestController
@RequestMapping("/diary")
@RequiredArgsConstructor
public class DiaryController {
    
    private final DiaryService diaryService;
    private final SecurityContext securityContext;
    
    /**
     * 创建日记
     */
    @PostMapping("/create")
    public ApiResponse<DiaryResponse> create(@Valid @RequestBody DiaryRequest request) {
        String userId = securityContext.getCurrentUserId();
        DiaryResponse response = diaryService.create(userId, request);
        return ApiResponse.success("创建成功", response);
    }
    
    /**
     * 获取日记列表
     */
    @GetMapping("/list")
    public ApiResponse<PageResponse<DiaryResponse>> list(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword) {
        
        String userId = securityContext.getCurrentUserId();
        PageResponse<DiaryResponse> response = diaryService.list(userId, page, pageSize, startDate, endDate, keyword);
        return ApiResponse.success(response);
    }
    
    /**
     * 获取日记详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DiaryResponse> getById(@PathVariable String id) {
        String userId = securityContext.getCurrentUserId();
        DiaryResponse response = diaryService.getById(userId, id);
        return ApiResponse.success(response);
    }
    
    /**
     * 更新日记
     */
    @PutMapping("/{id}")
    public ApiResponse<DiaryResponse> update(@PathVariable String id, @Valid @RequestBody DiaryRequest request) {
        String userId = securityContext.getCurrentUserId();
        DiaryResponse response = diaryService.update(userId, id, request);
        return ApiResponse.success("更新成功", response);
    }
    
    /**
     * 删除日记
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        String userId = securityContext.getCurrentUserId();
        diaryService.delete(userId, id);
        return ApiResponse.success("删除成功");
    }
}
