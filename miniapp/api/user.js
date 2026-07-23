/**
 * 用户相关 API
 */
import { get, post } from '../utils/request'

/**
 * 注册
 * @param {Object} data { username, password, email }
 */
export function register (data) {
  return post('/api/user/register', data)
}

/**
 * 登录
 * @param {Object} data { loginType: 1|2, userInfo, password }
 * @returns {Promise} ResultVO<String> token
 */
export function login (data) {
  return post('/api/user/login', data)
}

/**
 * 获取用户信息
 * @returns {Promise} ResultVO<UserVo>
 */
export function getUserInfo () {
  return get('/api/user/user-info')
}

/**
 * 获取用户详细信息（含权限）
 */
export function getUserDetail () {
  return get('/api/user/info')
}

/**
 * 修改密码
 * @param {Object} data { oldPassword, newPassword }
 */
export function changePassword (data) {
  return post('/api/user/change-password', data)
}

/**
 * 更新个人信息
 * @param {Object} data { nickname, avatar, description, email, phone }
 */
export function updateUserInfo (data) {
  return post('/api/user/update', data)
}
