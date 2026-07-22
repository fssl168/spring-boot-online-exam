<template>
  <div class="card-list" ref="content">
    <a-list
      :grid="{gutter: 24, lg: 3, md: 2, sm: 1, xs: 1}"
      :dataSource="dataSource"
    >
      <a-list-item slot="renderItem" slot-scope="item">
        <a-card :hoverable="item.status === 1" @click="joinExam(item)">
          <a-card-meta>
            <div style="margin-bottom: 3px" slot="title">
              {{ item.title }}
              <a-tag :color="statusColor(item.status)" style="margin-left: 8px;">{{ statusText(item.status) }}</a-tag>
            </div>
            <a-avatar class="card-avatar" slot="avatar" :src="item.avatar | imgSrcFilter" size="large" />
            <div class="meta-content" slot="description">{{ item.content }}</div>
          </a-card-meta>
          <template class="ant-card-actions" slot="actions">
            <a>满分：{{ item.score }}分</a>
            <a>限时：{{ item.elapse }}分钟</a>
          </template>
        </a-card>
      </a-list-item>
    </a-list>
  </div>
</template>

<script>
import { getExamCardList } from '../../api/exam'
import { mixinDevice } from '../../utils/mixin'
import { isMobileDevice } from '../../utils/mobile'

export default {
  name: 'ExamCardList',
  mixins: [mixinDevice],
  data () {
    return {
      description: '您可以随意点击下面的考试卡片开始一场属于您的考试',
      extraImage: 'https://gw.alipayobjects.com/zos/rmsportal/RzwpdLnhmvDJToTdfDPe.png',
      dataSource: []
    }
  },
  methods: {
    joinExam (item) {
      // 非进行中状态的考试不允许参加
      if (item.status !== 1) {
        this.$notification.warning({
          message: '无法参加考试',
          description: '该考试' + this.statusText(item.status) + '，无法进入'
        })
        return
      }
      const routeUrl = this.$router.resolve({
        path: `/exam/${item.id}`
      })
      // 批次 3.3：移动端 window.open 会被浏览器拦截，改用 router.push 当前页跳转
      if (isMobileDevice() || this.isMobile()) {
        this.$router.push({ path: `/exam/${item.id}` })
      } else {
        window.open(routeUrl.href, '_blank')
      }
    },
    statusText (status) {
      const map = { 0: '未开始', 1: '进行中', 2: '已结束' }
      return map[status] || '未知'
    },
    statusColor (status) {
      const map = { 0: 'orange', 1: 'green', 2: 'red' }
      return map[status] || 'default'
    }
  },
  mounted () {
    // 从后端数据获取考试列表，适配前端卡片
    getExamCardList().then(res => {
      console.log(res)
      if (res.code === 0) {
        this.dataSource = res.data
      } else {
        // Batch 7.3.4：改用统一错误通知工具
        this.$errorNotify.fromResponse('获取考试列表失败', res)
      }
    }).catch(err => {
      // 失败就弹出警告消息
      // Batch 7.3.4：改用统一错误通知工具
      this.$errorNotify.fromError('获取考试列表失败', err)
    })
  }
}
</script>

<style lang="less" scoped>
  .card-avatar {
    width: 48px;
    height: 48px;
    border-radius: 48px;
  }

  .ant-card-actions {
    background: #f7f9fa;

    li {
      float: left;
      text-align: center;
      margin: 12px 0;
      color: rgba(0, 0, 0, 0.45);
      width: 50%;

      &:not(:last-child) {
        border-right: 1px solid #e8e8e8;
      }

      a {
        color: rgba(0, 0, 0, .45);
        line-height: 22px;
        display: inline-block;
        width: 100%;

        &:hover {
          color: #1890ff;
        }
      }
    }
  }

  .new-btn {
    background-color: #fff;
    border-radius: 2px;
    width: 100%;
    height: 188px;
  }

  .meta-content {
    position: relative;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    height: 64px;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
  }
</style>

<style lang="less">
  /* 批次 3.3：移动端卡片样式优化 */
  @media (max-width: 767px) {
    .card-list {
      .ant-card {
        .ant-card-meta-title {
          font-size: 15px;
        }

        .ant-card-actions {
          li {
            font-size: 13px;
          }
        }
      }
    }
  }
</style>
