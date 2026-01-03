import request from './request'
import type { ApiResponse } from '@/types'

/**
 * 获取近七天的日记记录
 */
export const getRecentDiaries = () => {
  return request.get<ApiResponse<{ count: number }>>('/counselor/recent-diaries').then(res => res.data)
}

/**
 * 发送消息给AI心理咨询师
 */
export const sendCounselorMessage = (message: string) => {
  return request.post<ApiResponse<{ reply: string }>>('/counselor/chat', { message }).then(res => res.data)
}

