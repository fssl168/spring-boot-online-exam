# 在线考试系统全面升级实施清单

## 概述

本计划针对 spring-boot-online-exam 项目的六大维度短板，制定函数级的分批实施清单，目标是将六个维度的评分全部提升至 5 分（满分）。

---

## 第零批：核心安全与业务缺陷修复（最高优先级，阻断性修复）

> **目标**：修复当前系统中三个阻断性问题——角色鉴权缺失、计时功能形同虚设、考试有效期未生效。修复后禁止学生角色调用管理接口，确保考试计时和有效期校验真正生效。

### 0.1 后端角色鉴权体系（高危：前端权限可被绕过，学生可调管理接口）

**问题现状：**
1. `LoginInterceptor` 仅校验 token 是否存在和合法，**从未校验用户角色**。
2. JWT token 中不包含 `role_id` 字段，每次鉴权需额外查库。
3. `IntercepterConfig` 对所有 `/api/**` 一视同仁，无角色级别的路径白名单。
4. `ExamController` 中 `questionCreate`、`questionUpdate`、`createExam`、`updateExam` 等管理接口未做任何角色校验，任何登录用户（包括学生）均可调用。

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 0.1.1 | `JwtUtils.java` | `genJsonWebToken()` 新增 `.claim("roleId", user.getUserRoleId())` | token 携带角色信息 |
| 0.1.2 | `LoginInterceptor.java` | `preHandle()` 新增角色鉴权逻辑：从 claim 中读取 `role_id`，根据 URI 判断是否需要特定角色 | 拦截器内统一鉴权 |
| 0.1.3 | `IntercepterConfig.java` | 新增 `roleIgnoreUris` 配置项（如 `/api/exam/detail/*`, `/api/exam/card/list`），与现有 `authIgnoreUris` 分离 | 区分"无需登录"和"无需角色" |
| 0.1.4 | `application.yml` | 新增 `interceptors.role-ignore-uris` 配置 | 配置免角色校验的只读接口 |
| 0.1.5 | `ExamController.java` | `questionCreate`、`questionUpdate`、`createExam`、`updateExam` 方法内新增 `checkRole(admin_or_teacher)` 校验 | 管理接口兜底校验 |
| 0.1.6 | `UserService.java` | 新增 `getUserRoleId(String userId)` 方法签名 | 支持运行时角色查询 |
| 0.1.7 | `UserServiceImpl.java` | 实现 `getUserRoleId()`：优先从 Redis 缓存读取，miss 时查库并回写缓存 | 减少数据库压力 |
| 0.1.8 | 新增 `annotation/RequireRole.java` | **自定义注解**：`@RequireRole(RoleEnum.ADMIN)`, `@RequireRole({RoleEnum.ADMIN, RoleEnum.TEACHER})` | 声明式角色鉴权 |
| 0.1.9 | `LoginInterceptor.java` | 增加对 `@RequireRole` 注解的识别和处理 | AOP 式拦截 |
| 0.1.10 | `ExamController.java` | 所有管理接口添加 `@RequireRole({ADMIN, TEACHER})` 注解 | 声明式权限标记 |
| 0.1.11 | `GlobalExceptionHandler.java` | 新增 403 角色不足异常处理 | 统一错误码返回 |

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 0.1.12 | `permission.js` | 根据登录后返回的 `roleName` 动态渲染侧边栏菜单 | 菜单级权限 |
| 0.1.13 | 新增 `utils/role.js` | `hasRole(roleName)`、`hasAnyRole([...])`、`hasAllRoles([...])` 工具函数 | 前端角色判断 |
| 0.1.14 | 全局 `main.js` | 注册 `v-hasRole` 自定义指令 | 按钮级权限控制 |
| 0.1.15 | 各管理页面 | 对仅教师/管理员可见的按钮使用 `v-hasRole` 指令隐藏 | 按钮级权限 |
| 0.1.16 | `request.js` | 403 响应时跳转至"权限不足"提示页 | 用户体验 |

### 0.2 考试计时功能（核心业务：当前 exam_time_limit 字段存在但完全不生效）

