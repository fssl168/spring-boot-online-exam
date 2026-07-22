<template>
  <a-layout>
    <a-layout-header class="header" style="color: #fff">
      <!--   v-if="examDetail.exam" 是为了防止 异步请求时页面渲染的时候还没有拿到这个值而报错， 下面多处这个判断都是这个道理 -->
      <span style="font-size:25px;margin-left: 0px;" v-if="examDetail.exam">
        <a-avatar slot="avatar" size="large" shape="circle" :src="examDetail.exam.examAvatar | imgSrcFilter"/>
        {{ examDetail.exam.examName }}
        <span style="font-size:15px;">{{ examDetail.exam.examDescription }} </span>
      </span>
      <span style="float: right;">
        <span style="margin-right: 60px; font-size: 20px" v-if="examDetail.exam">考试限时：{{ examDetail.exam.examTimeLimit }}分钟 剩余：{{ formatTime(remainingSeconds) }}</span>
        <a-button type="danger" ghost style="margin-right: 60px;" @click="finishExam()" :disabled="isSubmitting || isTimeUp">交卷</a-button>
        <a-avatar class="avatar" size="small" :src="avatar()"/>
        <span style="margin-left: 12px">{{ nickname() }}</span>
      </span>
    </a-layout-header>
    <a-layout>
      <a-layout-sider width="190" :style="{background: '#444',overflow: 'auto', height: '100vh', position: 'fixed', left: 0 }">
        <a-menu
          mode="inline"
          :defaultSelectedKeys="['1']"
          :defaultOpenKeys="['question_radio', 'question_check', 'question_judge']"
          :style="{ height: '100%', borderRight: 0 }"
        >
          <a-sub-menu key="question_radio">
            <span slot="title" v-if="examDetail.exam"><a-icon type="check-circle" theme="twoTone"/>单选题(每题{{ examDetail.exam.examScoreRadio }}分)</span>
            <a-menu-item v-for="(item, index) in visibleRadioIds" :key="item" @click="getQuestionDetail(item)">
              <a-icon type="eye" theme="twoTone" twoToneColor="#52c41a" v-if="answersMap.get(item)"/>
              题目{{ index + 1 }}
            </a-menu-item>
            <a-menu-item v-if="examDetail.radioIds && examDetail.radioIds.length > visibleRadioCount" key="radio-more" disabled @click.stop.prevent="loadMoreRadio">
              <span style="color:#faad14;font-size:12px">显示更多 ({{ examDetail.radioIds.length - visibleRadioCount }}题)</span>
            </a-menu-item>
          </a-sub-menu>
          <a-sub-menu key="question_check">
            <span slot="title" v-if="examDetail.exam"><a-icon type="check-square" theme="twoTone"/>多选题(每题{{ examDetail.exam.examScoreCheck }}分)</span>
            <a-menu-item v-for="(item, index) in visibleCheckIds" :key="item" @click="getQuestionDetail(item)">
              <a-icon type="eye" theme="twoTone" twoToneColor="#52c41a" v-if="answersMap.get(item)"/>
              题目{{ index + 1 }}
            </a-menu-item>
            <a-menu-item v-if="examDetail.checkIds && examDetail.checkIds.length > visibleCheckCount" key="check-more" disabled @click.stop.prevent="loadMoreCheck">
              <span style="color:#faad14;font-size:12px">显示更多 ({{ examDetail.checkIds.length - visibleCheckCount }}题)</span>
            </a-menu-item>
          </a-sub-menu>
          <a-sub-menu key="question_judge">
            <span slot="title" v-if="examDetail.exam"><a-icon type="like" theme="twoTone"/>判断题(每题{{ examDetail.exam.examScoreJudge }}分)</span>
            <a-menu-item v-for="(item, index) in visibleJudgeIds" :key="item" @click="getQuestionDetail(item)">
              <a-icon type="eye" theme="twoTone" twoToneColor="#52c41a" v-if="answersMap.get(item)"/>
              题目{{ index + 1 }}
            </a-menu-item>
            <a-menu-item v-if="examDetail.judgeIds && examDetail.judgeIds.length > visibleJudgeCount" key="judge-more" disabled @click.stop.prevent="loadMoreJudge">
              <span style="color:#faad14;font-size:12px">显示更多 ({{ examDetail.judgeIds.length - visibleJudgeCount }}题)</span>
            </a-menu-item>
          </a-sub-menu>
        </a-menu>
      </a-layout-sider>
      <a-layout :style="{ marginLeft: '200px' }">
        <a-layout-content :style="{ margin: '24px 16px 0',height: '84vh', overflow: 'initial' }">
          <div :style="{ padding: '24px', background: '#fff',height: '84vh'}">
            <span v-show="currentQuestion === ''" style="font-size: 30px;font-family: Consolas">欢迎参加考试，请点击左侧题目编号开始答题</span>
            <strong>{{ currentQuestion.type }} </strong> <p v-html="currentQuestion.name"></p>
            <!-- 单选题和判断题 --> <!-- key不重复只需要在一个for循环中保证即可 -->
            <a-radio-group @change="onRadioChange" v-model="radioValue" :disabled="isTimeUp" v-if="currentQuestion.type === '单选题' || currentQuestion.type === '判断题'">
              <a-radio v-for="option in currentQuestion.options" :key="option.questionOptionId" :style="optionStyle" :value="option.questionOptionId">
                {{ option.questionOptionContent }}
              </a-radio>
            </a-radio-group>

            <!-- 多选题 -->
            <a-checkbox-group @change="onCheckChange" v-model="checkValues" :disabled="isTimeUp" v-if="currentQuestion.type === '多选题'">
              <a-checkbox v-for="option in currentQuestion.options" :key="option.questionOptionId" :style="optionStyle" :value="option.questionOptionId">
                {{ option.questionOptionContent }}
              </a-checkbox>
            </a-checkbox-group>
          </div>
        </a-layout-content>
        <a-layout-footer :style="{ textAlign: 'center' }">
          Spting Boot Online Exam ©2020 Crated by Liang Shan Guang
        </a-layout-footer>
      </a-layout>
    </a-layout>
  </a-layout>
