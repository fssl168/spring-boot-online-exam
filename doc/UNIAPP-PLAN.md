# uniapp 小程序端实施计划（函数级）

## 0. 背景与目标

### 0.1 目标
为现有 Spring Boot 在线考试项目新建一个独立的 uniapp 子项目，编译为微信小程序（同时兼容 H5），面向**学生端**使用。学生可通过小程序完成登录、参加考试、查看成绩等核心操作。

### 0.2 技术选型
- **框架**：uniapp（Vue 2 语法，与现有 web 前端技术栈一致，便于复用业务逻辑）
- **UI 库**：uView UI 2.x（uniapp 生态最成熟的 UI 框架）
- **目标平台**：微信小程序（主）+ H5（次）
- **后端**：复用现有 Spring Boot 后端 REST API，不修改后端代码

### 0.3 项目位置
独立目录 `miniapp/`，与 `frontend/`（web 端）、`backend/`（后端）平级。

### 0.4 与 web 端的关系
| 维度 | web 端 (frontend) | 小程序端 (miniapp) |
|---|---|---|
| 框架 | Vue 2.7 + Ant Design Vue | uniapp + uView UI |
| 目标用户 | 教师 + 学生 + 管理员 | 学生（主） |
| 功能范围 | 全功能（管理 + 答题） | 答题 + 成绩查看（核心） |
| 富文本 | Summernote/WangEditor | rich-text 组件渲染 |
| 路由 | vue-router | uniapp pages.json |

## 1. 后端 API 调研结果（学生端所需）

### 1.1 认证机制
- **登录接口**：`POST /api/user/login`
  - 入参：`{ loginType: 1|2, userInfo: string, password: string }`（1=用户名，2=邮箱）
  - 出参：`ResultVO<String>`，data 为 JWT token 字符串
- **Token 传递**：HTTP Header `Access-Token: <token>`
- **Token 有效期**：2 小时
- **角色枚举**：ADMIN=1, TEACHER=2, STUDENT=3

### 1.2 学生端核心接口

| 功能 | 方法 | 路径 | 鉴权 | 入参 | 出参 |
|---|---|---|---|---|---|
| 注册 | POST | /api/user/register | 无 | RegisterDTO | UserVo |
| 登录 | POST | /api/user/login | 无 | LoginQo | String(token) |
| 获取用户信息 | GET | /api/user/user-info | 登录 | - | UserVo |
| 获取用户详情 | GET | /api/user/info | 登录 | - | UserInfoVo |
| 修改密码 | POST | /api/user/change-password | 登录 | ChangePasswordQo | Void |
| 更新个人信息 | POST | /api/user/update | 登录 | UserUpdateVo | UserVo |
| 考试卡片列表 | GET | /api/exam/card/list | 登录 | - | List<ExamCardVo> |
| 考试详情 | GET | /api/exam/detail/{id} | 登录 | examId | ExamDetailVo |
| 题目详情 | GET | /api/exam/question/detail/{id} | 登录 | questionId | QuestionDetailVo |
| 提交考试 | POST | /api/exam/finish/{examId} | 登录 | answersMap + X-Time-Cost header | ExamRecord |
| 考试记录列表 | GET | /api/exam/record/list | 登录 | - | List<ExamRecordVo> |
| 考试记录详情 | GET | /api/exam/record/detail/{recordId} | 登录 | recordId | RecordDetailVo |
| 删除考试记录 | DELETE | /api/exam/record/{recordId} | 登录 | recordId | Void |

### 1.3 核心数据模型

**ExamCardVo**（考试卡片）：`{ id, title, avatar, content, score, elapse(分钟), status(0未开始/1进行中/2已结束), startDate, endDate }`

**ExamDetailVo**（考试详情）：`{ exam: Exam, radioIds: [], checkIds: [], judgeIds: [] }`
- Exam：`{ examId, examName, examAvatar, examDescription, examScore, examScoreRadio/Check/Judge, examTimeLimit, examStartDate, examEndDate, ... }`

