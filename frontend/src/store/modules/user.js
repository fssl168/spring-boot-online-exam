import Vue from 'vue'
import { login, getInfo, logout } from '../../api/login'
import { ACCESS_TOKEN, REMEMBER_ME_FLAG } from '../../store/mutation-types'
import { welcome } from '../../utils/util'

const user = {
  state: {
    token: '',
    name: '',
    welcome: '',
    avatar: '',
    roles: [],
    info: {}
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
    },
    SET_NAME: (state, { name, welcome }) => {
      state.name = name
      state.welcome = welcome
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles
    },
    SET_INFO: (state, info) => {
      state.info = info
    }
  },

  actions: {
    // 登录
    // Batch 7.3.2：新增 rememberMe 参数，勾选时延长 token 有效期至 7 天，否则保持 24 小时
    Login ({ commit }, userInfo) {
      return new Promise((resolve, reject) => {
        login(userInfo).then(response => {
          if (response.code === 0) {
            const token = response.data
            // 根据是否勾选"记住我"决定 token 有效期：
            //   勾选 -> 7 天（长时间保持登录）
            //   未勾选 -> 24 小时（会话级，浏览器关闭/到期需重新登录）
            const rememberMe = userInfo && userInfo.rememberMe
            const expireMs = rememberMe
              ? 7 * 24 * 60 * 60 * 1000
              : 24 * 60 * 60 * 1000
            Vue.ls.set(ACCESS_TOKEN, token, expireMs)
            // 持久化记住我标志，便于后续刷新页面时识别
            if (rememberMe) {
              Vue.ls.set(REMEMBER_ME_FLAG, true, expireMs)
            } else {
              Vue.ls.remove(REMEMBER_ME_FLAG)
            }
            // 设置token事件,修改全局变量state中的token值，讲mutations中的SET_TOKEN事件
            commit('SET_TOKEN', token)
            resolve()
          } else {
            // 自定义错误
            reject(new Error('用户名或密码错误'))
          }
        }).catch(error => {
          console.log(error)
          reject(error)
        })
      })
    },

    // 获取用户信息
    GetInfo ({ commit }) {
      return new Promise((resolve, reject) => {
        getInfo().then(response => {
          console.log('/user/info的响应如下：')
          console.log(response)
          const result = response.data // 取出响应体

          if (result.role && result.role.permissions.length > 0) { // 如果权限
            const role = result.role
            role.permissions = result.role.permissions // permissions是给页面行为设置权限
            role.permissions.map(per => {
              if (per.actionEntitySet != null && per.actionEntitySet.length > 0) {
                const action = per.actionEntitySet.map(action => {
                  return action.action
                })
                per.actionList = action
              }
            })
            role.permissionList = role.permissions.map(permission => { // permissionList是从permissions中遍历解析得来的
              return permission.permissionId
            })

            // 这些设置都在Vuex的getters里面了
            commit('SET_ROLES', result.role) // 在store中设置用户的权限
            commit('SET_INFO', result) // 在store中设置用户信息
          } else {
            reject(new Error('getInfo: roles must be a non-null array !'))
          }

          // 这些设置都在Vuex的getters里面了
          commit('SET_NAME', { name: result.name, welcome: welcome() }) // 设置用户名称
          commit('SET_AVATAR', result.avatar) // 设置用户头像

          resolve(response)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 登出
    Logout ({ commit, state }) {
      return new Promise((resolve) => {
        commit('SET_TOKEN', '')
        commit('SET_ROLES', [])
        Vue.ls.remove(ACCESS_TOKEN)
        // Batch 7.3.2：登出时清除记住我标志（但保留记住的用户名，便于下次自动填充）
        Vue.ls.remove(REMEMBER_ME_FLAG)

        logout(state.token).then(() => {
          resolve()
        }).catch(() => {
          resolve()
        })
      })
    }

  }
}

export default user
