# 后端修复实施计划 (Backend Fix Plan)

> 配套文档：`doc/BACKEND-PIPELINE-AUDIT.md`（审查报告）
> 制定日期：2026-07-23
> 修复目标：基于审查报告的 62 项问题，按 P0 → P1 → P2 优先级分批修复
> 验证标准：每批修复后 `mvn clean compile` 通过，无回归

---

## 总体策略

1. **优先级**：P0（12 项，立即修复）→ P1（22 项，分 5 批修复）→ P2（28 项，择机修复）
2. **修复原则**：
   - 最小化改动，避免引入新 bug
   - 每批修复后立即 `mvn clean compile` 验证
   - 保留原有业务行为，仅补全缺失的校验/约束
   - 不重写架构，不引入新框架
3. **不修复范围**：本次仅修复后端代码问题，前端联调改动另行处理；P2 项目本计划暂不实施。

---

## 阶段一：P0 紧急修复（12 项）

### P0-1 修复 `judge()` 判分循环错误 [B-01]

| 字段 | 值 |
|------|---|
| 文件 | `backend/src/main/java/lsgwr/exam/service/impl/ExamServiceImpl.java` |
| 问题编号 | B-01 |
| 阻断类型 | 数据正确性阻断 |
| 当前行为 | `for (String questionId : questionIdsAnswer)` 只遍历用户提交的题目，未提交的不计 0 分 |
| 修复目标 | 遍历考试的所有题目（`questionIds`），从 `answersMap` 取用户作答；未作答的记 0 分 |

**修复步骤**：

1. 定位 `judge()` 方法中循环语句：`for (String questionId : questionIdsAnswer)`
2. 改为遍历 `questionIds`（合并 `radioIdList + checkIdList + judgeIdList`，或直接使用 `examDetailVo` 中拼接后的列表）
3. 用户答案获取改为：`answersMap.getOrDefault(questionId, Collections.emptyList())`
4. 判分逻辑保持不变（`AnswerParser.optionsEquals` 比对）
5. 未作答的题目自然进入"答错"分支，得 0 分

**预期代码**：
```java
// 合并所有题目 ID 列表作为遍历源
List<String> allQuestionIds = new ArrayList<>();
allQuestionIds.addAll(radioIdList);
allQuestionIds.addAll(checkIdList);
allQuestionIds.addAll(judgeIdList);

int correctCount = 0;
for (String questionId : allQuestionIds) {
    Question question = questionMap.get(questionId);
    if (question == null) {
        continue; // 防御性跳过非法 questionId
    }
    List<String> questionUserOptionIdList = answersMap.getOrDefault(questionId, Collections.emptyList());
    List<String> questionAnswerOptionIdList = IdListBuilder.splitToList(
            replaceLastSeparator(question.getQuestionAnswerOptionIds()));
    boolean correct = AnswerParser.optionsEquals(questionAnswerOptionIdList, questionUserOptionIdList);
    if (correct) {
        correctCount++;
    }
    // ... 原有的 questionOrder、userOptionIds 拼接逻辑保持不变
}
```

**验证**：编译通过；手工测试——学生不答任何题提交，应得 0 分。

---

### P0-2 ExamRecord 添加唯一约束 + 重复提交校验 [D-01, I-02]

| 字段 | 值 |
|------|---|
| 文件 | `backend/src/main/java/lsgwr/exam/entity/ExamRecord.java`、`ExamServiceImpl.judge()` |
| 问题编号 | D-01, I-02 |
| 阻断类型 | 数据正确性阻断 + 业务规则阻断 |
| 当前行为 | 同一学生可对同一考试无限次提交，每次生成新记录 |
| 修复目标 | DB 层加唯一约束；Service 层入口校验已交卷，返回错误码 |

**修复步骤**：