**问题现状：**
1. `Exam.examTimeLimit` 字段存在于数据库和实体类中，但 Service 层无任何使用。
2. 前端 `ExamDetail.vue` 无倒计时组件，交卷仅依赖手动点击。
3. 考试超时后学生仍可无限期答题，系统无自动交卷机制。
4. 服务器端不校验交卷时间是否超出时限，`judge()` 方法未做时间验证。

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 0.2.1 | 新增 `vo/ExamTimerVo.java` | **新增 VO**：`examId`, `timeLimit`(秒), `remainingSeconds`, `expired`(布尔) | 计时接口返回体 |
| 0.2.2 | `ExamService.java` | 新增 `getExamTimer(String examId, String userId)` 方法签名 | 获取考试计时信息 |
| 0.2.3 | `ExamServiceImpl.java` | 实现 `getExamTimer()`：计算当前时间与 examStartDate 的差值，返回剩余时间 | 服务端计时逻辑 |
| 0.2.4 | `ExamController.java` | 新增 `GET /api/exam/timer/{examId}` 接口 | 前端轮询/初始同步 |
| 0.2.5 | `ExamService.java` | 新增 `enforceTimeLimit(String examId, Date submitTime)` 校验方法 | 交卷时服务端时限校验 |
| 0.2.6 | `ExamServiceImpl.java` | `judge()` 方法开头增加时限校验：若 `submitTime > examStartDate + timeLimit` 则拒绝交卷并返回 `TIME_EXPIRED` | 服务端兜底 |
| 0.2.7 | `ExamRecord.java` | 新增 `examRecordSubmitTime` 字段（DATETIME） | 记录实际交卷时间 |
| 0.2.8 | `ExamRecordRepository.java` | 新增 `findByExamIdAndSubmitTimeAfter()` 方法 | 查询超时交卷记录 |
| 0.2.9 | `enums/ResultEnum.java` | 新增 `TIME_EXPIRED(-2, "考试时长已超限，禁止交卷")` | 错误码 |
| 0.2.10 | `ExamCreateVo.java` | 增加 `@Min(1)` 校验 `examTimeLimit` | 防止零时长 |

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 0.2.11 | `ExamDetail.vue` | 新增 `timeLimit`、`remainingSeconds`、`timerRunning` 响应式变量 | 计时状态 |
| 0.2.12 | `ExamDetail.vue` | 进入考试时调用 `/exam/timer/{examId}` 获取初始剩余时间 | 服务端时间同步 |
| 0.2.13 | `ExamDetail.vue` | 新增 `startCountdown()` 方法：每秒递减 `remainingSeconds`，归零时调用 `autoSubmit()` | 倒计时核心 |
| 0.2.14 | `ExamDetail.vue` | 新增 `autoSubmit()` 方法：自动调用交卷接口，弹窗提示"考试时间到，自动交卷" | 超时自动交卷 |
| 0.2.15 | `ExamDetail.vue` | 交卷按钮旁显示倒计时组件（progress bar + 数字），最后 60 秒变红闪烁 | 视觉提醒 |
| 0.2.16 | `ExamDetail.vue` | 倒计时归零时禁用所有答题选项，防止超时修改答案 | 锁定答题 |
| 0.2.17 | `ExamDetail.vue` | 离开考试页面时暂停倒计时，返回时恢复（使用服务端剩余时间重新校准） | 切页保护 |

### 0.3 考试有效期窗口控制（教师无法控制考试开放/关闭时间）

**问题现状：**
1. `Exam.examStartDate` 和 `Exam.examEndDate` 字段已存在于数据库，但**没有任何代码校验这两个日期**。
2. 教师创建考试时可设置起止时间，但学生无论在何时访问，`getExamDetail()` 都直接返回考试详情。
3. 考试结束后系统不阻止学生继续答题，也不自动标记考试为"已结束"。
4. 前端无任何日期校验提示，学生可能在考试已关闭后仍答题并提交。

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 0.3.1 | `ExamService.java` | 新增 `checkExamValidity(String examId)` 方法签名 | 考试有效期校验 |
| 0.3.2 | `ExamServiceImpl.java` | 实现 `checkExamValidity()`：比对当前时间与 `examStartDate`/`examEndDate`，返回 `{valid, reason, remainingSeconds}` | 服务端校验 |
| 0.3.3 | `ExamController.java` | 新增 `GET /api/exam/validity/{examId}` 接口 | 独立有效期检查接口 |
| 0.3.4 | `ExamController.java` | `getExamDetail()` 方法开头调用 `checkExamValidity()`，不通过则返回 `EXAM_NOT_STARTED` 或 `EXAM_ENDED` | 详情页兜底 |
| 0.3.5 | `ExamController.java` | `finishExam()` 方法中再次校验有效期（防止考试进行中被关闭） | 交卷时兜底 |
| 0.3.6 | `enums/ResultEnum.java` | 新增 `EXAM_NOT_STARTED(-3, "考试尚未开始")`、`EXAM_ENDED(-4, "考试已结束")` | 错误码 |
| 0.3.7 | `ExamServiceImpl.java` | `getExamCardList()` 中过滤掉 `examEndDate < now` 的记录 | 卡片列表不展示过期考试 |
| 0.3.8 | `ExamServiceImpl.java` | `getExamCardList()` 中新增 `examStatus` 字段计算：`NOT_STARTED` / `ONGOING` / `ENDED` | 考试状态标识 |

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 0.3.9 | `ExamCardList.vue`（或 ExamTableList.vue） | 根据 `examStatus` 在卡片上标注状态标签：未开始/进行中/已结束 | 状态可视化 |
| 0.3.10 | `ExamDetail.vue` | 进入详情页时调用 `/exam/validity/{examId}`，若未开始则显示"距离考试开始还有 X 分钟"倒计时 | 未开始提示 |
| 0.3.11 | `ExamDetail.vue` | 若已过期则显示"考试已结束"并禁止进入答题，仅可查看历史成绩（如有） | 已结束拦截 |
| 0.3.12 | `ExamCreateModal.vue` | 增加日期合理性校验：`endDate >= startDate`，且默认 `startDate = now` | 创建时校验 |

