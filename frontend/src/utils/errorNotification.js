/**
 * Batch 7.3.4：统一错误通知工具
 *
 * 目标：将散落在 38+ 处的 `notification.error({ message, description })` 调用收口到此处，
 * 便于后续统一控制样式、显示时长、文案规范、埋点等。
 *
 * 使用方式：
 *   1. 组件中：this.$errorNotify.fromResponse('获取考试列表失败', res)
 *   2. 组件中：this.$errorNotify.fromError('提交失败', err)
 *   3. 组件中：this.$errorNotify.error({ message: '...', description: '...' })
 *   4. 非组件（如 request.js / permission.js）：直接 import 后调用
 */
import { notification } from 'ant-design-vue'

const DEFAULT_DURATION = 4.5

/**
 * 基础错误通知
 * @param {Object} options
 * @param {string} options.message - 通知标题（必填）
 * @param {string} [options.description] - 通知描述
 * @param {number} [options.duration] - 显示时长（秒），默认 4.5，设为 0 则不自动关闭
 */
function error ({ message, description, duration }) {
  notification.error({
    message,
    description: description || '',
    duration: duration != null ? duration : DEFAULT_DURATION
  })
}

/**
 * 从后端 ResultVO/JsonData 响应中提取错误描述并展示
 * 兼容后端返回的 msg 与 message 两个字段
 * @param {string} message - 通知标题
 * @param {Object} [res] - 后端响应对象
 * @param {Object} [options] - 额外配置（duration 等）
 */
function fromResponse (message, res, options = {}) {
  const description = (res && (res.msg || res.message)) || '请稍后重试'
  error({ message, description, ...options })
}

/**
 * 从 JS Error / catch err 中提取错误描述并展示
 * @param {string} message - 通知标题
 * @param {Error|Object|string} [err] - JS 错误对象或任意 catch 到的值
 * @param {Object} [options] - 额外配置（duration 等）
 */
function fromError (message, err, options = {}) {
  let description
  if (err == null) {
    description = '请稍后重试'
  } else if (err instanceof Error || typeof err === 'object') {
    description = err.message || String(err)
  } else {
    description = String(err)
  }
  error({ message, description, ...options })
}

export default {
  error,
  fromResponse,
  fromError
}
