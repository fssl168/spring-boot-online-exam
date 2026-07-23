<template>
  <view class="index-page">
    <!-- 头部 -->
    <view class="header">
      <text class="header-title">考试列表</text>
    </view>

    <!-- 考试列表 -->
    <view class="exam-list" v-if="examList.length > 0">
      <view
        class="exam-card card"
        v-for="(item, index) in examList"
        :key="index"
      >
        <!-- 考试封面 -->
        <image
          class="exam-avatar"
          :src="item.avatar || '/static/placeholder.png'"
          mode="aspectFill"
        />

        <!-- 考试信息 -->
        <view class="exam-info">
          <text class="exam-name">{{ item.title }}</text>
          <text class="exam-desc">{{ item.content }}</text>
          <view class="exam-meta">
            <text class="meta-item">满分: {{ item.score }}分</text>
            <text class="meta-item">时长: {{ item.elapse }}分钟</text>
          </view>
          <view class="exam-footer">
            <text :class="['exam-status', 'status-' + getExamStatusText(item).status]">
              {{ getExamStatusText(item).text }}
            </text>
            <button
              class="btn-join"
              :disabled="getExamStatusText(item).status !== 1"
              @click="joinExam(item)"
            >参加考试</button>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty" v-if="!loading && examList.length === 0">
      <text class="empty-text">暂无可参加的考试</text>
    </view>
  </view>
</template>

<script>
import { getExamCardList } from '../../api/exam'
import { getExamStatus } from '../../utils/util'
import { checkAuth } from '../../utils/auth'

export default {
  data () {
    return {
      examList: [],
      loading: false
    }
  },
  onShow () {
    // 每次显示检查登录状态
    if (!checkAuth(true)) return
    this.loadExamList()
  },
  onPullDownRefresh () {
    this.loadExamList().finally(() => {
      uni.stopPullDownRefresh()
    })
  },
  methods: {
    /**
     * 加载考试列表
     */
    async loadExamList () {
      this.loading = true
      try {
        const res = await getExamCardList()
        this.examList = res.data || []
      } catch (e) {
        // 错误已统一处理
      } finally {
        this.loading = false
      }
    },

    /**
     * 获取考试状态文字
     */
    getExamStatusText (exam) {
      return getExamStatus(exam)
    },

    /**
     * 参加考试
     */
    joinExam (item) {
      const status = getExamStatus(item)
      if (status.status !== 1) {
        uni.showToast({ title: '考试' + status.text + '，无法参加', icon: 'none' })
        return
      }
      uni.navigateTo({
        url: '/pages/exam/exam?examId=' + item.id + '&examName=' + encodeURIComponent(item.title)
      })
    }
  }
}
</script>

<style lang="scss">
.index-page {
  min-height: 100vh;
  padding-bottom: 40rpx;
}

.header {
  padding: 32rpx;
  background: #1890ff;

  .header-title {
    color: #fff;
    font-size: 36rpx;
    font-weight: bold;
  }
}

.exam-list {
  padding: 16rpx;
}

.exam-card {
  display: flex;
  padding: 24rpx;
  margin-bottom: 16rpx;

  .exam-avatar {
    width: 160rpx;
    height: 160rpx;
    border-radius: 12rpx;
    flex-shrink: 0;
    background: #f0f0f0;
  }

  .exam-info {
    flex: 1;
    margin-left: 24rpx;
    display: flex;
    flex-direction: column;

    .exam-name {
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
      margin-bottom: 8rpx;
    }

    .exam-desc {
      font-size: 24rpx;
      color: #999;
      margin-bottom: 12rpx;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }

    .exam-meta {
      display: flex;
      margin-bottom: 12rpx;

      .meta-item {
        font-size: 24rpx;
        color: #666;
        margin-right: 24rpx;
      }
    }

    .exam-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: auto;

      .exam-status {
        font-size: 24rpx;
        padding: 4rpx 16rpx;
        border-radius: 20rpx;

        &.status-0 {
          color: #faad14;
          background: #fff7e6;
        }
        &.status-1 {
          color: #52c41a;
          background: #f6ffed;
        }
        &.status-2 {
          color: #999;
          background: #f5f5f5;
        }
      }

      .btn-join {
        font-size: 24rpx;
        padding: 8rpx 24rpx;
        background: #1890ff;
        color: #fff;
        border-radius: 24rpx;
        line-height: 1.5;

        &[disabled] {
          background: #ccc;
        }
      }
    }
  }
}

.empty {
  text-align: center;
  padding: 120rpx 0;

  .empty-text {
    color: #999;
    font-size: 28rpx;
  }
}
</style>