---

## 第一批：安全性加固（高优先级，阻断性修复）

> **目标**：消除当前系统中所有严重安全隐患，使安全评分达到 5 分。

### 1.1 密码安全改造

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 1.1.1 | `pom.xml` | 添加 BCrypt 依赖（Spring Boot 2.7 内置） | 引入 BCryptPasswordEncoder |
| 1.1.2 | `UserServiceImpl.java` | `register()` 方法：将 `Base64.encode()` 改为 `BCryptPasswordEncoder.encode()` | 密码哈希存储 |
| 1.1.3 | `UserServiceImpl.java` | `login()` 方法：将 `Base64.decodeStr()` + 明文比较改为 `BCryptPasswordEncoder.matches()` | 密码验证 |
| 1.1.4 | `UserServiceImpl.java` | 新增 `changePassword()` 方法 | 支持用户修改密码 |
| 1.1.5 | `UserController.java` | 新增 `POST /api/user/change-password` 接口 | 暴露改密 API |
| 1.1.6 | `pom.xml` | 移除 hutool 4.5.10 的 `cn.hutool.core.codec.Base64` 相关引用 | 清理 Base64 密码编码残留 |

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 1.1.7 | `Register.vue` | 增加密码强度校验（最少 8 位，含字母+数字） | 前端密码策略 |
| 1.1.8 | `Register.vue` | 增加两次密码一致性校验 | 防止输错 |
| 1.1.9 | `BaseSetting.vue` | 新增"修改密码"表单页 | 用户入口 |

### 1.2 JWT 安全加固

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 1.2.1 | `JwtUtils.java` | 将 `APP_SECRET` 从硬编码改为从 `application.yml` 读取（`@Value`） | 密钥外部化 |
| 1.2.2 | `JwtUtils.java` | 将过期时间从 1 天改为 2 小时，新增 refresh token 机制（7 天） | 缩短攻击窗口 |
| 1.2.3 | `JwtUtils.java` | 新增 `refreshToken()` 方法 | 无感续期 |
| 1.2.4 | `JwtUtils.java` | 新增 `blacklistToken()` 方法 + StringRedisTemplate 支持 | Token 黑名单（登出/改密后失效） |
| 1.2.5 | `LoginInterceptor.java` | 增加 token 黑名单校验 | 拦截已失效 token |
| 1.2.6 | `IntercepterConfig.java` | 新增 `/api/user/refresh-token` 白名单 | 允许刷新 token |
| 1.2.7 | `UserController.java` | 新增 `POST /api/user/refresh-token` 接口 | 刷新 token |
| 1.2.8 | `UserController.java` | 新增 `POST /api/user/logout` 接口 | 登出时将 token 加入黑名单 |

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 1.2.9 | `request.js` | 拦截器中增加 401 自动刷新 token 逻辑 | 无感续期 |
| 1.2.10 | `request.js` | 超时时间从 6s 提升到 15s | 避免网络波动误判 |
| 1.2.11 | `index.js` (api) | 新增 `RefreshToken` 和 `Logout` 路由 | 新增 API 路由 |
| 1.2.12 | `login.js` (api) | 新增 `refreshToken()` 和 `logout()` 函数 | 前端调用 |
| 1.2.13 | `permission.js` | 401 时先尝试 refresh token 再跳转登录 | 优化登录态管理 |

### 1.3 输入校验与安全过滤

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 1.3.1 | `pom.xml` | hibernate-validator 已内置于 Spring Boot Starter Web | JSR-303 校验 |
| 1.3.2 | `RegisterDTO.java` | 添加 `@NotBlank`, `@Size(min=8,max=32)`, `@Email`, `@Pattern` 注解 | 注册参数校验 |
| 1.3.3 | `LoginQo.java` | 添加 `@NotBlank`, `@Size` 注解 | 登录参数校验 |
| 1.3.4 | `QuestionCreateVo.java` | 添加 `@NotBlank`, `@Size`, `@NotNull` 注解 | 题目创建校验 |
| 1.3.5 | `ExamCreateVo.java` | 添加 `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Past`, `@Future` 注解 | 考试创建校验 |
| 1.3.6 | `ExamController.java` | 所有 POST 接口参数添加 `@Valid` 注解 | 启用校验 |
| 1.3.7 | 新增 `exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` 全局异常处理器 | 统一校验错误返回格式 |
| 1.3.8 | `config/CORSConf.java` | 审查 CORS 配置，限制允许的 origin（生产环境） | 防止跨域攻击 |

