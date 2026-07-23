<template>
  <view class="record-detail-page">
    <!-- 加载中 -->
    <view class="loading" v-if="loading">
      <text>加载中...</text>
    </view>

    <view v-if="recordDetail && !loading">
      <!-- 得分概览 -->
      <view class="score-overview card">
        <text class="overview-title">考试得分</text>
        <view class="score-display">
          <text class="score-num">{{ recordDetail.examRecord.examJoinScore }}</text>
          <text class="score-total">/ {{ totalScore }} 分</text>
        </view>
        <view class="meta-row">
          <text class="meta-item">耗时: {{ formatTimeCost(recordDetail.examRecord.examTimeCost) }}</text>
          <text class="meta-item">{{ formatDate(recordDetail.examRecord.examJoinDate) }}</text>
        </view>
      </view>

      <!-- 题目导航 -->
      <view class="question-nav-bar">
        <text class="nav-info" v-if="currentQuestion">
          {{ currentTypeText }} - 第 {{ currentIndex + 1 }} / {{ currentTypeTotal }} 题
        </text>
        <view class="nav-buttons">
          <button class="btn-nav" @click="prevQuestion" :disabled="!canPrev">上一题</button>
          <button class="btn-nav" @click="nextQuestion" :disabled="!canNext">下一题</button>
        </view>
      </view>

      <!-- 题目内容 -->
      <view class="question-section" v-if="currentQuestion">
        <question-item
          :question="currentQuestion"
          :type="currentType"
          :selectedAnswers="getUserAnswer()"
          :isReadOnly="true"
          :showDescription="true"
        />

        <!-- 答题结果 -->
        <view class="answer-result">
          <view class="result-row" :class="isCorrect() ? 'correct' : 'wrong'">
            <text class="result-label">答题结果：</text>
            <text class="result-value">{{ isCorrect() ? '✓ 回答正确' : '✗ 回答错误' }}</text>
          </view>
          <view class="result-row">
            <text class="result-label">你的答案：</text>
            <text class="result-value">{{ formatAnswer(getUserAnswer()) }}</text>
          </view>
          <view class="result-row" v-if="!isCorrect()">
            <text class="result-label">正确答案：</text>
            <text class="result-value correct-answer">{{ formatAnswer(getRightAnswer()) }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getExamRecordDetail, getQuestionDetail } from '../../api/exam'
import { formatDate, formatTimeCost } from '../../utils/util'
import QuestionItem from '../exam/components/question-item.vue'

