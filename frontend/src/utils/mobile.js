/**
 * 移动端工具函数模块
 *
 * 提供：
 * - 设备检测：isMobileDevice() / getViewportWidth()
 * - 触摸优化：preventDoubleTapZoom() — 禁止双击缩放
 * - 滚动辅助：scrollToPosition() / scrollToElement()
 * - 格式化：formatMobileTime() — 紧凑时间显示
 *
 * 与 mixin.js 的 mixinDevice 配合使用：
 *   mixin.isMobile()       基于 Vuex app.device 状态（响应式，用于模板）
 *   mobile.isMobileDevice() 基于 window.innerWidth（瞬时值，用于 JS 逻辑）
 *
 * 断点：MOBILE_BREAKPOINT = 768px（与 enquire.js 配置一致）
 */

// 移动端断点（与 frontend/src/utils/device.js enquire.js 配置保持一致）
export const MOBILE_BREAKPOINT = 768

/**
 * 检测当前是否为移动端视口（基于 window.innerWidth 瞬时值）
 * 用于 JS 逻辑判断（如决定 window.open 还是 router.push）
 * 模板内响应式判断请使用 mixinDevice 的 isMobile()
 * @returns {boolean}
 */
export function isMobileDevice () {
  if (typeof window === 'undefined') return false
  return window.innerWidth < MOBILE_BREAKPOINT
}

/**
 * 获取当前视口宽度
 * @returns {number}
 */
export function getViewportWidth () {
  if (typeof window === 'undefined') return 1920
  return window.innerWidth
}

/**
 * 禁止移动端双击缩放
 * 原理：监听 touchend 事件，若两次 touchend 间隔 < 300ms 且无显著移动，preventDefault
 * 应在 App.vue 的 mounted 钩子中调用一次
 * @param {HTMLElement} [target=document] 监听目标
 */
export function preventDoubleTapZoom (target) {
  if (typeof window === 'undefined') return
  const el = target || document
  let lastTouchEnd = 0
  el.addEventListener('touchend', function (e) {
    const now = Date.now()
    if (now - lastTouchEnd <= 300) {
      e.preventDefault()
    }
    lastTouchEnd = now
  }, { passive: false })
}

/**
 * 滚动到指定位置
 * @param {number} [top=0] 目标 scrollTop
 * @param {boolean} [smooth=true] 是否平滑滚动
 */
export function scrollToPosition (top = 0, smooth = true) {
  if (typeof window === 'undefined') return
  window.scrollTo({
    top,
    behavior: smooth ? 'smooth' : 'auto'
  })
}

/**
 * 滚动到指定元素位置
 * @param {HTMLElement|string} el 目标元素或选择器
 * @param {number} [offset=-12] 偏移量（负数表示提前停止，避免被固定 header 遮挡）
 */
export function scrollToElement (el, offset = -12) {
  if (typeof window === 'undefined' || !el) return
  const target = typeof el === 'string' ? document.querySelector(el) : el
  if (!target) return
  const rect = target.getBoundingClientRect()
  const top = window.pageYOffset + rect.top + offset
  scrollToPosition(top, true)
}

/**
 * 移动端紧凑时间格式化
 * - 桌面端：'2026-07-23 14:30:00'
 * - 移动端：'07-23 14:30'
 * @param {Date|string|number} date 日期
 * @param {boolean} [mobile=null] 是否强制移动端格式（默认根据视口自动判断）
 * @returns {string}
 */
export function formatMobileTime (date, mobile = null) {
  if (!date) return ''
  const d = new Date(date)
  if (isNaN(d.getTime())) return ''
  const pad = n => String(n).padStart(2, '0')
  const y = d.getFullYear()
  const mo = pad(d.getMonth() + 1)
  const da = pad(d.getDate())
  const h = pad(d.getHours())
  const mi = pad(d.getMinutes())
  const s = pad(d.getSeconds())
  const isMobile = mobile === null ? isMobileDevice() : mobile
  return isMobile
    ? `${mo}-${da} ${h}:${mi}`
    : `${y}-${mo}-${da} ${h}:${mi}:${s}`
}

export default {
  MOBILE_BREAKPOINT,
  isMobileDevice,
  getViewportWidth,
  preventDoubleTapZoom,
  scrollToPosition,
  scrollToElement,
  formatMobileTime
}
