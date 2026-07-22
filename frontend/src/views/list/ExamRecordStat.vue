<template>
  <a-card :bordered="false">
    <!-- 顶部信息：考试名称与返回按钮 -->
    <div slot="title">
      <a-button icon="arrow-left" style="margin-right: 12px" @click="goBack">返回</a-button>
      <span v-if="stat">{{ stat.examName }} - 成绩统计</span>
    </div>
    <div slot="extra">
      <a-button type="primary" icon="reload" @click="loadData">刷新</a-button>
    </div>

    <!-- 统计概览卡片 -->
    <a-row :gutter="16" style="margin-top: 16px" v-if="stat">
      <a-col :span="4">
        <a-statistic title="参考人数" :value="stat.totalCount" :valueStyle="{ color: '#1890ff' }">
          <a-icon type="team" slot="prefix" />
        </a-statistic>
      </a-col>
      <a-col :span="4">
        <a-statistic title="满分" :value="stat.examScore">
          <a-icon type="trophy" slot="prefix" />
        </a-statistic>
      </a-col>
      <a-col :span="4">
        <a-statistic title="平均分" :value="stat.avgScore" :precision="2" :valueStyle="{ color: '#52c41a' }">
          <a-icon type="calculator" slot="prefix" />
        </a-statistic>
      </a-col>
      <a-col :span="4">
        <a-statistic title="最高分" :value="stat.maxScore" :valueStyle="{ color: '#cf1322' }">
          <a-icon type="arrow-up" slot="prefix" />
        </a-statistic>
      </a-col>
      <a-col :span="4">
        <a-statistic title="最低分" :value="stat.minScore" :valueStyle="{ color: '#999' }">
          <a-icon type="arrow-down" slot="prefix" />
        </a-statistic>
      </a-col>
      <a-col :span="4">
        <a-statistic title="及格率(%)" :value="stat.passRate" :precision="2" :valueStyle="{ color: stat.passRate >= 60 ? '#52c41a' : '#cf1322' }">
          <a-icon type="like" slot="prefix" />
        </a-statistic>
      </a-col>
    </a-row>

    <!-- 分数段分布柱状图 -->
    <a-card title="分数段分布" style="margin-top: 24px" v-if="stat && stat.scoreDistribution">
      <v-chart :forceFit="true" :height="300" :data="chartData" :scale="chartScale">
        <v-tooltip :showTitle="true" />
        <v-axis dataKey="range" title />
        <v-axis dataKey="count" title />
        <v-bar position="range*count" />
      </v-chart>
      <a-empty v-if="chartData.length === 0" description="暂无数据" />
    </a-card>

    <!-- 学生考试记录表格 -->
    <a-card title="学生考试记录" style="margin-top: 24px">
      <a-table
        :columns="columns"
        :dataSource="records"
        rowKey="examRecordId"
        :pagination="{ pageSize: 10, showSizeChanger: true }"
        :loading="loading"
      >
        <span slot="index" slot-scope="text, record, index">{{ index + 1 }}</span>
        <span slot="username" slot-scope="text, record">
          {{ record.user ? record.user.userUsername : '-' }}
        </span>
        <span slot="nickname" slot-scope="text, record">
          {{ record.user ? record.user.userNickname : '-' }}
        </span>
        <span slot="score" slot-scope="text, record">
          <a-tag :color="getScoreColor(record.examRecord.examJoinScore, stat.examScore)">
            {{ record.examRecord.examJoinScore }} / {{ stat ? stat.examScore : 0 }}
          </a-tag>
        </span>
        <span slot="timeCost" slot-scope="text, record">
          {{ formatTimeCost(record.examRecord.examTimeCost) }}
        </span>
        <span slot="action" slot-scope="text, record">
          <a @click="viewDetail(record)">查看详情</a>
        </span>
      </a-table>
    </a-card>
  </a-card>
</template>

<script>
import { getExamAllRecords, getExamScoreStat } from '../../api/exam'
import { Chart } from 'viser-vue'

export default {
  name: 'ExamRecordStat',
  components: {
    'v-chart': Chart
  },
  data () {
    return {
      examId: this.$route.params.examId,
      stat: null,
      records: [],
      loading: false,
      chartScale: [
        { dataKey: 'range', alias: '分数段' },
        { dataKey: 'count', alias: '人数', min: 0 }
      ],
      columns: [
        { title: '序号', scopedSlots: { customRender: 'index' }, width: 60 },
        { title: '用户名', scopedSlots: { customRender: 'username' } },
        { title: '昵称', scopedSlots: { customRender: 'nickname' } },
        { title: '分数', scopedSlots: { customRender: 'score' }, width: 120 },
        { title: '耗时', scopedSlots: { customRender: 'timeCost' }, width: 120 },
        { title: '参加时间', dataIndex: 'examRecord.examJoinDate', width: 180 },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 100 }
      ]
    }
  },
  computed: {
    chartData () {
      if (!this.stat || !this.stat.scoreDistribution) return []
      const result = []
      const dist = this.stat.scoreDistribution
      // 保持顺序：0-59, 60-69, 70-79, 80-89, 90-100
      const order = ['0-59', '60-69', '70-79', '80-89', '90-100']
      for (const key of order) {
        if (dist[key] !== undefined) {
          result.push({ range: key, count: dist[key] })
        }
      }
      return result
    }
  },
  mounted () {
    this.loadData()
  },
  methods: {
    loadData () {
      this.loading = true
      const that = this
      // 并行请求统计信息和记录列表
      Promise.all([
        getExamScoreStat(this.examId),
        getExamAllRecords(this.examId)
      ]).then(([statRes, recRes]) => {
        if (statRes.code === 0) {
          that.stat = statRes.data
        } else {
          // Batch 7.3.4：改用统一错误通知工具
          that.$errorNotify.fromResponse('获取统计信息失败', statRes)
        }
        if (recRes.code === 0) {
          that.records = recRes.data || []
        } else {
          // Batch 7.3.4：改用统一错误通知工具
          that.$errorNotify.fromResponse('获取考试记录失败', recRes)
        }
      }).catch(err => {
        // Batch 7.3.4：改用统一错误通知工具
        that.$errorNotify.fromError('加载数据失败', err)
      }).finally(() => {
        that.loading = false
      })
    },
    /**
     * 根据得分比例返回标签颜色
     */
    getScoreColor (score, total) {
      if (!total) return 'default'
      const ratio = score / total
      if (ratio >= 0.9) return 'green'
      if (ratio >= 0.6) return 'blue'
      if (ratio >= 0.4) return 'orange'
      return 'red'
    },
    /**
     * 把秒数格式化为 mm:ss
     */
    formatTimeCost (seconds) {
      if (!seconds && seconds !== 0) return '-'
      const m = Math.floor(seconds / 60)
      const s = seconds % 60
      return (m < 10 ? '0' + m : m) + ':' + (s < 10 ? '0' + s : s)
    },
    /**
     * 跳转到考试记录详情页
     */
    viewDetail (record) {
      const routeUrl = this.$router.resolve({
        path: `/exam/record/${record.examRecord.examId}/${record.examRecord.examRecordId}`
      })
      window.open(routeUrl.href, '_blank')
    },
    goBack () {
      this.$router.back()
    }
  }
}
</script>

<style scoped>
</style>
