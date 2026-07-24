import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const username = ref('')
  const nickname = ref('')
  // 角色编码与权限标识：用于按钮级权限控制（v-hasPerm 指令）
  const roles = ref([])
  const perms = ref([])

  async function login(loginForm) {
    const res = await loginApi(loginForm)
    token.value = res.data.token
    username.value = res.data.username
    nickname.value = res.data.nickname
    roles.value = res.data.roles || []
    perms.value = res.data.perms || []
    setToken(res.data.token)
    return res
  }

  // 拉取当前用户信息（含 roles/perms），登录后及刷新页面时调用
  async function fetchUserInfo() {
    const res = await getUserInfo()
    username.value = res.data.username
    nickname.value = res.data.nickname
    roles.value = res.data.roles || []
    perms.value = res.data.perms || []
    return res
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      username.value = ''
      nickname.value = ''
      roles.value = []
      perms.value = []
      removeToken()
    }
  }

  return { token, username, nickname, roles, perms, login, fetchUserInfo, logout }
})
