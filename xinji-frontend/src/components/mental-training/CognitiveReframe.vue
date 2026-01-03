<template>
  <el-card class="exercise-card">
    <div class="psychology-note">
      <el-icon><HelpFilled /></el-icon>
      <p>
        <strong>思维决定感受</strong>。认知行为疗法（CBT）指出，我们对事件的解释（而非事件本身）引发情绪。通过识别并挑战“自动负性思维”，可建立更平衡的认知模式（Beck, 1979）。
      </p>
    </div>

    <template #header>
      <div class="card-header">
        <span>🔍 认知重构</span>
      </div>
    </template>

    <el-form @submit.prevent="handleSubmit" class="reframe-form">
      <el-form-item label="触发事件">
        <el-input v-model="situation" placeholder="发生了什么？" />
      </el-form-item>
      <el-form-item label="自动想法">
        <el-input
          v-model="negativeThought"
          type="textarea"
          :rows="2"
          placeholder="当时你脑中闪过的负面想法？例如：“我总是搞砸”"
        />
      </el-form-item>
      <el-form-item label="更平衡的想法">
        <el-input
          v-model="balancedThought"
          type="textarea"
          :rows="2"
          placeholder="换个角度：证据是什么？最坏/最好/最可能的结果？"
        />
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="saving">保存重构</el-button>
    </el-form>

    <div v-if="saved" class="saved-tip">✅ 已保存！</div>

    <div class="tip-section">
      💡 <em>问自己：“这个想法有证据吗？有没有其他解释？”</em>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { saveToStorage } from '@/utils/storage'
import { HelpFilled } from '@element-plus/icons-vue'

const situation = ref('')
const negativeThought = ref('')
const balancedThought = ref('')
const saved = ref(false)
const saving = ref(false)

const handleSubmit = () => {
  if (!situation.value.trim() || !negativeThought.value.trim()) return
  
  saving.value = true
  const date = new Date().toISOString().split('T')[0]
  saveToStorage(`reframe_${date}`, {
    situation: situation.value.trim(),
    negative: negativeThought.value.trim(),
    balanced: balancedThought.value.trim()
  })
  saved.value = true
  saving.value = false
  setTimeout(() => saved.value = false, 2000)
}
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

.reframe-form {
  .el-form-item {
    margin-bottom: $spacing-lg;
    
    .el-form-item__label {
      color: $text-primary;
      font-weight: 500;
      margin-bottom: $spacing-xs;
    }
  }

  .el-button {
    width: 100%;
  }
}

.saved-tip {
  text-align: center;
  color: $success-color;
  margin-top: $spacing-md;
  font-weight: 500;
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