<template>
  <div class="ai-counselor-page">
    <!-- 对话窗口 -->
    <div class="chat-container" ref="chatContainer">
      <!-- 消息列表 -->
      <div class="messages-list" v-loading="loadingDiaries">
        <!-- 欢迎消息 -->
        <div v-if="messages.length === 0 && !loadingDiaries" class="welcome-message">
          <div class="welcome-content">
            <div class="welcome-icon-wrapper">
              <el-icon class="welcome-icon"><ChatDotRound /></el-icon>
            </div>
            <h3>AI心理咨询师</h3>
            <p>我已经读取了您近七天的日记记录，可以为您提供专业的心理支持和指导。</p>
             <div class="diary-info" v-if="diaryCount > 0">
               <el-tag type="success" size="small">
                 已读取 {{ diaryCount }} 篇日记
               </el-tag>
             </div>
             <div class="diary-info" v-else>
               <el-tag type="info" size="small">
                 您近七天还没有日记记录
               </el-tag>
               <p class="diary-hint">建议先记录一些日记，这样我可以更好地了解您的情况</p>
             </div>
          </div>
        </div>

        <!-- 消息气泡 -->
        <div 
          v-for="(msg, index) in messages" 
          :key="index"
          class="message-item"
          :class="{ 'user-message': msg.role === 'user', 'ai-message': msg.role === 'assistant' }"
        >
          <div class="message-avatar" v-if="msg.role === 'assistant'">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="message-bubble" v-if="msg.role === 'user'">
            <div class="message-content">{{ msg.content }}</div>
            <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
          </div>
          <div class="message-avatar user-avatar" v-if="msg.role === 'user'">
            <el-icon><User /></el-icon>
          </div>
          <div class="message-bubble" v-if="msg.role === 'assistant'">
            <div class="message-content">{{ msg.content }}</div>
            <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
          </div>
        </div>

        <!-- AI 正在输入 -->
        <div v-if="isTyping" class="message-item ai-message">
          <div class="message-avatar">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="message-bubble">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            placeholder="输入您的问题或想法..."
            :disabled="isSending"
            @keyup.ctrl.enter="sendMessage"
            resize="none"
            class="message-input"
            maxlength="1000"
            show-word-limit
          />
          <el-button
            type="primary"
            :loading="isSending"
            :disabled="!inputMessage.trim() || isSending"
            @click="sendMessage"
            class="send-button"
            size="default"
          >
            <el-icon v-if="!isSending"><Promotion /></el-icon>
            <span>发送</span>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ChatDotRound, User, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getRecentDiaries, sendCounselorMessage } from '@/api/counselor'

interface Message {
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
}

const loadingDiaries = ref(false)
const diaryCount = ref(0)
const messages = ref<Message[]>([])
const inputMessage = ref('')
const isSending = ref(false)
const isTyping = ref(false)
const chatContainer = ref<HTMLElement | null>(null)

// 获取近七天的日记
const fetchRecentDiaries = async () => {
  loadingDiaries.value = true
  try {
    const res = await getRecentDiaries()
    diaryCount.value = res.data?.count || 0
  } catch (error) {
    console.error('获取日记失败:', error)
    ElMessage.error('获取日记记录失败')
  } finally {
    loadingDiaries.value = false
  }
}

// 发送消息
const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || isSending.value) return

  // 添加用户消息
  const userMessage: Message = {
    role: 'user',
    content,
    timestamp: new Date()
  }
  messages.value.push(userMessage)
  inputMessage.value = ''
  
  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 发送到后端
  isSending.value = true
  isTyping.value = true
  
  try {
    const res = await sendCounselorMessage(content)
    const aiMessage: Message = {
      role: 'assistant',
      content: res.data?.reply || '抱歉，我暂时无法回答您的问题。',
      timestamp: new Date()
    }
    messages.value.push(aiMessage)
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败，请稍后重试')
    const errorMessage: Message = {
      role: 'assistant',
      content: '抱歉，我暂时无法回答您的问题，请稍后重试。',
      timestamp: new Date()
    }
    messages.value.push(errorMessage)
  } finally {
    isSending.value = false
    isTyping.value = false
    await nextTick()
    scrollToBottom()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (chatContainer.value) {
    const messagesList = chatContainer.value.querySelector('.messages-list')
    if (messagesList) {
      messagesList.scrollTop = messagesList.scrollHeight
    }
  }
}

// 格式化时间
const formatTime = (date: Date) => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  fetchRecentDiaries()
})
</script>

