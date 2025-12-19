// 通用类型
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface PageResponse<T> {
  total: number
  page: number
  pageSize: number
  list: T[]
}

// 用户相关
export interface UserProfile {
  id: string
  phone: string
  nickname?: string
  avatar?: string
  memberStatus: 'FREE' | 'PRO'
  memberExpireTime?: string
  registerTime: string
  diaryCount: number
  todayAiQuota: number
  usedAiQuota: number
}

export interface LoginResponse {
  token: string
  expiresIn: number
  user: {
    id: string
    phone: string
    memberStatus: string
    memberExpireTime?: string
    registerTime: string
  }
}

// 日记相关
export interface DiaryItem {
  id: string
  userId: string
  title?: string
  preview: string
  isDraft: boolean
  diaryDate: string
  createdAt: string
  updatedAt: string
  analyzed: boolean
  analysisId?: string
  emotion?: {
    primary: string
    intensity: number
  }
}

export interface DiaryDetail extends DiaryItem {
  content: string
}

export interface DiaryRequest {
  title?: string
  content: string
  isDraft?: boolean
  diaryDate?: string
}

// AI分析相关
export interface AnalysisResult {
  id: string
  diaryId: string
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED'
  emotions?: Record<string, number>
  primaryEmotion?: string
  emotionIntensity?: number
  keywords?: string[]
  cognitiveDistortions?: {
    type: string
    description: string
  }[]
  suggestions?: string[]
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH'
  analyzedAt?: string
}

// 报告相关
export interface WeeklyReport {
  weekStart: string
  weekEnd: string
  diaryCount: number
  analyzedCount: number
  emotionTrend: {
    date: string
    emotion: string
    intensity: number
  }[]
  emotionDistribution: Record<string, number>
  averageIntensity?: number
  mostFrequentEmotion?: string
  keywords: string[]
  summary?: string
}

export interface InsightsReport {
  timeRange: string
  startDate: string
  endDate: string
  insights: {
    type: string
    title: string
    content: string
    confidence: number
  }[]
  growthPlan: string[]
  emotionForecast: {
    nextWeekRisk: string
    triggers: string[]
  }
  mindfulnessSuggestions: {
    title: string
    duration: string
    url: string
  }[]
}

// 订单相关
export interface Order {
  orderId: string
  userId: string
  planType: 'MONTHLY' | 'QUARTERLY' | 'ANNUAL'
  amount: number
  status: 'PENDING' | 'PAID' | 'CANCELLED' | 'REFUNDED' | 'EXPIRED'
  autoRenew: boolean
  paymentMethod?: string
  transactionId?: string
  createdAt: string
  paidAt?: string
  expireAt: string
}

export interface WechatPrepay {
  prepayId: string
  appId: string
  timeStamp: string
  nonceStr: string
  packageValue: string
  signType: string
  paySign: string
}

// 情绪类型映射
export const EmotionMap: Record<string, string> = {
  HAPPY: '快乐',
  SAD: '悲伤',
  ANGRY: '愤怒',
  FEAR: '恐惧',
  SURPRISE: '惊讶',
  DISGUST: '厌恶',
  NEUTRAL: '平静',
  ANXIOUS: '焦虑'
}

// 情绪颜色映射
export const EmotionColorMap: Record<string, string> = {
  HAPPY: '#FFD93D',
  SAD: '#7986CB',
  ANGRY: '#EF5350',
  FEAR: '#9575CD',
  SURPRISE: '#FF8A65',
  DISGUST: '#8D6E63',
  NEUTRAL: '#90A4AE',
  ANXIOUS: '#FFB74D'
}

// 套餐类型映射
export const PlanTypeMap: Record<string, string> = {
  MONTHLY: '月卡',
  QUARTERLY: '季卡',
  ANNUAL: '年卡'
}
