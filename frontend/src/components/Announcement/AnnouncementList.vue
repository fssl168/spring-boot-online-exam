<template>
  <a-card :bordered="false" class="announcement-card" :loading="loading" title="系统公告">
    <div v-if="announcements.length === 0 && !loading" class="empty-text">
      暂无公告
    </div>
    <a-list v-else :data-source="announcements" item-layout="vertical">
      <a-list-item slot="renderItem" slot-scope="item">
        <a-list-item-meta>
          <a-tag slot="title" :color="tagColor(item.type)">
            <a-icon :type="iconType(item.type)" />
            {{ item.title }}
          </a-tag>
          <span slot="description" class="announcement-time">{{ formatTime(item.createTime) }}</span>
        </a-list-item-meta>
        <div class="announcement-content" v-html="item.content"></div>
      </a-list-item>
    </a-list>
  </a-card>
</template>

<script>
import { getAnnouncementList } from '../../api/announcement'

export default {
  name: 'AnnouncementList',
  data () {
    return {
      loading: false,
      announcements: []
    }
  },
  mounted () {
    this.loadAnnouncements()
  },
  methods: {
    loadAnnouncements () {
      const that = this
      that.loading = true
      getAnnouncementList()
        .then(res => {
          if (res.code === 0) {
            that.announcements = res.data || []
          } else {
            // Batch 7.3.4：改用统一错误通知工具
            that.$errorNotify.fromResponse('获取公告失败', res)
          }
        })
        .catch(() => {
          // 静默失败：公告为非核心功能，不阻塞首页渲染
          that.announcements = []
        })
        .finally(() => {
          that.loading = false
        })
    },
    /**
     * 根据 type 返回 a-tag 颜色
     */
    tagColor (type) {
      switch (type) {
        case 'success': return 'green'
        case 'warning': return 'orange'
        case 'error': return 'red'
        case 'info':
        default: return 'blue'
      }
    },
    /**
     * 根据 type 返回 a-icon 类型
     */
    iconType (type) {
      switch (type) {
        case 'success': return 'check-circle'
        case 'warning': return 'warning'
        case 'error': return 'close-circle'
        case 'info':
        default: return 'info-circle'
      }
    },
    formatTime (time) {
      if (!time) return ''
      return time
    }
  }
}
</script>

<style lang="less" scoped>
  .announcement-card {
    margin-bottom: 16px;

    .empty-text {
      text-align: center;
      color: #999;
      padding: 24px 0;
    }

    .announcement-time {
      color: #999;
      font-size: 12px;
      margin-left: 8px;
    }

    .announcement-content {
      padding: 8px 0;
      color: #333;
      line-height: 1.6;
    }
  }
</style>