**QuestionDetailVo**（题目详情）：`{ id, name(HTML富文本), description(HTML富文本), type, options: [{ questionOptionId, questionOptionContent, questionOptionDescription }], answers: [] }`

**ExamRecordVo**（考试记录）：`{ exam: Exam, examRecord: ExamRecord, user: User }`
- ExamRecord：`{ examRecordId, examId, examJoinerId, examJoinDate, examTimeCost, examJoinScore, examResultLevel, status, ... }`

**RecordDetailVo**（记录详情）：`{ examRecord, answersMap: {questionId: [optionId]}, resultsMap: {questionId: 'True'|'False'}, answersRightMap: {questionId: [optionId]} }`

**UserVo**：`{ id, username, nickname, role, avatar, description, email, phone }`

## 2. 函数级实施清单

### 批次 1：项目初始化 + 基础设施（6 项）

#### 1.1 项目脚手架
- **文件**：`miniapp/manifest.json`、`miniapp/pages.json`、`miniapp/main.js`、`miniapp/App.vue`
- **内容**：
  - `manifest.json`：配置 appid、微信小程序设置、H5 设置
  - `pages.json`：定义 tabBar（首页/我的）+ 页面路由
  - `main.js`：引入 uView UI、全局配置
  - `App.vue`：全局生命周期、token 检查

#### 1.2 API 请求封装
- **文件**：`miniapp/utils/request.js`
- **函数**：
  - `request(options)` — 核心请求函数，封装 uni.request，自动注入 Access-Token header、统一错误处理、token 过期跳登录
  - `get(url, data)` — GET 快捷方法
  - `post(url, data)` — POST 快捷方法
  - `del(url, data)` — DELETE 快捷方法
- **配置**：`BASE_URL` 常量（可按环境切换）、`TOKEN_KEY = 'access_token'`

#### 1.3 API 模块
- **文件**：`miniapp/api/user.js`
  - `register(data)` → POST /api/user/register
  - `login(data)` → POST /api/user/login
  - `getUserInfo()` → GET /api/user/user-info
  - `getUserDetail()` → GET /api/user/info
  - `changePassword(data)` → POST /api/user/change-password
  - `updateUserInfo(data)` → POST /api/user/update
- **文件**：`miniapp/api/exam.js`
  - `getExamCardList()` → GET /api/exam/card/list
  - `getExamDetail(examId)` → GET /api/exam/detail/{examId}
  - `getQuestionDetail(questionId)` → GET /api/exam/question/detail/{questionId}
  - `finishExam(examId, answersMap, timeCost)` → POST /api/exam/finish/{examId}
  - `getExamRecordList()` → GET /api/exam/record/list
  - `getExamRecordDetail(recordId)` → GET /api/exam/record/detail/{recordId}
  - `deleteExamRecord(recordId)` → DELETE /api/exam/record/{recordId}

#### 1.4 认证状态管理
- **文件**：`miniapp/store/index.js` + `miniapp/store/modules/user.js`
- **函数**：
  - `login(token, userInfo)` — 保存 token 和用户信息到 state + uni.setStorageSync
  - `logout()` — 清除 token 和用户信息
  - `checkAuth()` — 检查是否登录，未登录跳转登录页
  - `refreshUserInfo()` — 调用 API 刷新用户信息
- **state**：`{ token, userInfo, isLoggedIn }`
- **getters**：`isLoggedIn`、`userRole`、`userNickname`

#### 1.5 工具函数
- **文件**：`miniapp/utils/auth.js`
  - `getToken()` — 从 storage 读取 token
  - `setToken(token)` — 保存 token 到 storage
  - `removeToken()` — 清除 token
  - `isLoggedIn()` — 判断是否登录
