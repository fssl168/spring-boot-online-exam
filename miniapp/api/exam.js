/**
 * 考试相关 API
 */
import { get, post, del } from '../utils/request'

/**
 * 获取考试卡片列表
 * @returns {Promise} ResultVO<List<ExamCardVo>>
 */
export function getExamCardList () {
  return get('/api/exam/card/list')
}

/**
 * 获取考试详情
 * @param {string} examId 考试ID
 * @returns {Promise} ResultVO<ExamDetailVo>
 */
export function getExamDetail (examId) {
  return get('/api/exam/detail/' + examId)
}

/**
 * 获取题目详情
 * @param {string} questionId 题目ID
 * @returns {Promise} ResultVO<QuestionDetailVo>
 */
export function getQuestionDetail (questionId) {
  return get('/api/exam/question/detail/' + questionId)
}

/**
 * 提交考试答案（交卷判分）
 * @param {string} examId 考试ID
 * @param {Object} answersMap { questionId: [optionId, ...] }
 * @param {number} timeCost 耗时（秒）
 * @returns {Promise} ResultVO<ExamRecord>
 */
export function finishExam (examId, answersMap, timeCost) {
  return post('/api/exam/finish/' + examId, answersMap, {
    header: { 'X-Time-Cost': timeCost }
  })
}

/**
 * 获取当前用户的考试记录列表
 * @returns {Promise} ResultVO<List<ExamRecordVo>>
 */
export function getExamRecordList () {
  return get('/api/exam/record/list')
}

/**
 * 获取考试记录详情（答题回顾）
 * @param {string} recordId 记录ID
 * @returns {Promise} ResultVO<RecordDetailVo>
 */
export function getExamRecordDetail (recordId) {
  return get('/api/exam/record/detail/' + recordId)
}

/**
 * 删除考试记录
 * @param {string} recordId 记录ID
 */
export function deleteExamRecord (recordId) {
  return del('/api/exam/record/' + recordId)
}