</template>

<script>
import { getExamDetail, getQuestionDetail, finishExam } from '../../api/exam'
import UserMenu from '../../components/tools/UserMenu'
import { mapGetters } from 'vuex'
import { Modal } from 'ant-design-vue'

export default {
  name: 'ExamDetail',
  components: {
    UserMenu
  },
  data () {
    return {
      // 考试详情对象
      examDetail: {},
      // 用户做过的问题都放到这个数组中，键为问题id, 值为currentQuestion(其属性answers属性用于存放答案选项地id或ids),，用于存放用户勾选的答案
      answersMap: {},
      // 当前用户的问题
      currentQuestion: '',
      // 单选或判断题的绑定值，用于从answersMap中初始化做题状态
      radioValue: '',
      // 多选题的绑定值，用于从answersMap中初始化做题状态
      checkValues: [],
      // 倒计时剩余秒数
      remainingSeconds: 0,
      // 倒计时定时器id
      timerId: null,
      // 考试开始时间戳（毫秒）
      startTime: 0,
      // 防止重复交卷
      isSubmitting: false,
      // 草稿自动保存定时器id
      draftTimerId: null,
      // 草稿存储key（基于examId）
      draftKey: '',
      optionStyle: {
        display: 'block',
        height: '30px',
        lineHeight: '30px',
        marginLeft: '0px'
      },
      // Batch 7.2.1：窗口化渲染阈值，超过则只渲染前 N 条并提供"显示更多"按钮，避免一次性渲染大量 a-menu-item 造成卡顿
      VISIBLE_STEP: 30,
      visibleRadioCount: 30,
      visibleCheckCount: 30,
      visibleJudgeCount: 30
    }
  },
  computed: {
    // 考试时间是否已到：仅在倒计时已启动且剩余秒数 <= 0 时为 true
    isTimeUp () {
      return this.timerId !== null && this.remainingSeconds <= 0
    },
    // Batch 7.2.1：每种题型只渲染前 visibleXxxCount 条，超出部分通过"显示更多"按钮增量加载
    visibleRadioIds () {
      if (!this.examDetail.radioIds) return []
      return this.examDetail.radioIds.slice(0, this.visibleRadioCount)
    },
    visibleCheckIds () {
      if (!this.examDetail.checkIds) return []
      return this.examDetail.checkIds.slice(0, this.visibleCheckCount)
    },
    visibleJudgeIds () {
      if (!this.examDetail.judgeIds) return []
      return this.examDetail.judgeIds.slice(0, this.visibleJudgeCount)
    }
  },
  mounted () {
    this.answersMap = new Map()
    this.startTime = Date.now()
    // 初始化草稿key，基于考试id
    this.draftKey = 'exam_draft_' + this.$route.params.id
    const that = this
    // 从后端获取考试的详细信息，渲染到考试详情里
    getExamDetail(this.$route.params.id)
      .then(res => {
        if (res.code === 0) {
          // 赋值考试对象
          that.examDetail = res.data
          // 初始化倒计时（考试限时单位为分钟，转换为秒）
          if (that.examDetail.exam && that.examDetail.exam.examTimeLimit > 0) {
            that.remainingSeconds = that.examDetail.exam.examTimeLimit * 60
            that.startCountdown()
          }
          // 加载本地草稿
          that.loadDraft()
          return res.data
        } else {
          // Batch 7.3.4：改用统一错误通知工具
          this.$errorNotify.fromResponse('获取考试详情失败', res)
        }
      })
    // 防止用户误关闭或刷新页面丢失答题数据
    window.addEventListener('beforeunload', this.beforeUnloadHandler)
    // 每30秒自动保存草稿
    this.draftTimerId = setInterval(() => {
      that.saveDraft()
    }, 30000)
  },
  methods: {
    // 从全局变量中获取用户昵称和头像,
    ...mapGetters(['nickname', 'avatar']),
    // Batch 7.2.1：增量加载更多题目，避免一次性渲染大量 DOM 节点
    loadMoreRadio () {
      this.visibleRadioCount += this.VISIBLE_STEP
    },
    loadMoreCheck () {
      this.visibleCheckCount += this.VISIBLE_STEP
    },
    loadMoreJudge () {
      this.visibleJudgeCount += this.VISIBLE_STEP
    },
    /**
     * 启动倒计时定时器，到0时自动交卷
     */
    startCountdown () {
      const that = this
      this.timerId = setInterval(() => {
        if (that.remainingSeconds > 0) {
          that.remainingSeconds--
        } else {
          // 时间到，自动交卷
          clearInterval(that.timerId)
          that.timerId = null
          that.$notification.warning({
            message: '考试时间已到，系统自动交卷'
          })
          that.finishExam(true)
        }
      }, 1000)
    },
    /**
     * 格式化剩余秒数为 mm:ss
     */
    formatTime (seconds) {
      if (!seconds || seconds < 0) return '00:00'
      const m = Math.floor(seconds / 60)
      const s = seconds % 60
      return (m < 10 ? '0' + m : m) + ':' + (s < 10 ? '0' + s : s)
    },
    /**
     * beforeunload 事件处理器，提示用户即将离开
     */
    beforeUnloadHandler (e) {
      e.preventDefault()
      e.returnValue = '离开页面将丢失答题数据，确定离开吗？'
      return e.returnValue
    },
    getQuestionDetail (questionId) {
      // 问题切换时从后端拿到问题详情，渲染到前端content中
      const that = this
      // 清空问题绑定的值
      this.radioValue = ''
      this.checkValues = []
      getQuestionDetail(questionId)
        .then(res => {
          if (res.code === 0) {
            // 赋值当前考试对象
            that.currentQuestion = res.data
            // 查看用户是不是已经做过这道题又切换回来的，answersMap中查找，能找到这个题目id对应的值数组不为空说明用户做过这道题
            if (that.answersMap.get(that.currentQuestion.id)) {
              // 说明之前做过这道题了
              if (that.currentQuestion.type === '单选题' || that.currentQuestion.type === '判断题') {
                that.radioValue = that.answersMap.get(that.currentQuestion.id)[0]
              } else if (that.currentQuestion.type === '多选题') {
                // 数组是引用类型，因此需要进行拷贝，千万不要直接赋值
                Object.assign(that.checkValues, that.answersMap.get(that.currentQuestion.id))
              }
            }
            return res.data
          } else {
            // Batch 7.3.4：改用统一错误通知工具
            this.$errorNotify.fromResponse('获取问题详情失败', res)
          }
        })
    },
    /**
     * 单选题勾选是触发的变化事件
     * @param e
     */
    onRadioChange (e) {
      const userOptions = []
      userOptions.push(e.target.value)
      // 更新做题者选择的答案
      this.answersMap.set(this.currentQuestion.id, userOptions)
      // 自动保存草稿
      this.saveDraft()
    },
    /**
     * 多选题触发的变化事件
     * @param checkedValues
     */
    onCheckChange (checkedValues) {
      // 更新做题者选择的答案
      this.answersMap.set(this.currentQuestion.id, checkedValues)
      // 自动保存草稿
      this.saveDraft()
    },
    _strMapToObj (strMap) {
      const obj = Object.create(null)
      for (const [k, v] of strMap) {
        obj[k] = v
      }
      return obj
    },
    /**
     *map转换为json
     */
    _mapToJson (map) {
      return JSON.stringify(this._strMapToObj(map))
    },
    /**
     * 结束考试并交卷
     * @param autoSubmit 是否为自动交卷（时间到），自动交卷无需确认
     */
    finishExam (autoSubmit) {
      // 自动交卷（时间到）无需确认，直接提交
      if (autoSubmit) {
        this.doSubmit()
        return
      }
      // 手动交卷：弹出确认框，提示未答题数
      const unanswered = this.countUnanswered()
      const content = unanswered > 0
        ? '您还有 ' + unanswered + ' 道题目未作答，确定要交卷吗？'
        : '确定要交卷吗？'
      const that = this
      Modal.confirm({
        title: '交卷确认',
        content: content,
        okText: '确定交卷',
        cancelText: '继续答题',
        onOk () {
          that.doSubmit()
        }
      })
    },
    /**
     * 执行实际提交逻辑
     */
    doSubmit () {
      // 防止重复交卷
      if (this.isSubmitting) return
      this.isSubmitting = true
      // 计算考试耗时（秒）
      const timeCost = Math.floor((Date.now() - this.startTime) / 1000)
      // 向后端提交作答信息数组answersMap和耗时
      finishExam(this.$route.params.id, this._mapToJson(this.answersMap), timeCost)
        .then(res => {
          if (res.code === 0) {
            // 考试交卷，后端判分完成，然后跳转到我的考试界面
            this.$notification.success({
              message: '考卷提交成功！'
            })
            // 清除倒计时和事件监听
            if (this.timerId) {
              clearInterval(this.timerId)
              this.timerId = null
            }
            if (this.draftTimerId) {
              clearInterval(this.draftTimerId)
              this.draftTimerId = null
            }
            // 清除草稿
            this.clearDraft()
            window.removeEventListener('beforeunload', this.beforeUnloadHandler)
            this.$router.push('/list/exam-record-list')
            return res.data
          } else {
            // Batch 7.3.4：改用统一错误通知工具
            this.$errorNotify.fromResponse('交卷失败！', res)
            this.isSubmitting = false
          }
        })
        .catch(() => {
          this.isSubmitting = false
        })
    },
    /**
     * 统计未作答的题目数量
     */
    countUnanswered () {
      if (!this.examDetail.exam) return 0
      let total = 0
      if (this.examDetail.radioIds) total += this.examDetail.radioIds.length
      if (this.examDetail.checkIds) total += this.examDetail.checkIds.length
      if (this.examDetail.judgeIds) total += this.examDetail.judgeIds.length
      return total - this.answersMap.size
    },
    /**
     * 保存答题草稿到localStorage
     */
    saveDraft () {
      if (!this.draftKey || this.answersMap.size === 0) return
      try {
        localStorage.setItem(this.draftKey, this._mapToJson(this.answersMap))
      } catch (e) {
        console.log('草稿保存失败', e)
      }
    },
    /**
     * 从localStorage加载答题草稿
     */
    loadDraft () {
      if (!this.draftKey) return
      try {
        const draft = localStorage.getItem(this.draftKey)
        if (draft) {
          const obj = JSON.parse(draft)
          for (const key in obj) {
            this.answersMap.set(key, obj[key])
          }
          if (this.answersMap.size > 0) {
            this.$notification.info({
              message: '已恢复上次未完成的答题记录'
            })
          }
        }
      } catch (e) {
        console.log('草稿加载失败', e)
      }
    },
    /**
     * 清除答题草稿
     */
    clearDraft () {
      if (this.draftKey) {
        localStorage.removeItem(this.draftKey)
      }
    }
  },
  beforeDestroy () {
    // 组件销毁时清除倒计时和事件监听，防止内存泄漏
    if (this.timerId) {
      clearInterval(this.timerId)
      this.timerId = null
    }
    if (this.draftTimerId) {
      clearInterval(this.draftTimerId)
      this.draftTimerId = null
    }
    window.removeEventListener('beforeunload', this.beforeUnloadHandler)
  }
}
</script>

<style scoped>

</style>