- **文件**：`miniapp/utils/util.js`
  - `formatDate(date, fmt)` — 格式化日期
  - `formatTimeCost(seconds)` — 秒转 mm:ss
  - `stripHtml(html)` — 去除 HTML 标签（用于题目预览）
  - `getExamStatus(exam)` — 计算考试状态（未开始/进行中/已结束）

#### 1.6 全局样式与配置
- **文件**：`miniapp/uni.scss` — 全局 scss 变量（主题色、间距）
- **文件**：`miniapp/static/` — 静态资源（logo、占位图）

### 批次 2：登录注册 + 考试列表（4 项）

#### 2.1 登录页
- **文件**：`miniapp/pages/login/login.vue`
- **函数**：
  - `handleLogin()` — 表单校验 + 调用 login API + 保存 token + 跳转首页
  - `switchLoginType(type)` — 切换用户名/邮箱登录
  - `goRegister()` — 跳转注册页
- **UI**：用户名/邮箱输入、密码输入、登录按钮、注册链接

#### 2.2 注册页
- **文件**：`miniapp/pages/register/register.vue`
- **函数**：
  - `handleRegister()` — 表单校验 + 调用 register API + 注册成功跳登录
  - `validateForm()` — 校验用户名/密码/确认密码/邮箱
- **UI**：用户名、密码、确认密码、邮箱输入 + 注册按钮

#### 2.3 考试列表页（首页）
- **文件**：`miniapp/pages/index/index.vue`
- **函数**：
  - `loadExamList()` — 调用 getExamCardList API
  - `joinExam(item)` — 检查考试状态，进行中则跳转答题页
  - `formatExamStatus(status)` — 状态转文字
  - `onPullDownRefresh()` — 下拉刷新
- **UI**：考试卡片列表（封面、名称、分数、时长、状态、参加按钮）

#### 2.4 个人中心页（tabBar）
- **文件**：`miniapp/pages/mine/mine.vue`
- **函数**：
  - `loadUserInfo()` — 调用 getUserInfo API
  - `goLogin()` — 未登录跳登录页
  - `handleLogout()` — 清除 token + 跳登录页
  - `goExamRecords()` — 跳转考试记录列表
  - `goChangePassword()` — 跳转修改密码页
- **UI**：头像、昵称、角色标签、功能入口（考试记录、修改密码、退出登录）

### 批次 3：考试答题 + 交卷（4 项）

#### 3.1 考试详情页（答题页）
- **文件**：`miniapp/pages/exam/exam.vue`
- **函数**：
  - `loadExamDetail()` — 调用 getExamDetail API，获取 radioIds/checkIds/judgeIds
  - `loadQuestion(questionId)` — 调用 getQuestionDetail API，获取题目内容
  - `selectOption(questionId, optionId)` — 记录用户选择（单选/多选/判断）
  - `switchQuestion(type, index)` — 切换题目
  - `prevQuestion()` / `nextQuestion()` — 上一题/下一题
  - `showQuestionNav()` — 弹出题目导航面板
  - `startTimer()` / `stopTimer()` — 考试计时
  - `handleSubmit()` — 确认交卷弹窗
  - `submitExam()` — 组装 answersMap + 调用 finishExam API
  - `buildAnswersMap()` — 将用户答案组装为 `{ questionId: [optionId, ...] }`
- **data**：`{ examDetail, currentQuestion, currentType, currentIndex, answers: {}, timeLeft, timer, showNav }`
- **UI**：
  - 顶部：考试名称 + 剩余时间倒计时
  - 中部：题干（rich-text 渲染 HTML）+ 选项列表（可点选）
  - 底部：上一题/下一题/题目导航/交卷按钮
  - 题目导航弹层：显示所有题号 + 答题状态

#### 3.2 交卷确认组件
- **文件**：`miniapp/pages/exam/components/submit-confirm.vue`
- **函数**：
  - `getAnsweredCount()` — 统计已答题数
  - `getUnansweredCount()` — 统计未答题数
  - `confirm()` — 确认交卷
  - `cancel()` — 取消，返回继续答题
