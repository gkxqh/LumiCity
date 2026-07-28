<template>
  <div class="login-page">
    <!-- 顶部标识 -->
    <div class="login-brand">
      <div class="login-logo">✦</div>
      <h1 class="login-title">智慧照明</h1>
      <p class="login-sub">城市照明综合管控平台</p>
    </div>

    <!-- 玻璃登录卡片 -->
    <div class="glass-card login-card">
      <van-form @submit="handleLogin">
        <van-field
          v-model="form.username"
          name="username"
          label="用户名"
          placeholder="admin"
          :rules="[{ required: true, message: '请输入用户名' }]"
          left-icon="contact"
        />
        <van-field
          v-model="form.password"
          type="password"
          name="password"
          label="密码"
          placeholder="123456"
          :rules="[{ required: true, message: '请输入密码' }]"
          left-icon="lock"
        />
        <div style="margin-top: 24px">
          <van-button round block native-type="submit" :loading="loading" style="height:44px;border-radius:12px;background:rgba(255,255,255,0.08);border:0.5px solid rgba(255,255,255,0.15);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);color:rgba(255,255,255,0.9);font-size:15px;letter-spacing:3px">
            登 录
          </van-button>
        </div>
      </van-form>
    </div>

    <div class="login-tip">演示账号：admin / 123456</div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456'
})

async function handleLogin() {
  loading.value = true
  try {
    await userStore.login(form)
    showToast('登录成功')
    router.replace('/home')
  } catch (e) {
    showToast(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 0 24px 100px;
}
.login-brand {
  text-align: center;
  margin-bottom: 36px;
}
.login-logo {
  font-size: 44px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 8px;
}
.login-title {
  color: rgba(255, 255, 255, 0.92);
  font-size: 28px;
  font-weight: 600;
  margin: 0;
  letter-spacing: 4px;
}
.login-sub {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  margin: 6px 0 0;
  letter-spacing: 1px;
}
.login-card {
  width: 100%;
  padding: 28px 20px 20px;
  max-width: 360px;
}
.login-tip {
  text-align: center;
  color: rgba(255, 255, 255, 0.3);
  font-size: 12px;
  margin-top: 20px;
}
</style>