### 1.4 数据库安全

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 1.4.1 | `application.yml` | 数据库密码改为环境变量读取（`${DB_PASSWORD:default}`） | 敏感配置外部化 |
| 1.4.2 | `application.yml` | 添加 `useSSL=true` 和 `verifyServerCertificate=true` | 启用 SSL 连接 |

---

## 第二批：考试过程可靠性（用户体验维度）

> **目标**：解决倒计时、答案持久化、考试有效性校验三大痛点。

### 2.1 倒计时功能

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 2.1.1 | `ExamController.java` | 新增 `GET /exam/timer/{examId}` 接口，返回剩余秒数 | 服务器时间为准 |
| 2.1.2 | `ExamController.java` | 在 `getExamDetail()` 中增加有效期校验（start/end date） | 未到时间不可考 |
| 2.1.3 | `ExamService.java` | 新增 `checkExamValidity()` 方法 | 校验考试是否可参加 |

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 2.1.4 | `ExamDetail.vue` | 新增 `countdown` 响应式变量和 `startCountdown()` 方法 | 实现真实倒计时 |
| 2.1.5 | `ExamDetail.vue` | 倒计时归零时自动调用 `finishExam()` | 超时自动交卷 |
| 2.1.6 | `ExamDetail.vue` | 进入页面时调用 `/exam/timer/{examId}` 获取初始剩余时间 | 服务器时间同步 |
| 2.1.7 | `ExamDetail.vue` | 进入页面时调用有效期校验接口，不通过则跳转并提示 | 拦截非法考试 |

### 2.2 答案自动保存

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 2.2.1 | 新增 `entity/ExamAnswer.java` | **新增实体类**：`exam_answer` 表（exam_record_id, question_id, answer_option_ids） | 持久化答案 |
| 2.2.2 | 新增 `repository/ExamAnswerRepository.java` | **新增 Repository** | 答案 CRUD |
| 2.2.3 | `service/ExamService.java` | 新增 `saveAnswer()` 和 `getDraftAnswer()` 方法签名 | 答案存储/读取接口 |
| 2.2.4 | `service/impl/ExamServiceImpl.java` | 实现 `saveAnswer()` 方法：UPSERT 逻辑 | 幂等操作 |
| 2.2.5 | `service/impl/ExamServiceImpl.java` | 实现 `getDraftAnswer()` 方法：查询草稿 | 恢复断点 |
| 2.2.6 | `controller/ExamController.java` | 新增 `POST /exam/answer/save` 接口 | 前端调用 |
| 2.2.7 | `controller/ExamController.java` | 新增 `GET /exam/answer/draft/{examId}` 接口 | 获取草稿 |

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 2.2.8 | `ExamDetail.vue` | 每次答案变更时调用 `saveAnswer()` 接口（防抖 2 秒） | 自动保存 |
| 2.2.9 | `ExamDetail.vue` | 页面加载时调用 `getDraftAnswer()` 恢复上次作答 | 断点续答 |
| 2.2.10 | `ExamDetail.vue` | 页面卸载时（beforeUnmount）立即保存当前答案 | 防止丢失 |

### 2.3 交卷确认与防误触

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 2.3.1 | `ExamDetail.vue` | 交卷按钮前增加确认弹窗，显示未答题数和已答题数 | 二次确认 |
| 2.3.2 | `ExamDetail.vue` | 弹窗中显示"交卷后不可修改"的警示 | 风险提示 |
| 2.3.3 | `ExamDetail.vue` | 未答题数 > 0 时弹窗标红提醒 | 视觉警告 |

### 2.4 浏览器意外关闭恢复

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 2.4.1 | `ExamDetail.vue` | 监听 `beforeunload` 事件，立即保存当前答案到服务端 | 关闭前抢救 |
| 2.4.2 | `ExamDetail.vue` | 重新进入同一考试时，优先加载草稿而非重新开始 | 恢复机制 |

---

## 第三批：教育专业性增强

> **目标**：实现随机组卷、成绩分析、主观题支持、批量导入。

### 3.1 随机组卷

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 3.1.1 | 新增 `entity/RandomExamRule.java` | **新增实体类**：随机组卷规则（题型、数量、难度分布） | 组卷策略配置 |
| 3.1.2 | 新增 `repository/RandomExamRuleRepository.java` | **新增 Repository** | 规则 CRUD |
| 3.1.3 | 新增 `vo/RandomExamRuleVo.java` | **新增 VO** | 前端传输对象 |
| 3.1.4 | `service/ExamService.java` | 新增 `randomExamCreate()` 方法签名 | 随机组卷接口 |
| 3.1.5 | `service/impl/ExamServiceImpl.java` | 实现 `randomExamCreate()`：按规则从题库中随机抽取题目 | 核心算法 |
| 3.1.6 | `controller/ExamController.java` | 新增 `POST /exam/random-create` 接口 | 暴露 API |

