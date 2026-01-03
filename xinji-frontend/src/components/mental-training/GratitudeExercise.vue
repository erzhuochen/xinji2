<template>
  <el-card class="exercise-card">
    <!-- 心理学引导 -->
    <div class="psychology-note">
      <el-icon><HelpFilled /></el-icon>
      <p>
        <strong>为什么感恩有效？</strong> 心理学研究发现，每天记录3件感恩之事，持续2周可显著提升幸福感、减少抑郁情绪（Emmons & McCullough, 2003）。它帮助大脑从“问题导向”转向“资源导向”。
      </p>
    </div>

    <template #header>
      <div class="card-header">
        <span>🌼 心灵小菜园</span>
      </div>
    </template>

    <!-- 花园区域 -->
    <div class="garden">
      <div
        v-for="(plant, index) in plants"
        :key="index"
        class="plant-pot"
      >
        <div class="plant" :class="getPlantClass(plant.stage)">
          <!-- 根据阶段显示不同图标 -->
          <span v-if="plant.stage === 0">🪴</span>
          <span v-else-if="plant.stage === 1">🌱</span>
          <span v-else-if="plant.stage === 2">🌿</span>
          <span v-else-if="plant.stage >= 3">🌼</span>
        </div>
        <div v-if="plant.text" class="plant-label">{{ plant.text }}</div>
      </div>

      <!-- 空花盆提示 -->
      <div v-if="plants.length === 0" class="empty-garden">
        点击下方“播种感恩”开始培育你的小花园吧！
      </div>
    </div>

    <!-- 控制区 -->
    <div class="control-section" v-if="!completed">
      <el-button
        type="primary"
        @click="openInput"
        :disabled="plants.filter(p => p.stage >= 3).length >= 3"
      >
        {{ plants.filter(p => p.stage >= 3).length >= 3 ? '花园已满' : '播种感恩' }}
      </el-button>
    </div>

    <!-- 输入弹窗（模拟种田操作） -->
    <el-dialog
      v-model="showInput"
      title="🌱 播种一件感恩小事"
      width="90%"
      max-width="500px"
      @close="tempEntry = ''"
    >
      <el-input
        v-model="tempEntry"
        placeholder="例如：今天阳光很好 / 同事帮我带了咖啡..."
        maxlength="100"
        show-word-limit
        autofocus
      />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showInput = false">取消</el-button>
          <el-button
            type="primary"
            :disabled="!tempEntry.trim()"
            @click="submitGratitude"
          >
            播种
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 完成提示 -->
    <div v-if="completed" class="completion-message">
      🌸 你的感恩花园今日已满园绽放！
    </div>

    <!-- 小贴士 -->
    <div class="tip-section">
      💡 <em>感恩可以很小：一杯热茶、一句问候、完成一项任务……</em>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { HelpFilled } from '@element-plus/icons-vue'

// 状态
const plants = ref<Array<{ text: string; stage: number }>>([])
const showInput = ref(false)
const tempEntry = ref('')
const completed = ref(false)

// 最多3株植物
const maxPlants = 3

// 打开输入框
const openInput = () => {
  if (plants.value.length >= maxPlants) return
  showInput.value = true
}

// 提交感恩事项
const submitGratitude = () => {
  const text = tempEntry.value.trim()
  if (!text) return

  // 添加新植物（初始阶段0：空花盆）
  plants.value.push({ text, stage: 0 })
  showInput.value = false
  tempEntry.value = ''

  // 模拟生长过程：0 → 1 → 2 → 3（每1000ms升一级）
  let currentStage = 0
  const growInterval = setInterval(() => {
    const lastPlant = plants.value[plants.value.length - 1]
    if (lastPlant && currentStage < 3) {
      currentStage++
      lastPlant.stage = currentStage
    } else {
      clearInterval(growInterval)
      // 检查是否全部完成
      if (plants.value.length === maxPlants && plants.value.every(p => p.stage >= 3)) {
        completed.value = true
        setTimeout(() => {
          completed.value = false
        }, 5000)
      }
    }
  }, 1000)
}

// 植物样式类（可扩展动画）
const getPlantClass = (stage: number) => {
  return `plant-stage-${stage}`
}
</script>

<style scoped lang="scss">
@import '@/styles/variables.scss';

.exercise-card {
  max-width: 600px;
  margin: 0 auto;
}

.psychology-note {
  background-color: rgba($primary-light, 0.15);
  border-left: 4px solid $primary-color;
  padding: $spacing-sm $spacing-md;
  margin-bottom: $spacing-lg;
  border-radius: 0 $border-radius-md $border-radius-md 0;
  font-size: $font-size-sm;
  color: $text-secondary;

  .el-icon {
    color: $primary-color;
    vertical-align: middle;
    margin-right: 8px;
  }

  strong {
    color: $text-primary;
  }
}

.garden {
  display: flex;
  justify-content: center;
  gap: $spacing-lg;
  margin: $spacing-xl 0;
  min-height: 120px;
  flex-wrap: wrap;
}

.plant-pot {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 80px;
}

.plant {
  font-size: 28px;
  margin-bottom: $spacing-xs;
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.plant-label {
  font-size: $font-size-sm;
  color: $text-muted;
  text-align: center;
  max-width: 80px;
  word-break: break-word;
}

.empty-garden {
  color: $text-muted;
  font-size: $font-size-sm;
  text-align: center;
  width: 100%;
  padding: $spacing-md 0;
}

.control-section {
  text-align: center;
  margin: $spacing-lg 0;
}

.completion-message {
  text-align: center;
  color: $success-color;
  font-weight: 600;
  margin: $spacing-lg 0;
  padding: $spacing-md;
  background: rgba($success-color, 0.1);
  border-radius: $border-radius-md;
}

.tip-section {
  margin-top: $spacing-lg;
  padding-top: $spacing-md;
  border-top: 1px dashed $border-color;
  font-size: $font-size-sm;
  color: $text-muted;
  text-align: center;
}

// 弹窗底部按钮对齐
:deep(.dialog-footer) {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-sm;
}
</style>