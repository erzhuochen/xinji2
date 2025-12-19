<template>
  <div class="report-page" v-loading="loading">
    <!-- 周选择器 -->
    <div class="week-selector">
      <el-button :icon="ArrowLeft" circle size="small" @click="prevWeek" />
      <span class="week-label">{{ weekLabel }}</span>
      <el-button 
        :icon="ArrowRight" 
        circle 
        size="small" 
        :disabled="isCurrentWeek"
        @click="nextWeek" 
      />
    </div>

    <template v-if="report">
      <!-- 概览卡片 -->
      <div class="overview-card">
        <div class="stat-item">
          <span class="stat-value">{{ report.diaryCount }}</span>
          <span class="stat-label">日记数</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ report.analyzedCount }}</span>
          <span class="stat-label">已分析</span>
        </div>
        <div class="stat-item" v-if="report.mostFrequentEmotion">
          <div 
            class="emotion-badge"
            :style="{ background: getEmotionColor(report.mostFrequentEmotion) }"
          >
            {{ getEmotionLabel(report.mostFrequentEmotion) }}
          </div>
          <span class="stat-label">主要情绪</span>
        </div>
      </div>

      <!-- 情绪趋势图 -->
      <div class="chart-section" v-if="report.emotionTrend.length > 0">
        <h2 class="section-title">情绪趋势</h2>
        <div ref="trendChartRef" class="chart-container"></div>
      </div>

      <!-- 情绪分布图 -->
      <div class="chart-section" v-if="Object.keys(report.emotionDistribution).length > 0">
        <h2 class="section-title">情绪分布</h2>
        <div ref="pieChartRef" class="chart-container pie-chart"></div>
      </div>

      <!-- 关键词云 -->
      <div class="keywords-section" v-if="report.keywords.length > 0">
        <h2 class="section-title">本周关键词</h2>
        <div class="keyword-cloud">
          <span 
            v-for="(kw, i) in report.keywords" 
            :key="kw"
            class="keyword"
            :style="{ fontSize: getKeywordSize(i) + 'px' }"
          >
            {{ kw }}
          </span>
        </div>
      </div>

      <!-- 周总结 -->
      <div class="summary-section" v-if="report.summary">
        <h2 class="section-title">AI周总结</h2>
        <div class="summary-content">{{ report.summary }}</div>
      </div>

      <!-- 深度洞察入口 -->
      <div class="insights-entry" v-if="userStore.isPro">
        <router-link to="/insights" class="insights-btn">
          <el-icon><TrendCharts /></el-icon>
          查看深度洞察报告
          <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>
      <div class="insights-entry pro-tip" v-else>
        <p>升级PRO解锁深度洞察报告</p>
        <el-button type="primary" size="small" @click="goMembership">
          了解更多
        </el-button>
      </div>
    </template>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty-state">
      <el-icon :size="64" color="var(--text-tertiary)">
        <DataAnalysis />
      </el-icon>
      <p>本周还没有足够的数据</p>
      <p class="hint">多写几篇日记并进行AI分析，即可生成周报</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, TrendCharts, DataAnalysis } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getWeeklyReport } from '@/api/report'
import { EmotionMap, EmotionColorMap, type WeeklyReport } from '@/types'
import dayjs from 'dayjs'
import weekOfYear from 'dayjs/plugin/weekOfYear'
import * as echarts from 'echarts'

dayjs.extend(weekOfYear)

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const report = ref<WeeklyReport | null>(null)
const currentWeekStart = ref(dayjs().startOf('week'))

const trendChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

// 周标签
const weekLabel = computed(() => {
  const start = currentWeekStart.value
  const end = start.add(6, 'day')
  return `${start.format('M月D日')} - ${end.format('M月D日')}`
})

// 是否是当前周
const isCurrentWeek = computed(() => {
  return currentWeekStart.value.isSame(dayjs().startOf('week'), 'day')
})

// 上一周
const prevWeek = () => {
  currentWeekStart.value = currentWeekStart.value.subtract(1, 'week')
  fetchReport()
}

// 下一周
const nextWeek = () => {
  if (!isCurrentWeek.value) {
    currentWeekStart.value = currentWeekStart.value.add(1, 'week')
    fetchReport()
  }
}

// 获取周报
const fetchReport = async () => {
  loading.value = true
  try {
    const res = await getWeeklyReport(currentWeekStart.value.format('YYYY-MM-DD'))
    report.value = res.data
    
    await nextTick()
    initCharts()
  } catch (error) {
    console.error('获取周报失败:', error)
    report.value = null
  } finally {
    loading.value = false
  }
}

