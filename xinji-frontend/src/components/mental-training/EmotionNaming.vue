<template>
  <el-card class="exercise-card">
    <!-- 心理学依据 -->
    <div class="psychology-note">
      <el-icon><HelpFilled /></el-icon>
      <p>
        <strong>想法只是想法</strong>。接纳承诺疗法（ACT）指出，当我们把负面思维看作“脑海中的文字”而非事实，就能减少其控制力。点击气球，练习与想法解离（Hayes et al., 1999）。
      </p>
    </div>

    <template #header>
      <div class="card-header">
        <span>🎈 思维气球</span>
      </div>
    </template>

    <!-- 游戏区域 -->
    <div class="game-area" ref="gameArea">
      <div
        v-for="balloon in balloons"
        :key="balloon.id"
        class="thought-balloon"
        :class="{ 'exploding': explodingId === balloon.id }"
        :style="{
          left: balloon.x + 'px',
          bottom: balloon.y + 'px',
          backgroundColor: balloon.color
        }"
        @click="popBalloon(balloon.id)"
      >
        {{ explodingId === balloon.id ? '这只是一个想法' : balloon.text }}
      </div>

      <!-- 完成提示 -->
      <div v-if="completed" class="completion-message">
        🎉 你成功解离了 {{ targetCount }} 个自动负性思维！
      </div>
    </div>

    <!-- 控制按钮 -->
    <div class="control-section" v-if="!completed">
      <el-button
        type="primary"
        :disabled="isActive"
        @click="startGame"
      >
        {{ isActive ? '进行中...' : '开始击破思维气球' }}
      </el-button>
    </div>

    <!-- 小贴士 -->
    <div class="tip-section">
      💡 <em>点击飘起的气球，提醒自己：“这只是一个想法，不是事实。”</em>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { HelpFilled } from '@element-plus/icons-vue'

// 游戏配置
const targetCount = 5
const gameDuration = 60000 // 1分钟超时保护
const balloonSpeed = 30

// 状态
const isActive = ref(false)
const completed = ref(false)
const balloons = ref<Array<{
  id: number
  text: string
  x: number
  y: number
  color: string
}>>([])
const poppedCount = ref(0)
const explodingId = ref<number | null>(null)
const gameArea = ref<HTMLDivElement | null>(null)
const gameTimer = ref<number | null>(null)
const spawnTimer = ref<number | null>(null)

// 负面思维库
const negativeThoughts = [
  '我肯定做不好',
  '没人喜欢我',
  '我又搞砸了',
  '未来没有希望',
  '我太敏感了',
  '别人都比我强',
  '我不值得被爱',
  '事情永远不会变好'
]

// 气球颜色
const thoughtColors = [
  '#FFB6C1', '#E6E6FA', '#FFFACD', '#E0FFFF', '#F0E68C'
]

const startGame = () => {
  if (isActive.value) return
  isActive.value = true
  completed.value = false
  poppedCount.value = 0
  balloons.value = []
  explodingId.value = null

  spawnBalloon()
  spawnTimer.value = window.setInterval(spawnBalloon, 2500)
  gameTimer.value = window.setTimeout(() => finishGame(), gameDuration)
}

const spawnBalloon = () => {
  if (!isActive.value || poppedCount.value >= targetCount) return

  const areaWidth = gameArea.value?.clientWidth || 400
  const id = Date.now() + Math.random()
  const text = negativeThoughts[Math.floor(Math.random() * negativeThoughts.length)]
  const color = thoughtColors[Math.floor(Math.random() * thoughtColors.length)]
  const x = Math.random() * (areaWidth - 80)

  balloons.value.push({ id, text, x, y: -60, color })
  animateBalloon(id)
}

const animateBalloon = (id: number) => {
  const balloon = balloons.value.find(b => b.id === id)
  if (!balloon || !isActive.value || explodingId.value === id) return

  balloon.y += balloonSpeed / 10
  if (balloon.y > 300) {
    balloons.value = balloons.value.filter(b => b.id !== id)
    return
  }
  setTimeout(() => animateBalloon(id), 100)
}

const popBalloon = (id: number) => {
  if (!isActive.value) return

  explodingId.value = id
  poppedCount.value++

  setTimeout(() => {
    balloons.value = balloons.value.filter(b => b.id !== id)
    explodingId.value = null

    if (poppedCount.value >= targetCount) {
      finishGame()
    }
  }, 300)
}

const finishGame = () => {
  isActive.value = false
  completed.value = true

  if (spawnTimer.value) clearInterval(spawnTimer.value)
  if (gameTimer.value) clearTimeout(gameTimer.value)

  // 不再保存任何数据 ✅

  setTimeout(() => {
    completed.value = false
  }, 5000)
}

onUnmounted(() => {
  if (spawnTimer.value) clearInterval(spawnTimer.value)
  if (gameTimer.value) clearTimeout(gameTimer.value)
})
</script>

<style scoped lang="scss">
@import '@/styles/variables.scss';

.exercise-card {
  max-width: 600px;
  margin: 0 auto;
}

.psychology-note {
  background-color: rgba($warning-color, 0.1);
  border-left: 4px solid $warning-color;
  padding: $spacing-sm $spacing-md;
  margin-bottom: $spacing-lg;
  border-radius: 0 $border-radius-md $border-radius-md 0;
  font-size: $font-size-sm;
  color: $text-secondary;

  .el-icon {
    color: $warning-color;
    vertical-align: middle;
    margin-right: 8px;
  }

  strong {
    color: $text-primary;
  }
}

.game-area {
  position: relative;
  height: 240px;
  background: #f9f9fb;
  border-radius: $border-radius-md;
  overflow: hidden;
  margin: $spacing-lg 0;
  border: 1px dashed $border-color;
}

.thought-balloon {
  position: absolute;
  padding: $spacing-xs $spacing-sm;
  border-radius: 20px;
  font-size: $font-size-sm;
  color: #333;
  cursor: pointer;
  user-select: none;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  transition: transform 0.1s ease, opacity 0.3s ease;
  max-width: 120px;
  word-break: break-word;
  text-align: center;
  z-index: 1;

  &:hover {
    transform: scale(1.05);
  }

  &:active {
    transform: scale(0.95);
  }

  &.exploding {
    animation: balloonPop 0.3s forwards;
    pointer-events: none;
  }
}

@keyframes balloonPop {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(0.6);
    opacity: 0;
  }
}

.completion-message {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba($success-color, 0.15);
  color: $success-color;
  padding: $spacing-md;
  border-radius: $border-radius-md;
  font-weight: 600;
  text-align: center;
  width: 80%;
  z-index: 2;
}

.control-section {
  text-align: center;
  margin: $spacing-lg 0;
}

.tip-section {
  margin-top: $spacing-lg;
  padding-top: $spacing-md;
  border-top: 1px dashed $border-color;
  font-size: $font-size-sm;
  color: $text-muted;
  text-align: center;
}
</style>