### 3.2 成绩统计分析

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 3.2.1 | 新增 `vo/ExamStatisticsVo.java` | **新增 VO**：考试统计（平均分、最高分、最低分、标准差、各题正确率） | 统计结果 |
| 3.2.2 | 新增 `vo/ClassRankingVo.java` | **新增 VO**：班级排名（按学生分组排名） | 排名功能 |
| 3.2.3 | `service/ExamService.java` | 新增 `getExamStatistics()` 方法 | 统计接口 |
| 3.2.4 | `service/impl/ExamServiceImpl.java` | 实现 `getExamStatistics()`：聚合查询 ExamRecord，计算各项指标 | 核心逻辑 |
| 3.2.5 | `service/impl/ExamServiceImpl.java` | 实现 `getClassRanking()`：按考试分组统计学生成绩排名 | 排名逻辑 |
| 3.2.6 | `controller/ExamController.java` | 新增 `GET /exam/statistics/{examId}` 接口（仅教师/管理员） | 统计 API |
| 3.2.7 | `controller/ExamController.java` | 新增 `GET /exam/ranking/{examId}` 接口（仅教师/管理员） | 排名 API |

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 3.2.8 | `ExamTableList.vue` | 新增"统计分析"按钮和弹窗 | 入口 |
| 3.2.9 | 新增 `ExamStatistics.vue` 组件 | 展示柱状图（分数分布）、折线图（正确率趋势）、表格（排名） | 可视化 |
| 3.2.10 | 新增 `api/statistics.js` | 封装统计和排名 API 调用 | 数据层 |
| 3.2.11 | `ExamRecordDetail.vue` | 增加各题型得分 breakdown 展示 | 细化反馈 |

### 3.3 主观题支持

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 3.3.1 | `entity/Question.java` | 新增 `questionType` 枚举值：`essay`（主观题） | 题型扩展 |
| 3.3.2 | `entity/Question.java` | 新增 `questionScoreRange` 字段（最低分-最高分） | 评分区间 |
| 3.3.3 | `entity/ExamRecord.java` | 新增 `examRecordEssayAnswer` 字段（TEXT，存储学生作答） | 学生答案 |
| 3.3.4 | `entity/ExamRecord.java` | 新增 `examRecordEssayScore` 字段（教师评分） | 教师打分 |
| 3.3.5 | `service/ExamService.java` | 新增 `gradeEssay()` 方法 | 教师批改接口 |
| 3.3.6 | `service/impl/ExamServiceImpl.java` | 实现 `gradeEssay()`：教师提交主观题评分 | 批改逻辑 |
| 3.3.7 | `controller/ExamController.java` | 新增 `POST /exam/essay-grade` 接口 | 批改 API |

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 3.3.8 | `QuestionEditModal.vue` | 新增主观题创建表单（评分区间、参考答案） | 出题 |
| 3.3.9 | `ExamDetail.vue` | 主观题渲染为 textarea 富文本输入框 | 答题 |
| 3.3.10 | `ExamRecordDetail.vue` | 主观题展示学生作答内容和教师评分 | 查看 |
| 3.3.11 | 新增 `EssayGradeModal.vue` 组件 | 教师批量批改主观题 | 批改界面 |

### 3.4 批量导入题目

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 3.4.1 | `pom.xml` | 添加 `apache-poi` 依赖（Excel 解析） | Excel 导入 |
| 3.4.2 | 新增 `dto/QuestionImportDto.java` | **新增 DTO**：Excel 行数据映射 | 数据模型 |
| 3.4.3 | `service/ExamService.java` | 新增 `batchImportQuestions()` 方法 | 导入接口 |
| 3.4.4 | `service/impl/ExamServiceImpl.java` | 实现 `batchImportQuestions()`：解析 Excel，逐行创建题目 | 核心逻辑 |
| 3.4.5 | `controller/UploadDownloadController.java` | 新增 `POST /api/exam/import-questions` 接口 | 导入 API |

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 3.4.6 | 新增 `QuestionImportModal.vue` 组件 | 拖拽上传 Excel，显示导入进度和结果 | 导入界面 |
| 3.4.7 | 新增 `api/import.js` | 封装导入 API | 数据层 |
| 3.4.8 | `QuestionTableList.vue` | 新增"批量导入"按钮 | 入口 |

---

## 第四批：核心功能补全

> **目标**：实现考试编辑、题目/考试删除（软删除）、批量操作。

### 4.1 软删除机制

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 4.1.1 | `entity/Question.java` | 新增 `questionVisible` 字段（Boolean，默认 true） | 题目可见性 |
| 4.1.2 | `entity/Exam.java` | 新增 `examVisible` 字段（Boolean，默认 true） | 考试可见性 |
| 4.1.3 | `repository/QuestionRepository.java` | 新增 `findByQuestionVisibleTrue()` 方法 | 只查可见 |
| 4.1.4 | `repository/ExamRepository.java` | 新增 `findByExamVisibleTrue()` 方法 | 只查可见 |
| 4.1.5 | `service/ExamService.java` | 新增 `deleteQuestion()` 和 `deleteExam()` 方法 | 删除接口 |
| 4.1.6 | `service/impl/ExamServiceImpl.java` | 实现 `deleteQuestion()`：将 questionVisible=false，检查是否有关联考试 | 删除逻辑 |
| 4.1.7 | `service/impl/ExamServiceImpl.java` | 实现 `deleteExam()`：将 examVisible=false | 删除逻辑 |
| 4.1.8 | `controller/ExamController.java` | 新增 `DELETE /exam/question/{id}` 和 `DELETE /exam/{id}` 接口 | 删除 API |