// 初始化图表
const initCharts = () => {
  initTrendChart()
  initPieChart()
}

// 初始化趋势图
const initTrendChart = () => {
  if (!trendChartRef.value || !report.value?.emotionTrend.length) return

  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)

  const data = report.value.emotionTrend
  const xData = data.map(d => dayjs(d.date).format('M/D'))
  const yData = data.map(d => d.intensity)
  const emotions = data.map(d => d.emotion)

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const i = params[0].dataIndex
        return `${xData[i]}<br/>${getEmotionLabel(emotions[i])}: ${Math.round(yData[i] * 100)}%`
      }
    },
    grid: {
      left: 40,
      right: 20,
      top: 20,
      bottom: 30
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLine: { lineStyle: { color: '#ddd' } },
      axisLabel: { color: '#999' }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 1,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      axisLabel: {
        color: '#999',
        formatter: (v: number) => Math.round(v * 100) + '%'
      }
    },
    series: [{
      type: 'line',
      data: yData,
      smooth: true,
      lineStyle: { color: '#9c27b0', width: 3 },
      itemStyle: {
        color: (params: any) => getEmotionColor(emotions[params.dataIndex])
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(156, 39, 176, 0.3)' },
          { offset: 1, color: 'rgba(156, 39, 176, 0)' }
        ])
      }
    }]
  })
}

// 初始化饼图
const initPieChart = () => {
  if (!pieChartRef.value || !report.value?.emotionDistribution) return

  if (pieChart) pieChart.dispose()
  pieChart = echarts.init(pieChartRef.value)

  const distribution = report.value.emotionDistribution
  const data = Object.entries(distribution).map(([key, value]) => ({
    name: getEmotionLabel(key),
    value,
    itemStyle: { color: getEmotionColor(key) }
  }))

  pieChart.setOption({
    tooltip: {
      formatter: '{b}: {c} ({d}%)'
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '50%'],
      data,
      label: {
        formatter: '{b}\n{d}%',
        color: '#666'
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        }
      }
    }]
  })
}

// 获取情绪标签
const getEmotionLabel = (emotion: string) => {
  return EmotionMap[emotion] || emotion
}

// 获取情绪颜色
const getEmotionColor = (emotion: string) => {
  return EmotionColorMap[emotion] || '#90A4AE'
}

// 关键词大小
const getKeywordSize = (index: number) => {
  const sizes = [18, 16, 16, 14, 14, 13, 13, 12, 12, 12]
  return sizes[index] || 12
}

// 跳转会员
const goMembership = () => {
  router.push('/membership')
}

// 监听窗口大小
const handleResize = () => {
  trendChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  fetchReport()
  window.addEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.report-page {
  padding: 16px;
  padding-bottom: 80px;
}

// 周选择器
.week-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;

  .week-label {
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
    min-width: 160px;
    text-align: center;
  }
}

// 概览卡片
.overview-card {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border-radius: 16px;
  padding: 24px 16px;
  margin-bottom: 20px;
  color: #fff;

  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;

    .stat-value {
      font-size: 28px;
      font-weight: 600;
    }

    .stat-label {
      font-size: 12px;
      opacity: 0.8;
    }

    .emotion-badge {
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 13px;
    }
  }
}

// 图表区域
.chart-section {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 16px;
  }

  .chart-container {
    width: 100%;
    height: 200px;

    &.pie-chart {
      height: 240px;
    }
  }
}

// 关键词
.keywords-section {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 16px;
  }

  .keyword-cloud {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    justify-content: center;

    .keyword {
      color: var(--primary-color);
      padding: 4px 8px;
    }
  }
}

// 周总结
.summary-section {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 16px;
  }

  .summary-content {
    font-size: 14px;
    line-height: 1.8;
    color: var(--text-secondary);
  }
}

// 深度洞察入口
.insights-entry {
  background: #fff;
  border-radius: 16px;
  padding: 16px 20px;

  .insights-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--primary-color);
    text-decoration: none;
    font-size: 15px;
    font-weight: 500;

    .el-icon:last-child {
      margin-left: auto;
    }
  }

  &.pro-tip {
    text-align: center;
    
    p {
      margin: 0 0 12px;
      color: var(--text-secondary);
      font-size: 14px;
    }
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;

  p {
    margin: 16px 0 0;
    color: var(--text-secondary);
  }

  .hint {
    font-size: 13px;
    color: var(--text-tertiary);
    margin-top: 8px;
  }
}
</style>
