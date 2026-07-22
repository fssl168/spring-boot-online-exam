<template>
  <a-layout>
    <a-layout-header class="header" style="color: #fff">
      <!--   v-if="examDetail.exam" 是为了防止 异步请求时页面渲染的时候还没有拿到这个值而报错， 下面多处这个判断都是这个道理 -->
      <span style="font-size:25px;margin-left: 0px;" v-if="examDetail.exam" class="exam-title-wrapper">
        <a-avatar slot="avatar" size="large" shape="circle" :src="examDetail.exam.examAvatar | imgSrcFilter"/>
        <span class="exam-title-text">{{ examDetail.exam.examName }}</span>
        <span class="exam-desc-text mobile-hide" style="font-size:15px;">{{ examDetail.exam.examDescription }} </span>
      </span>
      <span style="float: right;" class="exam-header-right">
        <span class="record-score" v-if="recordDetail.examRecord">
          得分：<span style="color: red">{{ recordDetail.examRecord.examJoinScore }}</span>&nbsp;分
          <span class="record-date mobile-hide" style="font-size:15px;">{{ recordDetail.examRecord.examJoinDate }}</span>
        </span>
        <a-avatar class="avatar mobile-hide" size="small" :src="avatar()"/>
        <span class="nickname-text mobile-hide" style="margin-left: 12px">{{ nickname() }}</span>
        <a-button class="mobile-show sider-trigger" type="primary" ghost icon="menu" @click="siderDrawerVisible = true">题目</a-button>
      </span>
    </a-layout-header>
    <a-layout>
      <!-- 桌面端固定 sider -->
      <a-layout-sider v-if="!isMobile()" width="190" :style="{background: '#444',overflow: 'auto', height: '100vh', position: 'fixed', left: 0 }">
        <a-menu
          mode="inline"
          :defaultSelectedKeys="['1']"
          :defaultOpenKeys="['question_radio', 'question_check', 'question_judge']"
          :style="{ height: '100%', borderRight: 0 }"
        >
          <a-sub-menu key="question_radio">
            <span slot="title" v-if="examDetail.exam"><a-icon type="check-circle" theme="twoTone"/>单选题(每题{{ examDetail.exam.examScoreRadio }}分)</span>
            <a-menu-item v-for="(item, index) in examDetail.radioIds" :key="item" @click="getQuestionDetail(item)">
              <a-icon type="check" v-if="resultsMap.get(item)==='True'"/>
              <a-icon type="close" v-if="resultsMap.get(item)==='False'"/>
              题目{{ index + 1 }}
            </a-menu-item>
          </a-sub-menu>
          <a-sub-menu key="question_check">
            <span slot="title" v-if="examDetail.exam"><a-icon type="check-square" theme="twoTone"/>多选题(每题{{ examDetail.exam.examScoreCheck }}分)</span>
            <a-menu-item v-for="(item, index) in examDetail.checkIds" :key="item" @click="getQuestionDetail(item)">
              <a-icon type="check" v-if="resultsMap.get(item)==='True'"/>
              <a-icon type="close" v-if="resultsMap.get(item)==='False'"/>
              题目{{ index + 1 }}
            </a-menu-item>
          </a-sub-menu>
          <a-sub-menu key="question_judge">
            <span slot="title" v-if="examDetail.exam"><a-icon type="like" theme="twoTone"/>判断题(每题{{ examDetail.exam.examScoreJudge }}分)</span>
            <a-menu-item v-for="(item, index) in examDetail.judgeIds" :key="item" @click="getQuestionDetail(item)">
              <a-icon type="check" v-if="resultsMap.get(item)==='True'"/>
              <a-icon type="close" v-if="resultsMap.get(item)==='False'"/>
              题目{{ index + 1 }}
            </a-menu-item>
          </a-sub-menu>
        </a-menu>
      </a-layout-sider>

      <!-- 移动端抽屉式 sider -->
      <a-drawer
        v-if="isMobile()"
        placement="left"
        :wrapClassName="'exam-sider-drawer'"
        :closable="true"
        :visible="siderDrawerVisible"
        @close="siderDrawerVisible = false"
        width="80%"
      >
        <a-menu
          mode="inline"
          :defaultSelectedKeys="['1']"
          :defaultOpenKeys="['question_radio', 'question_check', 'question_judge']"
          :style="{ height: '100%', borderRight: 0 }"
          @click="siderDrawerVisible = false"
        >
          <a-sub-menu key="question_radio">
            <span slot="title" v-if="examDetail.exam"><a-icon type="check-circle" theme="twoTone"/>单选题</span>
            <a-menu-item v-for="(item, index) in examDetail.radioIds" :key="item" @click="getQuestionDetail(item)">
              <a-icon type="check" v-if="resultsMap.get(item)==='True'"/>
              <a-icon type="close" v-if="resultsMap.get(item)==='False'"/>
              题目{{ index + 1 }}
            </a-menu-item>
          </a-sub-menu>
          <a-sub-menu key="question_check">
            <span slot="title" v-if="examDetail.exam"><a-icon type="check-square" theme="twoTone"/>多选题</span>
            <a-menu-item v-for="(item, index) in examDetail.checkIds" :key="item" @click="getQuestionDetail(item)">
              <a-icon type="check" v-if="resultsMap.get(item)==='True'"/>
              <a-icon type="close" v-if="resultsMap.get(item)==='False'"/>
              题目{{ index + 1 }}
            </a-menu-item>
          </a-sub-menu>
          <a-sub-menu key="question_judge">
            <span slot="title" v-if="examDetail.exam"><a-icon type="like" theme="twoTone"/>判断题</span>
            <a-menu-item v-for="(item, index) in examDetail.judgeIds" :key="item" @click="getQuestionDetail(item)">
              <a-icon type="check" v-if="resultsMap.get(item)==='True'"/>
              <a-icon type="close" v-if="resultsMap.get(item)==='False'"/>
              题目{{ index + 1 }}
            </a-menu-item>
          </a-sub-menu>
        </a-menu>
      </a-drawer>

      <a-layout :style="contentLayoutStyle">
        <a-layout-content :style="contentStyle">
          <div :style="contentInnerStyle">
            <span v-if="currentQuestion === ''" style="font-size: 30px;font-family: Consolas">欢迎查看本次考试情况，点击左侧题目编号可以查看答题详情</span>
            <span v-if="currentQuestion !== ''">
              <strong>{{ currentQuestion.type }} </strong> <p v-html="currentQuestion.name" class="exam-question-content"></p>
              <strong style="color: green;" v-if="questionRight">本题您答对啦！</strong>
              <strong style="color: red;" v-if="!questionRight">本题您答错啦！</strong>
            </span>
            <br><br>
            <!-- 单选题和判断题 --> <!-- key不重复只需要在一个for循环中保证即可 -->
            <a-radio-group v-model="radioValue" v-if="currentQuestion.type === '单选题' || currentQuestion.type === '判断题'">
              <a-radio v-for="option in currentQuestion.options" :key="option.questionOptionId" :style="optionStyle" :value="option.questionOptionId" class="exam-option">
                {{ option.questionOptionContent }}
              </a-radio>
            </a-radio-group>

            <!-- 题目出错的时候才显示这块 -->
            <div v-if="!questionRight && currentQuestion!=='' && (currentQuestion.type === '单选题' || currentQuestion.type === '判断题')">
              <span style="color: red;"><br/>正确答案是：<br/></span>
              <a-radio-group v-model="radioRightValue">
                <a-radio v-for="option in currentQuestion.options" :key="option.questionOptionId" :style="optionStyle" :value="option.questionOptionId" class="exam-option">
                  {{ option.questionOptionContent }}
                </a-radio>
              </a-radio-group>
            </div>

            <!-- 多选题 -->
            <a-checkbox-group v-model="checkValues" v-if="currentQuestion.type === '多选题'">
              <a-checkbox v-for="option in currentQuestion.options" :key="option.questionOptionId" :style="optionStyle" :value="option.questionOptionId" class="exam-option">
                {{ option.questionOptionContent }}
              </a-checkbox>
            </a-checkbox-group>

            <!-- 题目出错的时候才显示这块 -->
            <div v-if="!questionRight && currentQuestion!=='' && currentQuestion.type === '多选题'">
              <span style="color: red;"><br/>正确答案是：<br/></span>
              <a-checkbox-group v-model="checkRightValues">
                <a-checkbox v-for="option in currentQuestion.options" :key="option.questionOptionId" :style="optionStyle" :value="option.questionOptionId" class="exam-option">
                  {{ option.questionOptionContent }}
                </a-checkbox>
              </a-checkbox-group>
            </div>

            <span style="color: red;"><br/>答案解析：<br/></span>
            <p v-html="currentQuestion.description" class="exam-question-content"></p>
          </div>
        </a-layout-content>
        <a-layout-footer :style="{ textAlign: 'center' }">
          Spting Boot Online Exam ©2020 Created by Liang Shan Guang
        </a-layout-footer>
      </a-layout>
    </a-layout>
  </a-layout>
