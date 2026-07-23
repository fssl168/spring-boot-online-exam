import Vue from 'vue'
import App from './App'
import store from './store'

// 引入 uView UI（如已安装可取消注释）
// import uView from 'uview-ui'
// Vue.use(uView)

Vue.config.productionTip = false
Vue.prototype.$store = store

App.mpType = 'app'

const app = new Vue({
  store,
  ...App
})
app.$mount()