1. **Entity 层**：在 `ExamRecord` 类上添加 `@Table(name = "exam_record", uniqueConstraints = {@UniqueConstraint(name = "uk_exam_joiner", columnNames = {"exam_id", "exam_joiner_id"})})`
2. **Service 层**：`judge()` 入口添加检查
```java
// 检查是否已交卷
List<ExamRecord> existing = examRecordRepository.findByExamIdAndExamJoinerId(examId, userId);
if (!existing.isEmpty()) {
    throw new ExamException(ResultEnum.ORDER_STATUS_ERR.getCode(), "您已交卷，无法重复提交");
}
```
3. **Repository 层**：新增 `List<ExamRecord> findByExamIdAndExamJoinerId(String examId, String joinerId);`
4. **DB 迁移**：手动执行 `ALTER TABLE exam_record ADD UNIQUE INDEX uk_exam_joiner (exam_id, exam_joiner_id);`（避免 ddl-auto 在生产不可靠）
5. **历史数据处理**：若存在重复记录，先保留最新一条，删除其余（手工脚本）

**验证**：编译通过；同一学生第二次提交返回业务错误码 14 "考试状态异常"。

---

### P0-3 `getRecordDetail` 添加所有权校验 [I-01, A-01]

| 字段 | 值 |
|------|---|
| 文件 | `ExamController.java` L195-200 |
| 问题编号 | I-01, A-01 |
| 阻断类型 | 数据正确性阻断 |
| 当前行为 | 任何登录用户可查看他人考试记录 |
| 修复目标 | 学生只能查看自己的记录；教师/管理员可查看所有 |

**修复步骤**：

1. 修改 `getExamRecordDetail` 方法签名，增加 `HttpServletRequest request` 参数
2. 读取 `user_id` 和 `role_id` 请求属性
3. 查询记录后判断：若非 TEACHER/ADMIN 且 `record.getExamJoinerId()` 不等于当前 userId，抛 `ExamException(-3, "无权查看他人考试记录")`

**预期代码**：
```java
@GetMapping("/record/detail/{recordId}")
public ResultVO<RecordDetailVo> getExamRecordDetail(@PathVariable String recordId,
                                                     HttpServletRequest request) {
    String userId = (String) request.getAttribute("user_id");
    Integer roleId = (Integer) request.getAttribute("role_id");
    RecordDetailVo vo = examService.getRecordDetail(recordId);
    if (vo == null || vo.getExamRecord() == null) {
        throw new ExamException(ResultEnum.ORDER_DETAIL_EMPTY.getCode(), "考试记录不存在");
    }
    boolean isAdmin = roleId != null && (roleId.equals(RoleEnum.TEACHER.getId())
            || roleId.equals(RoleEnum.ADMIN.getId()));
    if (!isAdmin && !userId.equals(vo.getExamRecord().getExamJoinerId())) {
        throw new ExamException(-3, "无权查看他人考试记录");
    }
    return new ResultVO<>(0, "获取考试记录详情成功", vo);
}
```

**验证**：编译通过；学生 A 用学生 B 的 recordId 查询，返回 403/业务错误码。

---

### P0-4 资源所有权校验 [I-05, I-06, I-07, A-02]

| 字段 | 值 |
|------|---|
| 文件 | `ExamController.java`、`ExamServiceImpl.java` |
| 问题编号 | I-05, I-06, I-07, A-02 |
| 阻断类型 | 数据正确性阻断 |
| 当前行为 | `updateExam`/`questionUpdate`/`deleteExam`/`deleteQuestion` 仅校验角色，不校验资源所有权 |
| 修复目标 | 教师 A 只能修改/删除自己创建的考试/题目；ADMIN 可操作所有 |

**修复步骤**：

