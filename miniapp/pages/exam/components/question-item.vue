<template>
  <view class="question-item">
    <!-- 题干 -->
    <view class="question-name">
      <text class="question-type-tag">{{ typeText }}</text>
      <rich-text class="question-content" :nodes="question.name"></rich-text>
    </view>

    <!-- 题目解析（仅答题回顾时显示） -->
    <view class="question-desc" v-if="question.description && showDescription">
      <text class="desc-label">解析：</text>
      <rich-text class="desc-content" :nodes="question.description"></rich-text>
    </view>

    <!-- 选项列表 -->
    <view class="options-list">
      <view
        v-for="(option, idx) in question.options"
        :key="option.questionOptionId"
        :class="['option-item', isSelected(option.questionOptionId) ? 'selected' : '', isReadOnly ? 'readonly' : '']"
        @click="onSelectOption(option.questionOptionId)"
      >
        <view :class="['option-marker', type === 'check' ? 'checkbox' : 'radio']">
          <text v-if="isSelected(option.questionOptionId)" class="marker-text">{{ type === 'check' ? '✓' : '●' }}</text>
        </view>
        <rich-text class="option-content" :nodes="option.questionOptionContent"></rich-text>
      </view>
    </view>
  </view>
</template>

<script>
/**
 * 题目渲染组件
 * 支持单选(radio)、多选(check)、判断(judge)三种题型
 */
export default {
  props: {
    // 题目对象
    question: {
      type: Object,
      required: true
    },
    // 题型：radio 单选 / check 多选 / judge 判断
    type: {
      type: String,
      default: 'radio'
    },
    // 当前选中的答案数组
    selectedAnswers: {
      type: Array,
      default: () => []
    },
    // 是否只读（答题回顾模式）
    isReadOnly: {
      type: Boolean,
      default: false
    },
    // 是否显示解析
    showDescription: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    /**
     * 题型文字
     */
    typeText () {
      const map = { radio: '单选题', check: '多选题', judge: '判断题' }
      return map[this.type] || '题目'
    }
  },
  methods: {
    /**
     * 判断选项是否被选中
     */
    isSelected (optionId) {
      return this.selectedAnswers.indexOf(optionId) !== -1
    },

    /**
     * 选择选项
     */
    onSelectOption (optionId) {
      if (this.isReadOnly) return
      let newAnswers = [...this.selectedAnswers]
      if (this.type === 'check') {
        // 多选：切换选中状态
        const idx = newAnswers.indexOf(optionId)
        if (idx === -1) {
          newAnswers.push(optionId)
        } else {
          newAnswers.splice(idx, 1)
        }
      } else {
        // 单选/判断：直接替换
        newAnswers = [optionId]
      }
      this.$emit('select', { questionId: this.question.id, answers: newAnswers })
    }
  }
}
</script>

<style lang="scss" scoped>
.question-item {
  padding: 24rpx;
}

.question-name {
  margin-bottom: 24rpx;

  .question-type-tag {
    display: inline-block;
    font-size: 22rpx;
    color: #1890ff;
    background: #e6f7ff;
    padding: 4rpx 12rpx;
    border-radius: 4rpx;
    margin-right: 12rpx;
  }

  .question-content {
    font-size: 32rpx;
    color: #333;
    line-height: 1.6;
  }
}

.question-desc {
  margin: 24rpx 0;
  padding: 20rpx;
  background: #fafafa;
  border-radius: 8rpx;

  .desc-label {
    font-size: 26rpx;
    color: #999;
    font-weight: bold;
  }

  .desc-content {
    font-size: 26rpx;
    color: #666;
  }
}

.options-list {
  .option-item {
    display: flex;
    align-items: flex-start;
    padding: 24rpx;
    margin-bottom: 16rpx;
    border: 2rpx solid #e8e8e8;
    border-radius: 12rpx;

    &.selected {
      border-color: #1890ff;
      background: #e6f7ff;
    }

    &.readonly {
      opacity: 0.85;
    }

    .option-marker {
      width: 40rpx;
      height: 40rpx;
      border: 2rpx solid #d9d9d9;
      margin-right: 20rpx;
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;

      &.radio {
        border-radius: 50%;
      }

      &.checkbox {
        border-radius: 4rpx;
      }

      .marker-text {
        color: #1890ff;
        font-size: 28rpx;
      }
    }

    .option-content {
      flex: 1;
      font-size: 30rpx;
      color: #333;
      line-height: 1.5;
    }
  }
}
</style>
