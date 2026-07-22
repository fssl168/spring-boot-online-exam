<template>
  <div class="exam-record-list-wrapper">
    <a-card class="record-card" style="margin-top: 24px" :bordered="false" title="参加过的考试">
      <div slot="extra">
        <a-input-search class="record-search" style="margin-left: 16px; width: 272px;"/>
      </div>
      <a-list size="large">
        <a-list-item :key="index" v-for="(item, index) in data">
          <a-list-item-meta :description="item.exam.examDescription">
            <a-avatar slot="avatar" size="large" shape="square" :src="item.exam.examAvatar | imgSrcFilter"/>
            <a slot="title">{{ item.exam.examName }}</a>
          </a-list-item-meta>
          <div slot="actions" class="record-actions">
            <a @click="viewExamRecordDetail(item.examRecord)">查看考试详情</a>
          </div>
          <div class="list-content">
            <div class="list-content-item">
              <span>Owner</span>
              <p>{{ item.user.userUsername }}</p>
            </div>
            <div class="list-content-item">
              <span>开始时间</span>
              <p>{{ item.examRecord.examJoinDate }}</p>
            </div>
            <div class="list-content-item">
              <span>分数</span>
              <p>{{ item.examRecord.examJoinScore }}</p>
            </div>
          </div>
        </a-list-item>
      </a-list>

    </a-card>
  </div>
</template>

<script>
import HeadInfo from '../../components/tools/HeadInfo'
import { getExamRecordList } from '../../api/exam'
import { mixinDevice } from '../../utils/mixin'
import { isMobileDevice } from '../../utils/mobile'

export default {
  // 考试记录列表，记录考生参加过地所有考试和考试成绩
  name: 'ExamRecordList',
  components: {
    HeadInfo
  },
  mixins: [mixinDevice],
  data () {
    return {
      data: {}
    }
  },
  methods: {
    /**
     * 根据考试记录的id拿到本次考试的详情并查看
     * @param record 考试详情的记录
     */
    viewExamRecordDetail (record) {
      // 直接跳到参加考试的页面，查看所有题目的详细情况
      const routeUrl = this.$router.resolve({
        path: `/exam/record/${record.examId}/${record.examRecordId}`
      })
      // 批次 3.5：移动端 window.open 会被拦截，改用 router.push
      if (isMobileDevice() || this.isMobile()) {
        this.$router.push({ path: `/exam/record/${record.examId}/${record.examRecordId}` })
      } else {
        window.open(routeUrl.href, '_blank')
      }
    }
  },
  mounted () {
    // 从后端数据获取考试列表，适配前端卡片
    getExamRecordList().then(res => {
      if (res.code === 0) {
        this.data = res.data
      } else {
        // Batch 7.3.4：改用统一错误通知工具
        this.$errorNotify.fromResponse('获取考试记录失败', res)
      }
    }).catch(err => {
      // 失败就弹出警告消息
      // Batch 7.3.4：改用统一错误通知工具
      this.$errorNotify.fromError('获取考试记录失败', err)
    })
  }
}
</script>

<style lang="less" scoped>
  .ant-avatar-lg {
    width: 48px;
    height: 48px;
    line-height: 48px;
  }

  .list-content-item {
    color: rgba(0, 0, 0, .45);
    display: inline-block;
    vertical-align: middle;
    font-size: 14px;
    margin-left: 40px;

    span {
      line-height: 20px;
    }

    p {
      margin-top: 4px;
      margin-bottom: 0;
      line-height: 22px;
    }
  }

  /* 批次 3.5：移动端记录列表样式 */
  @media (max-width: 767px) {
    .record-card {
      margin-top: 12px !important;

      .record-search {
        width: 100% !important;
        margin-left: 0 !important;
        margin-top: 8px;
      }

      .ant-list-item {
        display: block !important;
        padding: 12px 0 !important;

        .ant-list-item-meta {
          margin-bottom: 8px;
        }

        .record-actions {
          margin-top: 8px;
          margin-bottom: 8px;

          a {
            display: inline-block;
            padding: 6px 12px;
            min-height: 36px;
            line-height: 24px;
          }
        }
      }

      .list-content {
        display: flex;
        flex-wrap: wrap;
        justify-content: space-between;

        .list-content-item {
          margin-left: 0;
          margin-right: 8px;
          margin-bottom: 8px;
          font-size: 12px;
          flex: 0 0 30%;

          span {
            font-size: 11px;
          }

          p {
            font-size: 13px;
            font-weight: 500;
          }
        }
      }
    }
  }
</style>
