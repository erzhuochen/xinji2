<template>
  <div class="login-page">
    <div class="login-container">
      <!-- Logo区域 -->
      <div class="logo-section">
        <div class="logo-icon">心</div>
        <h1 class="logo-title">心迹</h1>
        <p class="logo-subtitle">记录心情，见证成长</p>
      </div>

      <!-- 登录表单 -->
      <div class="login-form">
        <el-form ref="formRef" :model="form" :rules="rules" size="large">
          <el-form-item prop="phone">
            <el-input 
              v-model="form.phone" 
              placeholder="请输入手机号"
              maxlength="11"
              :prefix-icon="Phone"
            />
          </el-form-item>

          <el-form-item prop="code">
            <div class="code-input-group">
              <el-input 
                v-model="form.code" 
                placeholder="请输入验证码"
                maxlength="6"
                :prefix-icon="Key"
              />
              <el-button 
                type="primary"
                :disabled="countdown > 0 || !isPhoneValid"
                :loading="sendingCode"
                @click="handleSendCode"
              >
                {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button 
              type="primary" 
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登录 / 注册
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 协议 -->
        <div class="agreement">
          <el-checkbox v-model="agreed" />
          <span>
            登录即表示同意
            <a href="#" @click.prevent="showAgreement('user')">《用户协议》</a>
            和
            <a href="#" @click.prevent="showAgreement('privacy')">《隐私政策》</a>
          </span>
        </div>
      </div>

      <!-- 底部装饰 -->
      <div class="footer-decoration">
        <div class="decoration-line"></div>
        <span>AI 赋能心理健康</span>
        <div class="decoration-line"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Phone, Key } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { sendCode, login } from '@/api/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const form = ref({
  phone: '',
  code: ''
})
const agreed = ref(false)
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)

// 校验规则
const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

// 手机号是否有效
const isPhoneValid = computed(() => /^1[3-9]\d{9}$/.test(form.value.phone))

// 发送验证码
const handleSendCode = async () => {
  if (!isPhoneValid.value) {
    ElMessage.warning('请输入有效的手机号')
    return
  }

  sendingCode.value = true
  try {
    await sendCode(form.value.phone)
    ElMessage.success('验证码已发送')
    
    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  } finally {
    sendingCode.value = false
  }
}

// 登录
const handleLogin = async () => {
  if (!agreed.value) {
    ElMessage.warning('请先同意用户协议和隐私政策')
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form.value.phone, form.value.code)
    
    // 保存Token
    userStore.setToken(res.data.data.token)
    
    // 获取用户信息
    await userStore.fetchUserInfo()
    
    ElMessage.success('登录成功')
    
    // 跳转到目标页面
    const redirect = route.query.redirect as string
    router.push(redirect || '/diary')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

// 显示协议
const showAgreement = (type: string) => {
  ElMessage.info(`查看${type === 'user' ? '用户协议' : '隐私政策'}`)
  // TODO: 打开协议弹窗
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-light) 0%, #fff 50%, var(--accent-color) 100%);
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 24px;
  padding: 40px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
}

.logo-section {
  text-align: center;
  margin-bottom: 40px;

  .logo-icon {
    width: 72px;
    height: 72px;
    background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
    border-radius: 20px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 36px;
    font-weight: bold;
    color: #fff;
    margin-bottom: 16px;
    box-shadow: 0 8px 24px rgba(156, 39, 176, 0.3);
  }

  .logo-title {
    font-size: 28px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 8px;
  }

  .logo-subtitle {
    font-size: 14px;
    color: var(--text-secondary);
    margin: 0;
  }
}

.login-form {
  .code-input-group {
    display: flex;
    gap: 12px;

    .el-input {
      flex: 1;
    }

    .el-button {
      width: 120px;
      flex-shrink: 0;
    }
  }

  .login-btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    border-radius: 12px;
    background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
    border: none;

    &:hover {
      opacity: 0.9;
    }
  }
}

.agreement {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 16px;

  .el-checkbox {
    margin-top: 2px;
  }

  a {
    color: var(--primary-color);
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

.footer-decoration {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 40px;
  color: var(--text-tertiary);
  font-size: 12px;

  .decoration-line {
    width: 40px;
    height: 1px;
    background: var(--border-color);
  }
}

// 响应式
@media (max-width: 480px) {
  .login-container {
    padding: 32px 24px;
    border-radius: 20px;
  }

  .logo-section {
    .logo-icon {
      width: 60px;
      height: 60px;
      font-size: 28px;
    }

    .logo-title {
      font-size: 24px;
    }
  }

  .login-form {
    .code-input-group {
      flex-direction: column;

      .el-button {
        width: 100%;
      }
    }
  }
}
</style>
