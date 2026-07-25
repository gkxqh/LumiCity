<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '210px'">
      <div class="logo">
        <el-icon size="24" color="#409EFF"><Sunny /></el-icon>
        <span v-show="!isCollapse">智慧照明系统</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :collapse="isCollapse"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <template v-for="item in menuList" :key="item.path">
          <el-menu-item :index="item.path">
            <el-icon><component :is="item.meta.icon" /></el-icon>
            <span>{{ item.meta.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header>
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse" size="20">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ userStore.nickname || userStore.username || '用户' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, provide } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

// 提供给子组件（dashboard）控制侧边栏折叠
provide('sidebarCollapse', isCollapse)

const menuList = router.getRoutes().filter(r => r.meta && r.meta.title && r.path !== '/dashboard').map(r => ({
  path: r.path,
  meta: r.meta
}))
// 把大盘放第一个
const dashboardRoute = router.getRoutes().find(r => r.path === '/dashboard')
if (dashboardRoute) {
  menuList.unshift({ path: '/dashboard', meta: dashboardRoute.meta })
}

async function handleCommand(cmd) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container { height: 100%; }
.el-aside { background-color: #304156; transition: width 0.3s; overflow: hidden; }
.logo { height: 50px; display: flex; align-items: center; gap: 8px; color: #fff; padding: 0 16px; font-size: 16px; font-weight: 500; white-space: nowrap; }
.el-header { background: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: space-between; padding: 0 16px; }
.header-left { display: flex; align-items: center; gap: 16px; }
.collapse-btn { cursor: pointer; }
.header-right .user-info { display: flex; align-items: center; gap: 4px; cursor: pointer; color: #333; }
.el-main { background-color: #f0f2f5; padding: 16px; }
.el-menu { border-right: none; }
</style>
