<template>
  <div class="contribution-graph">
    <h2 class="section-title">日记记录</h2>

    <div class="graph-content">
      <!-- 贡献图网格 -->
      <div class="graph-grid" ref="graphGridRef">
        <div
          v-for="(day, index) in contributionData"
          :key="index"
          class="day-cell"
          :class="`level-${day.level}`"
          :title="getDayTooltip(day)"
          @mouseenter="handleDayHover(day, $event)"
          @mouseleave="hoveredDay = null"
        ></div>
      </div>
      </div>

      <!-- 悬停提示 -->
    <div v-if="hoveredDay && hoveredDay.count > 0" class="tooltip" :style="tooltipStyle">
      <div class="tooltip-content">
        <strong>{{ hoveredDay.count }}</strong> 篇日记
        <div class="tooltip-date">{{ formatTooltipDate(hoveredDay.date) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDiaryList } from '@/api/diary'
import dayjs from 'dayjs'

interface DayData {
  date: string
  count: number
  level: number
}

const contributionData = ref<DayData[]>([])
const hoveredDay = ref<DayData | null>(null)
const tooltipStyle = ref({ top: '0px', left: '0px' })
const graphGridRef = ref<HTMLElement>()

// 获取一年的日期数据（从今天往前推371天）
const generateYearDates = () => {
  const dates: DayData[] = []
  const today = dayjs().startOf('day')
  const targetDays = 53 * 7 // 53周 = 371天
  
  // 从今天往前推371天，得到起始日期（不一定是周日）
  const startDate = today.subtract(targetDays - 1, 'day').startOf('day')
  
  // 从起始日期开始生成正好371天的数据（包含今天）
  let current = startDate
  for (let i = 0; i < targetDays; i++) {
    dates.push({
      date: current.format('YYYY-MM-DD'),
      count: 0,
      level: 0
    })
    current = current.add(1, 'day')
  }
  
  return dates
}

// 获取日记数据并填充
const fetchDiaryData = async () => {
  try {
    const dates = generateYearDates()
    const startDate = dates[0].date
    const endDate = dates[dates.length - 1].date
    
    // 获取所有日记（可能需要分页获取）
    let allDiaries: any[] = []
    let page = 1
    const pageSize = 100
    
    while (true) {
      const res = await getDiaryList({
        page,
        pageSize,
        startDate,
        endDate
      })
      
      if (res.data && res.data.list) {
        allDiaries = allDiaries.concat(res.data.list)
        
        // 检查是否还有更多数据
        const total = res.data.total || 0
        const currentTotal = allDiaries.length
        
        if (res.data.list.length < pageSize || currentTotal >= total) {
          break
        }
        page++
      } else {
        break
      }
    }
    
    // 按日期统计日记数量
    const diaryMap: Record<string, number> = {}
    allDiaries.forEach((diary: any) => {
      // 使用 diaryDate 或 createdAt 字段
      const dateStr = diary.diaryDate || diary.createdAt || diary.createTime || diary.date
      if (dateStr) {
        const date = dayjs(dateStr).format('YYYY-MM-DD')
        diaryMap[date] = (diaryMap[date] || 0) + 1
      }
    })
    
    // 填充数据并计算等级
    dates.forEach(day => {
      const count = diaryMap[day.date] || 0
      day.count = count
      // 根据数量计算等级（0-4）
      if (count === 0) {
        day.level = 0
      } else if (count === 1) {
        day.level = 1
      } else if (count <= 2) {
        day.level = 2
      } else if (count <= 4) {
        day.level = 3
      } else {
        day.level = 4
      }
    })
    
    contributionData.value = dates
  } catch (error) {
    console.error('获取日记数据失败:', error)
    // 如果失败，至少显示空数据
    contributionData.value = generateYearDates()
  }
}

// 获取日期提示
const getDayTooltip = (day: DayData) => {
  if (day.count === 0) {
    return `${formatTooltipDate(day.date)}: 无记录`
  }
  return `${formatTooltipDate(day.date)}: ${day.count} 篇日记`
}

// 格式化工具提示日期
const formatTooltipDate = (date: string) => {
  return dayjs(date).format('YYYY年M月D日')
}

// 处理日期悬停
const handleDayHover = (day: DayData, event: MouseEvent) => {
  hoveredDay.value = day
  
  if (day.count === 0) return
  
  const rect = graphGridRef.value?.getBoundingClientRect()
  if (!rect) return
  
  const scrollLeft = graphGridRef.value?.scrollLeft || 0
  
  tooltipStyle.value = {
    top: `${event.clientY - rect.top - 50}px`,
    left: `${event.clientX - rect.left + scrollLeft - 60}px`
  }
}

onMounted(() => {
  fetchDiaryData()
})
</script>

<style lang="scss" scoped>
.contribution-graph {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px;
}

.graph-content {
  position: relative;
  width: 100%;
  overflow-x: auto;
}

.graph-grid {
  display: grid;
  grid-template-columns: repeat(53, 1fr);
  grid-template-rows: repeat(7, 1fr);
  gap: 2px;
  width: 99%;
  aspect-ratio: 53 / 7;
}

.day-cell {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.2s ease;
  
  // 暖色调：从浅到深
  &.level-0 {
    background: #f0f0f0;
    border: 1px solid #e0e0e0;
  }

  &.level-1 {
    background: #ffd4a3; // 浅橙色
  }

  &.level-2 {
    background: #ffb366; // 橙色
  }

  &.level-3 {
    background: #ff8c42; // 深橙色
  }

  &.level-4 {
    background: #ff6b35; // 最深的橙红色
  }

  &:hover {
    transform: scale(1.2);
    z-index: 10;
    position: relative;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  }
}


.tooltip {
  position: absolute;
  background: rgba(0, 0, 0, 0.85);
  color: #fff;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  pointer-events: none;
  z-index: 1000;
  white-space: nowrap;

  .tooltip-content {
    strong {
      font-size: 14px;
      font-weight: 600;
    }

    .tooltip-date {
      margin-top: 4px;
      font-size: 11px;
      opacity: 0.8;
    }
  }
}

@media (max-width: 768px) {
  .graph-grid {
    grid-template-columns: repeat(53, 10px);
    gap: 2px;
  }

  .day-cell {
    width: 10px;
    height: 10px;
  }
}
</style>

