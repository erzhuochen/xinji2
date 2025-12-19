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
    if (!token.value) return
    
    try {
      const res = await getUserProfile()
      if (res.code === 200) {
        userInfo.value = res.data
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
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
    // 方法
    setToken,
    clearToken,
    fetchUserProfile,
    logoutAction,
    incrementUsedQuota,
    checkTokenRefresh
  }
})
