<template>
  <div class="cheer-page-wrapper">
    <div class="cheer-container">
      <!-- 头部 -->
      <div class="cheer-header">
        <h1>✨ 心灵加油站</h1>
      </div>

      <!-- 主体 -->
      <div class="cheer-content">
        <!-- 抽签展示 -->
        <div class="draw-box">
          <div v-if="currentQuote" class="quote-display">
            <p class="quote-text">“{{ currentQuote }}”</p>
          </div>
          <p v-else class="hint">点击下方按钮抽取今日加油话语</p>
          
          <el-button type="primary" size="large" @click="drawQuote" class="draw-btn">
            抽取今日能量
          </el-button>
        </div>

        <!-- 分割线 -->
        <div class="divider"></div>

        <!-- 新增词条 -->
        <div class="add-box">
          <h3 class="sub-title">分享一句你的加油词</h3>
          <el-input
            v-model="newQuote"
            placeholder="输入一句鼓励的话，分享给他人"
            @keyup.enter="addQuote"
          />
          <el-button @click="addQuote" class="add-btn">分享善意</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRandomQuote, addQuote as apiAddQuote } from '@/api/cheer'

const currentQuote = ref('')
const newQuote = ref('')

// 抽取随机词条
const drawQuote = async () => {
  try {
    const res = await getRandomQuote()
    const apiRes = res.data
    if (apiRes && apiRes.data) {
      currentQuote.value = apiRes.data.content
    } else {
      currentQuote.value = '今天也要元气满满哦！'
    }
  } catch (error) {
    ElMessage.error('获取加油语句失败')
    console.error(error)
  }
}

// 添加词条
const addQuote = async () => {
  const content = newQuote.value.trim()
  if (!content) {
    ElMessage.warning('请输入内容')
    return
  }
  try {
    await apiAddQuote(content)
    ElMessage.success('感谢你的分享！')
    newQuote.value = ''
  } catch (error) {
    ElMessage.error('添加失败')
    console.error(error)
  }
}


</script>

<style scoped lang="scss">
.cheer-page-wrapper {
  min-height: 100vh;
  background: var(--background-color);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 60px 20px 20px;
}

.cheer-container {
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  max-width: 600px;
  width: 100%;
  min-height: 600px;
  display: flex;
  flex-direction: column;
}

.cheer-header {
  padding: 24px 32px 0;
  h1 {
    font-size: 24px;
    font-weight: 600;
    color: #333;
    margin: 0;
  }
}

.cheer-content {
  flex: 1;
  padding: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.draw-box {
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24px;

  .quote-display {
    min-height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .quote-text {
    font-size: 22px;
    font-weight: 500;
    color: var(--text-primary);
    line-height: 1.6;
    max-width: 400px;
  }

  .hint {
    color: var(--text-tertiary);
    min-height: 100px;
  }

  .draw-btn {
    padding: 16px 32px;
    font-size: 18px;
    border-radius: 12px;
  }
}

.divider {
  width: 80%;
  height: 1px;
  background: var(--border-color);
  margin: 24px 0;
}

.add-box {
  width: 100%;
  max-width: 400px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  .sub-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-secondary);
  }

  .add-btn {
    align-self: flex-end;
  }
}

@media (max-width: 768px) {
  .cheer-container {
    border-radius: 0;
    min-height: 100vh;
  }
}
</style>