/**
 * 角色工具：与后端 RoleEnum 保持一致
 *   ADMIN   = 1 管理员
 *   TEACHER = 2 教师
 *   STUDENT = 3 学生
 */
import store from '../store'

export const ROLE = {
  ADMIN: 1,
  TEACHER: 2,
  STUDENT: 3
}

export const ROLE_NAME = {
  [ROLE.ADMIN]: '管理员',
  [ROLE.TEACHER]: '教师',
  [ROLE.STUDENT]: '学生'
}

/**
 * 获取当前登录用户的角色 id
 * @returns {number|null}
 */
export function getCurrentRoleId () {
  const info = store.getters.userInfo
  return info && info.userRoleId ? info.userRoleId : null
}

/**
 * 判断当前用户是否拥有指定角色之一
 * @param {number|number[]} roles 单个角色 id 或角色 id 数组
 * @returns {boolean}
 */
export function hasRole (roles) {
  const current = getCurrentRoleId()
  if (current === null) {
    return false
  }
  const arr = Array.isArray(roles) ? roles : [roles]
  return arr.includes(current)
}

/**
 * 是否管理员
 */
export function isAdmin () {
  return getCurrentRoleId() === ROLE.ADMIN
}

/**
 * 是否教师
 */
export function isTeacher () {
  return getCurrentRoleId() === ROLE.TEACHER
}

/**
 * 是否学生
 */
export function isStudent () {
  return getCurrentRoleId() === ROLE.STUDENT
}

/**
 * 是否教师或管理员（管理类操作常用）
 */
export function isTeacherOrAdmin () {
  return hasRole([ROLE.TEACHER, ROLE.ADMIN])
}
