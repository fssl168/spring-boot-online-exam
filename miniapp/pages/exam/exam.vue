<template>
  <view class="exam-page">
    <!-- 顶部信息栏 -->
    <view class="exam-header">
      <text class="exam-title">{{ examName }}</text>
      <exam-timer
        v-if="examDetail"
        :timeLimit="examDetail.exam ? examDetail.exam.examTimeLimit : 0"
        @timeup="handleTimeUp"
        @tick="onTimerTick"
      />
    </view>

    <!-- 加载中 -->
    <view class="loading" v-if="loading">
      <text>加载中...</text>
    </view>

    <!-- 答题区域 -->
    <view class="exam-content" v-if="currentQuestion && !loading">
      <!-- 题目导航条 -->
      <view class="question-nav-bar">
        <text class="nav-info">
          {{ currentTypeText }} - 第 {{ currentIndex + 1 }} / {{ currentTypeTotal }} 题
        </text>
        <text class="nav-total">总进度: {{ answeredCount }} / {{ totalQuestions }}</text>
      </view>

      <!-- 题目组件 -->
      <question-item
        :question="currentQuestion"
        :type="currentType"
        :selectedAnswers="getCurrentAnswers()"
        @select="onSelectOption"
      />

      <!-- 操作按钮 -->
      <view class="action-bar">
        <button
          class="btn-action"
          :disabled="!canPrev"
          @click="prevQuestion"
        >上一题</button>
        <button class="btn-action btn-nav" @click="showQuestionNav">题目导航</button>
        <button
          v-if="canNext"
          class="btn-action btn-next"
          @click="nextQuestion"
        >下一题</button>
        <button
          v-else
          class="btn-action btn-submit"
          @click="handleSubmit"
        >交卷</button>
      </view>
    </view>

    <!-- 题目导航弹层 -->
    <view class="question-nav-panel" v-if="navVisible">
      <view class="mask" @click="navVisible = false"></view>
      <view class="nav-panel">
        <text class="panel-title">题目导航</text>
        <view class="nav-section" v-if="examDetail && examDetail.radioIds && examDetail.radioIds.length">
          <text class="section-title">单选题</text>
          <view class="nav-grid">
            <view
              v-for="(qid, idx) in examDetail.radioIds"
              :key="qid"
              :class="['nav-item', isAnswered(qid) ? 'answered' : '', isCurrent('radio', idx) ? 'current' : '']"
              @click="switchQuestion('radio', idx)"
            >{{ idx + 1 }}</view>
          </view>
        </view>
        <view class="nav-section" v-if="examDetail && examDetail.checkIds && examDetail.checkIds.length">
          <text class="section-title">多选题</text>
          <view class="nav-grid">
            <view
              v-for="(qid, idx) in examDetail.checkIds"
              :key="qid"
              :class="['nav-item', isAnswered(qid) ? 'answered' : '', isCurrent('check', idx) ? 'current' : '']"
              @click="switchQuestion('check', idx)"
            >{{ idx + 1 }}</view>
          </view>
        </view>
        <view class="nav-section" v-if="examDetail && examDetail.judgeIds && examDetail.judgeIds.length">
          <text class="section-title">判断题</text>
          <view class="nav-grid">
            <view
              v-for="(qid, idx) in examDetail.judgeIds"
              :key="qid"
              :class="['nav-item', isAnswered(qid) ? 'answered' : '', isCurrent('judge', idx) ? 'current' : '']"
              @click="switchQuestion('judge', idx)"
            >{{ idx + 1 }}</view>
          </view>
        </view>
        <button class="btn-close-nav" @click="navVisible = false">关闭</button>
      </view>
    </view>

    <!-- 交卷确认弹窗 -->
    <submit-confirm
      :visible="submitVisible"
      :totalQuestions="totalQuestions"
      :answeredCount="answeredCount"
      :submitting="submitting"
      @cancel="submitVisible = false"
      @confirm="submitExam"
    />
  </view>
</template>

<script>
import { getExamDetail, getQuestionDetail, finishExam } from '../../api/exam'
import ExamTimer from './components/exam-timer.vue'
import QuestionItem from './components/question-item.vue'
import SubmitConfirm from './components/submit-confirm.vue'

