const TOKEN_KEY = 'smart_lighting_token'
const REMEMBER_KEY = 'smart_lighting_remember'

/**
 * 获取 token
 * 优先从 cookie 读（记住我），没有再读 sessionStorage，最后回退 localStorage
 */
export function getToken() {
  return getTokenFromCookie() || sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY) || ''
}

/**
 * 保存 token
 * @param {string} token
 * @param {boolean} rememberMe - true 存 cookie(7天)，false 存 sessionStorage
 */
export function setToken(token, rememberMe = false) {
  removeToken() // 先清所有旧 token
  if (rememberMe) {
    setTokenToCookie(token, 7)
  } else {
    sessionStorage.setItem(TOKEN_KEY, token)
  }
}

export function removeToken() {
  document.cookie = `${TOKEN_KEY}=; path=/; max-age=0`
  sessionStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REMEMBER_KEY)
}

/** 是否开启"记住我"（用于登录页回填复选框状态） */
export function getRememberMe() {
  return localStorage.getItem(REMEMBER_KEY) === 'true'
}

export function setRememberMe(val) {
  if (val) {
    localStorage.setItem(REMEMBER_KEY, 'true')
  } else {
    localStorage.removeItem(REMEMBER_KEY)
  }
}

/** 记住的用户名（"记住我"时顺便记住用户名方便显示） */
const USERNAME_KEY = 'smart_lighting_username'

export function getSavedUsername() {
  return localStorage.getItem(USERNAME_KEY) || ''
}

export function saveUsername(username) {
  localStorage.setItem(USERNAME_KEY, username)
}

export function removeSavedUsername() {
  localStorage.removeItem(USERNAME_KEY)
}

// ---------- cookie 底层工具 ----------

function getTokenFromCookie() {
  const match = document.cookie.match(new RegExp('(^| )' + TOKEN_KEY + '=([^;]+)'))
  return match ? decodeURIComponent(match[2]) : ''
}

function setTokenToCookie(token, days) {
  const expires = new Date(Date.now() + days * 86400000).toUTCString()
  document.cookie = `${TOKEN_KEY}=${encodeURIComponent(token)}; expires=${expires}; path=/`
}
