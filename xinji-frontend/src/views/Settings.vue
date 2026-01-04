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
        <div class="setting-item danger" @click="deleteAccount">
          <span class="setting-label">注销账号</span>
          <el-icon><ArrowRight /></el-icon>
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

    <!-- 注销账号弹窗 -->
    <el-dialog
      v-model="showDeleteDialog"
      title="注销账号"
      width="320px"
      :close-on-click-modal="false"
    >
      <div class="delete-dialog">
        <el-alert
          title="注销账号后，您的所有数据将被永久删除且无法恢复！"
          type="error"
          :closable="false"
          show-icon
        />
        
        <el-form :model="deleteForm" label-position="top" class="delete-form">
          <el-form-item label="手机号">
            <el-input v-model="deleteForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="验证码">
            <div class="code-input-group">
              <el-input v-model="deleteForm.code" placeholder="请输入验证码" />
              <el-button 
                :disabled="deleteCountdown > 0"
                @click="sendDeleteCode"
              >
                {{ deleteCountdown > 0 ? `${deleteCountdown}s` : '发送' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="请输入「确认注销」">
            <el-input v-model="deleteForm.confirmText" placeholder="确认注销" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showDeleteDialog = false">取消</el-button>
        <el-button 
          type="danger" 
          :loading="deleting"
          :disabled="!canDelete"
          @click="confirmDelete"
        >
          确认注销
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { sendCode, deleteAccount as deleteAccountApi } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

const cacheSize = ref('0 KB')
const showDeleteDialog = ref(false)
const deleting = ref(false)
const deleteCountdown = ref(0)

const settings = reactive({})

const deleteForm = reactive({
  phone: '',
  code: '',
  confirmText: ''
})

// 是否可以注销
const canDelete = computed(() => {
  return deleteForm.phone && 
         deleteForm.code && 
         deleteForm.confirmText === '确认注销'
})

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

// 打开注销弹窗
const deleteAccount = () => {
  deleteForm.phone = ''
  deleteForm.code = ''
  deleteForm.confirmText = ''
  showDeleteDialog.value = true
}

// 发送注销验证码
const sendDeleteCode = async () => {
  if (!deleteForm.phone) {
    ElMessage.warning('请输入手机号')
    return
  }

  try {
    await sendCode(deleteForm.phone)
    ElMessage.success('验证码已发送')
    
    deleteCountdown.value = 60
    const timer = setInterval(() => {
      deleteCountdown.value--
      if (deleteCountdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

// 确认注销
const confirmDelete = async () => {
  if (!canDelete.value) return
  
  deleting.value = true
  try {
    await deleteAccountApi(deleteForm.phone, deleteForm.code, deleteForm.confirmText)
    ElMessage.success('账号已注销')
    userStore.clearToken()
    router.push('/login')
  } catch (error) {
    console.error('注销失败:', error)
  } finally {
    deleting.value = false
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

// 注销弹窗
.delete-dialog {
  .el-alert {
    margin-bottom: 20px;
  }

  .delete-form {
    .code-input-group {
      display: flex;
      gap: 8px;

      .el-input {
        flex: 1;
      }
    }
  }
}
</style>
