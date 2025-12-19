package com.xinji.controller;

import com.xinji.dto.response.ApiResponse;
import com.xinji.dto.response.InsightsReportResponse;
import com.xinji.dto.response.WeeklyReportResponse;
import com.xinji.security.SecurityContext;
import com.xinji.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 报告控制器
 */
@Slf4j
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {
    
    private final ReportService reportService;
    private final SecurityContext securityContext;
    
    /**
     * 获取周报数据
     */
    @GetMapping("/weekly")
    public ApiResponse<WeeklyReportResponse> getWeeklyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        String userId = securityContext.getCurrentUserId();
        WeeklyReportResponse response = reportService.getWeeklyReport(userId, startDate);
        return ApiResponse.success(response);
    }
    
    /**
     * 获取深度洞察报告(Pro用户专属)
     */
    @GetMapping("/insights")
    @PreAuthorize("hasRole('PRO')")
    public ApiResponse<InsightsReportResponse> getInsightsReport(
            @RequestParam(required = false, defaultValue = "month") String timeRange) {
        String userId = securityContext.getCurrentUserId();
        InsightsReportResponse response = reportService.getInsightsReport(userId, timeRange);
        return ApiResponse.success(response);
    }
}