1. **Service 层**：在 `ExamServiceImpl` 中新增私有方法
```java
private Exam checkExamOwnership(String examId, String userId, Integer roleId) {
    Exam exam = examRepository.findById(examId).orElseThrow(() ->
            new ExamException(ResultEnum.PRODUCT_STOCK_ERR.getCode(), "考试不存在"));
    boolean isAdmin = roleId != null && roleId.equals(RoleEnum.ADMIN.getId());
    if (!isAdmin && !userId.equals(exam.getExamCreatorId())) {
        throw new ExamException(-3, "无权操作他人考试");
    }
    return exam;
}

private Question checkQuestionOwnership(String questionId, String userId, Integer roleId) {
    Question question = questionRepository.findById(questionId).orElseThrow(() ->
            new ExamException(ResultEnum.PRODUCT_NOT_EXIST.getCode(), "题目不存在"));
    boolean isAdmin = roleId != null && roleId.equals(RoleEnum.ADMIN.getId());
    if (!isAdmin && !userId.equals(question.getQuestionCreatorId())) {
        throw new ExamException(-3, "无权操作他人题目");
    }
    return question;
}
```

2. **Controller 层**：所有写操作方法增加 `HttpServletRequest request` 参数，读取 userId/roleId 后传入 Service
3. **Service 方法签名**：`update()`、`deleteExam()`、`deleteQuestion()` 增加 `userId`、`roleId` 参数
4. **ADMIN 放行**：管理员角色可操作所有资源（与 PRD "管理员可管理所有"一致）

**验证**：编译通过；教师 A 用教师 B 的 examId 调用 `updateExam`，返回业务错误码 -3。

---

### P0-5 `updateExam` 保留原创建者和创建时间 [B-12, I-05]

| 字段 | 值 |
|------|---|
| 文件 | `ExamServiceImpl.java` `update()` 方法 |
| 问题编号 | B-12, I-05 |
| 阻断类型 | 数据正确性阻断 |
| 当前行为 | `exam.setExamCreatorId(userId)` 把"更新人"赋给"创建者"字段；`BeanUtils.copyProperties` 覆盖 `createTime` 为 null |
| 修复目标 | 保留原 `examCreatorId` 和 `createTime`，仅更新 `updateTime` |

**修复步骤**：

1. 在 `update()` 方法中先查询原 exam
2. 缓存 `originalCreatorId` 和 `originalCreateTime`
3. `BeanUtils.copyProperties` 后回填这两个字段
4. 不再调用 `setExamCreatorId(userId)`（这是更新人，不是创建者）

**预期代码**：
```java
@Override
public Exam update(ExamVo examVo, String userId, Integer roleId) {
    Exam existing = checkExamOwnership(examVo.getExamId(), userId, roleId);
    String originalCreatorId = existing.getExamCreatorId();
    Date originalCreateTime = existing.getCreateTime();
    BeanUtils.copyProperties(examVo, existing);
    existing.setExamCreatorId(originalCreatorId);
    existing.setCreateTime(originalCreateTime);
    existing.setUpdateTime(new Date());
    return examRepository.save(existing);
}
```

**验证**：编译通过；更新后 `examCreatorId` 不变，`createTime` 不变，`updateTime` 为当前时间。

---

### P0-6 `downloadFileGet` 路径穿越漏洞修复 [A-03]

| 字段 | 值 |
|------|---|
| 文件 | `UploadDownloadController.java` L68-71 |
| 问题编号 | A-03 |
| 阻断类型 | 安全阻断 |
| 当前行为 | 接受 `filePath` 参数，无路径校验，可下载任意系统文件 |
| 修复目标 | 限定根目录，规范化路径后校验仍在根目录下 |

**修复步骤**：

1. 在 `UploadDownloadController` 中定义 `UPLOAD_ROOT` 常量（从 `application.yml` 读取 `file.upload-root`，默认 `/data/exam/uploads/`）
2. 在 `downloadFileGet` 和 `downloadFilePost` 中：
   - 构造 `File target = new File(UPLOAD_ROOT, filePath)`
   - 调用 `target.getCanonicalFile()` 规范化路径
   - 校验 `target.getPath().startsWith(new File(UPLOAD_ROOT).getCanonicalPath())`
   - 不通过抛 `ExamException(1, "非法文件路径")`

