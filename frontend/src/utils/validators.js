/**
 * Batch 7.1.2 通用表单校验规则与工具
 *
 * 提供一组可复用的 antd v1 v-decorator rules 与 validator 函数：
 *   - 长度限制（防过载输入）
 *   - 特殊字符过滤（防 SQL 注入 / XSS 的前端第一道防线，后端仍需校验）
 *   - 用户名 / 邮箱 / 手机号 / 密码强度等常用规则
 *
 * 用法：
 *   import { validators, rules } from '@/utils/validators'
 *   v-decorator="['username', { rules: rules.username }]"
 *   v-decorator="['nickname', { rules: rules.nickname }]"
 *   v-decorator="['password', { rules: rules.password }]"
 *
 * 注：前端校验仅是体验优化与第一道防线，真正的安全防线在后端
 * （GlobalExceptionHandler + @Valid + @NotBlank/@Size）。
 */

// 通用：禁止出现的危险字符（仅做轻量过滤；HTML 清洗由 xss.js 完成）
const DANGEROUS_CHARS = /[<>"'`\\;]/

/**
 * 长度区间校验器
 * @param {number} min 最小长度
 * @param {number} max 最大长度
 * @param {string} label 字段中文名（用于错误提示）
 */
function length (min, max, label) {
  return {
    validator: (rule, value, callback) => {
      if (!value) {
        callback()
        return
      }
      if (value.length < min) {
        callback(new Error(`${label}至少 ${min} 个字符`))
        return
      }
      if (value.length > max) {
        callback(new Error(`${label}不能超过 ${max} 个字符`))
        return
      }
      callback()
    }
  }
}

/**
 * 危险字符校验器：拒绝 < > " ' ` \ ; 等可能用于 XSS / SQL 注入的字符
 * @param {string} label 字段中文名
 */
function noDangerousChars (label) {
  return {
    validator: (rule, value, callback) => {
      if (!value) {
        callback()
        return
      }
      if (DANGEROUS_CHARS.test(value)) {
        callback(new Error(`${label}不能包含 < > " ' \` \\ ; 等特殊字符`))
        return
      }
      callback()
    }
  }
}

/**
 * 密码强度校验器：至少 8 位，必须同时包含字母和数字
 */
function passwordStrength () {
  return {
    validator: (rule, value, callback) => {
      if (!value) {
        callback()
        return
      }
      if (value.length < 8) {
        callback(new Error('密码至少 8 位'))
        return
      }
      if (!/[a-zA-Z]/.test(value) || !/[0-9]/.test(value)) {
        callback(new Error('密码必须同时包含字母和数字'))
        return
      }
      callback()
    }
  }
}

/**
 * 用户名校验器：字母数字下划线，3-32 位
 */
function username () {
  return {
    validator: (rule, value, callback) => {
      if (!value) {
        callback()
        return
      }
      if (!/^[a-zA-Z0-9_]{3,32}$/.test(value)) {
        callback(new Error('用户名只能包含字母、数字、下划线，长度 3-32 位'))
        return
      }
      callback()
    }
  }
}

/**
 * 手机号校验器：11 位，1[3-9] 开头
 */
function mobile () {
  return {
    pattern: /^1[3-9]\d{9}$/,
    message: '请输入正确的 11 位手机号'
  }
}

/**
 * 邮箱校验器
 */
function email () {
  return {
    type: 'email',
    message: '请输入正确的邮箱地址'
  }
}

// 导出单个 validator 函数（供自定义 rules 数组组装）
export const validators = {
  length,
  noDangerousChars,
  passwordStrength,
  username,
  mobile,
  email
}

// 导出预组装的 rules 常用集合，直接用于 v-decorator
export const rules = {
  // 用户名：必填 + 字母数字下划线 3-32 位
  username: [
    { required: true, message: '请输入用户名' },
    validators.username()
  ],
  // 昵称：必填 + 长度 1-32 + 危险字符过滤
  nickname: [
    { required: true, message: '请输入昵称' },
    validators.length(1, 32, '昵称'),
    validators.noDangerousChars('昵称')
  ],
  // 密码：必填 + 8 位以上 + 字母+数字
  password: [
    { required: true, message: '请输入密码' },
    validators.passwordStrength()
  ],
  // 邮箱
  email: [
    { required: true, message: '请输入邮箱地址' },
    validators.email()
  ],
  // 手机号
  mobile: [
    { required: true, message: '请输入手机号' },
    validators.mobile
  ],
  // 验证码：必填 + 4-8 位
  captcha: [
    { required: true, message: '请输入验证码' },
    validators.length(4, 8, '验证码')
  ],
  // 考试/题目名称：必填 + 长度 1-100 + 危险字符过滤
  title: [
    { required: true, message: '请输入名称' },
    validators.length(1, 100, '名称'),
    validators.noDangerousChars('名称')
  ],
  // 描述/简介：可选 + 长度 0-1000
  description: [
    validators.length(0, 1000, '描述')
  ]
}

export default {
  validators,
  rules
}
