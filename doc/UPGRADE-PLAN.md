# 在线考试系统升级实施清单

> 基于《项目分析报告》识别的短板，按优先级分批实施。每项任务精确到函数级别。

---

## 批次 1：安全与核心功能（Critical）

### 1.1 后端角色鉴权（修复 API 越权漏洞）

**问题**：JWT token 仅携带 `id/username/avatar`，[LoginInterceptor](file:///persistent/home/wolf/spring-boot-online-exam/backend/src/main/java/lsgwr/exam/interceptor/LoginInterceptor.java) 只校验登录态不校验角色，学生可调用管理接口。

| # | 文件 | 函数/改动 | 状态 |
|---|------|----------|------|
| 1.1.1 | `JwtUtils.java` | `genJsonWebToken(User)` 新增 `.claim("roleId", user.getUserRoleId())` | ✅ 已完成 |
| 1.1.2 | `JwtUtils.java` | `checkJWT(String)` 返回的 Claims 现包含 roleId | ✅ 已完成 |
| 1.1.3 | `annotation/RoleRequired.java` | 新建注解类，`@Target(METHOD)`, 属性 `RoleEnum[] value()` | ✅ 已完成 |
| 1.1.4 | `interceptor/RoleInterceptor.java` | `preHandle()` 读取 `@RoleRequired`，从 token 取 roleId 比对 | ✅ 已完成 |
| 1.1.5 | `config/IntercepterConfig.java` | `addInterceptors()` 注册 RoleInterceptor，路径 `/api/**` | ✅ 已完成 |
| 1.1.6 | `ExamController.java` | `questionCreate()` 加 `@RoleRequired({TEACHER, ADMIN})` | ✅ 已完成 |
| 1.1.7 | `ExamController.java` | `questionUpdate()` 加 `@RoleRequired({TEACHER, ADMIN})` | ✅ 已完成 |
| 1.1.8 | `ExamController.java` | `createExam()` 加 `@RoleRequired({TEACHER, ADMIN})` | ✅ 已完成 |
| 1.1.9 | `ExamController.java` | `updateExam()` 加 `@RoleRequired({TEACHER, ADMIN})` | ✅ 已完成 |
| 1.1.10 | `ExamController.java` | `getExamAll()` 加 `@RoleRequired({TEACHER, ADMIN})` | ✅ 已完成 |
| 1.1.11 | `ExamController.java` | `getQuestionAll()` 加 `@RoleRequired({TEACHER, ADMIN})` | ✅ 已完成 |
| 1.1.12 | `LoginInterceptor.java` | `preHandle()` 将 roleId 存入 request.setAttribute | ✅ 已完成 |

### 1.2 考试计时强制约束

**问题**：[ExamDetail.vue#L11](file:///persistent/home/wolf/spring-boot-online-exam/frontend/src/views/list/ExamDetail.vue#L11) 显示"这里是倒计时"占位文字，无真实倒计时；[ExamServiceImpl.judge()](file:///persistent/home/wolf/spring-boot-online-exam/backend/src/main/java/lsgwr/exam/service/impl/ExamServiceImpl.java#L501) 从未设置 `examTimeCost`。

| # | 文件 | 函数/改动 | 状态 |
|---|------|----------|------|
| 1.2.1 | `ExamDetail.vue` | `data()` 新增 `remainingSeconds`, `timerId`, `startTime` | ✅ 已完成 |
| 1.2.2 | `ExamDetail.vue` | `mounted()` 调用 `startCountdown()` | ✅ 已完成 |
| 1.2.3 | `ExamDetail.vue` | `startCountdown()` setInterval 每秒递减，到 0 自动交卷 | ✅ 已完成 |
| 1.2.4 | `ExamDetail.vue` | `formatTime(seconds)` 格式化 mm:ss | ✅ 已完成 |
| 1.2.5 | `ExamDetail.vue` | template 替换占位文字为 `{{ formatTime(remainingSeconds) }}` | ✅ 已完成 |
| 1.2.6 | `ExamDetail.vue` | `finishExam()` 计算并传入 `timeCost` 参数 | ✅ 已完成 |
| 1.2.7 | `ExamDetail.vue` | `mounted()` 添加 `window.addEventListener('beforeunload', ...)` | ✅ 已完成 |
| 1.2.8 | `ExamDetail.vue` | `beforeDestroy()` 清除 timer 和事件监听 | ✅ 已完成 |
| 1.2.9 | `api/exam.js` | `finishExam()` 增加 timeCost 参数传递（X-Time-Cost header） | ✅ 已完成 |
| 1.2.10 | `ExamController.java` | `finishExam()` 从 `@RequestHeader` 获取 timeCost 传入 service | ✅ 已完成 |
| 1.2.11 | `ExamService.java` | `judge()` 签名增加 `Integer timeCost` 参数 | ✅ 已完成 |
| 1.2.12 | `ExamServiceImpl.java` | `judge()` 设置 `examRecord.setExamTimeCost(timeCost)` | ✅ 已完成 |

---

## 批次 2：考试公平性（High）

### 2.1 考试有效期校验

**问题**：[ExamServiceImpl.create()](file:///persistent/home/wolf/spring-boot-online-exam/backend/src/main/java/lsgwr/exam/service/impl/ExamServiceImpl.java) 硬编码 `setExamStartDate(new Date())`，后端无时间窗口校验。

| # | 文件 | 函数/改动 | 状态 |
|---|------|----------|------|
| 2.1.1 | `ExamServiceImpl.java` | `create()` 移除硬编码，使用 examCreateVo 传入的日期 | ✅ 已完成 |
| 2.1.2 | `ExamCreateVo.java` | 新增 `examStartDate`, `examEndDate` 字段（含 `@JsonFormat`） | ✅ 已完成 |
| 2.1.3 | `ExamService.java` | 新增 `checkExamAvailable(examId)` 接口 | ✅ 已完成 |
| 2.1.4 | `ExamServiceImpl.java` | 实现 `checkExamAvailable()` 返回枚举 | ✅ 已完成 |
| 2.1.5 | `ExamController.java` | `getExamDetail()` 调用 checkExamAvailable，非 AVAILABLE 时拒绝 | ✅ 已完成 |
| 2.1.6 | `ExamController.java` | `finishExam()` 二次校验考试有效期 | ✅ 已完成 |
| 2.1.7 | `enums/ExamStatusEnum.java` | 新建枚举 UPCOMING / AVAILABLE / ENDED | ✅ 已完成 |
| 2.1.8 | `ExamCardList.vue` | `joinExam()` 调用前检查状态，非 AVAILABLE 时提示 | ✅ 已完成 |
| 2.1.9 | `ExamCardList.vue` | template 显示考试状态标签 | ✅ 已完成 |

### 2.2 题目随机化

**问题**：所有学生看到相同题目、相同顺序，存在作弊风险。

| # | 文件 | 函数/改动 | 状态 |
|---|------|----------|------|
| 2.2.1 | `ExamController.java` | `getExamDetail()` 按 userId 为种子 shuffle 题目 ID 顺序 | ✅ 已完成 |
| 2.2.2 | `ExamServiceImpl.java` | 新建 `shuffleQuestionIds(ids, seed)` 基于 userId 的确定性 shuffle | ✅ 已完成 |
| 2.2.3 | `ExamServiceImpl.java` | `judge()` 适配 shuffle 后的题目顺序（按 ID 查找，不依赖顺序） | ✅ 已完成 |
| 2.2.4 | `ExamRecord.java` | 新增 `questionOrder` 字段记录该次考试的题目顺序 | ✅ 已完成（在 `judge()` 中持久化 shuffle 后的顺序快照） |

---

## 批次 3：学生体验（Medium）

### 3.1 交卷确认与未答提醒

**问题**：[ExamDetail.vue](file:///persistent/home/wolf/spring-boot-online-exam/frontend/src/views/list/ExamDetail.vue) `finishExam()` 直接提交，误触即交卷。

| # | 文件 | 函数/改动 | 状态 |
|---|------|----------|------|
| 3.1.1 | `ExamDetail.vue` | `finishExam()` 改为先调用 Modal.confirm | ✅ 已完成 |
| 3.1.2 | `ExamDetail.vue` | `finishExam()` + `doSubmit()` 新建：Modal.confirm + 统计未答题数 | ✅ 已完成 |
| 3.1.3 | `ExamDetail.vue` | `countUnanswered()` 新建：遍历 answersMap 对比题目总数 | ✅ 已完成 |

### 3.2 答题草稿自动保存

**问题**：刷新/关闭页面丢失全部作答。

| # | 文件 | 函数/改动 | 状态 |
|---|------|----------|------|
| 3.2.1 | `ExamDetail.vue` | `data()` 新增 `draftKey`（基于 examId） | ✅ 已完成 |
| 3.2.2 | `ExamDetail.vue` | `saveDraft()` 新建：localStorage 序列化 answersMap | ✅ 已完成 |
| 3.2.3 | `ExamDetail.vue` | `loadDraft()` 新建：mounted 时从 localStorage 恢复 | ✅ 已完成 |
| 3.2.4 | `ExamDetail.vue` | `onRadioChange()` / `onCheckChange()` 答题后调用 saveDraft() | ✅ 已完成 |
| 3.2.5 | `ExamDetail.vue` | `doSubmit()` 成功后调用 `clearDraft()` | ✅ 已完成 |
| 3.2.6 | `ExamDetail.vue` | `mounted()` setInterval 每 30s 自动保存 | ✅ 已完成 |

---

## 批次 4：教学反馈（Low）

### 4.1 教师成绩统计分析

**问题**：教师无法查看全班成绩，README Todo 第7、8项。

| # | 文件 | 函数/改动 | 状态 |
|---|------|----------|------|
| 4.1.1 | `ExamService.java` | 新增 `getExamAllRecords(examId)` | ✅ 已完成 |
| 4.1.2 | `ExamServiceImpl.java` | 实现 `getExamAllRecords()` | ✅ 已完成 |
| 4.1.3 | `ExamRecordRepository.java` | 新增 `findByExamId()` | ✅ 已完成 |
| 4.1.4 | `ExamService.java` | 新增 `getExamScoreStat(examId)` | ✅ 已完成 |
| 4.1.5 | `ExamServiceImpl.java` | 实现 `getExamScoreStat()` | ✅ 已完成（含 5 段分数分布、60% 及格线） |
| 4.1.6 | `vo/ExamScoreStatVo.java` | 新建 VO | ✅ 已完成 |
| 4.1.7 | `ExamController.java` | 新增 `getExamAllRecords()` | ✅ 已完成（GET `/record/all/{examId}`） |
| 4.1.8 | `ExamController.java` | 新增 `getExamScoreStat()` | ✅ 已完成（GET `/score/stat/{examId}`） |
| 4.1.9 | `api/exam.js` | 新增 `getExamAllRecords()`, `getExamScoreStat()` | ✅ 已完成（含 index.js 常量） |
| 4.1.10 | `views/list/ExamRecordStat.vue` | 新建页面 | ✅ 已完成（统计卡片 + Viser 柱状图 + 学生记录表格） |
| 4.1.11 | `router.config.js` | 新增路由 | ✅ 已完成（`/exam/stat/:examId`，置于 `/exam/:id` 之前避免冲突） |
| 4.1.12 | `ExamTableList.vue` | 新增"统计"按钮入口 | ✅ 已完成（`handleStat()`，新窗口打开） |

---

## 实施顺序

```
批次1 (1.1 → 1.2)  ──→  批次2 (2.1 → 2.2)  ──→  批次3 (3.1 → 3.2)  ──→  批次4
  安全+核心               公平性               体验               反馈
```

---

## 完成总结

| 批次 | 任务数 | 完成数 | 状态 |
|------|--------|--------|------|
| 批次 1 安全与核心功能 | 12 + 12 = 24 | 24 | ✅ 全部完成 |
| 批次 2 考试公平性 | 9 + 4 = 13 | 13 | ✅ 全部完成 |
| 批次 3 学生体验 | 3 + 6 = 9 | 9 | ✅ 全部完成 |
| 批次 4 教学反馈 | 12 | 12 | ✅ 全部完成 |
| **总计** | **58** | **58** | ✅ **全部完成** |

### 验证证据

- **后端编译**：`mvn clean compile -DskipTests` BUILD SUCCESS，88 个源文件全部编译通过
- **新增文件**：`RoleRequired.java`, `RoleInterceptor.java`, `ExamStatusEnum.java`, `ExamScoreStatVo.java`, `ExamRecordStat.vue`
- **修改文件**：`JwtUtils.java`, `LoginInterceptor.java`, `IntercepterConfig.java`, `ExamController.java`, `ExamService.java`, `ExamServiceImpl.java`, `ExamRecordRepository.java`, `ExamCreateVo.java`, `ExamCardVo.java`, `ExamRecord.java`, `ExamDetail.vue`, `ExamCardList.vue`, `ExamTableList.vue`, `api/exam.js`, `api/index.js`, `router.config.js`
- **数据库变更**：`ExamRecord` 新增 `questionOrder` 字段（由 Hibernate `ddl-auto: update` 自动添加列）

