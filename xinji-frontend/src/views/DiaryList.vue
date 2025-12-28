<template>
  <div class="diary-list-page">
    <!-- 统计卡片 -->
    <div class="stats-card">
      <div class="stat-item">
        <span class="stat-value">{{ userStore.diaryCount }}</span>
        <span class="stat-label">总日记数</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-value">{{ userStore.remainingQuota }}</span>
        <span class="stat-label">今日AI额度</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-value" :class="{ 'is-pro': userStore.isPro }">
          {{ userStore.isPro ? 'PRO' : 'FREE' }}
        </span>
        <span class="stat-label">会员状态</span>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        size="small"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        @change="handleDateChange"
      />
      <el-input
        v-model="keyword"
        placeholder="搜索日记"
        size="small"
        clearable
        :prefix-icon="Search"
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      />
    </div>

    <!-- 日记列表 -->
    <div class="diary-list" v-loading="loading">
      <template v-if="diaryStore.diaryList.length > 0">
        <div 
          v-for="diary in diaryStore.diaryList" 
          :key="diary.id"
          class="diary-card"
          @click="viewDiary(diary.id)"
        >
          <div class="diary-header">
            <span class="diary-date">{{ formatDate(diary.diaryDate) }}</span>
            <el-tag 
              v-if="diary.isDraft" 
              size="small" 
              type="info"
            >
              草稿
            </el-tag>
            <div 
              v-else-if="diary.emotion"
              class="emotion-badge"
              :style="{ background: getEmotionColor(diary.emotion.primary) }"
            >
              {{ getEmotionLabel(diary.emotion.primary) }}
            </div>
          </div>
          
          <h3 class="diary-title">{{ diary.title || '无标题' }}</h3>
          <p class="diary-preview">{{ diary.preview }}</p>
          
          <div class="diary-footer">
            <span class="diary-time">{{ formatTime(diary.updatedAt) }}</span>
            <el-icon 
              v-if="diary.analyzed" 
              class="analyzed-icon"
              title="已分析"
            >
              <DataAnalysis />
            </el-icon>
          </div>
        </div>
      </template>
      
      <!-- 空状态 -->
      <div v-else-if="!loading" class="empty-state">
        <el-icon :size="64" color="var(--text-tertiary)">
          <Document />
        </el-icon>
        <p>还没有日记</p>
        <p class="empty-hint">点击右下角按钮开始记录</p>
      </div>
    </div>

    <!-- 加载更多 -->
    <div 
      v-if="hasMore && diaryStore.diaryList.length > 0" 
      class="load-more"
    >
      <el-button 
        text 
        :loading="loadingMore"
        @click="loadMore"
      >
        加载更多
      </el-button>
    </div>

    <!-- 写日记按钮 -->
    <div class="fab" @click="createDiary">
      <el-icon :size="28">
        <Plus />
      </el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, DataAnalysis, Document, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useDiaryStore } from '@/store/diary'
import { getDiaryList } from '@/api/diary'
import { EmotionMap, EmotionColorMap } from '@/types'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const diaryStore = useDiaryStore()

const loading = ref(false)
const loadingMore = ref(false)
const dateRange = ref<string[]>([])
const keyword = ref('')
const page = ref(1)
const pageSize = 20
const hasMore = ref(true)

// 获取日记列表
const fetchDiaries = async (append = false) => {
  if (!append) {
    loading.value = true
    page.value = 1
  } else {
    loadingMore.value = true
  }

  try {
    const res = await getDiaryList({
      page: page.value,
      pageSize,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      keyword: keyword.value || undefined
    })

    if (append) {
      diaryStore.diaryList.push(...res.data.list)
    } else {
      diaryStore.diaryList = res.data.list
    }

    hasMore.value = diaryStore.diaryList.length < res.data.total
  } catch (error) {
    console.error('获取日记列表失败:', error)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 日期筛选
const handleDateChange = () => {
  fetchDiaries()
}

// 搜索
const handleSearch = () => {
  fetchDiaries()
}

// 加载更多
const loadMore = () => {
  page.value++
  fetchDiaries(true)
}

// 查看日记
const viewDiary = (id: string) => {
  router.push(`/diary/${id}`)
}

// 创建日记
const createDiary = () => {
  router.push('/diary/edit')
}

// 格式化日期
const formatDate = (date: string) => {
  return dayjs(date).format('M月D日')
}

// 格式化时间
const formatTime = (time: string) => {
  const d = dayjs(time)
  if (d.isToday()) return d.format('HH:mm')
  if (d.isYesterday()) return '昨天'
  return d.format('MM/DD')
}

// 获取情绪标签
const getEmotionLabel = (emotion: string) => {
  return EmotionMap[emotion] || emotion
}

// 获取情绪颜色
const getEmotionColor = (emotion: string) => {
  return EmotionColorMap[emotion] || '#90A4AE'
}

onMounted(() => {
  fetchDiaries()
  userStore.fetchUserProfile()
})
</script>

<style lang="scss" scoped>
.diary-list-page {
  padding: 0;
  max-width: 1200px;
  margin: 0 auto;
}

// 统计卡片
.stats-card {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: linear-gradient(135deg, #6B8DD6, #8BA4E0);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  color: #fff;

  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    .stat-value {
      font-size: 28px;
      font-weight: 600;

      &.is-pro {
        color: #FFD93D;
      }
    }

    .stat-label {
      font-size: 13px;
      opacity: 0.85;
    }
  }

  .stat-divider {
    width: 1px;
    height: 40px;
    background: rgba(255, 255, 255, 0.3);
  }
}

// 筛选区域
.filter-section {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;

  .el-date-picker {
    flex: 1;
    max-width: 320px;
  }

  .el-input {
    flex: 1;
    max-width: 240px;
  }
}

// 日记列表 - 网格布局
.diary-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

// 日记卡片
.diary-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid #E9ECEF;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
  }

  .diary-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;

    .diary-date {
      font-size: 13px;
      color: #6C757D;
    }

    .emotion-badge {
      font-size: 12px;
      color: #fff;
      padding: 3px 10px;
      border-radius: 12px;
    }
  }

  .diary-title {
    font-size: 17px;
    font-weight: 600;
    color: #2C3E50;
    margin: 0 0 10px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .diary-preview {
    font-size: 14px;
    color: #6C757D;
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    line-height: 1.6;
  }

  .diary-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 16px;
    padding-top: 12px;
    border-top: 1px solid #E9ECEF;

    .diary-time {
      font-size: 12px;
      color: #ADB5BD;
    }

    .analyzed-icon {
      color: #6B8DD6;
    }
  }
}

// 空状态
.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;

  p {
    margin: 16px 0 0;
    color: #6C757D;
    font-size: 16px;
  }

  .empty-hint {
    font-size: 14px;
    color: #ADB5BD;
    margin-top: 8px;
  }
}

// 加载更多
.load-more {
  grid-column: 1 / -1;
  text-align: center;
  padding: 20px 0;
}

// 浮动按钮
.fab {
  position: fixed;
  right: 32px;
  bottom: 32px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6B8DD6, #8BA4E0);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(107, 141, 214, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;
  z-index: 100;

  &:hover {
    transform: scale(1.08);
    box-shadow: 0 8px 28px rgba(107, 141, 214, 0.5);
  }

  &:active {
    transform: scale(0.95);
  }
}

@media (max-width: 768px) {
  .diary-list {
    grid-template-columns: 1fr;
  }

  .filter-section {
    flex-direction: column;

    .el-date-picker,
    .el-input {
      max-width: 100%;
    }
  }
}
</style>
