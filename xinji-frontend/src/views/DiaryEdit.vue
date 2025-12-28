<template>
  <div class="diary-edit-page">
    <!-- 日期选择 -->
    <div class="date-picker-section">
      <el-date-picker
        v-model="form.diaryDate"
        type="date"
        placeholder="选择日期"
        format="YYYY年MM月DD日"
        value-format="YYYY-MM-DD"
        :disabled-date="disabledDate"
      />
    </div>

    <!-- 编辑区域 -->
    <div class="editor-section">
      <el-input
        v-model="form.title"
        placeholder="标题（选填）"
        class="title-input"
        maxlength="50"
        show-word-limit
      />

      <el-input
        v-model="form.content"
        type="textarea"
        placeholder="记录今天的心情..."
        class="content-input"
        :autosize="{ minRows: 10 }"
        maxlength="10000"
        show-word-limit
      />
    </div>

    <!-- 底部操作栏 -->
    <div class="action-bar">
      <el-button 
        :loading="savingDraft"
        @click="saveDraft"
      >
        存为草稿
      </el-button>
      <el-button 
        type="primary"
        :loading="saving"
        :disabled="!canSave"
        @click="saveAndPublish"
      >
        保存
      </el-button>
    </div>

    <!-- AI分析提示 -->
    <el-dialog
      v-model="showAnalysisDialog"
      title="AI情绪分析"
      width="320px"
      :close-on-click-modal="false"
    >
      <div class="analysis-dialog-content">
        <p>日记已保存！是否进行AI情绪分析？</p>
        <p class="quota-hint">
          今日剩余额度：{{ userStore.remainingQuota }}
          <template v-if="!userStore.isPro">
            <br>
            <span class="pro-hint">升级PRO享受无限分析</span>
          </template>
        </p>
      </div>
      <template #footer>
        <el-button @click="goToDetail">稍后再说</el-button>
        <el-button 
          type="primary" 
          :disabled="userStore.remainingQuota <= 0"
          :loading="analyzing"
          @click="doAnalysis"
        >
          立即分析
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useDiaryStore } from '@/store/diary'
import { getDiaryDetail, createDiary, updateDiary } from '@/api/diary'
import { submitAnalysis } from '@/api/analysis'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const diaryStore = useDiaryStore()

const diaryId = ref<string | null>(null)
const form = ref({
  title: '',
  content: '',
  diaryDate: dayjs().format('YYYY-MM-DD')
})

const saving = ref(false)
const savingDraft = ref(false)
const analyzing = ref(false)
const showAnalysisDialog = ref(false)
const savedDiaryId = ref<string | null>(null)

// 是否可以保存
const canSave = computed(() => form.value.content.trim().length >= 10)

// 禁用未来日期
const disabledDate = (date: Date) => {
  return date > new Date()
}

// 加载日记详情（编辑模式）
const loadDiary = async (id: string) => {
  try {
    const res = await getDiaryDetail(id)
    form.value = {
      title: res.data.title || '',
      content: res.data.content,
      diaryDate: res.data.diaryDate
    }
    diaryId.value = id
  } catch (error) {
    console.error('加载日记失败:', error)
    ElMessage.error('日记加载失败')
    router.back()
  }
}

// 保存草稿
const saveDraft = async () => {
  if (!form.value.content.trim()) {
    ElMessage.warning('请输入日记内容')
    return
  }

  savingDraft.value = true
  try {
    const data = {
      ...form.value,
      isDraft: true
    }

    if (diaryId.value) {
      await updateDiary(diaryId.value, data)
    } else {
      const res = await createDiary(data)
      savedDiaryId.value = res.data.id
    }

    ElMessage.success('草稿已保存')
    router.push('/diary')
  } catch (error) {
    console.error('保存草稿失败:', error)
  } finally {
    savingDraft.value = false
  }
}

// 保存并发布
const saveAndPublish = async () => {
  if (!canSave.value) {
    ElMessage.warning('日记内容至少10个字')
    return
  }

  saving.value = true
  try {
    const data = {
      ...form.value,
      isDraft: false
    }

    let res
    if (diaryId.value) {
      res = await updateDiary(diaryId.value, data)
      savedDiaryId.value = diaryId.value
    } else {
      res = await createDiary(data)
      savedDiaryId.value = res.data.id
    }

    ElMessage.success('保存成功')
    
    // 弹出分析提示
    showAnalysisDialog.value = true
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    saving.value = false
  }
}

// 执行AI分析
const doAnalysis = async () => {
  if (!savedDiaryId.value) return
  
  analyzing.value = true
  try {
    await submitAnalysis(savedDiaryId.value)
    ElMessage.success('分析已提交')
    goToDetail()
  } catch (error) {
    console.error('提交分析失败:', error)
  } finally {
    analyzing.value = false
  }
}

// 跳转到详情页
const goToDetail = () => {
  showAnalysisDialog.value = false
  if (savedDiaryId.value) {
    router.push(`/diary/${savedDiaryId.value}`)
  } else {
    router.push('/diary')
  }
}

onMounted(() => {
  const id = route.params.id as string
  if (id) {
    loadDiary(id)
  }
})
</script>

<style lang="scss" scoped>
.diary-edit-page {
  min-height: calc(100vh - 56px - 60px);
  display: flex;
  flex-direction: column;
  padding: 16px;
  padding-bottom: 80px;
}

.date-picker-section {
  margin-bottom: 16px;

  :deep(.el-date-editor) {
    width: auto;
  }
}

.editor-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;

  .title-input {
    :deep(.el-input__inner) {
      font-size: 18px;
      font-weight: 500;
      border: none;
      background: transparent;
      padding: 0;
    }
  }

  .content-input {
    flex: 1;

    :deep(.el-textarea__inner) {
      font-size: 16px;
      line-height: 1.8;
      border: none;
      background: transparent;
      padding: 0;
      resize: none;
    }
  }
}

.action-bar {
  position: fixed;
  bottom: 60px;
  left: 0;
  right: 0;
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid var(--border-color);
  padding-bottom: calc(12px + env(safe-area-inset-bottom));

  .el-button {
    flex: 1;
    height: 44px;
  }
}

.analysis-dialog-content {
  text-align: center;

  p {
    margin: 0;
    color: var(--text-primary);
  }

  .quota-hint {
    margin-top: 12px;
    font-size: 13px;
    color: var(--text-secondary);
  }

  .pro-hint {
    color: var(--accent-color);
    font-size: 12px;
  }
}

@media (min-width: 768px) {
  .action-bar {
    max-width: 480px;
    left: 50%;
    transform: translateX(-50%);
  }
}
</style>
