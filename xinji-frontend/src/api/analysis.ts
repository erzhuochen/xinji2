import request from './request'
import type { ApiResponse, AnalysisResult } from '@/types'

/**
 * 提交AI分析
 */
export const submitAnalysis = (diaryId: string) => {
  return request.post<ApiResponse<AnalysisResult>>('/analysis/submit', { diaryId }).then(res => res.data)
}

/**
 * 获取分析结果
 */
export const getAnalysisResult = (id: string) => {
  return request.get<ApiResponse<AnalysisResult>>(`/analysis/${id}`).then(res => res.data)
}
