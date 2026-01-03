<template>
  <div class="meditation-page">
    <div class="meditation-container">
      <!-- 头部 -->
      <div class="meditation-header">
        <h1>🧘 5分钟呼吸冥想</h1>
      </div>

      <!-- 主体 -->
      <div class="meditation-content">
        <!-- 倒计时 -->
        <div class="timer-display" v-if="isActive">
          <div class="time-circle">
            <div class="time-text">{{ formatTime(remainingTime) }}</div>
            <svg class="progress-circle" width="200" height="200">
              <circle cx="100" cy="100" r="90" stroke="#e0e0e0" stroke-width="10" fill="none" />
              <circle
                cx="100"
                cy="100"
                r="90"
                stroke="var(--primary-color)"
                stroke-width="10"
                fill="none"
                stroke-linecap="round"
                :stroke-dasharray="circleLength"
                :stroke-dashoffset="circleLength * (1 - progress)"
                transform="rotate(-90 100 100)"
                class="progress-bar"
              />
            </svg>
          </div>
          <div class="breath-instruction">
            <div class="breath-text">{{ currentInstruction }}</div>
            <div class="breath-guide">{{ breathGuide }}</div>
          </div>
        </div>

        <!-- 开始 -->
        <div class="start-screen" v-else-if="!isCompleted">
          <div class="start-icon">🧘‍♀️</div>
          <h2>准备开始5分钟呼吸冥想</h2>
          <p class="start-description">
            在接下来的5分钟里，我们将通过专注呼吸来帮助你放松身心，缓解压力和焦虑。
          </p>

          <!-- 指南 -->
          <div class="instructions">
            <h3>练习指导</h3>
            <div class="instruction-steps">
              <div class="step" v-for="(step, i) in steps" :key="i">
                <div class="step-number">{{ i + 1 }}</div>
                <div class="step-content" v-html="step"></div>
              </div>
            </div>
          </div>

          <div class="tips">
            <h3>练习提示</h3>
            <ul>
              <li v-for="tip in tips" :key="tip">{{ tip }}</li>
            </ul>
          </div>

          <el-button type="primary" size="large" @click="startMeditation" class="start-btn">
            开始冥想练习
          </el-button>
        </div>

        <!-- 完成 -->
        <div class="completion-screen" v-if="isCompleted">
          <h2>冥想练习完成！</h2>
          <p class="completion-message">
            恭喜你完成了5分钟的呼吸冥想练习！<br />希望这次练习能帮助你感到更加放松和平静。
          </p>

          <div class="completion-actions">
            <el-button @click="goBack" type="default">返回</el-button>
            <el-button @click="restartMeditation" type="primary">再来一次</el-button>
          </div>
        </div>
      </div>

      <!-- 背景声音控制 -->
      <div class="background-controls" v-if="isActive">
        <el-button text @click="toggleSound">{{ isSoundOn ? '🔊 关闭提示音' : '🔇 开启提示音' }}</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'


const router = useRouter()

// 状态
const isActive = ref(false)
const isCompleted = ref(false)
const totalTime = 300 // s
const remainingTime = ref(totalTime)
const currentPhase = ref<'inhale' | 'hold' | 'exhale'>('inhale')
const phaseTime = ref(0)
const isSoundOn = ref(true)

const circleLength = 2 * Math.PI * 90

let timer: number | null = null
let audioContext: AudioContext | null = null
let audioBuffers: Record<string, AudioBuffer> = {}

// 文字步骤
const steps = [
  '<strong>深呼吸</strong><br>用鼻子慢慢吸气4秒',
  '<strong>屏息</strong><br>保持呼吸4秒',
  '<strong>呼气</strong><br>用嘴慢慢呼气4秒',
  '<strong>重复</strong><br>循环这个过程5分钟'
]

const tips = [
  '找一个安静舒适的地方',
  '保持舒适的坐姿或躺姿',
  '闭上眼睛或保持柔和的目光',
  '专注于呼吸的感觉',
  '当思维游移时，温和地拉回注意力'
]

// 进度
const progress = computed(() => (totalTime - remainingTime.value) / totalTime)

