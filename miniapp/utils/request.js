/**
 * 请求封装模块
 * 封装 uni.request，自动注入 Access-Token，统一错误处理
 */
import { getToken, removeToken } from './auth'

// 后端 API 基础地址，按环境切换
// 开发环境：本地 Spring Boot 后端
// 生产环境：部署后的后端地址
const BASE_URL = 'http://localhost:8080'

/**
 * 核心请求函数
 * @param {Object} options { url, method, data, header, hideError }
 * @returns {Promise}
 */
export function request (options) {
  const { url, method = 'GET', data = {}, header = {}, hideError = false } = options
  const token = getToken()

  // 自动注入 Access-Token
  const finalHeader = { ...header }
  if (token) {
    finalHeader['Access-Token'] = token
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: finalHeader,
      success (res) {
        // HTTP 状态码处理
        if (res.statusCode === 401 || res.statusCode === 403) {
          // token 失效或权限不足，清除 token 跳登录
          removeToken()
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          setTimeout(() => {
            uni.navigateTo({ url: '/pages/login/login' })
          }, 1500)
          reject(new Error('未授权'))
          return
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
          if (!hideError) {
            uni.showToast({ title: '网络错误(' + res.statusCode + ')', icon: 'none' })
          }
          reject(new Error('HTTP ' + res.statusCode))
          return
        }
        // 业务状态码处理：后端 ResultVO { code, message, data }
        const resData = res.data
        if (resData && resData.code !== undefined && resData.code !== 0) {
          if (!hideError) {
            uni.showToast({ title: resData.message || '请求失败', icon: 'none' })
          }
          reject(new Error(resData.message || '业务错误'))
          return
        }
        resolve(resData)
      },
      fail (err) {
        if (!hideError) {
          uni.showToast({ title: '网络请求失败', icon: 'none' })
        }
        reject(err)
      }
    })
  })
}

/**
 * GET 请求快捷方法
 */
export function get (url, data = {}, options = {}) {
  return request({ url, method: 'GET', data, ...options })
}

/**
 * POST 请求快捷方法
 */
export function post (url, data = {}, options = {}) {
  return request({ url, method: 'POST', data, ...options })
}

/**
 * DELETE 请求快捷方法
 */
export function del (url, data = {}, options = {}) {
  return request({ url, method: 'DELETE', data, ...options })
}

export default { request, get, post, del, BASE_URL }
