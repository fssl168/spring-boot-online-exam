/**
 * 全局 Loading 状态管理
 *
 * 用于防止表单重复提交和按钮重复点击。请求拦截器在发起请求时 +1，
 * 响应/错误拦截器在结束时 -1。组件可通过 mapState/mapGetters 读取
 * `loadingPending` 来禁用提交按钮或显示全屏 loading。
 *
 * 注意：当前实现为全局计数器，适用于低并发场景（如考试系统）。
 * 若后续需要更精细的按 URL 区分 loading，可扩展为 Map 结构。
 */
import Vue from 'vue'

const loading = {
  state: {
    // 当前进行中的请求数量
    pendingCount: 0
  },
  mutations: {
    LOADING_INC (state) {
      state.pendingCount += 1
    },
    LOADING_DEC (state) {
      state.pendingCount = Math.max(0, state.pendingCount - 1)
    },
    LOADING_RESET (state) {
      state.pendingCount = 0
    }
  },
  actions: {
    LoadingInc ({ commit }) {
      commit('LOADING_INC')
    },
    LoadingDec ({ commit }) {
      commit('LOADING_DEC')
    },
    LoadingReset ({ commit }) {
      commit('LOADING_RESET')
    }
  },
  getters: {
    // 是否存在进行中的请求
    isLoading: state => state.pendingCount > 0
  }
}

// 兼容老代码中使用 Vue.$loading 的写法
Vue.prototype.$isLoading = () => loading.state.pendingCount > 0

export default loading
