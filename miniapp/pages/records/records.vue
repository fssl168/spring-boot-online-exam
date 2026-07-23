<template>
  <view class="records-page">
    <view class="header">
      <text class="header-title">考试记录</text>
    </view>

    <!-- 记录列表 -->
    <view class="record-list" v-if="records.length > 0">
      <view
        class="record-card card"
        v-for="(item, index) in records"
        :key="index"
        @click="viewDetail(item)"
      >
        <view class="record-header">
          <text class="exam-name">{{ item.exam ? item.exam.examName : '未知考试' }}</text>
          <text :class="['record-status', 'status-' + (item.examRecord ? item.examRecord.status : 1)]">
            {{ formatRecordStatus(item.examRecord ? item.examRecord.status : 1) }}
          </text>
        </view>
        <view class="record-body">
          <view class="score-section">
            <text class="score-label">得分</text>
            <text class="score-value">
              {{ item.examRecord ? item.examRecord.examJoinScore : 0 }}
              <text class="score-total">/ {{ item.exam ? item.exam.examScore : 0 }}</text>
            </text>
          </view>
          <view class="meta-section">
            <text class="meta-item">
              耗时: {{ formatTimeCost(item.examRecord ? item.examRecord.examTimeCost : 0) }}
            </text>
            <text class="meta-item">
              {{ item.examRecord ? formatDate(item.examRecord.examJoinDate) : '' }}
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty" v-if="!loading && records.length === 0">
      <text class="empty-text">暂无考试记录</text>
    </view>
  </view>
</template>

<script>
import { getExamRecordList } from '../../api/exam'
import { formatDate, formatTimeCost, formatRecordStatus } from '../../utils/util'

export default {
  data () {
    return {
      records: [],
      loading: false
    }
  },
  onShow () {
    this.loadRecords()
  },
  onPullDownRefresh () {
    this.loadRecords().finally(() => {
      uni.stopPullDownRefresh()
    })
  },
  methods: {
    formatDate,
    formatTimeCost,
    formatRecordStatus,

    /**
     * 加载考试记录
     */
    async loadRecords () {
      this.loading = true
      try {
        const res = await getExamRecordList()
        this.records = res.data || []
      } catch (e) {
        // 错误已统一处理
      } finally {
        this.loading = false
      }
    },

    /**
     * 查看记录详情
     */
    viewDetail (record) {
      const recordId = record.examRecord ? record.examRecord.examRecordId : ''
      if (!recordId) return
      uni.navigateTo({
        url: '/pages/record-detail/record-detail?recordId=' + recordId
      })
    }
  }
}
</script>

<style lang="scss">
.records-page {
  min-height: 100vh;
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

.record-list {
  padding: 16rpx;
}

.record-card {
  padding: 24rpx;
  margin-bottom: 16rpx;

  .record-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16rpx;

    .exam-name {
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .record-status {
      font-size: 24rpx;
      padding: 4rpx 16rpx;
      border-radius: 20rpx;

      &.status-0 {
        color: #faad14;
        background: #fff7e6;
      }
      &.status-1 {
        color: #1890ff;
        background: #e6f7ff;
      }
      &.status-2 {
        color: #52c41a;
        background: #f6ffed;
      }
    }
  }

  .record-body {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .score-section {
      .score-label {
        font-size: 24rpx;
        color: #999;
        margin-right: 8rpx;
      }

      .score-value {
        font-size: 40rpx;
        font-weight: bold;
        color: #ff4d4f;

        .score-total {
          font-size: 24rpx;
          color: #999;
          font-weight: normal;
        }
      }
    }

    .meta-section {
      text-align: right;

      .meta-item {
        display: block;
        font-size: 24rpx;
        color: #666;
        margin-bottom: 4rpx;
      }
    }
  }
}

.empty {
  text-align: center;
  padding: 200rpx 0;

  .empty-text {
    color: #999;
    font-size: 28rpx;
  }
}
</style>
