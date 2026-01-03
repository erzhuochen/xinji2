<template>
  <div class="main-layout">
    <!-- 侧边导航 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1 class="logo">心迹</h1>
        <p class="logo-subtitle">AI心理成长助手</p>
      </div>
      
      <nav class="sidebar-nav">
        <router-link 
          v-for="item in navItems" 
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <el-icon :size="20">
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
      
      <div class="sidebar-footer">
        <div class="user-info" v-if="userStore.userInfo">
          <el-avatar :size="40" :src="userStore.userInfo.avatar">
            {{ userStore.userInfo.nickname?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="user-meta">
            <span class="nickname">{{ userStore.userInfo.nickname || '用户' }}</span>
            <el-tag size="small" :type="userStore.isPro ? 'warning' : 'info'">
              {{ userStore.isPro ? 'PRO会员' : '免费版' }}
            </el-tag>
          </div>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="main-wrapper">
      <!-- 顶部导航栏 -->
      <header class="top-header">
        <div class="header-left">
          <el-icon v-if="showBackButton" class="back-btn" @click="goBack">
            <ArrowLeft />
          </el-icon>
          <h2 class="page-title">{{ pageTitle }}</h2>
        </div>
        <div class="header-right">
          <slot name="header-right"></slot>
        </div>
      </header>

      <!-- 内容区域 -->
      <main class="main-content">
        <router-view v-slot="{ Component, route }">
          <transition name="fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, EditPen, DataAnalysis, User, Setting, Medal, Connection } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 初始化用户信息
onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    await userStore.fetchUserProfile()
  }
})

// 导航项
const navItems = [
  { path: '/diary', label: '我的日记', icon: EditPen },
  { path: '/report', label: '情绪周报', icon: DataAnalysis },
  { path: '/mental-training', label: '心理训练', icon: Connection },
  { path: '/insights', label: '深度洞察', icon: Medal },
  { path: '/profile', label: '个人中心', icon: User },
  { path: '/settings', label: '设置', icon: Setting }
]

// 页面标题映射
const pageTitleMap: Record<string, string> = {
  '/diary': '我的日记',
  '/diary/edit': '写日记',
  '/diary/detail': '日记详情',
  '/report': '情绪周报',
  '/mental-training': '心理训练',
  '/insights': '深度洞察',
  '/profile': '个人中心',
  '/membership': '会员中心',
  '/settings': '设置'
}

// 当前页面标题
const pageTitle = computed(() => {
  const path = route.path
  // 精确匹配
  if (pageTitleMap[path]) return pageTitleMap[path]
  // 前缀匹配
  const prefix = Object.keys(pageTitleMap).find(key => path.startsWith(key) && key !== '/')
  return prefix ? pageTitleMap[prefix] : '心迹'
})

// 是否显示返回按钮
const showBackButton = computed(() => {
  const noBackPaths = ['/diary', '/report', '/mental-training', '/profile']
  // 如果是心理训练详情页，不显示返回按钮（因为详情页有自己的返回按钮）
  if (route.path.startsWith('/mental-training/')) {
    return false
  }
  return !noBackPaths.includes(route.path)
})

// 返回上一页
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/diary')
  }
}

// 判断导航是否激活
const isActive = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<style lang="scss" scoped>
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: row;
  background: #F8F9FA;
}

// 侧边栏
.sidebar {
  width: 240px;
  min-width: 240px;
  height: 100vh;
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #E9ECEF;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.03);

  .sidebar-header {
    padding: 24px;
    border-bottom: 1px solid #E9ECEF;

    .logo {
      font-size: 24px;
      font-weight: 700;
      color: #6B8DD6;
      margin: 0;
    }

    .logo-subtitle {
      font-size: 12px;
      color: #8E9AAF;
      margin: 4px 0 0;
    }
  }

  .sidebar-nav {
    flex: 1;
    padding: 16px 12px;
    overflow-y: auto;

    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      margin-bottom: 4px;
      border-radius: 8px;
      color: #6C757D;
      text-decoration: none;
      font-size: 15px;
      transition: all 0.2s ease;

      &:hover {
        background: #F8F9FA;
        color: #6B8DD6;
      }

      &.active {
        background: linear-gradient(135deg, #6B8DD6 0%, #8BA4E0 100%);
        color: #fff;
        font-weight: 500;

        .el-icon {
          color: #fff;
        }
      }
    }
  }

  .sidebar-footer {
    padding: 16px;
    border-top: 1px solid #E9ECEF;

    .user-info {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px;
      border-radius: 8px;
      cursor: pointer;
      transition: background 0.2s;

      &:hover {
        background: #F8F9FA;
      }

      .user-meta {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .nickname {
          font-size: 14px;
          font-weight: 500;
          color: #2C3E50;
        }
      }
    }
  }
}

// 主内容区包装器
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

// 顶部导航栏
.top-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #E9ECEF;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .back-btn {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 8px;
      cursor: pointer;
      transition: background 0.2s;
      color: #6C757D;

      &:hover {
        background: #F8F9FA;
        color: #6B8DD6;
      }
    }

    .page-title {
      font-size: 18px;
      font-weight: 600;
      color: #2C3E50;
      margin: 0;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

// 主内容区
.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: #F8F9FA;
}

// 页面切换动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

// 响应式：小屏幕下侧边栏隐藏
@media (max-width: 1024px) {
  .sidebar {
    width: 200px;
    min-width: 200px;
  }
}

@media (max-width: 768px) {
  .main-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    min-width: 100%;
    height: auto;
    position: relative;
    flex-direction: row;
    border-right: none;
    border-bottom: 1px solid #E9ECEF;

    .sidebar-header {
      display: none;
    }

    .sidebar-nav {
      display: flex;
      flex-direction: row;
      padding: 8px;
      overflow-x: auto;

      .nav-item {
        flex-shrink: 0;
        padding: 8px 16px;
        margin-bottom: 0;
        margin-right: 4px;
      }
    }

    .sidebar-footer {
      display: none;
    }
  }

  .main-content {
    padding: 16px;
  }
}
</style>
