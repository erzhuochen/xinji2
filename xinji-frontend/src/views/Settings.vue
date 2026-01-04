<template>
  <div class="settings-page">
    <!-- 账号安全 -->
    <div class="settings-section">
      <h3 class="section-title">账号安全</h3>
      <div class="settings-card">
        <div class="setting-item" @click="showBindPhone">
          <span class="setting-label">手机号</span>
          <div class="setting-value">
            <span>{{ maskPhone(userStore.userInfo?.phone) }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据管理 -->
    <div class="settings-section">
      <h3 class="section-title">数据管理</h3>
      <div class="settings-card">
        <div class="setting-item" @click="clearCache">
          <span class="setting-label">清除缓存</span>
          <div class="setting-value">
            <span class="cache-size">{{ cacheSize }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 关于 -->
    <div class="settings-section">
      <h3 class="section-title">关于</h3>
      <div class="settings-card">
        <div class="setting-item" @click="showAgreement('user')">
          <span class="setting-label">用户协议</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="setting-item" @click="showAgreement('privacy')">
          <span class="setting-label">隐私政策</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="setting-item">
          <span class="setting-label">版本号</span>
          <span class="setting-value">v1.0.0</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const cacheSize = ref('0 KB')

const settings = reactive({})

// 手机号脱敏
const maskPhone = (phone?: string) => {
  if (!phone) return ''
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

// 保存设置
const saveSettings = () => {
  // TODO: 保存设置到后端
  localStorage.setItem('xinjiSettings', JSON.stringify(settings))
  ElMessage.success('设置已保存')
}

// 加载设置
const loadSettings = () => {
  const saved = localStorage.getItem('xinjiSettings')
  if (saved) {
    Object.assign(settings, JSON.parse(saved))
  }
}

// 计算缓存大小
const calcCacheSize = () => {
  let size = 0
  for (let key in localStorage) {
    if (localStorage.hasOwnProperty(key)) {
      size += localStorage[key].length * 2
    }
  }
  
  if (size < 1024) {
    cacheSize.value = size + ' B'
  } else if (size < 1024 * 1024) {
    cacheSize.value = (size / 1024).toFixed(1) + ' KB'
  } else {
    cacheSize.value = (size / 1024 / 1024).toFixed(1) + ' MB'
  }
}

// 绑定手机号
const showBindPhone = () => {
  ElMessage.info('更换手机号功能开发中')
}

// 清除缓存
const clearCache = async () => {
  try {
    await ElMessageBox.confirm(
      '清除缓存后需要重新登录，确定要清除吗？',
      '清除缓存',
      { type: 'warning' }
    )
    
    localStorage.clear()
    sessionStorage.clear()
    ElMessage.success('缓存已清除')
    router.push('/login')
  } catch (error) {
    // 取消
  }
}

// 显示协议
const showAgreement = (type: string) => {
  ElMessage.info(`查看${type === 'user' ? '用户协议' : '隐私政策'}`)
}

onMounted(() => {
  loadSettings()
  calcCacheSize()
})
</script>

<style lang="scss" scoped>
.settings-page {
  padding: 16px;
  padding-bottom: 80px;
}

.settings-section {
  margin-bottom: 24px;

  .section-title {
    font-size: 13px;
    color: var(--text-secondary);
    margin: 0 0 8px;
    padding-left: 8px;
  }
}

.settings-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;

  .setting-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px;
    border-bottom: 1px solid var(--border-color);
    cursor: pointer;

    &:last-child {
      border-bottom: none;
    }

    &.danger {
      .setting-label {
        color: var(--error-color);
      }
    }

    .setting-label {
      font-size: 15px;
      color: var(--text-primary);
    }

    .setting-value {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      color: var(--text-secondary);

      .cache-size {
        color: var(--text-tertiary);
      }

      .el-icon {
        color: var(--text-tertiary);
      }
    }

    .el-switch {
      --el-switch-on-color: var(--primary-color);
    }

    .el-time-picker {
      width: 100px;
    }
  }
}
</style>
