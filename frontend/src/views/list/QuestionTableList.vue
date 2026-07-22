<template>
  <a-card :bordered="false">
    <div id="toolbar">
      <a-button type="primary" icon="plus" @click="$refs.createQuestionModal.create()">新建</a-button>&nbsp;
      <a-button type="primary" icon="reload" @click="loadAll()">全量刷新</a-button>
    </div>
    <BootstrapTable
      ref="table"
      :columns="columns"
      :data="tableData"
      :options="options"
    />
    <!-- ref是为了方便用this.$refs.modal直接引用，下同 -->
    <step-by-step-question-modal ref="createQuestionModal" @ok="handleOk" />
    <summernote-update-modal ref="questionUpdateModal" @ok="handleOk" />
    <question-view-modal ref="modalView" @ok="handleOk" />
    <question-edit-modal ref="modalEdit" @ok="handleOk" />
  </a-card>
</template>

<script>
import '../../plugins/bootstrap-table'
import QuestionViewModal from './modules/QuestionViewModal'
import QuestionEditModal from './modules/QuestionEditModal'
import StepByStepQuestionModal from './modules/StepByStepQuestionModal'
import { getQuestionPage, questionUpdate, getQuestionSelection } from '../../api/exam'
import SummernoteUpdateModal from '@views/list/modules/SummernoteUpdateModal'
import $ from 'jquery'

