import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const username = ref('')
  const nickname = ref('')

  async function login(loginForm) {
    const res = await loginApi(loginForm)
    token.value = res.data.token
    username.value = res.data.username
    nickname.value = res.data.nickname
    setToken(res.data.token)
    return res
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    username.value = res.data.username
    nickname.value = res.data.nickname
    return res
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      username.value = ''
      nickname.value = ''
      removeToken()
    }
  }

  return { token, username, nickname, login, fetchUserInfo, logout }
})
