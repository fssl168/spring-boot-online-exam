<template>
  <a-card :bordered="false">
    <div id="toolbar">
      <a-button type="primary" icon="plus" @click="$refs.createExamModal.create()">新建</a-button>&nbsp;
      <a-button type="primary" icon="reload" @click="loadAll()">刷新</a-button>
    </div>
    <BootstrapTable
      ref="table"
      :columns="columns"
      :data="tableData"
      :options="options"
    />
    <!-- ref是为了方便用this.$refs.modal直接引用，下同 -->
    <step-by-step-exam-modal ref="createExamModal" @ok="handleOk" />
    <!-- 这里的详情需要传进去  -->
    <exam-edit-modal ref="editExamModal" @ok="handleOk" />
    <!--  更新考试封面图片  -->
    <update-avatar-modal ref="updateAvatarModal" @ok="handleOk" />
  </a-card>
</template>

<script>
import '../../plugins/bootstrap-table'
import { getExamPage } from '../../api/exam'
import StepByStepExamModal from './modules/StepByStepExamModal'
import ExamEditModal from './modules/ExamEditModal'
import UpdateAvatarModal from '@views/list/modules/UpdateAvatarModal'

export default {
  name: 'ExamTableList',
  components: {
    UpdateAvatarModal,
    ExamEditModal,
    StepByStepExamModal
  },
  data () {
    const that = this // 方便在bootstrap-table中引用methods
    return {
      // 表头
      columns: [
        {
          title: '序号',
          field: 'serial',
          formatter: function (value, row, index) {
            return index + 1 // 这样的话每翻一页都会重新从1开始，
          }
        },
        {
          title: '封面',
          field: 'avatar',
          width: 50,
          formatter: (value, row) => {
            return '<div class="exam-avatar">' + value + '</div>'
          },
          events: {
            'click .exam-avatar': function (e, value, row, index) {
              that.handleAvatarEdit(row)
            }
          }
        },
        {
          title: '名称',
          field: 'name',
          width: 250
        },
        {
          title: '总分数',
          field: 'score'
        },
        {
          title: '创建人',
          field: 'creator'
        },
        {
          title: '时长',
          field: 'elapse'
        },
        {
          title: '更新时间',
          field: 'updateTime'
        },
        {
          title: '操作',
          field: 'action',
          width: '240px',
          formatter: (value, row) => {
            return '<button type="button" class="btn btn-success view-exam">详情</button>' +
              '&nbsp;&nbsp;' +
              '<button type="button" class="btn btn-info stat-exam">统计</button>' +
              '&nbsp;&nbsp;' +
              '<button type="button" class="btn btn-success edit-exam">编辑</button>'
          },
          events: {
            'click .view-exam': function (e, value, row, index) {
              that.handleSub(row)
            },
            'click .stat-exam': function (e, value, row, index) {
              that.handleStat(row)
            },
            'click .edit-exam': function (e, value, row, index) {
              that.handleEdit(row)
            }
          }
        }
      ],
      tableData: [], // bootstrap-table的数据（当前页）
      // Batch 7.2.3：服务端分页状态
      currentPage: 1, // BootstrapTable 使用 1-based 页码
      pageSize: 10,
      totalRows: 0, // 服务端返回的总数，用于渲染分页器
      // custom bootstrap-table
      options: {
        search: false, // Batch 7.2.3：服务端分页时禁用客户端搜索，避免误用
        showColumns: true,
        showExport: false, // 服务端分页下导出需要后端配合，先禁用
        pagination: true,
        sidePagination: 'server', // Batch 7.2.3：切换为服务端分页
        pageNumber: 1,
        pageSize: 10,
        pageList: [10, 20, 50, 100],
        totalRows: 0, // 初始值，会被 data 中的 totalRows 覆盖
        toolbar: '#toolbar',
        onPageChange: that.onPageChange, // 服务端分页：翻页或切换每页条数时触发
        // 下面两行是支持高级搜索，即按照字段搜索
        advancedSearch: true,
        idTable: 'advancedTable'
        // 下面是常用的事件，更多的点击事件可以参考：http://www.itxst.com/bootstrap-table-events/tutorial.html
        // onClickRow: that.clickRow,
        // onClickCell: that.clickCell, // 单元格单击事件
        // onDblClickCell: that.dblClickCell // 单元格双击事件
      }
    }
  },
  mounted () {
    this.loadAll() // 加载所有问题的数据
  },
  methods: {
    /**
     * Batch 7.2.3：BootstrapTable 翻页/切换每页条数回调
     * number 为 1-based 页码，size 为每页条数
     */
    onPageChange (number, size) {
      this.currentPage = number
      this.pageSize = size
      this.loadAll()
    },
    handleEdit (record) {
      // Todo:修改考试信息和下面的题目，弹出一个可修改的输入框，实际上复用创建题目的模态框即可，还没做完
      console.log('开始编辑啦')
      console.log(record)
      this.$refs.editExamModal.edit(record)
    },
    handleAvatarEdit (record) {
      // Todo:修改考试信息和下面的题目，弹出一个可修改的输入框，实际上复用创建题目的模态框即可，还没做完
      console.log('开始更新封面啦')
      console.log(record)
      this.$refs.updateAvatarModal.edit(record)
    },
    handleSub (record) {
      // 查看考试，不在模态框里查啦，太麻烦
      // console.log(record)
      // this.$refs.modalView.edit(record)

      // 直接跳到参加考试的页面，查看所有题目的详细情况
      const routeUrl = this.$router.resolve({
        path: `/exam/${record.id}`
      })
      // 和点击考试卡片效果一样，跳转到考试页面，里面有所有题目的情况，相当于就是详情了
      window.open(routeUrl.href, '_blank')
    },
    handleStat (record) {
      // 查看考试的成绩统计信息
      const routeUrl = this.$router.resolve({
        path: `/exam/stat/${record.id}`
      })
      window.open(routeUrl.href, '_blank')
    },
    handleOk () {
      this.loadAll()
    },
    loadAll () {
      const that = this
      // Batch 7.2.3：调用服务端分页接口，传递 0-based page 参数
      const params = {
        page: Math.max(this.currentPage - 1, 0),
        size: this.pageSize,
        sort: 'updateTime,desc'
      }
      getExamPage(params)
        .then(res => {
          if (res.code === 0) {
            // 后端返回 { rows, total, page, size }
            const data = res.data
            that.tableData = data.rows || []
            that.totalRows = data.total || 0
            // 同步 totalRows 到 options，让 BootstrapTable 正确渲染分页器
            that.options.totalRows = that.totalRows
            that.$refs.table._initTable()
          } else {
            // Batch 7.3.4：改用统一错误通知工具
            that.$errorNotify.fromResponse('获取考试的列表失败', res)
          }
        })
        .catch(() => {
          // Batch 7.3.4：改用统一错误通知工具
          that.$errorNotify.error({ message: '获取考试的列表失败', description: '网络或服务异常' })
        })
    }
  }
}
</script>
