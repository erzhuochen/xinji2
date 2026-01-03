<template>
  <div class="mental-training-container">
    <h2>🧠 心理训练</h2>
    <p>根据你的日记与情绪状态，推荐以下练习：</p>

    <div class="training-cards">
      <el-card
        v-for="item in trainingItems"
        :key="item.id"
        class="mental-training-card"
        shadow="never"
        @click="goToTraining(item.route)"
      >
        <div class="card-content">
          <h3 class="card-title">{{ item.title }}</h3>
          <p class="card-desc">{{ item.description }}</p>
          <el-button type="primary" link>开始练习 →</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import GratitudeExercise from '@/components/mental-training/GratitudeExercise.vue'
import EmotionNaming from '@/components/mental-training/EmotionNaming.vue'
import BreathingGuide from '@/components/mental-training/BreathingGuide.vue'
import CognitiveReframe from '@/components/mental-training/CognitiveReframe.vue'
import SmallGoalSetting from '@/components/mental-training/SmallGoalSetting.vue'

const router = useRouter()

const trainingItems = [
  {
    id: 'gratitude',
    title: '感恩练习',
    description: '每天记录3件值得感恩的小事，培养积极心态。',
    component: GratitudeExercise,
    route: '/mental-training/gratitude'
  },
  {
    id: 'emotion-naming',
    title: '情绪命名',
    description: '精准识别并命名你的情绪，是情绪管理的第一步。',
    component: EmotionNaming,
    route: '/mental-training/emotion-naming'
  },
  {
    id: 'breathing',
    title: '呼吸引导',
    description: '通过4-7-8呼吸法，快速缓解焦虑与压力。',
    component: BreathingGuide,
    route: '/mental-training/breathing'
  },
  {
    id: 'cognitive-reframe',
    title: '认知重构',
    description: '挑战自动负性思维，用更平衡的视角看问题。',
    component: CognitiveReframe,
    route: '/mental-training/cognitive-reframe'
  },
  {
    id: 'small-goal',
    title: '微目标设定',
    description: '设定一个5分钟内可完成的小目标，积累掌控感。',
    component: SmallGoalSetting,
    route: '/mental-training/small-goal'
  }
]

const goToTraining = (path: string) => {
  router.push(path)
}
</script>