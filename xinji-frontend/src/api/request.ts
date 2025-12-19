import axios, { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'
import type { ApiResponse } from '@/types'

// 创建axios实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    
    // 添加Token
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    
    return config
  },
  (error: AxiosError) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<any>>) => {
    const res = response.data
    
    // 业务状态码处理
    if (res.code !== 200) {
      // 特殊状态码处理
      if (res.code === 401) {
        // 未授权，跳转登录
        const userStore = useUserStore()
        userStore.clearToken()
        router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
        ElMessage.error('登录已过期，请重新登录')
      } else if (res.code === 403) {
        ElMessage.warning(res.message || '无权限访问')
      } else if (res.code === 429) {
        ElMessage.warning(res.message || '请求过于频繁，请稍后重试')
      } else {
        ElMessage.error(res.message || '操作失败')
      }
      
      return Promise.reject(new Error(res.message || '操作失败'))
    }
    
    return response
  },
  (error: AxiosError<ApiResponse<any>>) => {
    console.error('响应错误:', error)
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 401:
          const userStore = useUserStore()
          userStore.clearToken()
          router.push({ name: 'Login' })
          ElMessage.error('登录已过期，请重新登录')
          break
        case 403:
          ElMessage.warning(data?.message || '无权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 429:
          ElMessage.warning(data?.message || '请求过于频繁')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(data?.message || '网络错误')
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络连接失败')
    }
    
    return Promise.reject(error)
  }
)

export default request
