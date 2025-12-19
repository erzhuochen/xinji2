<template>
  <div class="profile-page">
    <!-- 用户信息卡片 -->
    <div class="profile-card">
      <div class="avatar-section">
        <el-avatar :size="72" :src="userStore.userInfo?.avatar">
          {{ userStore.userInfo?.nickname?.charAt(0) || '用' }}
        </el-avatar>
        <el-upload
          class="avatar-uploader"
          action=""
          :show-file-list="false"
          :before-upload="handleAvatarUpload"
        >
          <el-button type="primary" link size="small">更换头像</el-button>
        </el-upload>
      </div>

      <div class="info-section">
        <div class="info-item" @click="editNickname">
          <label>昵称</label>
          <div class="info-value">
            <span>{{ userStore.userInfo?.nickname || '未设置' }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
        <div class="info-item">
          <label>手机号</label>
          <div class="info-value">
            <span>{{ maskPhone(userStore.userInfo?.phone) }}</span>
          </div>
        </div>
        <div class="info-item">
          <label>注册时间</label>
          <div class="info-value">
            <span>{{ formatDate(userStore.userInfo?.registerTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 会员状态 -->
    <div class="membership-card" @click="goMembership">
      <div class="membership-info">
        <div class="status-badge" :class="{ pro: userStore.isPro }">
          {{ userStore.isPro ? 'PRO' : 'FREE' }}
        </div>
        <div class="membership-text">
          <span class="title">
            {{ userStore.isPro ? 'PRO会员' : '免费版' }}
          </span>
          <span class="desc" v-if="userStore.isPro">
            有效期至 {{ formatDate(userStore.userInfo?.memberExpireTime) }}
          </span>
          <span class="desc" v-else>
            升级PRO解锁更多功能
          </span>
        </div>
      </div>
      <el-icon><ArrowRight /></el-icon>
    </div>

    <!-- 统计信息 -->
    <div class="stats-card">
      <div class="stat-item">
        <span class="stat-value">{{ userStore.diaryCount }}</span>
        <span class="stat-label">日记总数</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-value">{{ userStore.remainingQuota }}</span>
        <span class="stat-label">今日AI额度</span>
      </div>
    </div>

    <!-- 功能菜单 -->
    <div class="menu-card">
      <div class="menu-item" @click="goSettings">
        <el-icon><Setting /></el-icon>
        <span>设置</span>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </div>
      <div class="menu-item" @click="exportData">
        <el-icon><Download /></el-icon>
        <span>导出数据</span>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </div>
      <div class="menu-item" @click="showFeedback">
        <el-icon><ChatDotRound /></el-icon>
        <span>意见反馈</span>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </div>
      <div class="menu-item" @click="showAbout">
        <el-icon><InfoFilled /></el-icon>
        <span>关于心迹</span>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 退出登录 -->
    <div class="logout-section">
      <el-button @click="handleLogout" type="danger" plain>
        退出登录
      </el-button>
    </div>

    <!-- 修改昵称弹窗 -->
    <el-dialog
      v-model="showNicknameDialog"
      title="修改昵称"
      width="320px"
    >
      <el-input 
        v-model="newNickname" 
        placeholder="请输入新昵称"
        maxlength="20"
        show-word-limit
      />
      <template #footer>
        <el-button @click="showNicknameDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveNickname">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Setting, Download, ChatDotRound, InfoFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { updateProfile, exportUserData, logout } from '@/api/user'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

const showNicknameDialog = ref(false)
const newNickname = ref('')
const saving = ref(false)

// 手机号脱敏
const maskPhone = (phone?: string) => {
  if (!phone) return ''
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

// 格式化日期
const formatDate = (date?: string) => {
  if (!date) return ''
  return dayjs(date).format('YYYY-MM-DD')
}

// 编辑昵称
const editNickname = () => {
  newNickname.value = userStore.userInfo?.nickname || ''
  showNicknameDialog.value = true
}

// 保存昵称
const saveNickname = async () => {
  if (!newNickname.value.trim()) {
    ElMessage.warning('请输入昵称')
    return
  }

  saving.value = true
  try {
    await updateProfile({ nickname: newNickname.value })
    await userStore.fetchUserInfo()
    showNicknameDialog.value = false
    ElMessage.success('昵称已更新')
  } catch (error) {
    console.error('更新昵称失败:', error)
  } finally {
    saving.value = false
  }
}

// 头像上传
const handleAvatarUpload = async (file: File) => {
  // TODO: 实现上传到OSS
  ElMessage.info('头像上传功能开发中')
  return false
}

// 跳转会员中心
const goMembership = () => {
  router.push('/membership')
}

// 跳转设置
const goSettings = () => {
  router.push('/settings')
}

// 导出数据
const exportData = async () => {
  try {
    await ElMessageBox.confirm(
      '导出将包含您的所有日记数据，确定要导出吗？',
      '导出数据',
      { type: 'info' }
    )

    const res = await exportUserData()
    ElMessage.success('导出成功，下载链接有效期24小时')
    
    // 打开下载链接
    window.open(res.data.data.exportUrl, '_blank')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('导出失败:', error)
    }
  }
}

// 意见反馈
const showFeedback = () => {
  ElMessage.info('意见反馈功能开发中')
}

// 关于
const showAbout = () => {
  ElMessageBox.alert(
    '<div style="text-align: center;">' +
    '<p>心迹 v1.0.0</p>' +
    '<p style="color: #999; font-size: 12px;">基于AI的个人心理成长服务</p>' +
    '</div>',
    '关于心迹',
    { dangerouslyUseHTMLString: true }
  )
}

// 退出登录
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '退出登录',
      { type: 'warning' }
    )

    await logout()
    userStore.clearToken()
    router.push('/login')
    ElMessage.success('已退出登录')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出登录失败:', error)
    }
  }
}
</script>