</template>

<script>
import { getExamDetail, getQuestionDetail, getExamRecordDetail } from '../../api/exam'
import UserMenu from '../../components/tools/UserMenu'
import { mapGetters } from 'vuex'
import { mixinDevice } from '../../utils/mixin'

export default {
  name: 'ExamRecordDetail',
  components: {
    UserMenu
  },
  mixins: [mixinDevice],
  data () {
    return {
      // 考试详情对象
      examDetail: {},
      // 考试记录详情对象
      recordDetail: {},
      // 用户做过的问题都放到这个数组中，键为问题id, 值为currentQuestion(其属性answers属性用于存放答案选项地id或ids),，用于存放用户勾选的答案
      answersMap: {},
      // 题目的正确答案
      answersRightMap: {},
      // 题目的作答结果(正确或错误)
      resultsMap: {},
      // 当前用户的问题
      currentQuestion: '',
      // 单选或判断题的绑定值，用于从answersMap中初始化做题状态
      radioValue: '',
      // 单选题的正确答案，用于从answersRightMap中初始化做题状态
      radioRightValue: '',
      // 多选题的绑定值，用于从answersMap中初始化做题状态
      checkValues: [],
      // 多选题的绑定值，用于从answersRightMap中初始化做题状态
      checkRightValues: [],
      // 批次 3.6：移动端抽屉式 sider 显隐状态
      siderDrawerVisible: false,
      optionStyle: {
        display: 'block',
        height: '30px',
        lineHeight: '30px',
        marginLeft: '0px'
      }
    }
  },
  computed: {
    /**
     * 当前题目用户是否作答正确
     * */
    questionRight () {
      return this.resultsMap !== '' && this.resultsMap.get(this.currentQuestion.id) === 'True'
    },
    // 批次 3.6：移动端布局样式动态计算
    contentLayoutStyle () {
      return this.isMobile()
        ? { marginLeft: '0' }
        : { marginLeft: '200px' }
    },
    contentStyle () {
      return this.isMobile()
        ? { margin: '12px 8px 0', height: '80vh', overflow: 'initial' }
        : { margin: '24px 16px 0', height: '84vh', overflow: 'initial' }
    },
    contentInnerStyle () {
      return this.isMobile()
        ? { padding: '12px', background: '#fff', height: '80vh' }
        : { padding: '24px', background: '#fff', height: '84vh' }
    }
  },
  mounted () {
    this.answersMap = new Map()
    this.answersRightMap = new Map()
    this.resultsMap = new Map()
    const that = this
    // 从后端获取考试的详细信息，渲染到考试详情里,需要加个延时，要不拿不到参数
    getExamDetail(this.$route.params.exam_id)
      .then(res => {
        if (res.code === 0) {
          // 赋值考试对象
          that.examDetail = res.data
          return res.data
        } else {
          // Batch 7.3.4：改用统一错误通知工具
          this.$errorNotify.fromResponse('获取考试详情失败', res)
        }
      })
    // 查看考试记录详情，渲染到前端界面
    getExamRecordDetail(this.$route.params.record_id)
      .then(res => {
        if (res.code === 0) {
          console.log(res.data)
          // 赋值考试对象
          that.recordDetail = res.data
          // 赋值用户的作答答案
          that.objToMap()
          return res.data
        } else {
          // Batch 7.3.4：改用统一错误通知工具
          this.$errorNotify.fromResponse('获取考试记录详情失败', res)
        }
      })
  },
  methods: {
    // 从全局变量中获取用户昵称和头像,
    ...mapGetters(['nickname', 'avatar']),
    /**
     * 把后端传过来的对象Object转换成Map
     **/
    objToMap () {
      for (const item in this.recordDetail.answersMap) {
        this.answersMap.set(item, this.recordDetail.answersMap[item])
      }

      for (const item in this.recordDetail.answersRightMap) {
        this.answersRightMap.set(item, this.recordDetail.answersRightMap[item])
      }

      for (const item in this.recordDetail.resultsMap) {
        this.resultsMap.set(item, this.recordDetail.resultsMap[item])
      }
    },
    getQuestionDetail (questionId) {
      // 问题切换时从后端拿到问题详情，渲染到前端content中
      const that = this
      // 清空问题绑定的值
      this.radioValue = ''
      this.radioRightValue = ''
      this.checkValues = []
      this.checkRightValues = []
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
                that.radioRightValue = that.answersRightMap.get(that.currentQuestion.id)[0]
              } else if (that.currentQuestion.type === '多选题') {
                // 数组是引用类型，因此需要进行拷贝，千万不要直接赋值
                Object.assign(that.checkValues, that.answersMap.get(that.currentQuestion.id))
                Object.assign(that.checkRightValues, that.answersRightMap.get(that.currentQuestion.id))
              }
            }
            return res.data
          } else {
            // Batch 7.3.4：改用统一错误通知工具
            this.$errorNotify.fromResponse('获取问题详情失败', res)
          }
        })
    }
  }
}
</script>

