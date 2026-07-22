import Vue from 'vue'
import { hasRole } from '../../utils/role'

/**
 * 角色权限指令：根据当前用户角色控制元素的显示/隐藏
 *
 * 用法：
 *   <!-- 仅管理员可见 -->
 *   <a-button v-hasRole="1">仅管理员</a-button>
 *
 *   <!-- 教师或管理员可见（传数组） -->
 *   <a-button v-hasRole="[2, 1]">教师/管理员</a-button>
 *
 *   <!-- 使用 ROLE 常量 -->
 *   <a-button v-hasRole="ROLE.ADMIN">管理员</a-button>
 *
 * 当前用户角色不匹配时，元素会从 DOM 中移除
 */
const hasRoleDirective = Vue.directive('hasRole', {
  inserted: function (el, binding) {
    const value = binding.value
    if (value === undefined || value === null) {
      return
    }
    const roles = Array.isArray(value) ? value : [value]
    if (!hasRole(roles)) {
      el.parentNode && el.parentNode.removeChild(el) || (el.style.display = 'none')
    }
  }
})

export default hasRoleDirective