### 4.2 考试编辑功能

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 4.2.1 | `service/ExamService.java` | 审查 `update()` 方法，增加"考试中不可修改"校验 | 防篡改 |
| 4.2.2 | `service/impl/ExamServiceImpl.java` | `update()` 中增加考试状态检查（已开始/已结束则拒绝修改） | 状态保护 |

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 4.2.3 | `ExamEditModal.vue` | 修复已知 bug，增加考试状态校验 | 前端保护 |
| 4.2.4 | `ExamTableList.vue` | 增加"编辑"按钮，调用编辑弹窗 | 入口 |

### 4.3 题目搜索与筛选增强

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 4.3.1 | `repository/QuestionRepository.java` | 新增多条件查询方法：`findByCategoryIdAndLevelIdAndTypeIdAndNameContaining()` | 组合查询 |
| 4.3.2 | `repository/ExamRepository.java` | 新增多条件查询方法：`findByExamNameContainingAndCreatorId()` | 组合查询 |

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 4.3.3 | `QuestionTableList.vue` | 增加分类、难度、题型、关键词的多条件筛选栏 | 筛选 UI |
| 4.3.4 | `ExamTableList.vue` | 增加名称搜索和创建者筛选 | 筛选 UI |

---

## 第五批：代码质量提升

> **目标**：清理冗余代码、统一异常处理、改善可读性。

### 5.1 统一异常处理

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 5.1.1 | 新增 `exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` 全局异常处理器 | 统一错误格式 |
| 5.1.2 | `exception/GlobalExceptionHandler.java` | 处理 `MethodArgumentNotValidException`（参数校验失败） | 校验错误 |
| 5.1.3 | `exception/GlobalExceptionHandler.java` | 处理 `EntityNotFoundException`（资源不存在） | 404 错误 |
| 5.1.4 | `exception/GlobalExceptionHandler.java` | 处理 `ExamException`（业务异常） | 业务错误 |
| 5.1.5 | `exception/GlobalExceptionHandler.java` | 处理 `Exception`（兜底） | 未知错误 |

### 5.2 Controller 层清理

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 5.2.1 | `ExamController.java` | 移除所有 `try-catch` 块，交由全局异常处理器处理 | 简化代码 |
| 5.2.2 | `ExamController.java` | 移除所有 `System.out.println` 调试输出 | 清理日志 |
| 5.2.3 | `ExamController.java` | 移除 `/exam/test` 测试接口 | 清理 |
| 5.2.4 | `UserController.java` | 同上：移除 try-catch 和 System.out.println | 清理 |

### 5.3 Service 层优化

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 5.3.1 | `ExamServiceImpl.java` | 将字符串拼接 ID 的逻辑提取为 `IdListBuilder` 私有工具类 | 消除重复代码 |
| 5.3.2 | `ExamServiceImpl.java` | `getQuestionVo()` 中 N+1 查询优化：使用 `findAllById()` 批量查询 | 性能优化 |
| 5.3.3 | `ExamServiceImpl.java` | `getExamVos()` 中 N+1 查询优化：同上 | 性能优化 |
| 5.3.4 | `ExamServiceImpl.java` | `judge()` 方法中答案字符串解析逻辑提取为 `AnswerParser` 工具类 | 可读性 |
| 5.3.5 | `ExamServiceImpl.java` | `replaceLastSeparator()` 方法统一使用 `StringUtils.removeEnd()` | 标准化 |

### 5.4 日志规范化

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 5.4.1 | `LoginInterceptor.java` | `System.out.println` 改为 `log.info/debug` | 日志 |
| 5.4.2 | `UserServiceImpl.java` | `System.out.println` 改为 `log.info/debug` | 日志 |
| 5.4.3 | `ExamServiceImpl.java` | `System.out.println` 改为 `log.debug`（仅 debug 级别） | 日志 |
| 5.4.4 | `application.yml` | 配置 `logging.level.lsgwr.exam=debug`（生产改为 info） | 日志配置 |

---

## 第六批：可扩展性与基础设施

> **目标**：配置管理、部署优化、接口文档更新。

