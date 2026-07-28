import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('./views/Login.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('./views/Home.vue')
  },
  {
    path: '/alarm',
    name: 'Alarm',
    component: () => import('./views/Alarm.vue')
  },
  {
    path: '/control',
    name: 'Control',
    component: () => import('./views/Control.vue')
  },
  {
    path: '/pole',
    name: 'PoleList',
    component: () => import('./views/PoleList.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.noAuth) return next()
  if (!getToken()) return next('/login')
  next()
})

export default router
