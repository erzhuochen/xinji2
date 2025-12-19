import request from './request'
import type { ApiResponse, LoginResponse, UserProfile } from '@/types'

/**
 * 发送验证码
 */
export const sendCode = (phone: string) => {
  return request.post<ApiResponse<void>>('/auth/send-code', { phone })
}

/**
 * 登录/注册
 */
export const login = (phone: string, code: string) => {
  return request.post<ApiResponse<LoginResponse>>('/auth/login', { phone, code })
}

/**
 * 退出登录
 */
export const logout = () => {
  return request.post<ApiResponse<void>>('/auth/logout')
}

/**
 * 刷新Token
 */
export const refreshToken = () => {
  return request.post<ApiResponse<LoginResponse>>('/auth/refresh-token')
}

/**
 * 获取用户信息
 */
export const getUserProfile = () => {
  return request.get<ApiResponse<UserProfile>>('/user/profile').then(res => res.data)
}

/**
 * 更新用户信息
 */
export const updateProfile = (data: { nickname?: string; avatar?: string }) => {
  return request.put<ApiResponse<void>>('/user/profile', data)
}

/**
 * 导出用户数据
 */
export const exportUserData = () => {
  return request.post<ApiResponse<{ exportUrl: string; expiresAt: string }>>('/user/export')
}

/**
 * 注销账号
 */
export const deleteAccount = (phone: string, code: string, confirmText: string) => {
  return request.post<ApiResponse<void>>('/user/delete', { phone, code, confirmText })
}
