<template>
  <div class="diary-detail-page" v-loading="loading">
    <template v-if="diary">
      <!-- 日记内容 -->
      <div class="diary-content">
        <div class="diary-meta">
          <span class="diary-date">{{ formatDate(diary.diaryDate) }}</span>
          <el-tag v-if="diary.isDraft" size="small" type="info">草稿</el-tag>
        </div>

        <h1 class="diary-title">{{ diary.title || '无标题' }}</h1>
        <div class="diary-text">{{ diary.content }}</div>

        <div class="diary-info">
          <span>创建于 {{ formatDateTime(diary.createdAt) }}</span>
          <span v-if="diary.updatedAt !== diary.createdAt">
            · 更新于 {{ formatDateTime(diary.updatedAt) }}
          </span>
        </div>
      </div>

      <!-- AI分析结果 -->
      <div v-if="analysis" class="analysis-section">
        <h2 class="section-title">
          <el-icon><DataAnalysis /></el-icon>
          AI情绪分析
        </h2>

        <!-- 分析中 -->
        <div v-if="analysis.status === 'PROCESSING'" class="analysis-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>AI正在分析中...</span>
        </div>

        <!-- 分析完成 -->
        <div v-else-if="analysis.status === 'COMPLETED'" class="analysis-result">
          <!-- 情绪雷达图 -->
          <div class="emotion-chart">
            <div ref="chartRef" class="chart-container"></div>
          </div>

          <!-- 主要情绪 -->
          <div class="primary-emotion">
            <div 
              class="emotion-badge"
              :style="{ background: getEmotionColor(analysis.primaryEmotion!) }"
            >
              {{ getEmotionLabel(analysis.primaryEmotion!) }}
            </div>
            <span class="intensity">
              强度 {{ Math.round((analysis.emotionIntensity || 0) * 100) }}%
            </span>
          </div>

          <!-- 关键词 -->
          <div v-if="analysis.keywords?.length" class="keywords">
            <h3>关键词</h3>
            <div class="keyword-tags">
              <el-tag 
                v-for="kw in analysis.keywords" 
                :key="kw"
                effect="plain"
              >
                {{ kw }}
              </el-tag>
            </div>
          </div>

          <!-- 认知扭曲 -->
          <div v-if="analysis.cognitiveDistortions?.length" class="distortions">
            <h3>认知模式提醒</h3>
            <div 
              v-for="(d, i) in analysis.cognitiveDistortions" 
              :key="i"
              class="distortion-item"
            >
              <span class="distortion-type">{{ d.type }}</span>
              <span class="distortion-desc">{{ d.description }}</span>
            </div>
          </div>

          <!-- 建议 -->
          <div v-if="analysis.suggestions?.length" class="suggestions">
            <h3>心理调适建议</h3>
            <ul>
              <li v-for="(s, i) in analysis.suggestions" :key="i">{{ s }}</li>
            </ul>
          </div>

          <!-- 风险提示 -->
          <div 
            v-if="analysis.riskLevel && analysis.riskLevel !== 'LOW'" 
            class="risk-alert"
            :class="analysis.riskLevel.toLowerCase()"
          >
            <el-icon><Warning /></el-icon>
            <span>
              {{ analysis.riskLevel === 'MEDIUM' ? '建议关注心理状态' : '建议寻求专业帮助' }}
            </span>
          </div>
        </div>

        <!-- 分析失败 -->
        <div v-else-if="analysis.status === 'FAILED'" class="analysis-failed">
          <el-icon><CircleClose /></el-icon>
          <span>分析失败，请稍后重试</span>
        </div>
      </div>

      <!-- 未分析提示 -->
      <div v-else-if="!diary.isDraft" class="no-analysis">
        <el-button type="primary" plain @click="handleAnalysis">
          <el-icon><DataAnalysis /></el-icon>
          进行AI情绪分析
        </el-button>
        <p class="hint">今日剩余额度：{{ userStore.remainingQuota }}</p>
      </div>

      <!-- 底部操作栏 -->
      <div class="action-bar">
        <el-button @click="editDiary">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button type="danger" plain @click="handleDelete">
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataAnalysis, Loading, Warning, CircleClose, Edit, Delete } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getDiaryDetail, deleteDiary } from '@/api/diary'
import { submitAnalysis, getAnalysisResult } from '@/api/analysis'
import { EmotionMap, EmotionColorMap, type DiaryDetail, type AnalysisResult } from '@/types'
import dayjs from 'dayjs'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const diary = ref<DiaryDetail | null>(null)
const analysis = ref<AnalysisResult | null>(null)
const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

// 加载日记详情
const loadDiary = async () => {
  const id = route.params.id as string
  if (!id) return

  loading.value = true
  try {
    const res = await getDiaryDetail(id)
    diary.value = res.data

    // 如果已有分析，加载分析结果
    if (res.data.analysisId) {
      loadAnalysis(res.data.analysisId)
    }
  } catch (error) {
    console.error('加载日记失败:', error)
    ElMessage.error('日记加载失败')
    router.back()
  } finally {
    loading.value = false
  }
}

// 加载分析结果
const loadAnalysis = async (analysisId: string) => {
  try {
    const res = await getAnalysisResult(analysisId)
    analysis.value = res.data

    // 如果正在处理，轮询查询
    if (res.data.status === 'PROCESSING') {
      setTimeout(() => loadAnalysis(analysisId), 3000)
    } else if (res.data.status === 'COMPLETED') {
      await nextTick()
      initChart()
    }
  } catch (error) {
    console.error('加载分析结果失败:', error)
  }
}

