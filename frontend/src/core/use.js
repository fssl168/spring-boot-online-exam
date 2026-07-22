import Vue from 'vue'
import VueStorage from 'vue-ls'
import config from '../config/defaultSettings'

// base library
import Antd from 'ant-design-vue'
import Viser from 'viser-vue'
import VueCropper from 'vue-cropper'
import 'ant-design-vue/dist/antd.less'

// ext library
import VueClipboard from 'vue-clipboard2'
import PermissionHelper from '../utils/helper/permission'
// import '../components/use'
import './directives/action'
import './directives/hasRole'
import errorNotify from '../utils/errorNotification'

VueClipboard.config.autoSetContainer = true

Vue.use(Antd)
Vue.use(Viser)

Vue.use(VueStorage, config.storageOptions)
Vue.use(VueClipboard)
Vue.use(PermissionHelper)
Vue.use(VueCropper)

// Batch 7.3.4：注册统一错误通知工具，组件内可用 this.$errorNotify 调用
Vue.prototype.$errorNotify = errorNotify