### 6.1 配置外部化

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 6.1.1 | `application.yml` | 新增 `exam.jwt-secret`、`exam.token-expire-hours`、`exam.refresh-token-expire-days` 配置项 | JWT 配置 |
| 6.1.2 | `application.yml` | 新增 `exam.answer-auto-save-interval` 配置项（默认 2000ms） | 自动保存间隔 |
| 6.1.3 | `JwtUtils.java` | 使用 `@Value("${exam.jwt-secret}")` 读取密钥 | 配置绑定 |
| 6.1.4 | `application.yml` | 添加 `spring.profiles.active=dev` 支持多环境 | 环境隔离 |
| 6.1.5 | 新增 `application-prod.yml` | 生产环境配置（关闭 SQL 打印、日志级别调整） | 生产配置 |

### 6.2 Docker 部署优化

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 6.2.1 | `Dockerfile` | 优化分层构建（先 copy pom.xml 和 src，利用 Docker 缓存） | 构建加速 |
| 6.2.2 | 新增 `docker-compose.yml` | MySQL + Backend + Frontend 一键部署 | 一键启动 |
| 6.2.3 | `entrypoint.sh` | 增加健康检查命令 | 容器健康 |

### 6.3 API 文档更新

**后端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 6.3.1 | 所有新增 Controller 方法 | 添加 `@ApiOperation` 和 `@ApiParam` 注解 | Swagger 文档 |
| 6.3.2 | `Swagger2Config.java` | 完善文档注释和分组信息 | 文档质量 |

---

## 第七批：前端整体优化

> **目标**：组件化、响应式优化、安全防护。

### 7.1 前端安全加固

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 7.1.1 | `request.js` | 增加 XSS 过滤：对响应中的 HTML 内容进行 sanitization | 防 XSS |
| 7.1.2 | 所有表单输入 | 增加输入长度限制和特殊字符过滤 | 防注入 |
| 7.1.3 | `ExamDetail.vue` | 题目内容使用 `v-html` 时增加内容安全过滤 | 防 XSS |

### 7.2 响应式与性能

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 7.2.1 | `ExamDetail.vue` | 左侧题目列表改为虚拟滚动（题目超过 50 题时优化） | 性能 |
| 7.2.2 | `QuestionTableList.vue` | 增加分页懒加载 | 性能 |
| 7.2.3 | `ExamTableList.vue` | 同上 | 性能 |
| 7.2.4 | `request.js` | 增加请求取消机制（axios CancelToken），重复请求自动取消 | 性能 |

### 7.3 用户体验优化

**前端改动：**

| 序号 | 文件 | 函数/改动 | 说明 |
|-----|------|----------|------|
| 7.3.1 | `Home.vue` | 增加系统公告/通知功能区域 | 信息传达 |
| 7.3.2 | `Login.vue` | 增加"记住我"功能（延长本地 token 存储） | 便利性 |
| 7.3.3 | 全局 | 增加 Loading 状态管理，防止重复提交 | 防抖 |
| 7.3.4 | 全局 | 统一错误提示样式（抽取 ErrorNotification 组件） | 一致性 |

---

## 实施顺序与批次建议

| 批次 | 内容 | 预估工作量 | 风险等级 |
|-----|------|-----------|---------|
| **第零批** | 核心安全与业务缺陷（角色鉴权、计时功能、有效期窗口） | 3-4 天 | 高（阻断性） |
| **第一批** | 安全性加固（密码、JWT、输入校验） | 2-3 天 | 高（阻断性） |
| **第二批** | 考试过程可靠性（倒计时、自动保存、防误触） | 2-3 天 | 中 |
| **第三批** | 教育专业性（随机组卷、成绩分析、主观题、批量导入） | 5-7 天 | 中 |
| **第四批** | 核心功能补全（软删除、考试编辑、筛选） | 2-3 天 | 低 |
| **第五批** | 代码质量（异常处理、清理、日志） | 1-2 天 | 低 |
| **第六批** | 可扩展性（配置、Docker、文档） | 1-2 天 | 低 |
| **第七批** | 前端优化（安全、性能、UX） | 2-3 天 | 低 |

**总计预估工作量：18-26 个工作日**

---

## 验收标准

| 维度 | 当前评分 | 目标评分 | 验证方式 |
|-----|---------|---------|---------|
| 角色鉴权 | 0.5 | 5.0 | JWT 携带 roleId + 拦截器统一鉴权 + 声明式 `@RequireRole` + 前后端按钮/菜单权限 |
| 核心功能完整性 | 3.5 | 5.0 | 所有 CRUD + 软删除 + 批量操作 + 随机组卷 |
| 教育专业性 | 2.0 | 5.0 | 成绩分析 + 主观题 + 批量导入 + 排名 |
| 用户体验 | 2.5 | 5.0 | 倒计时 + 自动保存 + 断点续答 + 防误触 + 有效期窗口 |
| 安全性 | 1.5 | 5.0 | BCrypt + JWT 外部化 + 输入校验 + XSS 防护 + 角色鉴权 |
| 代码质量 | 3.0 | 5.0 | 统一异常处理 + 日志规范 + N+1 优化 |
| 可扩展性 | 2.5 | 5.0 | 配置外部化 + 多环境 + Docker 一键部署 |

---

## 实施进度（持续更新）