export default {
  components: { QuestionItem },
  data () {
    return {
      recordId: '',
      recordDetail: null,
      currentQuestion: null,
      currentType: 'radio',
      currentIndex: 0,
      questionCache: {},
      loading: true
    }
  },
  computed: {
    totalScore () {
      // 从 examRecord 取不到考试总分，从记录列表进入时已知，这里用 100 兜底
      return 100
    },
    /**
     * 所有题目 ID 按题型分组
     */
    allQuestionIds () {
      if (!this.recordDetail || !this.recordDetail.answersMap) return { radio: [], check: [], judge: [] }
      // answersMap 的 key 是 questionId，但不知道题型，从 questionCache 推断
      const ids = Object.keys(this.recordDetail.answersMap)
      // 简化处理：所有题目放一个列表，按顺序展示
      return { all: ids }
    },
    currentTypeTotal () {
      return this.allQuestionIds.all.length
    },
    currentTypeText () {
      if (!this.currentQuestion) return ''
      // 从题目详情推断题型
      if (this.currentQuestion.type) return this.currentQuestion.type
      return '题目'
    },
    canPrev () {
      return this.currentIndex > 0
    },
    canNext () {
      return this.currentIndex < this.currentTypeTotal - 1
    }
  },
  onLoad (options) {
    this.recordId = options.recordId
    this.loadRecordDetail()
  },
  methods: {
    formatDate,
    formatTimeCost,

    /**
     * 加载记录详情
     */
    async loadRecordDetail () {
      this.loading = true
      try {
        const res = await getExamRecordDetail(this.recordId)
        if (res.code === 0 && res.data) {
          this.recordDetail = res.data
          // 加载第一题
          if (this.currentTypeTotal > 0) {
            await this.loadCurrentQuestion()
          }
        }
      } catch (e) {
        // 错误已统一处理
      } finally {
        this.loading = false
      }
    },

    /**
     * 获取当前题目 ID
     */
    getCurrentQuestionId () {
      return this.allQuestionIds.all[this.currentIndex]
    },

    /**
     * 加载当前题目
     */
    async loadCurrentQuestion () {
      const qid = this.getCurrentQuestionId()
      if (!qid) return
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
     * 获取用户答案
     */
    getUserAnswer () {
      if (!this.recordDetail || !this.recordDetail.answersMap) return []
      const qid = this.getCurrentQuestionId()
      return this.recordDetail.answersMap[qid] || []
    },

    /**
     * 获取正确答案
     */
    getRightAnswer () {
      if (!this.recordDetail || !this.recordDetail.answersRightMap) return []
      const qid = this.getCurrentQuestionId()
      return this.recordDetail.answersRightMap[qid] || []
    },

    /**
     * 判断是否答对
     */
    isCorrect () {
      if (!this.recordDetail || !this.recordDetail.resultsMap) return false
      const qid = this.getCurrentQuestionId()
      return this.recordDetail.resultsMap[qid] === 'True'
    },

    /**
     * 格式化答案显示
     */
    formatAnswer (optionIds) {
      if (!optionIds || optionIds.length === 0) return '未作答'
      // 从题目选项中查找内容
      if (!this.currentQuestion || !this.currentQuestion.options) return optionIds.join(', ')
      const contents = optionIds.map(oid => {
        const opt = this.currentQuestion.options.find(o => o.questionOptionId === oid)
        // 去除 HTML 标签
        const text = opt ? opt.questionOptionContent : oid
        return text.replace(/<[^>]+>/g, '').trim()
      })
      return contents.join('； ')
    },

    /**
     * 上一题
     */
    async prevQuestion () {
      if (this.canPrev) {
        this.currentIndex--
        await this.loadCurrentQuestion()
      }
    },

    /**
     * 下一题
     */
    async nextQuestion () {
      if (this.canNext) {
        this.currentIndex++
        await this.loadCurrentQuestion()
      }
    }
  }
}
</script>

<style lang="scss">
.record-detail-page {
  min-height: 100vh;
  padding-bottom: 40rpx;
}

.loading {
  text-align: center;
  padding: 200rpx 0;
  color: #999;
}

.score-overview {
  text-align: center;
  padding: 40rpx;

  .overview-title {
    display: block;
    font-size: 28rpx;
    color: #999;
    margin-bottom: 16rpx;
  }

  .score-display {
    margin-bottom: 24rpx;

    .score-num {
      font-size: 80rpx;
      font-weight: bold;
      color: #ff4d4f;
    }

    .score-total {
      font-size: 32rpx;
      color: #999;
    }
  }

  .meta-row {
    display: flex;
    justify-content: center;

    .meta-item {
      font-size: 24rpx;
      color: #666;
      margin: 0 16rpx;
    }
  }
}

.question-nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;

  .nav-info {
    font-size: 26rpx;
    color: #1890ff;
    font-weight: bold;
  }

  .nav-buttons {
    display: flex;

    .btn-nav {
      font-size: 26rpx;
      padding: 8rpx 24rpx;
      margin-left: 12rpx;
      background: #f5f5f5;
      color: #333;
      border-radius: 8rpx;

      &[disabled] {
        opacity: 0.5;
      }
    }
  }
}

.question-section {
  background: #fff;
  margin-top: 16rpx;
}

.answer-result {
  padding: 24rpx;
  border-top: 2rpx solid #f0f0f0;

  .result-row {
    display: flex;
    padding: 12rpx 0;

    &.correct .result-value {
      color: #52c41a;
    }

    &.wrong .result-value {
      color: #ff4d4f;
    }

    .result-label {
      font-size: 28rpx;
      color: #999;
      width: 180rpx;
      flex-shrink: 0;
    }

    .result-value {
      font-size: 28rpx;
      color: #333;
      flex: 1;
    }

    .correct-answer {
      color: #52c41a;
      font-weight: bold;
    }
  }
}
</style>
