<!--
  登录页
  - 居中卡片样式，渐变背景
  - 新增"记住我"复选框：勾选后 token 存 cookie（7天），否则存 sessionStorage
  - 页面加载时自动检测 cookie token → 有则跳转首页
  - 调用 useUserStore().login() 完成登录，成功后跳转 /dashboard
-->
<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 标题 -->
      <div class="login-title">智慧城市照明综合控制系统</div>
      <div class="login-subtitle">Smart City Lighting Control System</div>

      <!-- 登录表单：ref 用于调用 validate，rules 用于校验 -->
      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        label-width="0"
        size="large"
        @submit.prevent="handleLogin"
      >
        <!-- 用户名 -->
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <!-- 密码 -->
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

        <!-- 记住我 复选框 -->
        <el-form-item>
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
        </el-form-item>

        <!-- 登录按钮 -->
        <el-form-item>
          <el-button
            type="primary"
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getToken, getRememberMe, setRememberMe, getSavedUsername, saveUsername } from '@/utils/auth'

const router = useRouter()
const userStore = useUserStore()

// 表单 ref，用于调用 validate / resetFields
const formRef = ref()
// 登录中 loading 状态
const loading = ref(false)

// 记住我复选框
const rememberMe = ref(false)

// 表单数据（reactive 适合对象类型状态）
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单校验规则：用户名、密码均非空
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

// 页面加载时：如果 cookie 中有有效 token，直接跳转首页
onMounted(() => {
  // 恢复"记住我"复选框状态
  rememberMe.value = getRememberMe()
  // 恢复上次记住的用户名
  const savedUsername = getSavedUsername()
  if (savedUsername) {
    loginForm.username = savedUsername
  }

  // 如果已有 token（cookie 或 sessionStorage），直接跳首页
  const token = getToken()
  if (token) {
    router.replace('/dashboard')
  }
})

// 登录处理
async function handleLogin() {
  // 1. 先做前端表单校验，校验不通过则直接返回
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  // 2. 调用 store 的 login 方法（内部会请求后端并保存 token）
  loading.value = true
  try {
    await userStore.login(loginForm, rememberMe.value)
    // 持久化记住我的状态
    setRememberMe(rememberMe.value)
    if (rememberMe.value) {
      saveUsername(loginForm.username)
    }
    ElMessage.success('登录成功')
    // 3. 登录成功后跳转首页
    router.push('/dashboard')
  } catch (err) {
    // 失败提示（请求拦截器已弹一次错误，这里兜底显示）
    ElMessage.error(err.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 外层容器：全屏 + 渐变背景 + 居中 */
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
}

/* 登录卡片 */
.login-card {
  width: 420px;
  padding: 40px 40px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
}

.login-title {
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  color: #1e3c72;
  margin-bottom: 6px;
}

.login-subtitle {
  text-align: center;
  font-size: 12px;
  color: #999;
  letter-spacing: 1px;
  margin-bottom: 28px;
}

.login-tips {
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 8px;
}

.login-footer {
  text-align: center;
  font-size: 13px;
  color: #999;
  margin-top: 12px;
}

.login-footer .link {
  color: #1e3c72;
  text-decoration: none;
  font-weight: 500;
}

.login-footer .link:hover {
  text-decoration: underline;
}
</style>
