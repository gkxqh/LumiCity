<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/auth'

// 应用启动：若已登录（本地有 token），拉取用户信息（含角色/权限），
// 保证刷新页面后按钮级权限（v-hasPerm）依然可用
onMounted(async () => {
  if (getToken()) {
    try {
      await useUserStore().fetchUserInfo()
    } catch {
      // 拉取失败不阻塞页面（拦截器已处理 401 跳转登录）
    }
  }
})
</script>
