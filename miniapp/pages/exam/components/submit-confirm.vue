<template>
  <view class="submit-confirm" v-if="visible">
    <view class="mask" @click="cancel"></view>
    <view class="confirm-box">
      <text class="confirm-title">确认交卷</text>
      <view class="confirm-content">
        <view class="stat-row">
          <text class="stat-label">已答题数：</text>
          <text class="stat-value answered">{{ answeredCount }}</text>
        </view>
        <view class="stat-row">
          <text class="stat-label">未答题数：</text>
          <text class="stat-value unanswered">{{ unansweredCount }}</text>
        </view>
        <text class="confirm-tip" v-if="unansweredCount > 0">
          您还有 {{ unansweredCount }} 题未作答，确定要交卷吗？
        </text>
        <text class="confirm-tip" v-else>
          所有题目已作答，确定要交卷吗？
        </text>
      </view>
      <view class="confirm-actions">
        <button class="btn-cancel" @click="cancel">取消</button>
        <button class="btn-confirm" @click="confirm" :disabled="submitting">
          {{ submitting ? '提交中...' : '确认交卷' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script>
/**
 * 交卷确认组件
 */
export default {
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    // 所有题目数量
    totalQuestions: {
      type: Number,
      default: 0
    },
    // 已答题数
    answeredCount: {
      type: Number,
      default: 0
    },
    submitting: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    /**
     * 未答题数
     */
    unansweredCount () {
      return this.totalQuestions - this.answeredCount
    }
  },
  methods: {
    /**
     * 取消交卷
     */
    cancel () {
      this.$emit('cancel')
    },

    /**
     * 确认交卷
     */
    confirm () {
      this.$emit('confirm')
    }
  }
}
</script>

<style lang="scss" scoped>
.submit-confirm {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;

  .mask {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
  }

  .confirm-box {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 600rpx;
    background: #fff;
    border-radius: 16rpx;
    padding: 40rpx;

    .confirm-title {
      display: block;
      text-align: center;
      font-size: 36rpx;
      font-weight: bold;
      color: #333;
      margin-bottom: 32rpx;
    }

    .confirm-content {
      margin-bottom: 40rpx;

      .stat-row {
        display: flex;
        justify-content: space-between;
        margin-bottom: 16rpx;

        .stat-label {
          font-size: 30rpx;
          color: #666;
        }

        .stat-value {
          font-size: 32rpx;
          font-weight: bold;

          &.answered {
            color: #52c41a;
          }

          &.unanswered {
            color: #ff4d4f;
          }
        }
      }

      .confirm-tip {
        display: block;
        margin-top: 24rpx;
        font-size: 28rpx;
        color: #999;
        text-align: center;
      }
    }

    .confirm-actions {
      display: flex;

      .btn-cancel,
      .btn-confirm {
        flex: 1;
        height: 80rpx;
        font-size: 30rpx;
        border-radius: 8rpx;
        margin: 0 8rpx;
      }

      .btn-cancel {
        background: #f5f5f5;
        color: #666;
      }

      .btn-confirm {
        background: #1890ff;
        color: #fff;

        &[disabled] {
          background: #a0cfff;
        }
      }
    }
  }
}
</style>