- **UI**：已答/未答数量 + 确认/取消按钮

#### 3.3 答题计时器组件
- **文件**：`miniapp/pages/exam/components/exam-timer.vue`
- **函数**：
  - `start(timeLimit)` — 启动倒计时（分钟转秒）
  - `stop()` — 停止计时
  - `formatTime(seconds)` — 秒转 mm:ss 显示
  - `onTimeUp()` — 时间到自动交卷
- **UI**：剩余时间显示 + 到时红色警告

#### 3.4 题目渲染组件
- **文件**：`miniapp/pages/exam/components/question-item.vue`
- **函数**：
  - `renderQuestion(name)` — rich-text 渲染题干 HTML
  - `onSelectOption(optionId)` — 选项点击事件
  - `isSelected(optionId)` — 判断选项是否被选中
- **props**：`question`, `type`(radio/check/judge), `selectedAnswers`
- **UI**：题干 rich-text + 选项列表（单选用 radio、多选用 checkbox、判断用 radio）

### 批次 4：成绩查看 + 个人中心扩展（4 项）

#### 4.1 考试记录列表页
- **文件**：`miniapp/pages/records/records.vue`
- **函数**：
  - `loadRecords()` — 调用 getExamRecordList API
  - `viewDetail(record)` — 跳转记录详情页
  - `formatScore(score, total)` — 分数格式化
  - `formatStatus(status)` — 记录状态转文字
  - `onPullDownRefresh()` — 下拉刷新
- **UI**：考试记录卡片列表（考试名称、得分、耗时、日期、查看详情按钮）

#### 4.2 考试记录详情页（答题回顾）
- **文件**：`miniapp/pages/record-detail/record-detail.vue`
- **函数**：
  - `loadRecordDetail()` — 调用 getExamRecordDetail API
  - `loadQuestionDetail(questionId)` — 调用 getQuestionDetail API
  - `switchQuestion(type, index)` — 切换题目
  - `isCorrect(questionId)` — 判断答题是否正确
  - `getUserAnswer(questionId)` — 获取用户答案
  - `getRightAnswer(questionId)` — 获取正确答案
- **UI**：得分概览 + 题目列表（题干 + 用户答案 + 正确答案 + 对错标识）

#### 4.3 修改密码页
- **文件**：`miniapp/pages/change-password/change-password.vue`
- **函数**：
  - `handleSubmit()` — 校验 + 调用 changePassword API + 成功后退出登录
  - `validateForm()` — 校验旧密码/新密码/确认密码
- **UI**：旧密码、新密码、确认密码输入 + 提交按钮

#### 4.4 个人信息编辑页
- **文件**：`miniapp/pages/profile-edit/profile-edit.vue`
- **函数**：
  - `loadUserInfo()` — 加载当前用户信息
  - `handleSave()` — 调用 updateUserInfo API + 返回个人中心
- **UI**：头像、昵称、描述、邮箱、手机号编辑表单

## 3. 技术要点

### 3.1 认证流程
1. 登录成功 → `uni.setStorageSync('access_token', token)`
2. 每次请求 → request.js 自动注入 `Access-Token` header
3. 401/403 → 清除 token + 跳转登录页
4. token 过期 → 后端返回错误 → 同上

### 3.2 富文本渲染
- 后端题目内容为 HTML（Summernote 编辑）
- 小程序端使用 `rich-text` 组件渲染，nodes 属性传入 HTML 字符串
- 注意：rich-text 不支持 `<script>`，需在 `stripHtml()` 中过滤

### 3.3 考试计时
- 客户端计时器（setInterval），每秒递减
- 时间到自动调用 `submitExam()`
- 防作弊：后端 `checkExamAvailable()` 二次校验有效期

