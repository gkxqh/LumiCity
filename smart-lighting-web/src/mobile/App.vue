<template>
  <div class="lg-app">
    <div class="lg-bg"></div>
    <div class="lg-light"></div>

    <div class="lg-content">
      <router-view />
    </div>

    <!-- 底部 TabBar -->
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
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()
const active = ref(0)

const showTabBar = computed(() => {
  const noTab = ['/login', '/register']
  return userStore.token && !noTab.includes(route.path)
})
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
.lg-content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding-bottom: 60px;
}
</style>