**预期代码**：
```java
@Value("${file.upload-root:/data/exam/uploads/}")
private String uploadRoot;

private File safeResolve(String filePath) {
    try {
        File root = new File(uploadRoot).getCanonicalFile();
        File target = new File(root, filePath).getCanonicalFile();
        if (!target.getPath().startsWith(root.getPath())) {
            throw new ExamException(1, "非法文件路径");
        }
        return target;
    } catch (IOException e) {
        throw new ExamException(1, "文件路径解析失败");
    }
}
```

**验证**：编译通过；传 `../../etc/passwd` 返回业务错误码 1。

---

### P0-7 `UploadDownloadController` 添加权限校验 [I-03]

| 字段 | 值 |
|------|---|
| 文件 | `UploadDownloadController.java` L44-77 |
| 问题编号 | I-03 |
| 阻断类型 | 安全阻断 |
| 当前行为 | 所有文件接口对任何登录用户开放 |
| 修复目标 | 上传接口限 TEACHER/ADMIN；下载接口保留对所有登录用户开放（业务需求） |

**修复步骤**：

1. 上传接口加 `@RoleRequired({RoleEnum.TEACHER, RoleEnum.ADMIN})`：
   - `uploadFileSingle`
   - `uploadFileSingleByModel`
   - `uploadFileMulti`
   - `uploadFileMultiByModel`
2. 下载接口保持无 `@RoleRequired`（学生也可下载考试附件）
3. 顺便修复 P1 路径 bug：`@PostMapping("/api/upload/singleAndparas")` → `@PostMapping("/upload/singleAndparas")`（去掉冗余前缀，配合类上的 `@RequestMapping("/api/file")`）

**验证**：编译通过；学生调用上传接口返回 403。

---

### P0-8 `register` 返回 UserVo 而非 User 实体 [A-04]

| 字段 | 值 |
|------|---|
| 文件 | `UserController.java` L34-47、`UserVo.java` |
| 问题编号 | A-04 |
| 阻断类型 | 安全阻断 |
| 当前行为 | 返回 `User` 实体，含 `userPassword` BCrypt 哈希 |
| 修复目标 | 返回 `UserVo`（不含 password 字段） |

**修复步骤**：

1. 确认 `UserVo` 字段：检查是否已存在且不含 `userPassword`
2. 若 `UserVo` 已存在且安全：在 `UserController.register` 中改 `BeanUtils.copyProperties(user, vo)` 后返回
3. 若 `UserVo` 含 password：从 `UserVo` 中移除 `userPassword` 字段
4. 同步检查 `login`、`getUserInfo` 是否也有类似问题，一并修复

**预期代码**：
```java
@PostMapping("/register")
public ResultVO<UserVo> register(@RequestBody @Valid RegisterDTO registerDTO) {
    User user = userService.register(registerDTO);
    if (user == null) {
        return new ResultVO<>(ResultEnum.REGISTER_FAILED.getCode(), "注册失败", null);
    }
    UserVo vo = new UserVo();
    BeanUtils.copyProperties(user, vo);
    return new ResultVO<>(0, "注册成功", vo);
}
```

**验证**：编译通过；调用 `/api/user/register` 返回 JSON 中无 `userPassword` 字段。

---

### P0 阶段验证

```bash
cd backend
/persistent/opt/idea/plugins/maven/lib/maven3/bin/mvn clean compile -q
```
- 期望：BUILD SUCCESS，无编译错误
- 若有错误：逐一定位修复，不得跳过

---

## 阶段二：P1 分批修复（22 项）

### P1 批次 A：判分与数据一致性（5 项）