### 3.4 答案数据结构
```js
// 用户答案：{ questionId: [optionId, ...] }
answers = {
  'q001': ['opt_a'],
  'q002': ['opt_a', 'opt_c'],
  'q003': ['opt_true']
}
// 提交格式：answersMap = { questionId: [optionId, ...] }（与后端一致）
```

### 3.5 平台适配
- 微信小程序：主平台，使用 uni.request API
- H5：兼容，uni.request 自动适配为 XMLHttpRequest
- 条件编译：`#ifdef MP-WEIXIN` / `#ifdef H5`

## 4. 目录结构

```
miniapp/
├── manifest.json              # uniapp 配置
├── pages.json                 # 页面路由 + tabBar
├── main.js                    # 入口文件
├── App.vue                    # 根组件
├── uni.scss                   # 全局样式变量
├── package.json
├── api/
│   ├── user.js                # 用户相关 API
│   └── exam.js                # 考试相关 API
├── store/
│   ├── index.js               # Vuex store
│   └── modules/
│       └── user.js            # 用户模块
├── utils/
│   ├── request.js             # 请求封装
│   ├── auth.js                # 认证工具
│   └── util.js                # 通用工具
├── pages/
│   ├── login/
│   │   └── login.vue          # 登录页
│   ├── register/
│   │   └── register.vue       # 注册页
│   ├── index/
│   │   └── index.vue          # 首页（考试列表）
│   ├── mine/
│   │   └── mine.vue           # 个人中心
│   ├── exam/
│   │   ├── exam.vue           # 答题页
│   │   └── components/
│   │       ├── submit-confirm.vue
│   │       ├── exam-timer.vue
│   │       └── question-item.vue
│   ├── records/
│   │   └── records.vue        # 考试记录列表
│   ├── record-detail/
│   │   └── record-detail.vue  # 答题回顾
│   ├── change-password/
│   │   └── change-password.vue
│   └── profile-edit/
│       └── profile-edit.vue
└── static/
    ├── logo.png
    └── placeholder.png
```

## 5. 验收标准

1. **编译通过**：微信小程序开发者工具能正常编译预览
2. **登录流程**：用户名/邮箱 + 密码登录成功，token 持久化
3. **考试列表**：展示考试卡片，状态正确，可进入进行中的考试
4. **答题流程**：能加载题目、选择答案、切换题目、倒计时、交卷
5. **成绩查看**：查看考试记录列表 + 答题回顾（对错标识）
6. **个人中心**：查看信息、修改密码、退出登录

## 6. 实施进度

| 批次 | 状态 | 完成时间 |
|---|---|---|
| 批次 1 基础设施 | ✅ 已完成 | 2026-07-23 |
| 批次 2 登录注册+考试列表 | ✅ 已完成 | 2026-07-23 |
| 批次 3 考试答题+交卷 | ✅ 已完成 | 2026-07-23 |
| 批次 4 成绩查看+个人中心 | ✅ 已完成 | 2026-07-23 |
| 编译验证 | ✅ 已完成 | 2026-07-23 |

### 6.1 实施详情

**批次 1 基础设施（6 项）**
- `miniapp/manifest.json`：uniapp 配置（微信小程序 + H5）
- `miniapp/pages.json`：9 个页面路由 + tabBar（考试/我的）
- `miniapp/main.js`：入口文件，引入 Vuex store
- `miniapp/App.vue`：根组件，全局样式 + onLaunch 检查登录
- `miniapp/uni.scss`：全局 scss 变量
- `miniapp/package.json`：依赖声明
- `miniapp/utils/request.js`：`request()` / `get()` / `post()` / `del()` — 封装 uni.request，自动注入 Access-Token，401/403 跳登录
- `miniapp/utils/auth.js`：`getToken()` / `setToken()` / `removeToken()` / `isLoggedIn()` / `checkAuth()`
- `miniapp/utils/util.js`：`formatDate()` / `formatTimeCost()` / `stripHtml()` / `getExamStatus()` / `formatRole()` / `formatRecordStatus()`
- `miniapp/api/user.js`：`register()` / `login()` / `getUserInfo()` / `getUserDetail()` / `changePassword()` / `updateUserInfo()`
- `miniapp/api/exam.js`：`getExamCardList()` / `getExamDetail()` / `getQuestionDetail()` / `finishExam()` / `getExamRecordList()` / `getExamRecordDetail()` / `deleteExamRecord()`
- `miniapp/store/index.js` + `miniapp/store/modules/user.js`：Vuex user 模块（`login` / `logout` / `refreshUserInfo` actions + `isLoggedIn`/`userInfo`/`userRole`/`userNickname` getters）

