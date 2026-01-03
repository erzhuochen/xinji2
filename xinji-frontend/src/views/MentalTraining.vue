<template>
  <div class="mental-training-page">
    <div class="page-header">
      <h1>心理训练</h1>
      <p class="subtitle">通过科学的心理训练方法，提升情绪调节能力</p>
    </div>

    <div class="training-nav-grid">
      <router-link
        v-for="training in trainingList"
        :key="training.id"
        :to="training.path || `/mental-training/${training.id}`"
        class="training-card"
      >
        <div class="card-icon">{{ training.icon }}</div>
        <h3 class="card-title">{{ training.title }}</h3>
        <p class="card-description">{{ training.description }}</p>
        <div class="card-footer">
          <span class="card-tag">{{ training.tag }}</span>
          <el-icon class="arrow-icon"><ArrowRight /></el-icon>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowRight } from '@element-plus/icons-vue'

const trainingList = [
  {
    id: 'mindfulness',
    icon: '🧘',
    title: '正念练习',
    description: '5分钟呼吸冥想，专注呼吸放松身心，缓解压力和焦虑',
    tag: '正念冥想',
    path: '/mindfulness/breathing'
  },
  {
    id: 'cheer',
    icon: '✨',
    title: '心灵加油站',
    description: '抽取今日加油词，获得正能量，也可以分享你的鼓励话语',
    tag: '情绪支持',
    path: '/cheer'
  },
  {
    id: 'reframe',
    icon: '🔍',
    title: '认知重构',
    description: '识别并挑战自动负性思维，建立更平衡的认知模式',
    tag: '认知调整'
  },
  {
    id: 'emotion',
    icon: '🎈',
    title: '思维气球',
    description: '练习与负面想法解离，减少思维的控制力',
    tag: '正念练习'
  },
  {
    id: 'gratitude',
    icon: '🌼',
    title: '心灵小菜园',
    description: '记录感恩之事，提升幸福感，减少抑郁情绪',
    tag: '积极情绪'
  },
  {
    id: 'goal',
    icon: '🎯',
    title: '微目标设定',
    description: '设定5分钟小目标，打破瘫痪-自责循环，重建掌控感',
    tag: '行为激活'
  },
  {
    id: 'breathing',
    icon: '😮‍💨',
    title: '呼吸小游戏',
    description: '4-7-8呼吸法，快速缓解焦虑，激活副交感神经',
    tag: '缓解焦虑'
  }
]
</script>

<style lang="scss" scoped>
.mental-training-page {
  padding: 24px;
  padding-bottom: 80px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 40px;
  text-align: center;

  h1 {
    font-size: 32px;
    font-weight: 700;
    color: var(--text-primary, #2C3E50);
    margin: 0 0 12px;
  }

  .subtitle {
    font-size: 15px;
    color: var(--text-secondary, #6C757D);
    margin: 0;
  }
}

.training-nav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

.training-card {
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #E9ECEF;
  border-radius: 16px;
  padding: 24px;
  text-decoration: none;
  color: inherit;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(107, 141, 214, 0.15);
    border-color: #6B8DD6;

    .arrow-icon {
      transform: translateX(4px);
    }
  }

  .card-icon {
    font-size: 48px;
    margin-bottom: 16px;
    text-align: center;
  }

  .card-title {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary, #2C3E50);
    margin: 0 0 12px;
  }

  .card-description {
    font-size: 14px;
    color: var(--text-secondary, #6C757D);
    line-height: 1.6;
    margin: 0 0 16px;
    flex: 1;
  }

  .card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: auto;
    padding-top: 16px;
    border-top: 1px solid #F0F0F0;

    .card-tag {
      font-size: 12px;
      color: #6B8DD6;
      background: rgba(107, 141, 214, 0.1);
      padding: 4px 12px;
      border-radius: 12px;
      font-weight: 500;
    }

    .arrow-icon {
      color: #6B8DD6;
      transition: transform 0.3s ease;
    }
  }
}
</style>

