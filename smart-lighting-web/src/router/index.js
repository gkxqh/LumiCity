import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  { path: '/login', component: () => import('@/views/login/index.vue') },
  { path: '/register', component: () => import('@/views/register/index.vue') },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', meta: { title: '数据大盘', icon: 'DataLine' }, component: () => import('@/views/dashboard/index.vue') },
      { path: 'device', name: 'Device', meta: { title: '设备管理', icon: 'Cpu' }, component: () => import('@/views/device/index.vue') },
      { path: 'pole', name: 'Pole', meta: { title: '灯杆管理', icon: 'Locate' }, component: () => import('@/views/device/pole.vue') },
      { path: 'lighting', name: 'Lighting', meta: { title: '照明控制', icon: 'Sunny' }, component: () => import('@/views/lighting/index.vue') },
      { path: 'energy', name: 'Energy', meta: { title: '能耗管理', icon: 'Lightning' }, component: () => import('@/views/energy/index.vue') },
      { path: 'alarm', name: 'Alarm', meta: { title: '故障告警', icon: 'Warning' }, component: () => import('@/views/alarm/index.vue') },
      { path: 'video', name: 'Video', meta: { title: '视频监控', icon: 'VideoCamera' }, component: () => import('@/views/video/index.vue') },
      { path: 'environment', name: 'Environment', meta: { title: '环境监测', icon: 'Cloudy' }, component: () => import('@/views/environment/index.vue') },
      { path: 'publish', name: 'Publish', meta: { title: '信息发布', icon: 'Film' }, component: () => import('@/views/publish/index.vue') },
      { path: 'workorder', name: 'WorkOrder', meta: { title: '工单运维', icon: 'Document' }, component: () => import('@/views/workorder/index.vue') },
      { path: 'system', name: 'System', meta: { title: '系统管理', icon: 'Setting' }, component: () => import('@/views/system/index.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.path === '/login' || to.path === '/register') {
    next()
  } else if (!getToken()) {
    next('/login')
  } else {
    next()
  }
})

export default router
