<!--
  权限管理页（整合角色管理 + 菜单管理）
  - Tab「角色权限」：角色 CRUD + 给角色分配菜单（即"控制哪种角色可以操作哪种菜单"）
  - Tab「菜单维护」：菜单树 CRUD（目录/菜单/按钮 + perms 权限标识）
  - 复用 role.vue / menu.vue 作为 Tab 内部面板，避免重复实现
-->
<template>
  <div class="permission-page">
    <el-tabs v-model="activeTab" type="card" class="perm-tabs">
      <el-tab-pane label="角色权限" name="role">
        <RolePanel />
      </el-tab-pane>
      <el-tab-pane label="菜单维护" name="menu" lazy>
        <MenuPanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import RolePanel from './role.vue'
import MenuPanel from './menu.vue'

// 默认展示「角色权限」：这里是"哪种角色可操作哪些菜单"的核心入口
const activeTab = ref('role')
</script>

<style scoped>
.permission-page { padding: 0; }
/* 去掉 Tab 内容默认多余留白，让两个面板的 el-card 顶格 */
.perm-tabs :deep(.el-tabs__content) { padding-top: 8px; }
</style>