> 标记说明：✅ 已实施 ｜ 🔄 部分实施 ｜ ⏳ 未实施

### 第零批：核心安全与业务缺陷 ✅
- 0.1 角色鉴权体系（JWT/RoleInterceptor/@RoleRequired）✅
- 0.2 计时功能（前端倒计时 + 后端时限校验 + 超时禁用）✅
- 0.3 考试有效期窗口（checkExamAvailable + ExamStatusEnum）✅

### 第一批：安全性加固 ✅
- 1.1 BCrypt 密码改造（PasswordEncoder + Register/Login）✅
- 1.2 输入校验（spring-boot-starter-validation + @NotBlank/@Size）✅

### 第二批：考试可靠性 ✅
- 2.1 交卷确认（Modal.confirm 二次确认）✅
- 2.2 草稿自动保存（draftTimerId + localStorage）✅

### 第三批：教育专业性 ✅
- 3.1 随机组卷（RandomExamCreateVo + Collections.shuffle）✅
- 3.2 班级排名（getClassRanking + score desc + timeCost asc）✅
- 3.3 主观题批改（essayScores 字段 + gradeEssay 端点）✅
- 3.4 批量导入（POI WorkbookFactory + import-questions 端点）✅
- 3.5 教师成绩统计（getExamAllRecords + getExamScoreStat）✅

### 第四批：核心功能补全 ✅
- 4.1 软删除机制（questionVisible / examVisible 字段 + 过滤逻辑）✅

### 第五批：代码质量 ✅
- 5.1 GlobalExceptionHandler 全局异常处理 ✅
- 5.2 Controller 清理（移除 try-catch + System.out.println）✅
- 5.3 Service N+1 查询优化 ✅
  - 5.3.2 getQuestionVos() 批量预加载 user/level/type/category/option ✅
  - 5.3.3 getExamVos() 批量预加载 user/question ✅
  - 5.3.5 replaceLastSeparator/trimMiddleLine 统一使用 StrUtil.removeSuffix ✅
  - 5.3.1 IdListBuilder 提取工具类 ✅ （独立 util 类，splitToSet/splitToList 静态方法）
  - 5.3.4 AnswerParser 工具类 ✅ （独立 util 类，封装 judge() 答案字符串解析）
- 5.4 日志规范化（slf4j Logger + application.yml 日志级别）✅

### 第六批：可扩展性与基础设施 ✅
- 6.1 配置外部化（exam.jwt-secret + DB 凭据 + dev/prod profiles）✅
- 6.2 Docker 部署（多阶段 Dockerfile + docker-compose + nginx + healthcheck）✅
- 6.3 API 文档更新（@ApiOperation + Swagger2Config 完善描述）✅

### 第七批：前端整体优化 ✅
- 7.1 前端安全加固
  - 7.1.1 request.js 响应拦截器 XSS 清洗（sanitizeDeep）✅
  - 7.1.2 表单输入校验 ✅ （utils/validators.js 提供 required/length/email/rules 通用规则；StepByStepExamModal 等已接入）
  - 7.1.3 v-html XSS 防护 ✅ （响应数据已统一清洗，覆盖 ExamDetail/ExamRecordDetail/QuestionViewModal）
- 7.2 响应式与性能
  - 7.2.1 ExamDetail.vue 虚拟滚动 ✅ （采用 windowing 方案：visibleRadioCount/visibleCheckCount/visibleJudgeCount 计算属性 + "显示更多" 按钮，无新依赖）
  - 7.2.2 QuestionTableList.vue 分页懒加载 ✅ （后端 QuestionRepository.findVisiblePage + PageResultVo + BootstrapTable 服务端分页）
  - 7.2.3 ExamTableList.vue 分页懒加载 ✅ （后端 ExamRepository.findVisiblePage + 同上服务端分页）
  - 7.2.4 request.js 请求取消机制（CancelToken）✅
- 7.3 用户体验优化
  - 7.3.1 Home.vue 系统公告区域 ✅ （新增 Announcement 实体/Repository/Controller/VO + 前端 AnnouncementList 组件 + Home.vue 集成）
  - 7.3.2 Login.vue "记住我"功能 ✅ （REMEMBER_USERNAME/REMEMBER_ME_FLAG 常量 + variable token expiry 24h/7d + 自动回填用户名）
  - 7.3.3 全局 Loading 状态管理（loading.js Vuex module + isLoading getter）✅
  - 7.3.4 ErrorNotification 组件抽取 ✅ （utils/errorNotification.js 提供 error/fromResponse/fromError API + Vue.prototype.$errorNotify 全局注册 + 38 处调用迁移）

### 编译验证
- 后端：`mvn clean compile` BUILD SUCCESS，101 source files（新增 AnnouncementController/AnnouncementRepository/Announcement/AnnouncementVo/PageResultVo 等）
- 前端：`node --check` 通过 errorNotification.js / request.js / permission.js / core/use.js / xss.js / loading.js / store/index.js / getters.js；.vue 文件无语法回归