export default {
  components: { ExamTimer, QuestionItem, SubmitConfirm },
  data () {
    return {
      examId: '',
      examName: '',
      examDetail: null,
      currentQuestion: null,
      currentType: 'radio', // radio / check / judge
      currentIndex: 0,
      // 用户答案：{ questionId: [optionId, ...] }
      answers: {},
      // 题目缓存：{ questionId: QuestionDetailVo }
      questionCache: {},
      loading: true,
      navVisible: false,
      submitVisible: false,
      submitting: false,
      elapsedSeconds: 0
    }
  },
  computed: {
    /**
     * 当前题型的题号列表
     */
    currentIds () {
      if (!this.examDetail) return []
      if (this.currentType === 'radio') return this.examDetail.radioIds || []
      if (this.currentType === 'check') return this.examDetail.checkIds || []
      return this.examDetail.judgeIds || []
    },
    /**
     * 当前题型总数
     */
    currentTypeTotal () {
      return this.currentIds.length
    },
    /**
     * 当前题型文字
     */
    currentTypeText () {
      const map = { radio: '单选题', check: '多选题', judge: '判断题' }
      return map[this.currentType] || ''
    },
    /**
     * 总题数
     */
    totalQuestions () {
      if (!this.examDetail) return 0
      return (this.examDetail.radioIds ? this.examDetail.radioIds.length : 0) +
             (this.examDetail.checkIds ? this.examDetail.checkIds.length : 0) +
             (this.examDetail.judgeIds ? this.examDetail.judgeIds.length : 0)
    },
    /**
     * 已答题数
     */
    answeredCount () {
      return Object.keys(this.answers).filter(qid => this.answers[qid].length > 0).length
    },
    /**
     * 是否可以上一题
     */
    canPrev () {
      if (this.currentIndex > 0) return true
      // 跨题型：单选第 0 题不能往上，多选第 0 题可以回到单选最后
      if (this.currentType === 'check' && this.examDetail.radioIds && this.examDetail.radioIds.length) return true
      if (this.currentType === 'judge' && this.examDetail.checkIds && this.examDetail.checkIds.length) return true
      return false
    },
    /**
     * 是否可以下一题
     */
    canNext () {
      if (this.currentIndex < this.currentTypeTotal - 1) return true
      // 跨题型
      if (this.currentType === 'radio' && this.examDetail.checkIds && this.examDetail.checkIds.length) return true
      if (this.currentType === 'check' && this.examDetail.judgeIds && this.examDetail.judgeIds.length) return true
      return false
    }
  },
  onLoad (options) {
    this.examId = options.examId
    this.examName = decodeURIComponent(options.examName || '考试')
    this.loadExamDetail()
  },
  methods: {
    /**
     * 加载考试详情
     */
    async loadExamDetail () {
      this.loading = true
      try {
        const res = await getExamDetail(this.examId)
        if (res.code === 0 && res.data) {
          this.examDetail = res.data
          // 初始化答题数组
          this.initAnswers()
          // 加载第一题
          await this.loadCurrentQuestion()
        }
      } catch (e) {
        // 错误已统一处理
      } finally {
        this.loading = false
      }
    },

    /**
     * 初始化答案对象
     */
    initAnswers () {
      const ids = [
        ...(this.examDetail.radioIds || []),
        ...(this.examDetail.checkIds || []),
        ...(this.examDetail.judgeIds || [])
      ]
      ids.forEach(qid => {
        this.$set(this.answers, qid, [])
      })
    },

    /**
     * 加载当前题目详情
     */
    async loadCurrentQuestion () {
      const qid = this.currentIds[this.currentIndex]
      if (!qid) return
      // 使用缓存
      if (this.questionCache[qid]) {
        this.currentQuestion = this.questionCache[qid]
        return
      }
      try {
        const res = await getQuestionDetail(qid)
        if (res.code === 0) {
          this.questionCache[qid] = res.data
          this.currentQuestion = res.data
        }
      } catch (e) {
        // 错误已统一处理
      }
    },

    /**
     * 获取当前题目的用户答案
     */
    getCurrentAnswers () {
      const qid = this.currentIds[this.currentIndex]
      return this.answers[qid] || []
    },

    /**
     * 选择选项
     */
    onSelectOption ({ questionId, answers }) {
      this.$set(this.answers, questionId, answers)
    },

    /**
     * 判断题目是否已答
     */
    isAnswered (qid) {
      return this.answers[qid] && this.answers[qid].length > 0
    },

    /**
     * 判断是否为当前题
     */
    isCurrent (type, idx) {
      return this.currentType === type && this.currentIndex === idx
    },

    /**
     * 切换题目
     */
    async switchQuestion (type, index) {
      this.currentType = type
      this.currentIndex = index
      this.navVisible = false
      await this.loadCurrentQuestion()
    },

    /**
     * 上一题
     */
    async prevQuestion () {
      if (this.currentIndex > 0) {
        this.currentIndex--
      } else if (this.currentType === 'check') {
        this.currentType = 'radio'
        this.currentIndex = (this.examDetail.radioIds || []).length - 1
      } else if (this.currentType === 'judge') {
        this.currentType = 'check'
        this.currentIndex = (this.examDetail.checkIds || []).length - 1
      }
      await this.loadCurrentQuestion()
    },

    /**
     * 下一题
     */
    async nextQuestion () {
      if (this.currentIndex < this.currentTypeTotal - 1) {
        this.currentIndex++
      } else if (this.currentType === 'radio') {
        this.currentType = 'check'
        this.currentIndex = 0
      } else if (this.currentType === 'check') {
        this.currentType = 'judge'
        this.currentIndex = 0
      }
      await this.loadCurrentQuestion()
    },

    /**
     * 显示题目导航
     */
    showQuestionNav () {
      this.navVisible = true
    },

    /**
     * 计时器回调
     */
    onTimerTick (seconds) {
      this.elapsedSeconds = this.examDetail.exam.examTimeLimit * 60 - seconds
    },

    /**
     * 时间到自动交卷
     */
    handleTimeUp () {
      uni.showToast({ title: '考试时间已到，自动交卷', icon: 'none' })
      this.submitExam()
    },

    /**
     * 点击交卷按钮
     */
    handleSubmit () {
      this.submitVisible = true
    },

    /**
     * 组装答案 Map
     * 格式：{ questionId: [optionId, ...] }
     */
    buildAnswersMap () {
      const map = {}
      Object.keys(this.answers).forEach(qid => {
        if (this.answers[qid] && this.answers[qid].length > 0) {
          map[qid] = this.answers[qid]
        }
      })
      return map
    },

    /**
     * 提交考试
     */
    async submitExam () {
      if (this.submitting) return
      this.submitting = true
      try {
        const answersMap = this.buildAnswersMap()
        const timeCost = this.elapsedSeconds
        const res = await finishExam(this.examId, answersMap, timeCost)
        if (res.code === 0) {
          this.submitVisible = false
          const score = res.data ? res.data.examJoinScore : 0
          uni.showModal({
            title: '交卷成功',
            content: '您的得分：' + score + ' 分',
            showCancel: false,
            success: () => {
              uni.navigateBack()
            }
          })
        }
      } catch (e) {
        // 错误已统一处理
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style lang="scss">
.exam-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.exam-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #e8e8e8;

  .exam-title {
    font-size: 30rpx;
    font-weight: bold;
    color: #333;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-right: 16rpx;
  }
}

.loading {
  text-align: center;
  padding: 200rpx 0;
  color: #999;
}

.exam-content {
  background: #fff;
  min-height: calc(100vh - 100rpx);
}

.question-nav-bar {
  display: flex;
  justify-content: space-between;
  padding: 20rpx 24rpx;
  background: #fafafa;
  border-bottom: 1rpx solid #f0f0f0;

  .nav-info {
    font-size: 26rpx;
    color: #1890ff;
    font-weight: bold;
  }

  .nav-total {
    font-size: 24rpx;
    color: #999;
  }
}

.action-bar {
  display: flex;
  padding: 24rpx;
  border-top: 1rpx solid #f0f0f0;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.08);

  .btn-action {
    flex: 1;
    height: 80rpx;
    font-size: 28rpx;
    border-radius: 8rpx;
    margin: 0 8rpx;
    background: #f5f5f5;
    color: #333;

    &[disabled] {
      opacity: 0.5;
    }

    &.btn-nav {
      background: #e6f7ff;
      color: #1890ff;
    }

    &.btn-next {
      background: #1890ff;
      color: #fff;
    }

    &.btn-submit {
      background: #52c41a;
      color: #fff;
    }
  }
}

/* 题目导航弹层 */
.question-nav-panel {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 998;

  .mask {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
  }

  .nav-panel {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 640rpx;
    max-height: 80vh;
    overflow-y: auto;
    background: #fff;
    border-radius: 16rpx;
    padding: 32rpx;

    .panel-title {
      display: block;
      text-align: center;
      font-size: 36rpx;
      font-weight: bold;
      margin-bottom: 24rpx;
    }

    .nav-section {
      margin-bottom: 32rpx;

      .section-title {
        display: block;
        font-size: 28rpx;
        color: #666;
        margin-bottom: 16rpx;
        font-weight: bold;
      }

      .nav-grid {
        display: flex;
        flex-wrap: wrap;

        .nav-item {
          width: 72rpx;
          height: 72rpx;
          display: flex;
          align-items: center;
          justify-content: center;
          border: 2rpx solid #e8e8e8;
          border-radius: 8rpx;
          margin: 8rpx;
          font-size: 28rpx;
          color: #666;

          &.answered {
            background: #52c41a;
            color: #fff;
            border-color: #52c41a;
          }

          &.current {
            border-color: #1890ff;
            border-width: 4rpx;
            color: #1890ff;
            font-weight: bold;
          }
        }
      }
    }

    .btn-close-nav {
      width: 100%;
      height: 80rpx;
      background: #1890ff;
      color: #fff;
      font-size: 30rpx;
      border-radius: 8rpx;
    }
  }
}
</style>