<style scoped>

</style>

<style lang="less">
  /* 批次 3.6：移动端考试记录详情页样式 */
  @media (max-width: 767px) {
    .header {
      padding: 0 8px !important;
      height: 56px !important;
      line-height: 56px !important;

      .exam-title-wrapper {
        font-size: 16px !important;

        .ant-avatar {
          width: 32px !important;
          height: 32px !important;
          line-height: 32px !important;
        }

        .exam-title-text {
          font-size: 16px;
          display: inline-block;
          max-width: 120px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          vertical-align: middle;
        }
      }

      .exam-header-right {
        .record-score {
          font-size: 14px;
          margin-right: 8px;
        }

        .sider-trigger {
          padding: 0 8px;
        }
      }
    }

    // 题目选项触摸区域增大
    .exam-option {
      min-height: 44px !important;
      line-height: 44px !important;
      padding: 8px 0;
      margin-bottom: 8px;
    }

    // 富文本内容自适应
    .exam-question-content {
      img {
        max-width: 100% !important;
        height: auto !important;
      }
      table {
        max-width: 100% !important;
        display: block;
        overflow-x: auto;
      }
      pre {
        white-space: pre-wrap;
        word-wrap: break-word;
      }
    }

    // 抽屉式 sider 样式
    .exam-sider-drawer {
      .ant-drawer-content-wrapper {
        width: 80% !important;
      }
      .ant-menu-item {
        height: 44px;
        line-height: 44px;
      }
    }
  }
</style>
