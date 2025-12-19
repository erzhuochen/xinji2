import request from './request'
import type { ApiResponse, WeeklyReport, InsightsReport } from '@/types'

/**
 * 获取周报数据
 */
export const getWeeklyReport = (startDate?: string) => {
  return request.get<ApiResponse<WeeklyReport>>('/report/weekly', { 
    params: startDate ? { startDate } : {} 
  }).then(res => res.data)
}

/**
 * 获取深度洞察报告 (Pro用户)
 */
export const getInsightsReport = (timeRange?: string) => {
  return request.get<ApiResponse<InsightsReport>>('/report/insights', {
    params: timeRange ? { timeRange } : {}
  }).then(res => res.data)
}
