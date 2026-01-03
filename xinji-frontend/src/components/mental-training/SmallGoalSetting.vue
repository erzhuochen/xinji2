<template>
  <el-card class="exercise-card">
    <div class="psychology-note">
      <el-icon><HelpFilled /></el-icon>
      <p>
        <strong>小胜积累大信心</strong>。行为激活理论表明，完成微小目标能打破“瘫痪-自责”循环，释放多巴胺，重建掌控感。5分钟任务是启动行动的最佳单位（Martell et al., 2010）。
      </p>
    </div>

    <template #header>
      <div class="card-header">
        <span>🎯 微目标设定</span>
      </div>
    </template>

    <el-form @submit.prevent="handleSubmit" class="goal-form">
      <el-form-item>
        <el-input
          v-model="goal"
          placeholder="写下一件5分钟内能完成的小事，例如：整理桌面、给植物浇水..."
          maxlength="100"
          show-word-limit
        />
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="saving">设定目标</el-button>
    </el-form>

    <div v-if="saved" class="saved-tip">✅ 目标已设定！完成后记得打勾 ✅</div>

    <div class="tip-section">
      💡 <em>关键是“小”：越简单，越容易开始。完成比完美重要。</em>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { saveToStorage } from '@/utils/storage'
import { HelpFilled } from '@element-plus/icons-vue'

const goal = ref('')
const saved = ref(false)
const saving = ref(false)

const handleSubmit = () => {
  if (!goal.value.trim()) return
  
  saving.value = true
  const date = new Date().toISOString().split('T')[0]
  saveToStorage(`small_goal_${date}`, goal.value.trim())
  saved.value = true
  saving.value = false
  setTimeout(() => saved.value = false, 3000)
}
</script>

<style scoped lang="scss">
@import '@/styles/variables.scss';

.exercise-card {
  max-width: 600px;
  margin: 0 auto;
}

.psychology-note {
  background-color: rgba($success-color, 0.1);
  border-left: 4px solid $success-color;
  padding: $spacing-sm $spacing-md;
  margin-bottom: $spacing-lg;
  border-radius: 0 $border-radius-md $border-radius-md 0;
  font-size: $font-size-sm;
  color: $text-secondary;

  .el-icon {
    color: $success-color;
    vertical-align: middle;
    margin-right: 8px;
  }

  strong {
    color: $text-primary;
  }
}

.goal-form {
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