**批次 2 登录注册+考试列表（4 项）**
- `miniapp/pages/login/login.vue`：`switchLoginType()` / `validateForm()` / `handleLogin()` / `goRegister()` — 用户名/邮箱切换登录
- `miniapp/pages/register/register.vue`：`validateForm()` / `handleRegister()` / `goLogin()` — 注册表单校验
- `miniapp/pages/index/index.vue`：`loadExamList()` / `getExamStatusText()` / `joinExam()` / `onPullDownRefresh()` — 考试卡片列表
- `miniapp/pages/mine/mine.vue`：`goLogin()` / `goExamRecords()` / `goProfileEdit()` / `goChangePassword()` / `handleLogout()` — 个人中心

**批次 3 考试答题+交卷（4 项）**
- `miniapp/pages/exam/components/exam-timer.vue`：`start()` / `stop()` / `getElapsedSeconds()` — 倒计时组件，时间到触发 timeup 事件
- `miniapp/pages/exam/components/question-item.vue`：`isSelected()` / `onSelectOption()` — rich-text 渲染题干，支持单选/多选/判断
- `miniapp/pages/exam/components/submit-confirm.vue`：`getAnsweredCount()` / `confirm()` / `cancel()` — 交卷确认弹窗
- `miniapp/pages/exam/exam.vue`：`loadExamDetail()` / `loadCurrentQuestion()` / `onSelectOption()` / `switchQuestion()` / `prevQuestion()` / `nextQuestion()` / `showQuestionNav()` / `handleTimeUp()` / `handleSubmit()` / `buildAnswersMap()` / `submitExam()` — 核心答题页

**批次 4 成绩查看+个人中心扩展（4 项）**
- `miniapp/pages/records/records.vue`：`loadRecords()` / `viewDetail()` — 考试记录列表
- `miniapp/pages/record-detail/record-detail.vue`：`loadRecordDetail()` / `loadCurrentQuestion()` / `getUserAnswer()` / `getRightAnswer()` / `isCorrect()` / `formatAnswer()` / `prevQuestion()` / `nextQuestion()` — 答题回顾
- `miniapp/pages/change-password/change-password.vue`：`validateForm()` / `handleSubmit()` — 修改密码后强制重新登录
- `miniapp/pages/profile-edit/profile-edit.vue`：`loadUserInfo()` / `chooseAvatar()` / `validateForm()` / `handleSave()` — 编辑资料

### 6.2 编译验证

**JS 语法检查**：8 个 JS 文件全部通过 `node --check`（main.js / request.js / auth.js / util.js / user.js / exam.js / store/index.js / store/modules/user.js）

**页面路由匹配**：9 个页面路由全部与实际 .vue 文件匹配（ALL_PAGES_MATCH）

**项目结构**：25 个文件，覆盖 4 批次 18 项实施清单

**说明**：uniapp 项目需通过 HBuilderX 或 uni-app-cli 编译为微信小程序。当前环境未安装 uni-app-cli，已通过 JS 语法检查和路由匹配验证代码质量。实际编译请在 HBuilderX 中打开 `miniapp/` 目录运行「运行到微信小程序」。
