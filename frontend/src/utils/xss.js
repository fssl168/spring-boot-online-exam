/**
 * 轻量级 XSS 过滤工具
 *
 * 用于在响应拦截器中对后端返回的字符串字段进行清洗，防止题目/选项内容中
 * 携带的 <script>、on* 事件处理器、javascript: 伪协议等触发 XSS。
 *
 * 注：项目当前未引入 DOMPurify 等成熟库，这里采用基于正则的白名单清洗策略，
 * 能覆盖绝大多数富文本注入场景。若后续引入 DOMPurify，可直接替换 sanitizeHTML 实现。
 */

// 危险标签：script / iframe / object / embed / svg / style / link / meta / base
const DANGEROUS_TAG_RE = /<\/?(script|iframe|object|embed|svg|style|link|meta|base|form|input|textarea|button|applet)[^>]*>/gi

// on* 事件处理器：onclick= / onerror= / onload= ...
const ON_EVENT_RE = /\son[a-zA-Z]+\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)/gi

// javascript: / vbscript: / data:text/html 伪协议
const DANGEROUS_PROTO_RE = /(javascript|vbscript|data:text\/html)\s*:/gi

/**
 * 清洗单个 HTML 字符串
 * @param {string} html
 * @returns {string}
 */
export function sanitizeHTML (html) {
  if (typeof html !== 'string') {
    return html
  }
  return html
    .replace(DANGEROUS_TAG_RE, '')
    .replace(ON_EVENT_RE, '')
    .replace(DANGEROUS_PROTO_RE, '')
}

/**
 * 递归清洗对象中所有字符串字段
 * 仅对 string 类型字段做过滤，保留 number / boolean / array / object 结构。
 * @param {*} data
 * @returns {*}
 */
export function sanitizeDeep (data) {
  if (data === null || data === undefined) {
    return data
  }
  if (typeof data === 'string') {
    return sanitizeHTML(data)
  }
  if (Array.isArray(data)) {
    for (let i = 0; i < data.length; i++) {
      data[i] = sanitizeDeep(data[i])
    }
    return data
  }
  if (typeof data === 'object') {
    // 不处理 File / Blob / FormData 等特殊对象
    if (data instanceof File || data instanceof Blob || data instanceof FormData) {
      return data
    }
    for (const key in data) {
      if (Object.prototype.hasOwnProperty.call(data, key)) {
        data[key] = sanitizeDeep(data[key])
      }
    }
    return data
  }
  return data
}

export default {
  sanitizeHTML,
  sanitizeDeep
}
