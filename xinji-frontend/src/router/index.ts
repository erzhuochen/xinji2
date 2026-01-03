import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'
import NProgress from 'nprogress'
import { ElMessage } from 'element-plus'
import 'nprogress/nprogress.css'

// 配置进度条
NProgress.configure({ showSpinner: false })

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/MainLayout.vue'),
    redirect: '/diary',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'diary',
        name: 'DiaryList',
        component: () => import('@/views/DiaryList.vue'),
        meta: { title: '我的日记', keepAlive: true }
      },
      {
        path: 'diary/edit/:id?',
        name: 'DiaryEdit',
        component: () => import('@/views/DiaryEdit.vue'),
        meta: { title: '写日记' }
      },
      {
        path: 'diary/:id',
        name: 'DiaryDetail',
        component: () => import('@/views/DiaryDetail.vue'),
        meta: { title: '日记详情' }
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/Report.vue'),
        meta: { title: '情绪周报', keepAlive: true }
      },
      {
        path: 'mental-training',
        name: 'MentalTraining',
        component: () => import('@/views/MentalTraining.vue'),
        meta: { title: '心理训练', keepAlive: true }
      },
      {
        path: 'mental-training/:id',
        name: 'MentalTrainingDetail',
        component: () => import('@/views/MentalTrainingDetail.vue'),
        meta: { title: '心理训练详情' }
      },
      {
        path: 'insights',
        name: 'Insights',
        component: () => import('@/views/Insights.vue'),
        meta: { title: '深度洞察', requiresPro: true }
      },
      {
        path: 'ai-counselor',
        name: 'AICounselor',
        component: () => import('@/views/AICounselor.vue'),
        meta: { title: 'AI心理咨询师', requiresPro: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'membership',
        name: 'Membership',
        component: () => import('@/views/Membership.vue'),
        meta: { title: '会员中心' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '设置' }
      },
      {
        path: 'cheer',
        name: 'CheerStation',
        component: () => import('@/views/CheerStation.vue'),
        meta: { title: '心灵加油站' }
      },
      {
        path: 'mindfulness/breathing',
        name: 'MindfulnessBreathing',
        component: () => import('@/views/mindfulness/BreathingMeditation.vue'),
        meta: { title: '呼吸冥想' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  NProgress.start()
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 心迹` : '心迹'
  
  const userStore = useUserStore()
  const token = userStore.token
  
  // 检查是否需要登录
  if (to.meta.requiresAuth !== false) {
    // 如果没有token，直接跳转登录页
    if (!token) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
    
    // 如果有token，验证token是否有效（通过获取用户信息）
    // 如果用户信息未加载或需要刷新，则先加载
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserProfile()
        // 如果获取用户信息失败（token无效），fetchUserProfile会抛出错误
      } catch (error) {
        console.error('路由守卫中获取用户信息失败:', error)
        userStore.clearToken()
        next({ name: 'Login', query: { redirect: to.fullPath } })
        return
      }
    }
  }
  
  // 已登录用户访问登录页，跳转首页
  if (to.name === 'Login' && token) {
    // 验证token是否有效
    if (!userStore.userInfo) {
      try {
        await userStore.fetchUserProfile()
      } catch (error) {
        // token无效，清除并允许访问登录页
        userStore.clearToken()
        next()
        return
      }
    }
    next({ path: '/' })
    return
  }

  // 检查是否需要Pro权限
  if (to.meta.requiresPro && !userStore.isPro) {
    ElMessage.warning('此功能仅限Pro会员使用')
    next({ name: 'Membership' })
    return
  }
  
  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