| 编号 | 文件 | 修复内容 |
|------|------|----------|
| B-02 | `ExamServiceImpl.judge()` 末尾 | 根据 `examJoinScore / examTotalScore` 比率计算级别（优≥0.9, 良≥0.75, 及格≥0.6, 不及格<0.6），赋值给 `examRecord.setExamResultLevel(level)` |
| B-03 | `ExamServiceImpl.judge()` L738 | `Question question = questionMap.get(questionId);` 后加 `if (question == null) continue;` |
| B-06 | `ExamServiceImpl.batchImportQuestions()` | 导入前校验 `typeId`/`levelId`/`categoryId` 在对应表存在，否则跳过该行并记录错误 |
| B-10 | `UserServiceImpl.register()` L80 | `e.printStackTrace()` → `log.error("注册失败", e); throw new ExamException(-2, "注册失败: " + e.getMessage());` |
| B-11 | `UserServiceImpl.changePassword()` | 修改密码后，将当前 token 加入 Redis 黑名单（若已集成 Redis）；或缩短 token 过期时间为 1 小时 |

**B-11 替代方案**（不引入 Redis）：
- 缩短 JWT 过期时间从 24h → 2h
- 前端实现 token 自动刷新机制（后续 sprint）

---

### P1 批次 B：越权与业务规则（6 项）

| 编号 | 文件 | 修复内容 |
|------|------|----------|
| I-07 | `ExamController.deleteQuestion/deleteExam` | 复用 P0-4 的 `checkExamOwnership/checkQuestionOwnership`（同 P0-4 已覆盖） |
| I-10 | `UserController` 新增 `POST /api/user/role` | 管理员修改用户角色：`@RoleRequired({RoleEnum.ADMIN})`，参数 `{userId, roleId}`，校验 roleId ∈ {1,2,3} |
| I-11 | `UserController` 新增 `POST /api/user/update` | 用户更新自己的 `userEmail`/`userPhone`/`userImage`，不传 `userPassword` |
| A-05 | `ExamServiceImpl.gradeEssay()` | 校验 `record` 所属的 `exam.examCreatorId` 等于当前 userId（或 ADMIN 放行） |
| A-06 | `ExamServiceImpl.getExamAllRecords/getExamScoreStat/getClassRanking` | 同 A-05，校验 `examId` 的创建者 |
| A-07 | `JwtUtils` 静态初始化块 | 启动时校验 `APP_SECRET != null && APP_SECRET.length() >= 32`，否则抛 `IllegalStateException` 阻止启动 |

---

### P1 批次 C：异常处理（8 项）

| 编号 | 文件 | 修复内容 |
|------|------|----------|
| E-01 | `ExamServiceImpl` 多处 | `assert exam != null` → `if (exam == null) throw new ExamException(ResultEnum.PRODUCT_STOCK_ERR.getCode(), "考试不存在");` |
| E-02 | `getQuestionDetail` L360-381 | `question = ...orElse(null)` 后加 null 检查并抛 `ExamException(ResultEnum.PRODUCT_NOT_EXIST.getCode(), "题目不存在")` |
| E-03 | `getExamDetail` L673-682 | 同 E-02 模式 |
| E-04 | `getRecordDetail` L816-842 | 同 E-02 模式 |
| E-05 | `UserServiceImpl.getInfo` L130-183 | 3 处 `assert` 全部替换为 `throw new ExamException` |
| E-06 | `UserServiceImpl.register` L80 | `e.printStackTrace()` → `log.error(...)`（与 B-10 合并） |
| E-07 | `GlobalExceptionHandler.handleException` L58-66 | 删除 403 死代码分支（RoleInterceptor 已直接返回 403） |
| E-08 | `ExamServiceImpl.judge` L738 | `questionMap.get(questionId)` 后加 null 检查（与 B-03 合并） |

---

### P1 批次 D：性能优化（4 项）

