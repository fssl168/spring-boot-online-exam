import Vue from 'vue'
import axios from 'axios'
import store from '../store'
import {
  VueAxios
} from './axios'
import {
  ACCESS_TOKEN
} from '../store/mutation-types'
import { sanitizeDeep } from './xss'
import errorNotify from './errorNotification'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api', // api base_url
  timeout: 15000 // 请求超时时间（Batch 7.2：从 6000 提升到 15000，避免考试提交慢请求超时）
})

// Batch 7.2.4：请求取消机制（重复请求自动取消）
// 维护进行中的请求 Map：key = method+url，value = CancelTokenSource
const pendingRequests = new Map()

function getRequestKey (config) {
  const method = (config.method || 'get').toLowerCase()
  const url = config.url || ''
  // GET 请求附带 params 作为 key 的一部分，避免同 URL 不同分页互相取消
  const params = config.params ? JSON.stringify(config.params) : ''
  return `${method}&${url}&${params}`
}

function addPendingRequest (config) {
  const key = getRequestKey(config)
  if (pendingRequests.has(key)) {
    // 已存在相同请求：取消前一个
    const source = pendingRequests.get(key)
    source.cancel(`取消重复请求: ${key}`)
  }
  const source = axios.CancelToken.source()
  config.cancelToken = source.token
  pendingRequests.set(key, source)
}

function removePendingRequest (config) {
  const key = getRequestKey(config)
  if (pendingRequests.has(key)) {
    pendingRequests.delete(key)
  }
}

const err = (error) => {
  // Batch 7.2.4：被取消的请求不弹错误提示，但仍需 -1（因为请求前已 +1）
  if (axios.isCancel(error)) {
    store.dispatch('LoadingDec')
    return Promise.reject(error)
  }
  // 其他错误：loading -1
  store.dispatch('LoadingDec')
  if (error.response) {
    const data = error.response.data
    const token = Vue.ls.get(ACCESS_TOKEN)
    // 后端返回的 JsonData/ResultVO 使用 msg 字段，兼容 message 字段
    const errMsg = (data && (data.msg || data.message)) || '请求出现错误'
    if (error.response.status === 403) {
      // 权限不足：后端 RoleInterceptor 返回 403 + JsonData{msg}
      // Batch 7.3.4：改用统一错误通知工具
      errorNotify.error({ message: '权限不足', description: errMsg })
    }
    if (error.response.status === 401) {
      // 未登录或 token 失效：后端 LoginInterceptor 返回 401 + JsonData{msg}
      // Batch 7.3.4：改用统一错误通知工具
      errorNotify.error({ message: '未登录', description: errMsg || '登录已失效，请重新登录' })
      if (token) {
        store.dispatch('Logout').then(() => {
          setTimeout(() => {
            window.location.reload()
          }, 1500)
        })
      }
    }
    if (error.response.status === 400) {
      // 参数校验失败：GlobalExceptionHandler 返回 400 + ResultVO{msg}
      // Batch 7.3.4：改用统一错误通知工具
      errorNotify.error({ message: '参数错误', description: errMsg })
    }
  }
  return Promise.reject(error)
}

// request interceptor
service.interceptors.request.use(config => {
  const token = Vue.ls.get(ACCESS_TOKEN)
  if (token) { // 如果localStorage中有"Access-Token"属性，就在请求头里加上
    config.headers['Access-Token'] = token // 让每个请求携带自定义 token 请根据实际情况自行修改
  }
  // Batch 7.3.3：触发全局 loading +1
  // 文件上传/下载等不触发 loading，避免长时间阻塞 UI
  if (!config.skipLoading) {
    store.dispatch('LoadingInc')
  }
  // Batch 7.2.4：登记/取消重复请求
  addPendingRequest(config)
  return config
}, err)

// response interceptor
service.interceptors.response.use((response) => {
  // Batch 7.2.4：从 pending 列表中移除
  removePendingRequest(response.config)
  // Batch 7.3.3：loading -1
  if (!response.config.skipLoading) {
    store.dispatch('LoadingDec')
  }
  // Batch 7.1.1：对响应数据做 XSS 清洗（递归处理所有字符串字段）
  const data = response.data
  if (data && typeof data === 'object') {
    sanitizeDeep(data)
  }
  return data
}, err)

const installer = {
  vm: {},
  install (Vue) {
    Vue.use(VueAxios, service)
  }
}

export {
  installer as VueAxios,
  service as axios
}
