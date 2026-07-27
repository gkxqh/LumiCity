<!--
  登录页 — 苹果液态玻璃（Liquid Glass）设计风格
  - 全屏背景图 + 半透明磨砂覆盖层
  - 居中液态玻璃面板：多层 backdrop-filter blur、内发光、顶部高光边线
  - 输入框同款小玻璃样式，hover/focus 平滑过渡
  - 登录按钮渐变液态玻璃质感，点击压缩反馈
-->
<template>
  <div class="login-container" @mousemove="onMouseMove" @mouseenter="onMouseEnter" @mouseleave="onMouseLeave">
    <!-- 动态追光 — 跟随鼠标的环境光晕 -->
    <div
      class="light-source"
      :style="lightSourceStyle"
    ></div>

    <div class="login-card" ref="cardRef" :style="cardEdgeStyle">

      <!-- 标题 -->
      <div class="login-title">智慧城市照明综合控制系统</div>
      <div class="login-subtitle">Smart City Lighting Control System</div>

      <!-- 登录表单 -->
      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        label-width="0"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            class="glass-btn"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-tips">提示：初始账号 admin / 123456</div>
      <div class="login-footer">
        还没有账号？<router-link to="/register" class="link">去注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getToken, getRememberMe, setRememberMe, getSavedUsername, saveUsername } from '@/utils/auth'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

onMounted(() => {
  rememberMe.value = getRememberMe()
  const savedUsername = getSavedUsername()
  if (savedUsername) {
    loginForm.username = savedUsername
  }
  const token = getToken()
  if (token) {
    router.replace('/dashboard')
  }
  // 初始化光源位置为屏幕中心
  centerLight()
})

onBeforeUnmount(() => {
  if (rafId.value !== null) {
    cancelAnimationFrame(rafId.value)
    rafId.value = null
  }
})

const cardRef = ref(null)

/* ---- 动态追光 ---- */
const lightX = ref(50)
const lightY = ref(50)
const isHovering = ref(false)
const rafId = ref(null)

// 背景层环境光晕 — 鼠标位置决定光晕中心
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

// 卡片边缘光照 — 鼠标方向决定边缘亮起位置
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
    const w = window.innerWidth
    const h = window.innerHeight

    lightX.value = (x / w) * 100
    lightY.value = (y / h) * 100
  })
}

async function handleLogin() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.login(loginForm, rememberMe.value)
    setRememberMe(rememberMe.value)
    if (rememberMe.value) {
      saveUsername(loginForm.username)
    }
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (err) {
    ElMessage.error(err.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* =========================================================
   1. 全屏容器 — 背景图 + 磨砂覆盖层
   ========================================================= */
.login-container {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;            /* 防止 600px 光晕超出产生滚动条/抽搐 */
  display: flex;
  align-items: center;
  justify-content: center;
  /* 背景图：固定在中央、覆盖全屏 */
  background: #0a0a0f url('/login-bg.jpg') center / cover no-repeat fixed;
}

/* 背景上的半透明磨砂覆盖层 — 降低图片对比度，衬托玻璃面板 */
.login-container::before {
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
   2. 液态玻璃面板（登录卡片）
   ========================================================= */
.login-card {
  position: relative;
  width: 420px;
  padding: 44px 40px 28px;
  /*
    液态玻璃核心：
    - 极低饱和度的半透明底色
    - 多层 backdrop-filter blur 模拟物理玻璃折射
    - 细微白色内发光 (inset box-shadow)
    - 柔和外阴影
    - 大圆角
  */
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(24px) saturate(1.4);
  -webkit-backdrop-filter: blur(24px) saturate(1.4);
  border-radius: 24px;
  /* 内发光 + 柔和外阴影 */
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.15),
    0 8px 40px rgba(0, 0, 0, 0.25),
    0 2px 8px rgba(0, 0, 0, 0.1);
  border: 0.5px solid rgba(255, 255, 255, 0.08);
  z-index: 1;
  /* 溢出隐藏 — 使 ::after 的光晕不超出圆角边框 */
  overflow: hidden;
}

/* 卡片边缘动态光照 — 替代固定 .glass-edge，跟随鼠标方向 */
.login-card::after {
  content: '';
  position: absolute;
  /*
    用径向渐变模拟边缘扫光：
    - 中心定位跟随鼠标 (via CSS 自定义属性)
    - mask 抠掉中心区域，仅边缘可见
    - 边缘处呈现白色高光渐变
  */
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
  /* 宽环形遮罩 — 只露出边缘一圈，抠掉中心区域 */
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
.login-title {
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.92);
  margin-bottom: 6px;
  letter-spacing: 1px;
}

.login-subtitle {
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 1.5px;
  margin-bottom: 30px;
}

/* =========================================================
   4. Element Plus 输入框 — 液态玻璃覆盖样式
   ========================================================= */

/* 输入框外层包裹：让 el-form-item 内部元素统一玻璃风格 */
.login-card :deep(.el-input__wrapper) {
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

/* hover — 边框微亮 */
.login-card :deep(.el-input__wrapper:hover) {
  background: rgba(255, 255, 255, 0.09) !important;
  border-color: rgba(255, 255, 255, 0.2);
}

/* focus — 更亮边框 + 外发光 */
.login-card :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.1) !important;
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    0 0 0 3px rgba(255, 255, 255, 0.06),
    0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 输入框内的文字 */
.login-card :deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.9) !important;
  font-size: 14px;
}
.login-card :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.35) !important;
}

/* 输入框前缀图标 */
.login-card :deep(.el-input__prefix-inner) {
  color: rgba(255, 255, 255, 0.4);
}

/* clearable 图标 */
.login-card :deep(.el-input__clear) {
  color: rgba(255, 255, 255, 0.3);
}

/* 表单项间距 */
.login-card :deep(.el-form-item) {
  margin-bottom: 20px;
}

/* =========================================================
   5. 复选框 "记住我"
   ========================================================= */
.login-card :deep(.el-checkbox__label) {
  color: rgba(255, 255, 255, 0.65) !important;
  font-size: 13px;
}
.login-card :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
}
.login-card :deep(.el-checkbox__inner) {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.2);
}

/* =========================================================
   6. 登录按钮 — 同款液态玻璃质感（与输入框一致）
   ========================================================= */
.login-card .glass-btn {
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

/* hover — 略微提亮 */
.login-card .glass-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.25);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.18),
    0 6px 24px rgba(0, 0, 0, 0.25);
}

/* active/点击 — 压缩反馈 */
.login-card .glass-btn:active {
  transform: scale(0.97);
  background: rgba(255, 255, 255, 0.10);
}

/* loading 态 */
.login-card .glass-btn.is-loading {
  opacity: 0.7;
  transform: scale(0.98);
}

/* =========================================================
   7. 辅助文字
   ========================================================= */
.login-tips {
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
  margin-top: 10px;
}

.login-footer {
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  margin-top: 14px;
}

.login-footer .link {
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.login-footer .link:hover {
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
}

/* =========================================================
   8. 暗色模式适配
   ========================================================= */
@media (prefers-color-scheme: dark) {
  .login-container::before {
    background: rgba(0, 0, 5, 0.55);
  }
  .login-card {
    background: rgba(255, 255, 255, 0.05);
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.08),
      0 8px 40px rgba(0, 0, 0, 0.35);
  }
  .login-card .glass-btn {
    background: rgba(255, 255, 255, 0.05);
  }
}
</style>
