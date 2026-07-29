<template>
  <div class="lg-app">
    <div class="lg-bg"></div>
    <div class="lg-light"></div>

    <!-- 全局顶部标识条：随路由显示当前界面，无论从底部标签还是首页按钮进入都能正确标识 -->
    <header class="lg-topbar" v-if="showTabBar">
      <div class="tb-back" :class="{ hidden: route.path === '/home' }" @click="goBack">
        <span class="tb-arrow">‹</span>
      </div>
      <div class="tb-title">
        <span class="tb-dot"></span>
        <span class="tb-name">{{ currentTitle }}</span>
      </div>
      <div class="tb-right">
        <span class="tb-status">在线</span>
      </div>
    </header>

    <div class="lg-content">
      <router-view />
    </div>

    <!-- 底部 TabBar（5 个：首页 / 告警 / 控制 / 灯杆 / 工单） -->
    <van-tabbar v-if="showTabBar" v-model="active" route border placeholder>
      <van-tabbar-item to="/home" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/alarm" icon="warn-o">告警</van-tabbar-item>
      <van-tabbar-item to="/control">
        <template #icon>
          <span style="font-size:22px;line-height:1">⚡</span>
        </template>
        控制
      </van-tabbar-item>
      <van-tabbar-item to="/pole" icon="location-o">灯杆</van-tabbar-item>
      <van-tabbar-item to="/workorder" icon="orders-o">工单</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const active = ref(0)

const showTabBar = computed(() => {
  const noTab = ['/login', '/register']
  return userStore.token && !noTab.includes(route.path)
})

// 当前界面标识（路由驱动，按钮跳转也正确）
const titleMap = {
  '/home': '首页',
  '/alarm': '告警管理',
  '/control': '照明控制',
  '/pole': '灯杆查询',
  '/workorder': '工单运维'
}
const currentTitle = computed(() => titleMap[route.path] || '首页')

function goBack() {
  // 主界面不显示箭头；其余四个界面点击箭头一律返回主界面（首页）
  if (route.path === '/home') return
  router.push('/home')
}
</script>

<style>
.lg-app {
  position: relative;
  min-height: 100vh;
}
.lg-bg {
  position: fixed;
  inset: 0;
  background: #0a0a0f url('/login-bg.jpg') center / cover no-repeat fixed;
  z-index: 0;
}
.lg-bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(10, 10, 15, 0.35);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}
.lg-light {
  position: fixed;
  width: 500px;
  height: 500px;
  top: 10%;
  right: 5%;
  background: radial-gradient(
    circle at center,
    rgba(180, 200, 255, 0.08) 0%,
    rgba(140, 170, 240, 0.04) 30%,
    transparent 60%
  );
  border-radius: 50%;
  transform: translate(0, 0);
  pointer-events: none;
  z-index: 0;
}

/* ===== 全局顶部标识条 ===== */
.lg-topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  height: 48px;
  display: flex;
  align-items: center;
  padding: 0 8px;
  background: rgba(16, 18, 28, 0.72);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 0.5px solid rgba(255, 255, 255, 0.08);
}
.tb-back {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.75);
  transition: color 0.2s;
}
.tb-back:active { color: #4f8cff; }
.tb-back.hidden { visibility: hidden; }
.tb-arrow { font-size: 26px; line-height: 1; margin-top: -2px; }

.tb-title {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.tb-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #3ad07a;
  box-shadow: 0 0 0 0 rgba(58, 208, 122, 0.6);
  animation: tb-pulse 1.8s infinite;
}
@keyframes tb-pulse {
  0%   { box-shadow: 0 0 0 0 rgba(58, 208, 122, 0.55); }
  70%  { box-shadow: 0 0 0 7px rgba(58, 208, 122, 0); }
  100% { box-shadow: 0 0 0 0 rgba(58, 208, 122, 0); }
}
.tb-name {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.92);
}
.tb-right {
  width: 40px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.tb-status {
  font-size: 11px;
  color: #3ad07a;
  padding: 2px 7px;
  border-radius: 10px;
  background: rgba(58, 208, 122, 0.12);
}

.lg-content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding-top: 6px;
  padding-bottom: 60px;
}

/* ===== 底部标签激活态增强（更明显的当前界面指示） ===== */
.lg-app :deep(.van-tabbar) {
  background: rgba(16, 18, 28, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 0.5px solid rgba(255, 255, 255, 0.08);
}
.lg-app :deep(.van-tabbar-item) {
  color: rgba(255, 255, 255, 0.45);
  background: transparent;
  transition: color 0.2s;
}
.lg-app :deep(.van-tabbar-item__icon),
.lg-app :deep(.van-tabbar-item__text) {
  position: relative;
  z-index: 1;
  transition: transform 0.2s;
}
.lg-app :deep(.van-tabbar-item--active) {
  color: #ffffff;
  font-weight: 700;
  position: relative;
}
/* 选中项：整块区域从上往下逐渐淡出的背景染色，当前界面一目了然 */
.lg-app :deep(.van-tabbar-item--active)::after {
  content: '';
  position: absolute;
  top: 0;
  left: 4px;
  right: 4px;
  bottom: 0;
  border-radius: 0 0 16px 16px;
  background: linear-gradient(
    180deg,
    rgba(79, 140, 255, 0.62) 0%,
    rgba(79, 140, 255, 0.32) 40%,
    rgba(79, 140, 255, 0.08) 72%,
    transparent 100%
  );
  box-shadow:
    inset 0 10px 18px -6px rgba(120, 170, 255, 0.45),
    0 6px 18px rgba(79, 140, 255, 0.22);
  z-index: 0;
}
/* 选中项顶部加一条高亮光线 */
.lg-app :deep(.van-tabbar-item--active)::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 34px;
  height: 3px;
  border-radius: 0 0 4px 4px;
  background: #a8c8ff;
  box-shadow: 0 0 10px 2px rgba(120, 170, 255, 0.95);
  z-index: 2;
}
/* 选中项图标放大 + 发光，强化辨识 */
.lg-app :deep(.van-tabbar-item--active .van-tabbar-item__icon) {
  transform: scale(1.18);
  filter: drop-shadow(0 0 6px rgba(120, 170, 255, 0.85));
}
.lg-app :deep(.van-tabbar-item--active .van-tabbar-item__text) {
  transform: scale(1.05);
}
.lg-app :deep(.van-tabbar-item__icon) {
  font-size: 20px;
}
</style>
