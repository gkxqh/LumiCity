import { createRouter, createWebHistory } from 'vue-router'
import { getToken, removeToken } from '@/utils/auth'
import { useUserStore } from '@/store/user'

const routes = [
  { path: '/login', component: () => import('@/views/login/index.vue') },
  { path: '/register', component: () => import('@/views/register/index.vue') },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', meta: { title: '数据大盘', icon: 'DataLine' }, component: () => import('@/views/dashboard/index.vue') },
      { path: 'device', name: 'Device', meta: { title: '设备管理', icon: 'Cpu', perms: ['device:pole:list'] }, component: () => import('@/views/device/index.vue') },
      { path: 'pole', name: 'Pole', meta: { title: '灯杆管理', icon: 'Locate', perms: ['device:pole:list'] }, component: () => import('@/views/device/pole.vue') },
      { path: 'lighting', name: 'Lighting', meta: { title: '照明控制', icon: 'Sunny', perms: ['lighting:strategy:list'] }, component: () => import('@/views/lighting/index.vue') },
      { path: 'energy', name: 'Energy', meta: { title: '能耗管理', icon: 'Lightning', perms: ['energy:record:list'] }, component: () => import('@/views/energy/index.vue') },
      { path: 'alarm', name: 'Alarm', meta: { title: '故障告警', icon: 'Warning', perms: ['alarm:record:list'] }, component: () => import('@/views/alarm/index.vue') },
      { path: 'video', name: 'Video', meta: { title: '视频监控', icon: 'VideoCamera', perms: ['video:camera:list'] }, component: () => import('@/views/video/index.vue') },
      { path: 'environment', name: 'Environment', meta: { title: '环境监测', icon: 'Cloudy', perms: ['env:data:list'] }, component: () => import('@/views/environment/index.vue') },
      { path: 'publish', name: 'Publish', meta: { title: '信息发布', icon: 'Film', perms: ['publish:program:list'] }, component: () => import('@/views/publish/index.vue') },
      {
        path: 'workorder',
        name: 'WorkOrder',
        meta: { title: '工单运维', icon: 'Document', perms: ['workorder:list:list'] },
        redirect: '/workorder/alarm',
        children: [
          { path: 'alarm', name: 'WorkOrderAlarm', meta: { title: '告警工单', type: 'alarm' }, component: () => import('@/views/workorder/index.vue') },
          { path: 'manual', name: 'WorkOrderManual', meta: { title: '运维创建工单', type: 'manual' }, component: () => import('@/views/workorder/index.vue') }
        ]
      },
      { path: 'system', name: 'System', meta: { title: '系统管理', icon: 'Setting', perms: ['system:user:list'] }, component: () => import('@/views/system/index.vue') },
      { path: 'system/permission', name: 'Permission', meta: { title: '权限管理', icon: 'Lock', perms: ['system:role:list'] }, component: () => import('@/views/system/permission.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 白名单路由：无需登录即可访问
const whiteList = ['/login', '/register']

router.beforeEach(async (to, from, next) => {
  // 1. 白名单直接放行
  if (whiteList.includes(to.path)) {
    next()
    return
  }

  // 2. 未登录 → 跳登录页
  const token = getToken()
  if (!token) {
    next('/login')
    return
  }

  // 3. 已登录但 roles 尚未填充 → 拉取用户信息（含角色/权限）
  const store = useUserStore()
  if (store.roles.length === 0 && store.perms.length === 0) {
    try {
      await store.fetchUserInfo()
    } catch {
      // token 无效 → 清除并跳登录
      removeToken()
      next('/login')
      return
    }
  }

  // 4. 路由级权限校验：如果 meta.perms 有配置，检查用户是否拥有至少一个
  const requiredPerms = to.meta.perms
  if (requiredPerms && Array.isArray(requiredPerms) && requiredPerms.length > 0) {
    const isAdmin = store.roles.includes('ADMIN')
    const hasPerm = isAdmin || requiredPerms.some(p => store.perms.includes(p))
    if (!hasPerm) {
      // 无权限 → 跳转到大盘
      next('/dashboard')
      return
    }
  }

  next()
})

export default router