export default {
  name: 'QuestionTableList',
  components: {
    SummernoteUpdateModal,
    StepByStepQuestionModal,
    QuestionViewModal,
    QuestionEditModal
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
          title: '题干',
          field: 'name',
          width: 200,
          formatter: (value, row) => {
            return '<div class="question-name" style="height: 100%;width: 100%">' + value + '</div>'
          },
          events: {
            'click .question-name': function (e, value, row, index) {
              that.$refs.questionUpdateModal.edit('summernote-question-name-update', row, 'name', '更新题干', questionUpdate)
            }
          }
        },
        {
          title: '解析',
          field: 'description',
          width: 200,
          formatter: (value, row) => {
            return '<div class="question-desc">' + value + '</div>'
          },
          events: {
            'click .question-desc': function (e, value, row, index) {
              that.$refs.questionUpdateModal.edit('summernote-question-desc-update', row, 'description', '更新题目解析', questionUpdate)
            }
          }
        },
        {
          title: '分数',
          field: 'score',
          formatter: (value, row) => {
            return '<div class="question-score">' + value + '</div>'
          },
          events: {
            'click .question-score': function (e, value, row, index) {
              const $element = $(e.target) // 把元素转换成html对象
              $element.html('<input type="text" value="' + value + '">')
            }
          }
        },
        {
          title: '创建人',
          field: 'creator'
        },
        {
          title: '难度',
          field: 'level',
          formatter: (value, row) => {
            return '<div class="question-level">' + value + '</div>'
          },
          events: {
            'click .question-level': function (e, value, row, index) {
              const $element = $(e.target) // 把元素转换成html对象
              if ($element.children().length > 0) return // 防止重复渲染
              getQuestionSelection().then(res => {
                console.log(res)
                if (res.code === 0) {
                  console.log(res.data)
                  const levels = res.data.levels
                  let inner = '<select>'
                  for (let i = 0; i < levels.length; i++) {
                    if (levels[i].description === value) {
                      // 设置默认的选中值为当前的值
                      inner += '<option value ="' + levels[i].id + '" name="' + levels[i].name + '" selected="selected">' + levels[i].description + '</option>'
                    } else {
                      inner += '<option value ="' + levels[i].id + '" name="' + levels[i].name + '">' + levels[i].description + '</option>'
                    }
                  }
                  inner += '</select>'
                  $element.html(inner)
                } else {
                  // Batch 7.3.4：改用统一错误通知工具
                  that.$errorNotify.fromResponse('获取问题下拉选项失败', res)
                }
              })
            }
          }
        },
        {
          title: '题型',
          field: 'type',
          formatter: (value, row) => {
            return '<div class="question-type">' + value + '</div>'
          },
          events: {
            'click .question-type': function (e, value, row, index) {
              const $element = $(e.target) // 把元素转换成html对象
              if ($element.children().length > 0) return // 防止重复渲染
              getQuestionSelection().then(res => {
                console.log(res)
                if (res.code === 0) {
                  console.log(res.data)
                  const types = res.data.types
                  let inner = '<select>'
                  for (let i = 0; i < types.length; i++) {
                    if (types[i].description === value) {
                      // 设置默认的选中值为当前的值
                      inner += '<option value ="' + types[i].id + '" name="' + types[i].name + '" selected="selected">' + types[i].description + '</option>'
                    } else {
                      inner += '<option value ="' + types[i].id + '" name="' + types[i].name + '">' + types[i].description + '</option>'
                    }
                  }
                  inner += '</select>'
                  $element.html(inner)
                } else {
                  // Batch 7.3.4：改用统一错误通知工具
                  that.$errorNotify.fromResponse('获取问题下拉选项失败', res)
                }
              })
            }
          }
        },
        {
          title: '学科',
          field: 'category',
          formatter: (value, row) => {
            return '<div class="question-category">' + value + '</div>'
          },
          events: {
            'click .question-category': function (e, value, row, index) {
              const $element = $(e.target) // 把元素转换成html对象
              if ($element.children().length > 0) return // 防止重复渲染
              getQuestionSelection().then(res => {
                console.log(res)
                if (res.code === 0) {
                  console.log(res.data)
                  const categories = res.data.categories
                  let inner = '<select>'
                  for (let i = 0; i < categories.length; i++) {
                    if (categories[i].name === value) { // 学科还是用名字吧
                      // 设置默认的选中值为当前的值
                      inner += '<option value ="' + categories[i].id + '" name="' + categories[i].description + '" selected="selected">' + categories[i].name + '</option>'
                    } else {
                      inner += '<option value ="' + categories[i].id + '" name="' + categories[i].description + '">' + categories[i].name + '</option>'
                    }
                  }
                  inner += '</select>'
                  $element.html(inner)
                } else {
                  // Batch 7.3.4：改用统一错误通知工具
                  that.$errorNotify.fromResponse('获取问题下拉选项失败', res)
                }
              })
            }
          }
        },
        {
          title: '更新时间',
          field: 'updateTime'
        },
        {
          title: '操作',
          field: 'action',
          align: 'center',
          formatter: (value, row) => {
            return '<button type="button" class="btn btn-success view-question">详情</button>' +
              '&nbsp;&nbsp;' +
              '<button type="button" class="btn btn-success edit-question">编辑</button>'
          },
          events: {
            'click .view-question': function (e, value, row, index) {
              that.handleSub(row)
            },
            'click .edit-question': function (e, value, row, index) {
              that.handleEdit(row)
            }
          }
        }
      ],
      tableData: [], // bootstrap-table的数据（当前页）
      // Batch 7.2.2：服务端分页状态
      currentPage: 1, // BootstrapTable 使用 1-based 页码
      pageSize: 10,
      totalRows: 0, // 服务端返回的总数，用于渲染分页器
      // custom bootstrap-table
      options: {
        search: false, // Batch 7.2.2：服务端分页时禁用客户端搜索，避免误用
        showColumns: true,
        showExport: false, // 服务端分页下导出需要后端配合，先禁用
        pagination: true,
        sidePagination: 'server', // Batch 7.2.2：切换为服务端分页
        pageNumber: 1,
        pageSize: 10,
        pageList: [10, 20, 50, 100],
        totalRows: 0, // 初始值，会被 data 中的 totalRows 覆盖
        toolbar: '#toolbar',
        onPageChange: that.onPageChange, // 服务端分页：翻页或切换每页条数时触发
        // 下面两行是支持高级搜索，即按照字段搜索
        advancedSearch: true,
        idTable: 'advancedTable',
        // 下面是常用的事件，更多的点击事件可以参考：http://www.itxst.com/bootstrap-table-events/tutorial.html
        // onClickRow: that.clickRow,
        // onClickCell: that.clickCell // 单元格单击事件
        onDblClickCell: that.dblClickCell // 单元格双击事件
      }
    }
  },
  mounted () {
    this.loadAll() // 加载所有问题的数据
  },
  methods: {
    /**
     * Batch 7.2.2：BootstrapTable 翻页/切换每页条数回调
     * number 为 1-based 页码，size 为每页条数
     */
    onPageChange (number, size) {
      this.currentPage = number
      this.pageSize = size
      this.loadAll()
    },
    handleEdit (record) {
      this.$refs.modalEdit.edit(record)
    },
    handleSub (record) {
      // 查看题目
      console.log(record)
      this.$refs.modalView.edit(record)
    },
    handleOk () {
      this.loadAll() // 加载所有问题的数据
    },
    dblClickCell (field, value, row, $element) {
      if (field === 'score') { // 更新分数
        const childrenInput = $element.children('.question-score').children('input') // 获取输入框的值
        if (childrenInput.length === 0) return
        row.score = childrenInput[0].value
        const that = this
        questionUpdate(row).then(res => {
          // 成功就跳转到结果页面
          console.log(res)
          if (res.code === 0) {
            $element.children('.question-score').text(row.score)
            that.$notification.success({
              message: '更新成功',
              description: '更新成功'
            })
          }
        })
      }

      if (field === 'level') { // 更新难度
        const childrenSelect = $element.children('.question-level').children('select') // 获取输入框的值
        if (childrenSelect.length === 0) return
        const optionSelected = $(childrenSelect[0]).find('option:selected')
        row.levelId = optionSelected.val()
        console.log(row.levelId)
        row.level = optionSelected.text()
        console.log(row.level)
        const that = this
        questionUpdate(row).then(res => {
          // 成功就跳转到结果页面
          console.log(res)
          if (res.code === 0) {
            $element.children('.question-level').text(row.level)
            that.$notification.success({
              message: '更新成功',
              description: '更新成功'
            })
          }
        })
      }

      if (field === 'type') { // 更新题型
        const childrenSelect = $element.children('.question-type').children('select') // 获取输入框的值
        if (childrenSelect.length === 0) return
        const optionSelected = $(childrenSelect[0]).find('option:selected')
        row.typeId = optionSelected.val()
        row.type = optionSelected.text()
        const that = this
        questionUpdate(row).then(res => {
          // 成功就跳转到结果页面
          console.log(res)
          if (res.code === 0) {
            $element.children('.question-type').text(row.type)
            that.$notification.success({
              message: '更新成功',
              description: '更新成功'
            })
          }
        })
      }

      if (field === 'category') { // 更新学科
        const childrenSelect = $element.children('.question-category').children('select') // 获取输入框的值
        console.log(childrenSelect)
        if (childrenSelect.length === 0) return
        const optionSelected = $(childrenSelect[0]).find('option:selected')
        row.categoryId = optionSelected.val()
        row.category = optionSelected.text()
        const that = this
        questionUpdate(row).then(res => {
          // 成功就跳转到结果页面
          console.log(res)
          if (res.code === 0) {
            $element.children('.question-category').text(row.category)
            that.$notification.success({
              message: '更新成功',
              description: '更新成功'
            })
          }
        })
      }
    },
    loadAll () {
      const that = this
      // Batch 7.2.2：调用服务端分页接口，传递 0-based page 参数
      const params = {
        page: Math.max(this.currentPage - 1, 0),
        size: this.pageSize,
        sort: 'updateTime,desc'
      }
      getQuestionPage(params)
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
            that.$errorNotify.fromResponse('获取问题的列表失败', res)
          }
        })
        .catch(() => {
          // Batch 7.3.4：改用统一错误通知工具
          that.$errorNotify.error({ message: '获取问题的列表失败', description: '网络或服务异常' })
        })
    }
  }
}
</script>

<style lang="less" scoped>
  /* 批次 4.1：移动端题目列表样式 */
  @media (max-width: 767px) {
    /deep/ .ant-card-body {
      padding: 12px !important;
    }

    #toolbar {
      margin-bottom: 8px;

      .ant-btn {
        padding: 0 12px;
        margin-bottom: 4px;
      }
    }

    // BootstrapTable 容器横向滚动
    /deep/ .fixed-table-body,
    /deep/ .table-responsive {
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;

      table {
        min-width: 800px;
      }
    }

    // 表格单元格字体缩小
    /deep/ .table td,
    /deep/ .table th {
      font-size: 12px;
      padding: 8px 6px !important;
      white-space: nowrap;
    }

    // 操作按钮触摸区域
    /deep/ .table .btn {
      padding: 4px 8px;
      font-size: 12px;
    }

    // 分页器居中
    /deep/ .fixed-table-pagination {
      .pagination {
        margin: 8px 0;
        flex-wrap: wrap;
        justify-content: center;
      }

      .page-item .page-link {
        padding: 6px 10px;
        min-height: 32px;
      }
    }
  }
</style>
