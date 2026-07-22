// 公告相关接口（Batch 7.3.1）

import api from './index'
import { axios } from '../utils/request'

// 获取所有可见公告（已登录用户均可查看）
export function getAnnouncementList () {
  return axios({
    url: api.AnnouncementList,
    method: 'get',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })
}

// 创建公告（管理员）
export function createAnnouncement (parameter) {
  return axios({
    url: api.AnnouncementCreate,
    method: 'post',
    data: parameter
  })
}

// 更新公告（管理员）
export function updateAnnouncement (parameter) {
  return axios({
    url: api.AnnouncementUpdate,
    method: 'post',
    data: parameter
  })
}

// 删除公告（管理员）
export function deleteAnnouncement (id) {
  return axios({
    url: api.AnnouncementDelete + id,
    method: 'delete'
  })
}
