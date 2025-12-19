<template>
  <div class="main-layout">
    <!-- 移动端头部 -->
    <header class="mobile-header">
      <div class="header-left">
        <el-icon v-if="showBackButton" class="back-btn" @click="goBack">
          <ArrowLeft />
        </el-icon>
        <span v-else class="logo">心迹</span>
      </div>
      <h1 class="page-title">{{ pageTitle }}</h1>
      <div class="header-right">
        <slot name="header-right"></slot>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 底部导航 -->
    <nav class="bottom-nav">
      <router-link 
        v-for="item in navItems" 
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
      >
        <el-icon :size="24">
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, EditPen, DataAnalysis, User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 导航项
const navItems = [
  { path: '/diary', label: '日记', icon: EditPen },
  { path: '/report', label: '周报', icon: DataAnalysis },
  { path: '/profile', label: '我的', icon: User }
]

// 页面标题映射
const pageTitleMap: Record<string, string> = {
  '/diary': '我的日记',
  '/diary/edit': '写日记',
  '/diary/detail': '日记详情',
  '/report': '情绪周报',
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
  const noBackPaths = ['/diary', '/report', '/profile']
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
  flex-direction: column;
  background: var(--bg-page);
}

// 移动端头部
.mobile-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 16px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);

  .header-left, .header-right {
    width: 60px;
    display: flex;
    align-items: center;
  }

  .header-right {
    justify-content: flex-end;
  }

  .back-btn {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: var(--bg-secondary);
    }
  }

  .logo {
    font-size: 20px;
    font-weight: 600;
    color: var(--primary-color);
  }

  .page-title {
    font-size: 17px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }
}

// 主内容区
.main-content {
  flex: 1;
  padding-bottom: 70px;
  overflow-y: auto;
}

// 底部导航
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  height: 60px;
  padding-bottom: env(safe-area-inset-bottom);
  background: #fff;
  border-top: 1px solid var(--border-color);

  .nav-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    color: var(--text-tertiary);
    text-decoration: none;
    transition: color 0.2s;

    span {
      font-size: 11px;
    }

    &.active {
      color: var(--primary-color);
    }

    &:hover {
      color: var(--primary-color);
    }
  }
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

// 桌面端适配
@media (min-width: 768px) {
  .main-layout {
    max-width: 480px;
    margin: 0 auto;
    box-shadow: 0 0 40px rgba(0, 0, 0, 0.1);
  }

  .bottom-nav {
    max-width: 480px;
    left: 50%;
    transform: translateX(-50%);
  }
}
</style>
