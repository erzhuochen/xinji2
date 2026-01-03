<template>
  <div class="training-detail-page">
    <div class="back-button" @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回训练列表</span>
    </div>

    <div class="training-content">
      <component :is="currentComponent" v-if="currentComponent" />
      <div v-else class="not-found">
        <p>未找到该训练项目</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import BreathingGuide from '@/components/mental-training/BreathingGuide.vue'
import CognitiveReframe from '@/components/mental-training/CognitiveReframe.vue'
import EmotionNaming from '@/components/mental-training/EmotionNaming.vue'
import GratitudeExercise from '@/components/mental-training/GratitudeExercise.vue'
import SmallGoalSetting from '@/components/mental-training/SmallGoalSetting.vue'

const route = useRoute()
const router = useRouter()

const componentMap: Record<string, any> = {
  breathing: BreathingGuide,
  reframe: CognitiveReframe,
  emotion: EmotionNaming,
  gratitude: GratitudeExercise,
  goal: SmallGoalSetting
}

const titleMap: Record<string, string> = {
  breathing: '呼吸小游戏',
  reframe: '认知重构',
  emotion: '思维气球',
  gratitude: '心灵小菜园',
  goal: '微目标设定'
}

const currentComponent = computed(() => {
  const id = route.params.id as string
  return componentMap[id] || null
})

// 更新页面标题
watch(() => route.params.id, (id) => {
  if (id && typeof id === 'string') {
    const title = titleMap[id] || '心理训练'
    document.title = `${title} - 心迹`
  }
}, { immediate: true })

const goBack = () => {
  router.push('/mental-training')
}
</script>

<style lang="scss" scoped>
.training-detail-page {
  padding: 16px;
  padding-bottom: 80px;
  max-width: 800px;
  margin: 0 auto;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  padding: 8px 16px;
  color: #6B8DD6;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
  font-size: 14px;

  &:hover {
    background: rgba(107, 141, 214, 0.1);
  }

  .el-icon {
    font-size: 18px;
  }
}

.training-content {
  width: 100%;
}

.not-found {
  text-align: center;
  padding: 80px 20px;
  color: var(--text-secondary, #6C757D);
}
</style>

