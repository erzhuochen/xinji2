import request from './request'
import type { ApiResponse, PageResponse, DiaryItem, DiaryDetail, DiaryRequest } from '@/types'

/**
 * 获取日记列表
 */
export const getDiaryList = (params: {
  page?: number
  pageSize?: number
  startDate?: string
  endDate?: string
  keyword?: string
}) => {
  return request.get<ApiResponse<PageResponse<DiaryItem>>>('/diary/list', { params }).then(res => res.data)
}

/**
 * 获取日记详情
 */
export const getDiaryDetail = (id: string) => {
  return request.get<ApiResponse<DiaryDetail>>(`/diary/${id}`).then(res => res.data)
}

/**
 * 创建日记
 */
export const createDiary = (data: DiaryRequest) => {
  return request.post<ApiResponse<DiaryDetail>>('/diary/create', data).then(res => res.data)
}

/**
 * 更新日记
 */
export const updateDiary = (id: string, data: DiaryRequest) => {
  return request.put<ApiResponse<DiaryDetail>>(`/diary/${id}`, data).then(res => res.data)
}

/**
 * 删除日记
 */
export const deleteDiary = (id: string) => {
  return request.delete<ApiResponse<void>>(`/diary/${id}`).then(res => res.data)
}