<style lang="scss" scoped>
.profile-page {
  padding: 16px;
  padding-bottom: 80px;
}

// 用户信息卡片
.profile-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px 20px;
  margin-bottom: 16px;

  .avatar-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 24px;

    .el-avatar {
      background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
      font-size: 28px;
      font-weight: 500;
    }

    .avatar-uploader {
      margin-top: 8px;
    }
  }

  .info-section {
    .info-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid var(--border-color);
      cursor: pointer;

      &:last-child {
        border-bottom: none;
      }

      label {
        font-size: 14px;
        color: var(--text-secondary);
      }

      .info-value {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        color: var(--text-primary);

        .el-icon {
          color: var(--text-tertiary);
        }
      }
    }
  }
}

// 会员卡片
.membership-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  color: #fff;
  cursor: pointer;

  .membership-info {
    display: flex;
    align-items: center;
    gap: 12px;

    .status-badge {
      background: rgba(255, 255, 255, 0.2);
      padding: 4px 12px;
      border-radius: 16px;
      font-size: 12px;
      font-weight: 600;

      &.pro {
        background: #FFD93D;
        color: #333;
      }
    }

    .membership-text {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .title {
        font-size: 16px;
        font-weight: 600;
      }

      .desc {
        font-size: 12px;
        opacity: 0.8;
      }
    }
  }

  .el-icon {
    font-size: 20px;
    opacity: 0.8;
  }
}

// 统计卡片
.stats-card {
  display: flex;
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;

  .stat-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: var(--primary-color);
    }

    .stat-label {
      font-size: 12px;
      color: var(--text-secondary);
    }
  }

  .stat-divider {
    width: 1px;
    background: var(--border-color);
    margin: 0 16px;
  }
}

// 菜单卡片
.menu-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 16px;

  .menu-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 20px;
    border-bottom: 1px solid var(--border-color);
    cursor: pointer;
    transition: background 0.2s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: var(--bg-secondary);
    }

    .el-icon {
      font-size: 20px;
      color: var(--text-secondary);
    }

    span {
      flex: 1;
      font-size: 14px;
      color: var(--text-primary);
    }

    .arrow {
      color: var(--text-tertiary);
    }
  }
}

// 退出登录
.logout-section {
  text-align: center;
  padding: 20px;

  .el-button {
    width: 100%;
    max-width: 200px;
  }
}
</style>
