<template>
  <div class="insights-page" v-loading="loading">
    <!-- 时间范围选择 -->
    <div class="time-range-selector">
      <el-radio-group v-model="timeRange" @change="fetchInsights">
        <el-radio-button value="MONTH">近一月</el-radio-button>
        <el-radio-button value="QUARTER">近三月</el-radio-button>
        <el-radio-button value="HALF_YEAR">近半年</el-radio-button>
      </el-radio-group>
    </div>

    <template v-if="report">
      <!-- 洞察卡片列表 -->
      <div class="insights-list">
        <div 
          v-for="(insight, i) in report.insights" 
          :key="i"
          class="insight-card"
        >
          <div class="insight-header">
            <span class="insight-type">{{ getInsightTypeLabel(insight.type) }}</span>
            <span class="confidence">
              置信度 {{ Math.round(insight.confidence * 100) }}%
            </span>
          </div>
          <h3 class="insight-title">{{ insight.title }}</h3>
          <p class="insight-content">{{ insight.content }}</p>
        </div>
      </div>

      <!-- 成长计划 -->
      <div class="growth-plan" v-if="report.growthPlan.length > 0">
        <h2 class="section-title">
          <el-icon><Sunrise /></el-icon>
          成长计划建议
        </h2>
        <ul class="plan-list">
          <li v-for="(plan, i) in report.growthPlan" :key="i">{{ plan }}</li>
        </ul>
      </div>

      <!-- 情绪预测 -->
      <div class="emotion-forecast" v-if="report.emotionForecast">
        <h2 class="section-title">
          <el-icon><Sunny /></el-icon>
          下周情绪预测
        </h2>
        <div 
          class="risk-indicator"
          :class="report.emotionForecast.nextWeekRisk.toLowerCase()"
        >
          <span class="risk-label">风险等级</span>
          <span class="risk-value">{{ getRiskLabel(report.emotionForecast.nextWeekRisk) }}</span>
        </div>
        <div v-if="report.emotionForecast.triggers.length > 0" class="triggers">
          <p>可能触发因素：</p>
          <div class="trigger-tags">
            <el-tag 
              v-for="trigger in report.emotionForecast.triggers" 
              :key="trigger"
              effect="plain"
            >
              {{ trigger }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- AI心理咨询师卡片 -->
      <div class="insights-list">
        <div 
          class="insight-card ai-counselor-card"
          @click="goToAICounselor"
        >
          <div class="insight-header">
            <span class="insight-type">AI心理咨询师</span>
            <span class="confidence">
              <el-icon><ChatDotRound /></el-icon>
            </span>
          </div>
          <h3 class="insight-title">与AI心理咨询师对话</h3>
          <p class="insight-content">基于您近七天的日记记录，AI心理咨询师将为您提供专业的心理支持和指导</p>
        </div>
      </div>

    </template>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty-state">
      <el-icon :size="64" color="var(--text-tertiary)">
        <TrendCharts />
      </el-icon>
      <p>暂无足够数据生成深度洞察</p>
      <p class="hint">继续记录和分析日记，AI将为你生成个性化洞察</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Sunrise, Sunny, TrendCharts, ChatDotRound } from '@element-plus/icons-vue'
import { getInsightsReport } from '@/api/report'
import type { InsightsReport } from '@/types'

const router = useRouter()

const loading = ref(false)
const timeRange = ref('MONTH')
const report = ref<InsightsReport | null>(null)

// 获取洞察报告
const fetchInsights = async () => {
  loading.value = true
  try {
    const res = await getInsightsReport(timeRange.value)
    report.value = res.data
  } catch (error) {
    console.error('获取洞察报告失败:', error)
    report.value = null
  } finally {
    loading.value = false
  }
}

// 洞察类型标签
const getInsightTypeLabel = (type: string) => {
  const typeMap: Record<string, string> = {
    EMOTION_PATTERN: '情绪模式',
    TRIGGER_ANALYSIS: '触发因素',
    GROWTH_TREND: '成长趋势',
    BEHAVIOR_INSIGHT: '行为洞察',
    COGNITIVE_PATTERN: '认知偏差'
  }
  return typeMap[type] || type
}

// 风险标签
const getRiskLabel = (risk: string) => {
  const riskMap: Record<string, string> = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高'
  }
  return riskMap[risk] || risk
}

// 跳转到AI心理咨询师页面
const goToAICounselor = () => {
  router.push('/ai-counselor')
}

onMounted(() => {
  fetchInsights()
})
</script>

<style lang="scss" scoped>
.insights-page {
  padding: 16px;
  padding-bottom: 80px;
}

// 时间范围选择
.time-range-selector {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;

  :deep(.el-radio-group) {
    background: #fff;
    border-radius: 8px;
    padding: 4px;
  }

  :deep(.el-radio-button__inner) {
    border: none;
    border-radius: 6px;
  }
}

// 洞察卡片
.insights-list {
  margin-bottom: 20px;
}

.insight-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 12px;

  &.ai-counselor-card {
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }
  }

  .insight-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .insight-type {
      font-size: 12px;
      color: var(--primary-color);
      background: var(--primary-light);
      padding: 4px 8px;
      border-radius: 4px;
    }

    .confidence {
      font-size: 12px;
      color: var(--text-tertiary);
      
      .el-icon {
        color: var(--primary-color);
      }
    }
  }

  .insight-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 8px;
  }

  .insight-content {
    font-size: 14px;
    line-height: 1.7;
    color: var(--text-secondary);
    margin: 0;
  }
}

// 通用section标题
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px;
}

// 成长计划
.growth-plan {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;

  .plan-list {
    margin: 0;
    padding-left: 20px;

    li {
      margin-bottom: 12px;
      font-size: 14px;
      line-height: 1.6;
      color: var(--text-secondary);

      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}

// 情绪预测
.emotion-forecast {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;

  .risk-indicator {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-radius: 8px;
    margin-bottom: 16px;

    &.low {
      background: #e8f5e9;
      .risk-value { color: #2e7d32; }
    }

    &.medium {
      background: #fff3e0;
      .risk-value { color: #e65100; }
    }

    &.high {
      background: #ffebee;
      .risk-value { color: #c62828; }
    }

    .risk-label {
      font-size: 14px;
      color: var(--text-secondary);
    }

    .risk-value {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .triggers {
    p {
      margin: 0 0 8px;
      font-size: 13px;
      color: var(--text-secondary);
    }

    .trigger-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
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
