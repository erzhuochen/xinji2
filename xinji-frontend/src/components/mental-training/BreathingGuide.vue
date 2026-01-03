<template>
  <el-card class="exercise-card">
    <!-- 心理学依据 -->
    <div class="psychology-note">
      <el-icon><HelpFilled /></el-icon>
      <p>
        <strong>呼吸是情绪的开关</strong>。4-7-8呼吸法通过延长呼气激活副交感神经，5分钟内可降低心率、血压和皮质醇水平，快速缓解焦虑（Weil, 2015）。
      </p>
    </div>

    <template #header>
      <div class="card-header">
        <span>🌬️ 呼吸小游戏</span>
      </div>
    </template>

    <!-- 游戏区域 -->
    <div class="breathing-game">
      <div
        class="breath-circle"
        :class="{ 'inhale': currentPhase === 'inhale', 'hold': currentPhase === 'hold', 'exhale': currentPhase === 'exhale' }"
        :style="{ transform: `scale(${scale})` }"
      ></div>

      <!-- 阶段提示 -->
      <div class="phase-label">{{ phaseLabel }}</div>

      <!-- 轮次提示 -->
      <div class="round-info" v-if="isActive">
        第 {{ round }} 轮 · 共 4 轮
      </div>
    </div>

    <!-- 控制按钮 -->
    <div class="control-buttons">
      <el-button v-if="!isActive" type="primary" @click="start">开始呼吸练习</el-button>
      <el-button v-else plain @click="stop">提前结束</el-button>
    </div>

    <!-- 小贴士 -->
    <div class="tip-section">
      💡 <em>看着圆圈：它变大时吸气，静止时屏住，变小时缓缓呼气。</em>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { HelpFilled } from '@element-plus/icons-vue'

const isActive = ref(false)
const currentPhase = ref<'inhale' | 'hold' | 'exhale'>('inhale')
const scale = ref(1) // 1 = 默认大小，1.5 = 最大（吸气）
const round = ref(1)
const timer = ref<number | null>(null)

const phaseDurations = {
  inhale: 4,
  hold: 7,
  exhale: 8
}

const phaseLabels = {
  inhale: '吸气...',
  hold: '屏住...',
  exhale: '呼气...'
}

const phaseLabel = computed(() => phaseLabels[currentPhase.value])

// 平滑缩放函数（用于动画）
const animateScale = (target: number, duration: number) => {
  const start = scale.value
  const startTime = Date.now()
  const frame = () => {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)
    // 使用 ease-in-out 缓动
    const eased = 1 - Math.pow(1 - progress, 2) // 简单缓动
    scale.value = start + (target - start) * eased
    if (progress < 1 && isActive.value) {
      requestAnimationFrame(frame)
    }
  }
  requestAnimationFrame(frame)
}

const nextPhase = () => {
  if (!isActive.value) return

  if (currentPhase.value === 'inhale') {
    currentPhase.value = 'hold'
    // 保持最大尺寸
  } else if (currentPhase.value === 'hold') {
    currentPhase.value = 'exhale'
    animateScale(1, phaseDurations.exhale * 1000)
  } else {
    // exhale 结束
    if (round.value >= 4) {
      // 完成全部轮次
      finish()
      return
    }
    round.value++
    currentPhase.value = 'inhale'
    animateScale(1.5, phaseDurations.inhale * 1000)
  }

  // 启动下一阶段定时器
  const nextDuration = phaseDurations[currentPhase.value]
  setTimeout(nextPhase, nextDuration * 1000)
}

const start = () => {
  isActive.value = true
  round.value = 1
  currentPhase.value = 'inhale'
  scale.value = 1
  animateScale(1.5, phaseDurations.inhale * 1000)
  setTimeout(nextPhase, phaseDurations.inhale * 1000)
}

const finish = () => {
  isActive.value = false
  currentPhase.value = 'inhale'
  scale.value = 1
  // 可选：震动反馈
  document.querySelector('.breath-circle')?.classList.add('pulse')
  setTimeout(() => {
    document.querySelector('.breath-circle')?.classList.remove('pulse')
  }, 600)
}

const stop = () => {
  isActive.value = false
  currentPhase.value = 'inhale'
  scale.value = 1
  if (timer.value) clearInterval(timer.value)
}

onUnmounted(() => {
  if (timer.value) clearInterval(timer.value)
})
</script>

<style scoped lang="scss">
@import '@/styles/variables.scss';

.exercise-card {
  max-width: 600px;
  margin: 0 auto;
  text-align: center;
}

.psychology-note {
  background-color: rgba($info-color, 0.1);
  border-left: 4px solid $info-color;
  padding: $spacing-sm $spacing-md;
  margin-bottom: $spacing-lg;
  border-radius: 0 $border-radius-md $border-radius-md 0;
  font-size: $font-size-sm;
  color: $text-secondary;

  .el-icon {
    color: $info-color;
    vertical-align: middle;
    margin-right: 8px;
  }

  strong {
    color: $text-primary;
  }
}

.breathing-game {
  position: relative;
  height: 240px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  margin: $spacing-xl 0;
}

.breath-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba($primary-color, 0.2), rgba($emotion-happy, 0.2));
  box-shadow: 0 4px 12px rgba($primary-color, 0.2);
  transition: transform 0.2s ease;
  margin-bottom: $spacing-lg;
  
  &.inhale {
    background: linear-gradient(135deg, rgba($emotion-happy, 0.3), rgba($primary-light, 0.2));
  }
  &.hold {
    background: linear-gradient(135deg, rgba($emotion-neutral, 0.3), rgba($secondary-color, 0.2));
  }
  &.exhale {
    background: linear-gradient(135deg, rgba($emotion-sad, 0.3), rgba($primary-color, 0.2));
  }

  &.pulse {
    animation: pulse 0.6s ease-in-out;
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.phase-label {
  font-size: $font-size-lg;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: $spacing-sm;
}

.round-info {
  font-size: $font-size-sm;
  color: $text-secondary;
}

.control-buttons {
  margin: $spacing-lg 0;
}

.tip-section {
  margin-top: $spacing-lg;
  padding-top: $spacing-md;
  border-top: 1px dashed $border-color;
  font-size: $font-size-sm;
  color: $text-muted;
}
</style>
