/**
 * 用户模块
 * 管理 token、用户信息、登录状态
 */
import { login as loginApi, getUserInfo } from '../../api/user'
import { getToken, setToken, removeToken } from '../../utils/auth'

const state = {
  token: getToken(),
  userInfo: uni.getStorageSync('user_info') || null
}

const mutations = {
  SET_TOKEN (state, token) {
    state.token = token
    if (token) {
      setToken(token)
    } else {
      removeToken()
    }
  },
  SET_USER_INFO (state, userInfo) {
    state.userInfo = userInfo
    if (userInfo) {
      uni.setStorageSync('user_info', userInfo)
    } else {
      uni.removeStorageSync('user_info')
    }
  }
}

const actions = {
  /**
   * 登录
   * @param {Object} payload { loginType, userInfo, password }
   */
  async login ({ commit }, payload) {
    const res = await loginApi(payload)
    const token = res.data
    commit('SET_TOKEN', token)
    return res
  },

  /**
   * 退出登录
   */
  logout ({ commit }) {
    commit('SET_TOKEN', '')
    commit('SET_USER_INFO', null)
  },

  /**
   * 刷新用户信息
   */
  async refreshUserInfo ({ commit }) {
    if (!getToken()) return
    try {
      const res = await getUserInfo()
      commit('SET_USER_INFO', res.data)
      return res
    } catch (e) {
      // token 失效，清除
      commit('SET_TOKEN', '')
      commit('SET_USER_INFO', null)
    }
  }
}

const getters = {
  isLoggedIn: state => !!state.token,
  userInfo: state => state.userInfo,
  userRole: state => state.userInfo ? state.userInfo.role : null,
  userNickname: state => state.userInfo ? state.userInfo.nickname : '',
  userAvatar: state => state.userInfo ? state.userInfo.avatar : ''
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
