<template>
  <div class="membership-page">
    <!-- 当前会员状态 -->
    <div class="current-status" :class="{ pro: userStore.isPro }">
      <div class="status-badge">{{ userStore.isPro ? 'PRO' : 'FREE' }}</div>
      <div class="status-info">
        <span class="status-title">
          {{ userStore.isPro ? 'PRO会员' : '免费用户' }}
        </span>
        <span class="status-desc" v-if="userStore.isPro">
          有效期至 {{ formatDate(userStore.userInfo?.memberExpireTime) }}
        </span>
        <span class="status-desc" v-else>
          升级PRO享受完整功能
        </span>
      </div>
    </div>

    <!-- 功能对比 -->
    <div class="feature-compare">
      <h2 class="section-title">功能对比</h2>
      <div class="compare-table">
        <div class="table-header">
          <span class="feature-name">功能</span>
          <span class="free-col">免费版</span>
          <span class="pro-col">PRO版</span>
        </div>
        <div class="table-row" v-for="feature in features" :key="feature.name">
          <span class="feature-name">{{ feature.name }}</span>
          <span class="free-col">
            <el-icon v-if="feature.free === true" color="#4caf50"><Check /></el-icon>
            <span v-else>{{ feature.free }}</span>
          </span>
          <span class="pro-col">
            <el-icon v-if="feature.pro === true" color="#4caf50"><Check /></el-icon>
            <span v-else>{{ feature.pro }}</span>
          </span>
        </div>
      </div>
    </div>

    <!-- 套餐选择 -->
    <div class="plans-section" v-if="!userStore.isPro">
      <h2 class="section-title">选择套餐</h2>
      <div class="plans">
        <div 
          v-for="plan in plans" 
          :key="plan.type"
          class="plan-card"
          :class="{ selected: selectedPlan === plan.type, recommended: plan.recommended }"
          @click="selectedPlan = plan.type"
        >
          <div class="plan-badge" v-if="plan.recommended">推荐</div>
          <div class="plan-name">{{ plan.name }}</div>
          <div class="plan-price">
            <span class="currency">¥</span>
            <span class="amount">{{ plan.price }}</span>
            <span class="unit">{{ plan.unit }}</span>
          </div>
          <div class="plan-original" v-if="plan.originalPrice">
            原价 ¥{{ plan.originalPrice }}
          </div>
          <div class="plan-desc">{{ plan.desc }}</div>
        </div>
      </div>
    </div>

    <!-- 自动续费 -->
    <div class="auto-renew" v-if="!userStore.isPro">
      <el-checkbox v-model="autoRenew">开通自动续费（可随时取消）</el-checkbox>
    </div>

    <!-- 购买按钮 -->
    <div class="purchase-section" v-if="!userStore.isPro">
      <el-button 
        type="primary" 
        size="large"
        :loading="purchasing"
        @click="handlePurchase"
      >
        立即开通 PRO
      </el-button>
      <p class="hint">支付即表示同意《会员服务协议》</p>
    </div>

    <!-- 续费按钮（已是会员） -->
    <div class="renew-section" v-if="userStore.isPro">
      <el-button type="primary" @click="showRenewDialog = true">
        续费会员
      </el-button>
    </div>

    <!-- 续费弹窗 -->
    <el-dialog
      v-model="showRenewDialog"
      title="续费会员"
      width="320px"
    >
      <div class="renew-plans">
        <div 
          v-for="plan in plans" 
          :key="plan.type"
          class="renew-plan-item"
          :class="{ selected: selectedPlan === plan.type }"
          @click="selectedPlan = plan.type"
        >
          <span class="plan-name">{{ plan.name }}</span>
          <span class="plan-price">¥{{ plan.price }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showRenewDialog = false">取消</el-button>
        <el-button type="primary" :loading="purchasing" @click="handleRenew">
          去支付
        </el-button>
      </template>
    </el-dialog>

    <!-- 支付弹窗：模拟二维码 -->
    <el-dialog
      v-model="showPayDialog"
      title="微信支付"
      width="340px"
      :close-on-click-modal="false"
      @close="handlePayDialogClose"
    >
      <div class="pay-dialog">
        

        <div class="qr-card">
          <div class="qr-title">请使用微信扫码</div>
          <div class="qr-box" aria-label="mock-qrcode">
            <div class="qr-grid"></div>
          </div>
          <div class="qr-order" v-if="currentOrderId">
            订单号：{{ currentOrderId }}
          </div>
        </div>

        <div class="pay-actions">
          <el-button @click="handlePayDialogClose">稍后支付</el-button>
          <el-button type="primary" :loading="purchasing" class="pay-btn" @click="handleMockPaySuccess">
            我已支付
          </el-button>
        </div>

        <div class="pay-note">
          点击“我已支付”后，后端会将订单置为 PAID，并更新用户会员到期时间。
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { createOrder, mockPay } from '@/api/order'
import dayjs from 'dayjs'

const userStore = useUserStore()

const selectedPlan = ref('MONTHLY')
const autoRenew = ref(false)
const purchasing = ref(false)
const showRenewDialog = ref(false)
const showPayDialog = ref(false)
const currentOrderId = ref<string | null>(null)

// 功能对比
const features = [
  { name: '记录日记', free: true, pro: true },
  { name: 'AI情绪分析', free: '3次/天', pro: '无限' },
  { name: '情绪周报', free: true, pro: true },
  { name: '深度洞察报告', free: false, pro: true },
  { name: '情绪预测', free: false, pro: true },
  { name: '数据导出', free: false, pro: true },
  { name: '专属客服', free: false, pro: true }
]

// 套餐列表
const plans = [
  { 
    type: 'MONTHLY', 
    name: '月卡', 
    price: 19.9, 
    originalPrice: 29.9,
    unit: '/月',
    desc: '适合尝鲜体验'
  },
  { 
    type: 'QUARTERLY', 
    name: '季卡', 
    price: 49.9,
    originalPrice: 89.7,
    unit: '/3月',
    desc: '约16.6元/月',
    recommended: true
  },
  { 
    type: 'ANNUAL', 
    name: '年卡', 
    price: 149.9,
    originalPrice: 358.8,
    unit: '/年',
    desc: '约12.5元/月'
  }
]

// 格式化日期
const formatDate = (date?: string) => {
  if (!date) return ''
  return dayjs(date).format('YYYY年MM月DD日')
}

// 打开支付弹窗（创建订单后展示二维码）
const openPayDialogWithOrder = (orderId: string) => {
  currentOrderId.value = orderId
  showPayDialog.value = true
}

// 购买
const handlePurchase = async () => {
  purchasing.value = true
  try {
    const orderRes = await createOrder(selectedPlan.value, autoRenew.value)
    openPayDialogWithOrder(orderRes.data.orderId)
  } catch (error) {
    console.error('购买失败:', error)
  } finally {
    purchasing.value = false
  }
}

// 续费
const handleRenew = async () => {
  purchasing.value = true
  try {
    const orderRes = await createOrder(selectedPlan.value, false)
    openPayDialogWithOrder(orderRes.data.orderId)
    showRenewDialog.value = false
  } catch (error) {
    console.error('续费失败:', error)
  } finally {
    purchasing.value = false
  }
}

// 模拟“已扫码支付成功”
const handleMockPaySuccess = async () => {
  if (!currentOrderId.value) return

  purchasing.value = true
  try {
    await mockPay(currentOrderId.value)
    ElMessage.success('支付成功，PRO已生效')

    // 及时刷新用户信息（从数据库取最新 memberStatus / expireTime）
    await userStore.fetchUserProfile()

    showPayDialog.value = false
    currentOrderId.value = null
  } catch (error) {
    console.error('模拟支付失败:', error)
  } finally {
    purchasing.value = false
  }
}

const handlePayDialogClose = () => {
  showPayDialog.value = false
  currentOrderId.value = null
}
</script>

<style lang="scss" scoped>
.membership-page {
  padding: 16px;
  padding-bottom: 120px;
}

// 当前状态
.current-status {
  display: flex;
  align-items: center;
  gap: 16px;
  background: linear-gradient(135deg, #e0e0e0, #f5f5f5);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;

  &.pro {
    background: linear-gradient(135deg, #6f42c1, #4a148c);
    color: #fff;

    .status-badge {
      background: #FFD93D;
      color: #333;
    }
  }

  .status-badge {
    background: rgba(0, 0, 0, 0.1);
    padding: 6px 14px;
    border-radius: 16px;
    font-size: 13px;
    font-weight: 600;
  }

  .status-info {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .status-title {
      font-size: 18px;
      font-weight: 600;
    }

    .status-desc {
      font-size: 13px;
      opacity: 0.8;
    }
  }
}

// 功能对比
.feature-compare {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;

  .section-title {
    font-size: 16px;
    font-weight: 600;
    margin: 0 0 16px;
  }

  .compare-table {
    .table-header, .table-row {
      display: flex;
      padding: 12px 0;
      border-bottom: 1px solid var(--border-color);

      .feature-name {
        flex: 1;
        font-size: 14px;
      }

      .free-col, .pro-col {
        width: 60px;
        text-align: center;
        font-size: 13px;
      }
    }

    .table-header {
      font-weight: 500;
      color: var(--text-secondary);

      .pro-col {
        color: var(--primary-color);
      }
    }

    .table-row:last-child {
      border-bottom: none;
    }
  }
}

// 套餐选择
.plans-section {
  margin-bottom: 16px;

  .section-title {
    font-size: 16px;
    font-weight: 600;
    margin: 0 0 16px;
  }

  .plans {
    display: flex;
    gap: 12px;
  }

  .plan-card {
    flex: 1;
    background: #fff;
    border: 2px solid var(--border-color);
    border-radius: 16px;
    padding: 20px 12px;
    text-align: center;
    cursor: pointer;
    position: relative;
    transition: all 0.2s;

    &.selected {
      border: 2px solid var(--primary-color);
      background: var(--primary-light);
      box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.04);

      .plan-name,
      .plan-price {
        color: var(--primary-color);
        font-weight: 700;
      }
    }

    &.recommended {
      .plan-badge {
        display: block;
      }
    }

    .plan-badge {
      display: none;
      position: absolute;
      top: -10px;
      left: 50%;
      transform: translateX(-50%);
      background: var(--accent-color);
      color: #fff;
      font-size: 11px;
      padding: 2px 10px;
      border-radius: 10px;
    }

    .plan-name {
      font-size: 14px;
      font-weight: 500;
      margin-bottom: 8px;
    }

    .plan-price {
      margin-bottom: 4px;

      .currency {
        font-size: 14px;
        color: var(--primary-color);
      }

      .amount {
        font-size: 28px;
        font-weight: 600;
        color: var(--primary-color);
      }

      .unit {
        font-size: 12px;
        color: var(--text-secondary);
      }
    }

    .plan-original {
      font-size: 12px;
      color: var(--text-tertiary);
      text-decoration: line-through;
      margin-bottom: 8px;
    }

    .plan-desc {
      font-size: 12px;
      color: var(--text-secondary);
    }
  }
}

// 自动续费
.auto-renew {
  margin-bottom: 20px;
  padding: 0 8px;
}

// 购买按钮
.purchase-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px 16px 40px;



  .el-button {

    max-width: 400px;
    height: 48px;
    font-size: 16px;
    border-radius: 12px;

  }

  .hint {
    margin: 12px 0 0;
    font-size: 12px;
    color: var(--text-tertiary);
  }
}

// 续费区域
.renew-section {
  text-align: center;
  padding: 20px;
}

// 续费弹窗
.renew-plans {
  // 确保选中套餐样式醒目
  .renew-plan-item.selected {
    border: 2px solid var(--primary-color);
    background: var(--primary-light);
    box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.06);

    .plan-name,
    .plan-price {
      color: var(--primary-color);
      font-weight: 700;
    }
  }
  .renew-plan-item {
    display: flex;
    justify-content: space-between;
    padding: 12px 16px;
    margin-bottom: 8px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    cursor: pointer;

    &.selected {
      border-color: var(--primary-color);
      background: var(--primary-light);
    }

    .plan-price {
      color: var(--primary-color);
      font-weight: 500;
    }
  }
}

// 支付弹窗样式
.pay-dialog {
  text-align: center;
  .pay-tip {
    font-size: 13px;
    color: var(--text-secondary);
    margin-bottom: 12px;
  }
  .qr-card {
    background: #f8f8f8;
    padding: 16px;
    border-radius: 12px;
    margin-bottom: 20px;
    .qr-title {
      font-size: 14px;
      margin-bottom: 8px;
      font-weight: 500;
    }
    .qr-box {
      width: 160px;
      height: 160px;
      margin: 0 auto 12px;
      background-image: repeating-linear-gradient(45deg, #000 0 4px, #fff 4px 8px);
    }
    .qr-order {
      font-size: 12px;
      color: var(--text-tertiary);
    }
  }
  .pay-actions {
    display: flex;
    justify-content: center;
    gap: 12px;
    margin-bottom: 12px;
    .pay-btn {
      background: #000;
      color: #fff;
      border: none;
    }
  }
  .pay-note {
    font-size: 11px;
    color: var(--text-tertiary);
  }
}


</style>
