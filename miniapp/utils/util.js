/**
 * 通用工具函数
 */

/**
 * 格式化日期
 * @param {Date|string|number} date 日期
 * @param {string} fmt 格式，默认 'yyyy-MM-dd HH:mm:ss'
 * @returns {string}
 */
export function formatDate (date, fmt = 'yyyy-MM-dd HH:mm:ss') {
  if (!date) return ''
  const d = new Date(date)
  if (isNaN(d.getTime())) return ''
  const o = {
    'M+': d.getMonth() + 1,
    'd+': d.getDate(),
    'H+': d.getHours(),
    'm+': d.getMinutes(),
    's+': d.getSeconds()
  }
  if (/(y+)/.test(fmt)) {
    fmt = fmt.replace(RegExp.$1, (d.getFullYear() + '').substr(4 - RegExp.$1.length))
  }
  for (const k in o) {
    if (new RegExp('(' + k + ')').test(fmt)) {
      fmt = fmt.replace(RegExp.$1, RegExp.$1.length === 1 ? o[k] : ('00' + o[k]).substr(('' + o[k]).length))
    }
  }
  return fmt
}

/**
 * 秒数转 mm:ss 格式
 * @param {number} seconds 秒
 * @returns {string}
 */
export function formatTimeCost (seconds) {
  if (!seconds && seconds !== 0) return '-'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return (m < 10 ? '0' + m : m) + ':' + (s < 10 ? '0' + s : s)
}

/**
 * 去除 HTML 标签，用于题目预览
 * @param {string} html HTML 字符串
 * @returns {string}
 */
export function stripHtml (html) {
  if (!html) return ''
  return html.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim()
}

/**
 * 根据考试起止时间计算状态
 * @param {Object} exam 考试对象，含 examStartDate/examEndDate 或 startDate/endDate
 * @returns {Object} { status: 0|1|2, text: '未开始'|'进行中'|'已结束' }
 */
export function getExamStatus (exam) {
  const now = new Date()
  const start = new Date(exam.examStartDate || exam.startDate)
  const end = new Date(exam.examEndDate || exam.endDate)
  // 后端 status 字段优先：0-未开始 1-进行中 2-已结束
  if (exam.status !== undefined) {
    const text = exam.status === 0 ? '未开始' : exam.status === 1 ? '进行中' : '已结束'
    return { status: exam.status, text }
  }
  if (now < start) return { status: 0, text: '未开始' }
  if (now > end) return { status: 2, text: '已结束' }
  return { status: 1, text: '进行中' }
}

/**
 * 角色枚举转文字
 * @param {number} roleId 1=ADMIN 2=TEACHER 3=STUDENT
 * @returns {string}
 */
export function formatRole (roleId) {
  const map = { 1: '管理员', 2: '教师', 3: '学生' }
  return map[roleId] || '未知'
}

/**
 * 考试记录状态转文字
 * @param {number} status 0=IN_PROGRESS 1=SUBMITTED 2=GRADED
 * @returns {string}
 */
export function formatRecordStatus (status) {
  if (status === 0) return '进行中'
  if (status === 1) return '已提交'
  if (status === 2) return '已批改'
  return '已提交'
}
