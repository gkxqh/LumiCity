<!--
  注册页 — 苹果液态玻璃（Liquid Glass）设计风格，与登录页一致
  - 全屏背景图 + 半透明磨砂覆盖层
  - 居中液态玻璃面板：多层 backdrop-filter blur、内发光
  - 背景层动态追光、卡片边缘跟随鼠标扫光
  - 输入框同款小玻璃样式，hover/focus 平滑过渡
  - 注册按钮同款玻璃质感，点击压缩反馈
-->
<template>
  <div class="reg-container" @mousemove="onMouseMove" @mouseenter="onMouseEnter" @mouseleave="onMouseLeave">
    <!-- 动态追光 — 跟随鼠标的环境光晕 -->
    <div
      class="light-source"
      :style="lightSourceStyle"
    ></div>

    <div class="reg-card" :style="cardEdgeStyle">

      <!-- 标题 -->
      <div class="reg-title">用户注册</div>
      <div class="reg-subtitle">Smart City Lighting Control System</div>

      <!-- 注册表单 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名（4-16位）"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码（6-16位）"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请确认密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="请输入昵称"
            :prefix-icon="UserFilled"
            clearable
          />
        </el-form-item>

        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
            :prefix-icon="Phone"
            clearable
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱"
            :prefix-icon="Message"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button
            class="glass-btn"
            :loading="loading"
            style="width: 100%"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="reg-footer">
        已有账号？<router-link to="/login" class="link">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Phone, Message, UserFilled } from '@element-plus/icons-vue'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: ''
})

// 确认密码校验器
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 16, message: '用户名长度在4-16位之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度在6-16位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

async function handleRegister() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (err) {
    ElMessage.error(err.message || '注册失败')
  } finally {
    loading.value = false
  }
}

/* ---- 动态追光（与登录页一致） ---- */
const lightX = ref(50)
const lightY = ref(50)
const isHovering = ref(false)
const rafId = ref(null)

const lightSourceStyle = computed(() => {
  if (!isHovering.value) {
    return { opacity: '0.15' }
  }
  return {
    left: `${lightX.value}%`,
    top: `${lightY.value}%`,
    opacity: '0.5'
  }
})

const cardEdgeStyle = computed(() => ({
  '--edge-x': `${lightX.value}%`,
  '--edge-y': `${lightY.value}%`,
  '--edge-opacity': isHovering.value ? '0.85' : '0.12'
}))

function centerLight() {
  lightX.value = 85
  lightY.value = 15
}

function onMouseEnter() {
  isHovering.value = true
}

function onMouseLeave() {
  isHovering.value = false
  centerLight()
}

function onMouseMove(e) {
  if (rafId.value !== null) return

  rafId.value = requestAnimationFrame(() => {
    rafId.value = null
    const x = e.clientX
    const y = e.clientY
    lightX.value = (x / window.innerWidth) * 100
    lightY.value = (y / window.innerHeight) * 100
  })
}

onMounted(() => {
  centerLight()
})

onBeforeUnmount(() => {
  if (rafId.value !== null) {
    cancelAnimationFrame(rafId.value)
    rafId.value = null
  }
})
</script>

<style scoped>
/* =========================================================
   1. 全屏容器 — 背景图 + 磨砂覆盖层
   ========================================================= */
.reg-container {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0a0a0f url('/login-bg.jpg') center / cover no-repeat fixed;
}

.reg-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(10, 10, 15, 0.35);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  pointer-events: none;
}

/* 动态追光 — 跟随鼠标的环境光晕 */
.light-source {
  position: absolute;
  width: 600px;
  height: 600px;
  background: radial-gradient(
    circle at center,
    rgba(180, 200, 255, 0.10) 0%,
    rgba(140, 170, 240, 0.06) 30%,
    rgba(100, 140, 220, 0.03) 50%,
    transparent 70%
  );
  border-radius: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
  will-change: left, top, opacity;
  transition: opacity 0.6s cubic-bezier(0.25, 0.1, 0.25, 1);
  z-index: 0;
}

/* =========================================================
   2. 液态玻璃面板（注册卡片）
   ========================================================= */
