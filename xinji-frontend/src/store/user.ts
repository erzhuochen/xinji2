import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserProfile, logout } from '@/api/user'
import type { UserProfile } from '@/types'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserProfile | null>(null)
  
  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isPro = computed(() => userInfo.value?.memberStatus === 'PRO')
  const todayAiQuota = computed(() => userInfo.value?.todayAiQuota || 5)
  const usedAiQuota = computed(() => userInfo.value?.usedAiQuota || 0)
  const remainingAiQuota = computed(() => Math.max(0, todayAiQuota.value - usedAiQuota.value))
  const diaryCount = computed(() => userInfo.value?.diaryCount || 0)
  const remainingQuota = computed(() => remainingAiQuota.value) // 别名
  
  // 方法
  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }
  
  const clearToken = () => {
    token.value = ''
    localStorage.removeItem('token')
    userInfo.value = null
  }
  
  const fetchUserProfile = async () => {
    if (!token.value) {
      throw new Error('未登录')
    }
    
    try {
      const res = await getUserProfile()
      if (res.code === 200) {
        userInfo.value = res.data
      } else {
        throw new Error(res.message || '获取用户信息失败')
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      // 重新抛出错误，让调用者知道token无效
      throw error
    }
  }
  
  const logoutAction = async () => {
    try {
      await logout()
    } catch (error) {
      // 忽略登出错误
    } finally {
      clearToken()
    }
  }
  
  const incrementUsedQuota = () => {
    if (userInfo.value) {
      userInfo.value.usedAiQuota = (userInfo.value.usedAiQuota || 0) + 1
    }
  }
  
  // 检查Token是否需要刷新
  const checkTokenRefresh = () => {
    // Token在过期前1天自动刷新
    // 这里简化处理，可以在API拦截器中实现
  }
  
  return {
    // 状态
    token,
    userInfo,
    // 计算属性
    isLoggedIn,
    isPro,
    todayAiQuota,
    usedAiQuota,
    remainingAiQuota,
    diaryCount,
    remainingQuota,
    // 方法
    setToken,
    clearToken,
    fetchUserProfile,
    logoutAction,
    incrementUsedQuota,
    checkTokenRefresh
  }
})