const formatTime = (sec: number) => {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

const currentInstruction = computed(() => {
  if (!isActive.value) return ''
  return currentPhase.value === 'inhale'
    ? '吸气'
    : currentPhase.value === 'hold'
    ? '屏息'
    : '呼气'
})

const breathGuide = computed(() => {
  if (!isActive.value) return ''
  return `${4 - phaseTime.value}秒`
})

// 导航
const goBack = () => router.back()

// 音频生成
const initAudio = () => {
  try {
    audioContext = new (window.AudioContext || (window as any).webkitAudioContext)()
    audioBuffers.inhale = createBell(440)
    audioBuffers.hold = createBell(330)
    audioBuffers.exhale = createBell(523)
    audioBuffers.complete = createBell(660)
  } catch (e) {
    console.warn('Audio init failed', e)
  }
}

const createBell = (freq: number): AudioBuffer => {
  if (!audioContext) throw new Error('no ctx')
  const dur = 0.8
  const sr = audioContext.sampleRate
  const frames = sr * dur
  const buf = audioContext.createBuffer(1, frames, sr)
  const data = buf.getChannelData(0)
  for (let i = 0; i < frames; i++) {
    const t = i / sr
    const env = Math.exp(-3 * t)
    data[i] = Math.sin(2 * Math.PI * freq * t) * env * 0.4
  }
  return buf
}

const play = (key: string) => {
  if (!isSoundOn.value || !audioContext) return
  const buffer = audioBuffers[key]
  if (!buffer) return
  const src = audioContext.createBufferSource()
  src.buffer = buffer
  src.connect(audioContext.destination)
  src.start(0)
}

// 启动冥想
const startMeditation = () => {
  isActive.value = true
  isCompleted.value = false
  remainingTime.value = totalTime
  currentPhase.value = 'inhale'
  phaseTime.value = 0
  ElMessage.success('冥想开始 ✨')
  play('inhale')
  startTimer()
}

const restartMeditation = () => {
  clearTimer()
  startMeditation()
}

const startTimer = () => {
  timer = window.setInterval(() => {
    remainingTime.value--
    phaseTime.value++

    if (phaseTime.value >= 4) {
      switch (currentPhase.value) {
        case 'inhale':
          currentPhase.value = 'hold'
          play('hold')
          break
        case 'hold':
          currentPhase.value = 'exhale'
          play('exhale')
          break
        case 'exhale':
          currentPhase.value = 'inhale'
          play('inhale')
          break
      }
      phaseTime.value = 0
    }

    if (remainingTime.value <= 0) {
      complete()
    }
  }, 1000)
}

const complete = () => {
  clearTimer()
  isActive.value = false
  isCompleted.value = true
  play('complete')
  ElMessage.success('🎉 冥想完成')
}

const clearTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

const toggleSound = () => {
  isSoundOn.value = !isSoundOn.value
  ElMessage.info(isSoundOn.value ? '提示音已开启' : '提示音已关闭')
}

onMounted(() => {
  initAudio()
})

onUnmounted(() => {
  clearTimer()
  if (audioContext && audioContext.state !== 'closed') audioContext.close()
})
</script>

<style scoped>
.meditation-page {
  min-height: 100vh;
  background: var(--background-color);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.meditation-container {
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  max-width: 600px;
  width: 100%;
  min-height: 600px;
  display: flex;
  flex-direction: column;
}
.meditation-header {
  padding: 24px 32px 0;
  display: flex;
  align-items: center;
  gap: 16px;
}
.meditation-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
  flex: 1;
}
.back-btn {
  color: #666;
}
.meditation-content {
  flex: 1;
  padding: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.timer-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}
.time-circle { position: relative; display: flex; align-items: center; justify-content: center; }
.time-text { font-size: 48px; font-weight: 700; color: var(--primary-color); position: absolute; z-index: 2; }
.progress-bar { transition: stroke-dashoffset 1s ease-in-out; }
.start-screen { max-width: 500px; width: 100%; }
.start-icon { font-size: 64px; margin-bottom: 24px; }
.start-screen h2 { font-size: 28px; font-weight: 600; color: #333; margin-bottom: 16px; }
.start-description { color: #666; line-height: 1.6; margin-bottom: 32px; }
.instructions, .tips { text-align: left; margin-bottom: 24px; }
.instructions h3, .tips h3 { font-size: 18px; font-weight: 600; color: #333; margin-bottom: 16px; }
.instruction-steps { display: flex; flex-direction: column; gap: 12px; }
.step { display: flex; align-items: flex-start; gap: 12px; padding: 12px; background: #f8f9fa; border-radius: 8px; }
.step-number { width: 24px; height: 24px; background: var(--primary-color); color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; flex-shrink: 0; }
.step-content { flex: 1; text-align: left; line-height: 1.5; }
.tips ul { list-style: none; margin: 0; padding: 0; }
.tips li { padding: 8px 0 8px 20px; color: #666; position: relative; }
.tips li:before { content: '•'; color: var(--primary-color); font-weight: bold; position: absolute; left: 0; }
.start-btn { margin-top: 32px; padding: 16px 32px; font-size: 18px; border-radius: 12px; }
.completion-screen { max-width: 400px; width: 100%; }
.completion-screen h2 { font-size: 28px; font-weight: 600; color: #333; margin-bottom: 16px; }
.completion-message { color: #666; line-height: 1.6; margin-bottom: 32px; }
.completion-actions { display: flex; gap: 16px; justify-content: center; }
.background-controls { padding: 16px 32px; border-top: 1px solid #e0e0e0; text-align: center; }
.breath-text { font-size: 24px; font-weight: 600; color: var(--primary-color); margin-bottom: 8px; }
.breath-guide { font-size: 18px; color: #666; }
@media (max-width: 768px) {
  .meditation-container { border-radius: 0; min-height: 100vh; }
  .time-text { font-size: 36px; }
  .completion-actions { flex-direction: column; }
  .completion-actions .el-button { width: 100%; }
}
</style>

