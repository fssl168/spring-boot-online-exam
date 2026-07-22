// 考试相关的接口，包括考试、问题、选项和评分等接口

import api from './index'
import { axios } from '../utils/request'

export function getQuestionList (parameter) {
  return axios({
    url: api.ExamQuestionList,
    method: 'get',
    params: parameter
  })
}

export function getQuestionAll () {
  return axios({
    url: api.ExamQuestionAll,
    method: 'get'
  })
}

// Batch 7.2.2：分页查询题目列表（服务端分页）
// 参数：{ page: 0-based, size: 每页数, sort: '字段,asc|desc' }
export function getQuestionPage (parameter) {
  return axios({
    url: api.ExamQuestionPage,
    method: 'get',
    params: parameter
  })
}

export function questionUpdate (parameter) {
  console.log(parameter)
  return axios({
    url: api.ExamQuestionUpdate,
    method: 'post',
    data: parameter
  })
}

export function getQuestionSelection () {
  return axios({
    url: api.ExamQuestionSelection,
    method: 'get',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

export function questionCreate (parameter) {
  console.log(parameter)
  return axios({
    url: api.ExamQuestionCreate,
    method: 'post',
    data: parameter
  })
}

export function getExamList (parameter) {
  return axios({
    url: api.ExamList,
    method: 'get',
    params: parameter
  })
}

export function getExamAll () {
  return axios({
    url: api.ExamAll,
    method: 'get'
  })
}

// Batch 7.2.3：分页查询考试列表（服务端分页）
// 参数：{ page: 0-based, size: 每页数, sort: '字段,asc|desc' }
export function getExamPage (parameter) {
  return axios({
    url: api.ExamPage,
    method: 'get',
    params: parameter
  })
}

// 获取所有问题，按照单选、多选和判断进行分类
export function getExamQuestionTypeList () {
  return axios({
    url: api.ExamQuestionTypeList,
    method: 'get',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

export function getExamCardList () {
  return axios({
    url: api.ExamCardList,
    method: 'get',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

export function examCreate (parameter) {
  console.log(parameter)
  return axios({
    url: api.ExamCreate,
    method: 'post',
    data: parameter
  })
}

export function examUpdate (parameter) {
  console.log(parameter)
  return axios({
    url: api.ExamUpdate,
    method: 'post',
    data: parameter
  })
}

export function getExamDetail (examId) {
  return axios({
    url: api.ExamDetail + examId,
    method: 'get',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

export function getExamRecordDetail (recordId) {
  return axios({
    url: api.recordDetail + recordId,
    method: 'get',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

export function getQuestionDetail (questionId) {
  return axios({
    url: api.QuestionDetail + questionId,
    method: 'get',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

export function finishExam (examId, answersMap, timeCost) {
  console.log(answersMap)
  const headers = {
    'Content-Type': 'application/json;charset=UTF-8'
  }
  if (timeCost !== undefined && timeCost !== null) {
    headers['X-Time-Cost'] = timeCost
  }
  return axios({
    url: api.FinishExam + examId,
    method: 'post',
    headers: headers,
    data: answersMap
  })
}

export function getExamRecordList () {
  return axios({
    url: api.ExamRecordList,
    method: 'get',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

// 获取指定考试的所有学生考试记录（教师查看）
export function getExamAllRecords (examId) {
  return axios({
    url: api.ExamRecordAll + examId,
    method: 'get',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

// 获取指定考试的成绩统计信息（教师查看）
export function getExamScoreStat (examId) {
  return axios({
    url: api.ExamScoreStat + examId,
    method: 'get',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}
