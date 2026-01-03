package com.xinji.service;

import com.xinji.dto.response.InsightsReportResponse;
import com.xinji.dto.response.WeeklyReportResponse;

import java.time.LocalDate;

/**
 * 报告服务接口
 */
public interface ReportService {
    
    /**
     * 获取周报数据
     */
    WeeklyReportResponse getWeeklyReport(String userId, LocalDate startDate);
    
    /**
     * 获取深度洞察报告(Pro用户)
     */
    InsightsReportResponse getInsightsReport(String userId, String timeRange);
    
    /**
     * 生成用户周报(定时任务调用)
     */
    void generateWeeklyReports();

    /**
     * 触发指定日期的周报更新（异步）
     */
    void triggerWeeklyReportRefresh(String userId, LocalDate date);
}
