import axios from 'axios'
import { showToast } from 'vant'
import { getToken, removeToken } from '@/utils/auth'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器：自动带 token
service.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      removeToken()
      window.location.hash = '#/login'
    }
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  error => {
    const msg = error.response?.data?.message || error.message || '网络异常'
    showToast(msg)
    return Promise.reject(new Error(msg))
  }
)

export default service