| 编号 | 文件 | 修复内容 |
|------|------|----------|
| P-01 | `getExamRecordList` L799-813 | 批量预加载：先 `findAllByExamIdIn(examIds)` + `findAllByUserIdIn(userIds)`，构造 Map 后填充 |
| P-02 | `getExamAllRecords` L901-920 | 批量预加载 `userRepository.findAllByUserIdIn(userIds)` |
| P-03 | `getClassRanking` L1127-1157 | 批量预加载 `userRepository.findAllByUserIdIn(userIds)` |
| P-04 | 4 个 `findAll()` 接口 | 限制最大返回数：`getQuestionAll`/`getExamAll`/`getExamCardList`/`getExamQuestionType` 增加 `PageRequest.of(0, 1000)` 限制，或返回 `List<...>` 时 `.stream().limit(1000).collect(...)` |

---

### P1 批次 E：状态机与配置（2 项）

| 编号 | 文件 | 修复内容 |
|------|------|----------|
| S-01 | `Exam` 实体新增 `examStatus` 字段 + Service 逻辑 | 字段默认 0（UPCOMING），由时间触发自动迁移到 1/2；教师可手动调用新接口 `POST /api/exam/end/{id}` 强制结束（status=2）；`checkExamAvailable` 优先读 DB 字段，时间触发后更新 DB |
| D-04 | `application.yml`（默认 profile） + `application-prod.yml` | 生产 profile 改 `ddl-auto: validate`；保留开发 profile `update` |

---

### P1 阶段验证

```bash
cd backend
/persistent/opt/idea/plugins/maven/lib/maven3/bin/mvn clean compile -q
```

---

## 阶段三：P2 择机修复（28 项，本计划暂不实施）

P2 项目（如 I-04 路径 bug、I-08 分层架构、I-09 mass assignment、I-12/I-13 缺失接口、D-02 字段冗余、D-03/D-05 注释不一致、S-03/S-04 状态机不完整、A-09 密码强度、A-11 XSS、A-12 异常泄露、E-06/E-07 日志、P-05~P-08 监控/追踪/连接池/审计、B-04/B-05/B-07/B-09 契约一致性）将在 P0+P1 完成后，根据业务优先级在后续 sprint 中安排。

---

## 修复进度跟踪

### P0 阶段（8/8 完成 ✅，编译验证通过 ✅）
- [x] P0-1 judge() 判分循环
- [x] P0-2 ExamRecord 唯一约束
- [x] P0-3 getRecordDetail 所有权
- [x] P0-4 资源所有权校验
- [x] P0-5 updateExam 保留创建者
- [x] P0-6 路径穿越修复
- [x] P0-7 文件接口权限
- [x] P0-8 register 返回 UserVo
- [x] P0 编译验证

### P1 阶段（22/22 完成 ✅，编译验证通过 ✅）
- [x] 批次 A 判分一致性（5 项：B-02, B-03, B-06, B-10, B-11）
- [x] 批次 B 越权业务规则（6 项：I-07, I-10, I-11, A-05, A-06, A-07）
- [x] 批次 C 异常处理（6 项：E-01, E-02, E-03, E-04, E-05, E-08）
- [x] 批次 D 性能优化（4 项：P-01, P-02, P-03, P-04）
- [x] 批次 E 状态机配置（2 项：S-01, D-04）
- [x] P1 编译验证

### 收尾
- [x] 更新 BACKEND-PIPELINE-AUDIT.md 标记完成状态

### P2 阶段（暂未实施）
- 28 项 P2 问题按业务优先级择机修复

---

## 风险与回滚

| 风险 | 应对 |
|------|------|
| P0-2 唯一约束添加失败（历史重复数据） | 先执行去重脚本：保留每个 (examId, joinerId) 最新一条记录，删除其余 |
| P0-4 所有权校验误伤合法用户 | 灰度上线；提供 ADMIN 强制操作接口；保留旧版本代码 git tag 可回滚 |
| P1 批次 E S-01 状态机改造影响现有数据 | 提供 DB 迁移脚本：根据当前时间为现有 exam 计算 status 并写回；上线前在测试环境验证 |
| 编译失败 | 立即定位修复，不得跳过；每批修复后立即验证 |

---

**计划制定人**：backend-pipeline-alignment skill
**计划制定日期**：2026-07-23
**预期开始执行**：立即