<style lang="scss" scoped>
.ai-counselor-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  position: relative;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 100%;
  height: 100%;
  position: relative;
  background: #f8f9fa;
}

.messages-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  padding-bottom: 100px;
  background: #f8f9fa;
  
  // 自定义滚动条
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  
  &::-webkit-scrollbar-thumb {
    background: #d1d5db;
    border-radius: 3px;
    
    &:hover {
      background: #9ca3af;
    }
  }
}

.welcome-message {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  margin-bottom: 24px;

  .welcome-content {
    text-align: center;
    background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
    border-radius: 20px;
    padding: 40px 32px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    border: 1px solid #e9ecef;
    max-width: 800px;
    width: 100%;

    .welcome-icon-wrapper {
      margin-bottom: 20px;
      
      .welcome-icon {
        font-size: 56px;
        color: #6b8dd6;
        background: linear-gradient(135deg, rgba(107, 141, 214, 0.1) 0%, rgba(139, 164, 224, 0.1) 100%);
        padding: 20px;
        border-radius: 50%;
      }
    }

    h3 {
      font-size: 22px;
      font-weight: 600;
      color: #2c3e50;
      margin: 0 0 16px;
    }

    p {
      font-size: 15px;
      color: #495057;
      line-height: 1.7;
      margin: 12px 0;
    }

     .diary-info {
       margin-top: 20px;
       display: flex;
       flex-direction: column;
       align-items: center;
       gap: 12px;
       
       .el-tag {
         padding: 10px 32px;
         font-size: 15px;
         min-width: 240px;
         justify-content: center;
       }
       
       .diary-hint {
         font-size: 13px;
         color: #6c757d;
         margin: 0;
       }
     }
  }
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
  gap: 12px;
  animation: fadeIn 0.3s ease;

  &.user-message {
    justify-content: flex-end;

    .message-bubble {
      background: linear-gradient(135deg, #6b8dd6 0%, #8ba4e0 100%);
      border-radius: 18px 18px 4px 18px;
      box-shadow: 0 2px 8px rgba(107, 141, 214, 0.25);

      .message-content {
        color: #ffffff;
      }

      .message-time {
        color: rgba(255, 255, 255, 0.85);
      }
    }
  }

  &.ai-message {
    .message-bubble {
      background: #ffffff;
      border-radius: 18px 18px 18px 4px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      border: 1px solid #e9ecef;
    }
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(107, 141, 214, 0.15) 0%, rgba(139, 164, 224, 0.15) 100%);
  color: #6b8dd6;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

  &.user-avatar {
    background: linear-gradient(135deg, #6b8dd6 0%, #8ba4e0 100%);
    color: #ffffff;
  }
}

.message-bubble {
  max-width: 75%;
  padding: 14px 18px;
  word-wrap: break-word;

  .message-content {
    font-size: 15px;
    line-height: 1.7;
    color: #2c3e50;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .message-time {
    font-size: 11px;
    color: #6c757d;
    margin-top: 6px;
    text-align: right;
  }
}

.typing-indicator {
  display: flex;
  gap: 6px;
  padding: 10px 0;

  span {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #6b8dd6;
    animation: typing 1.4s infinite;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

.input-area {
  position: sticky;
  bottom: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  border-top: 1px solid #e9ecef;
  padding: 16px 20px;
  padding-bottom: calc(16px + env(safe-area-inset-bottom, 0));
  z-index: 10;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);

  .input-wrapper {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    gap: 12px;
    align-items: flex-end;
  }

  .message-input {
    flex: 1;
    
    :deep(.el-textarea__inner) {
      border-radius: 12px;
      border: 1px solid #dee2e6;
      padding: 12px 16px;
      font-size: 14px;
      line-height: 1.6;
      transition: all 0.2s;
      
      &:focus {
        border-color: #6b8dd6;
        box-shadow: 0 0 0 3px rgba(107, 141, 214, 0.1);
      }
    }
  }

  .send-button {
    height: 44px;
    padding: 0 24px;
    flex-shrink: 0;
    border-radius: 12px;
    font-weight: 500;
    box-shadow: 0 2px 8px rgba(107, 141, 214, 0.25);
    transition: all 0.2s;
    
    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(107, 141, 214, 0.35);
    }
    
    &:active:not(:disabled) {
      transform: translateY(0);
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .messages-list {
    padding: 16px;
    padding-bottom: 100px;
  }
  
  .message-bubble {
    max-width: 85%;
  }
  
  .input-area {
    padding: 12px 16px;
    
    .input-wrapper {
      gap: 8px;
    }
  }
}
</style>

