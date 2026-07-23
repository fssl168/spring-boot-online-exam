<template>
  <view class="exam-timer" :class="{ 'time-warning': isWarning }">
    <text class="timer-icon">⏱</text>
    <text class="timer-text">{{ displayTime }}</text>
  </view>
</template>

<script>
/**
 * 考试计时器组件
 * 倒计时显示，时间到自动触发交卷
 */
export default {
  props: {
    // 时间限制（分钟）
    timeLimit: {
      type: Number,
      default: 0
    }
  },
  data () {
    return {
      totalSeconds: 0,
      remainingSeconds: 0,
      timer: null,
      isWarning: false
    }
  },
  computed: {
    /**
     * 格式化显示时间 mm:ss
     */
    displayTime () {
      const m = Math.floor(this.remainingSeconds / 60)
      const s = this.remainingSeconds % 60
      return (m < 10 ? '0' + m : m) + ':' + (s < 10 ? '0' + s : s)
    }
  },
  watch: {
    timeLimit: {
      handler (val) {
        if (val > 0) {
          this.start(val)
        }
      },
      immediate: true
    }
  },
  methods: {
    /**
     * 启动倒计时
     * @param {number} minutes 分钟数
     */
    start (minutes) {
      this.totalSeconds = minutes * 60
      this.remainingSeconds = this.totalSeconds
      this.stop()
      this.timer = setInterval(() => {
        this.remainingSeconds--
        // 剩余 5 分钟时变红警告
        if (this.remainingSeconds <= 300) {
          this.isWarning = true
        }
        // 时间到
        if (this.remainingSeconds <= 0) {
          this.stop()
          this.$emit('timeup')
        }
        // 每秒上报剩余时间
        this.$emit('tick', this.remainingSeconds)
      }, 1000)
    },

    /**
     * 停止计时
     */
    stop () {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },

    /**
     * 获取已用时间（秒）
     */
    getElapsedSeconds () {
      return this.totalSeconds - this.remainingSeconds
    }
  },
  beforeDestroy () {
    this.stop()
  }
}
</script>

<style lang="scss" scoped>
.exam-timer {
  display: flex;
  align-items: center;
  padding: 8rpx 20rpx;
  background: #f0f0f0;
  border-radius: 24rpx;

  .timer-icon {
    font-size: 28rpx;
    margin-right: 8rpx;
  }

  .timer-text {
    font-size: 28rpx;
    color: #333;
    font-family: monospace;
  }

  &.time-warning {
    background: #fff1f0;

    .timer-text {
      color: #ff4d4f;
      font-weight: bold;
    }
  }
}
</style>