// 提交分析
const handleAnalysis = async () => {
  if (userStore.remainingQuota <= 0) {
    ElMessage.warning('今日AI分析额度已用完')
    return
  }

  if (!diary.value) return

  try {
    const res = await submitAnalysis(diary.value.id)
    analysis.value = res.data
    ElMessage.success('分析已提交')

    // 开始轮询
    if (res.data.status === 'PROCESSING') {
      setTimeout(() => loadAnalysis(res.data.id), 3000)
    }
  } catch (error) {
    console.error('提交分析失败:', error)
  }
}

// 初始化图表
const initChart = () => {
  if (!chartRef.value || !analysis.value?.emotions) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value)

  const emotions = analysis.value.emotions
  const indicators = Object.keys(emotions).map(key => ({
    name: EmotionMap[key] || key,
    max: 1
  }))
  const values = Object.values(emotions)

  chartInstance.setOption({
    radar: {
      indicator: indicators,
      radius: '60%',
      axisName: {
        color: '#666'
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        areaStyle: {
          color: 'rgba(156, 39, 176, 0.3)'
        },
        lineStyle: {
          color: '#9c27b0'
        },
        itemStyle: {
          color: '#9c27b0'
        }
      }]
    }]
  })
}

// 编辑日记
const editDiary = () => {
  if (diary.value) {
    router.push(`/diary/edit/${diary.value.id}`)
  }
}

// 删除日记
const handleDelete = async () => {
  if (!diary.value) return

  try {
    await ElMessageBox.confirm(
      '删除后无法恢复，确定要删除这篇日记吗？',
      '确认删除',
      { type: 'warning' }
    )

    await deleteDiary(diary.value.id)
    ElMessage.success('删除成功')
    router.push('/diary')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 格式化日期
const formatDate = (date: string) => {
  return dayjs(date).format('YYYY年M月D日')
}

const formatDateTime = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

// 获取情绪标签
const getEmotionLabel = (emotion: string) => {
  return EmotionMap[emotion] || emotion
}

// 获取情绪颜色
const getEmotionColor = (emotion: string) => {
  return EmotionColorMap[emotion] || '#90A4AE'
}

// 监听窗口大小变化
const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  loadDiary()
  window.addEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.diary-detail-page {
  padding: 16px;
  padding-bottom: 100px;
}

// 日记内容
.diary-content {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;

  .diary-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;

    .diary-date {
      font-size: 13px;
      color: var(--text-secondary);
    }
  }

  .diary-title {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 16px;
  }

  .diary-text {
    font-size: 15px;
    line-height: 1.8;
    color: var(--text-primary);
    white-space: pre-wrap;
    word-break: break-word;
  }

  .diary-info {
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px solid var(--border-color);
    font-size: 12px;
    color: var(--text-tertiary);
  }
}

// 分析区域
.analysis-section {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 16px;
  }
}

// 分析加载中
.analysis-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: var(--primary-color);
}

// 分析结果
.analysis-result {
  .emotion-chart {
    margin-bottom: 20px;

    .chart-container {
      width: 100%;
      height: 200px;
    }
  }

  .primary-emotion {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    margin-bottom: 20px;

    .emotion-badge {
      font-size: 14px;
      color: #fff;
      padding: 4px 16px;
      border-radius: 16px;
    }

    .intensity {
      font-size: 13px;
      color: var(--text-secondary);
    }
  }

  h3 {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
    margin: 0 0 12px;
  }

  .keywords {
    margin-bottom: 20px;

    .keyword-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
  }

  .distortions {
    margin-bottom: 20px;

    .distortion-item {
      display: flex;
      gap: 8px;
      padding: 8px 12px;
      background: var(--bg-secondary);
      border-radius: 8px;
      margin-bottom: 8px;
      font-size: 13px;

      .distortion-type {
        color: var(--accent-color);
        font-weight: 500;
        white-space: nowrap;
      }

      .distortion-desc {
        color: var(--text-secondary);
      }
    }
  }

  .suggestions {
    margin-bottom: 20px;

    ul {
      margin: 0;
      padding-left: 20px;

      li {
        margin-bottom: 8px;
        font-size: 14px;
        color: var(--text-secondary);
        line-height: 1.6;
      }
    }
  }

  .risk-alert {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px;
    border-radius: 8px;
    font-size: 14px;

    &.medium {
      background: #fff3e0;
      color: #e65100;
    }

    &.high {
      background: #ffebee;
      color: #c62828;
    }
  }
}

// 分析失败
.analysis-failed {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: var(--error-color);
}

// 未分析
.no-analysis {
  background: #fff;
  border-radius: 16px;
  padding: 32px 20px;
  text-align: center;
  margin-bottom: 16px;

  .hint {
    margin: 12px 0 0;
    font-size: 13px;
    color: var(--text-tertiary);
  }
}

// 底部操作栏
.action-bar {
  position: fixed;
  bottom: 60px;
  left: 0;
  right: 0;
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid var(--border-color);
  padding-bottom: calc(12px + env(safe-area-inset-bottom));

  .el-button {
    flex: 1;
    height: 44px;
  }
}

@media (min-width: 768px) {
  .action-bar {
    max-width: 480px;
    left: 50%;
    transform: translateX(-50%);
  }
}
</style>
