/**
 * 认证工具模块
 * 负责 token 的存取和登录状态判断
 */

const TOKEN_KEY = 'access_token'

/**
 * 获取 token
 * @returns {string} token 字符串，未登录返回空字符串
 */
export function getToken () {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

/**
 * 保存 token
 * @param {string} token JWT token
 */
export function setToken (token) {
  uni.setStorageSync(TOKEN_KEY, token)
}

/**
 * 清除 token
 */
export function removeToken () {
  uni.removeStorageSync(TOKEN_KEY)
}

/**
 * 判断是否已登录
 * @returns {boolean}
 */
export function isLoggedIn () {
  return !!getToken()
}

/**
 * 检查登录状态，未登录则跳转登录页
 * @param {boolean} redirect 是否跳转登录页，默认 true
 * @returns {boolean} 是否已登录
 */
export function checkAuth (redirect = false) {
  if (isLoggedIn()) {
    return true
  }
  if (redirect) {
    uni.navigateTo({ url: '/pages/login/login' })
  }
  return false
}