.reg-card {
  position: relative;
  width: 420px;
  padding: 38px 40px 24px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(24px) saturate(1.4);
  -webkit-backdrop-filter: blur(24px) saturate(1.4);
  border-radius: 24px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.15),
    0 8px 40px rgba(0, 0, 0, 0.25),
    0 2px 8px rgba(0, 0, 0, 0.1);
  border: 0.5px solid rgba(255, 255, 255, 0.08);
  z-index: 1;
  overflow: hidden;
}

/* 卡片边缘动态光照 — 跟随鼠标方向 */
.reg-card::after {
  content: '';
  position: absolute;
  width: 200%;
  height: 200%;
  left: calc(var(--edge-x, 50%) - 100%);
  top: calc(var(--edge-y, 10%) - 100%);
  background: radial-gradient(
    circle at center,
    rgba(255, 255, 255, 0.35) 0%,
    rgba(255, 255, 255, 0.12) 25%,
    rgba(255, 255, 255, 0.04) 50%,
    transparent 70%
  );
  mask: radial-gradient(circle at 50% 50%, transparent 52%, black 66%);
  -webkit-mask: radial-gradient(circle at 50% 50%, transparent 52%, black 66%);
  pointer-events: none;
  opacity: var(--edge-opacity, 0.12);
  transition: opacity 0.6s cubic-bezier(0.25, 0.1, 0.25, 1);
  z-index: -1;
}

/* =========================================================
   3. 标题文字
   ========================================================= */
.reg-title {
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.92);
  margin-bottom: 6px;
  letter-spacing: 1px;
}

.reg-subtitle {
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 1.5px;
  margin-bottom: 24px;
}

/* =========================================================
   4. Element Plus 输入框 — 液态玻璃覆盖样式
   ========================================================= */
.reg-card :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  backdrop-filter: blur(12px) saturate(1.3);
  -webkit-backdrop-filter: blur(12px) saturate(1.3);
  border: 0.5px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.25s cubic-bezier(0.25, 0.1, 0.25, 1);
  padding-left: 4px;
}

.reg-card :deep(.el-input__wrapper:hover) {
  background: rgba(255, 255, 255, 0.09) !important;
  border-color: rgba(255, 255, 255, 0.2);
}

.reg-card :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.1) !important;
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    0 0 0 3px rgba(255, 255, 255, 0.06),
    0 2px 8px rgba(0, 0, 0, 0.1);
}

.reg-card :deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.9) !important;
  font-size: 14px;
}

.reg-card :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.35) !important;
}

.reg-card :deep(.el-input__prefix-inner) {
  color: rgba(255, 255, 255, 0.4);
}

.reg-card :deep(.el-input__clear) {
  color: rgba(255, 255, 255, 0.3);
}

.reg-card :deep(.el-form-item) {
  margin-bottom: 16px;
}

/* =========================================================
   5. 注册按钮 — 同款液态玻璃质感
   ========================================================= */
.reg-card .glass-btn {
  height: 48px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 4px;
  border-radius: 14px;
  border: 0.5px solid rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px) saturate(1.3);
  -webkit-backdrop-filter: blur(12px) saturate(1.3);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.12),
    0 4px 16px rgba(0, 0, 0, 0.2);
  color: rgba(255, 255, 255, 0.92);
  transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.reg-card .glass-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.25);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.18),
    0 6px 24px rgba(0, 0, 0, 0.25);
}

.reg-card .glass-btn:active {
  transform: scale(0.97);
  background: rgba(255, 255, 255, 0.10);
}

.reg-card .glass-btn.is-loading {
  opacity: 0.7;
  transform: scale(0.98);
}

/* =========================================================
   6. 辅助文字
   ========================================================= */
.reg-footer {
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  margin-top: 10px;
}

.reg-footer .link {
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.reg-footer .link:hover {
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
}

/* =========================================================
   7. 暗色模式适配
   ========================================================= */
@media (prefers-color-scheme: dark) {
  .reg-container::before {
    background: rgba(0, 0, 5, 0.55);
  }
  .reg-card {
    background: rgba(255, 255, 255, 0.05);
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.08),
      0 8px 40px rgba(0, 0, 0, 0.35);
  }
  .reg-card .glass-btn {
    background: rgba(255, 255, 255, 0.05);
  }
}
</style>
