# spring-boot-online-exam

> 基于 Spring Boot 2.7 + Vue 2 + Ant Design Vue 的前后端分离在线考试系统，支持学生 / 教师 / 管理员三角色权限隔离、JWT 鉴权、考试计时、随机组卷、批量导入、教师成绩统计、班级排名、主观题批改、软删除、系统公告等完整功能。
>
> 视频讲解代码：https://www.bilibili.com/video/BV1FP4y1L7xt/
>
> 一个小伙伴做了 Python 实现，欢迎 star：https://github.com/xingxingzaixian/django-drf-online-exam

## 1. 快速体验

### 1.1 事先准备

```shell
git clone git@github.com:fssl168/spring-boot-online-exam.git
cd spring-boot-online-exam
```

### 1.2 Docker 一键部署（推荐）

项目根目录提供 `docker-compose.yml`、`Dockerfile`、`nginx.conf`，可一键拉起 MySQL + 后端 + 前端 Nginx 三件套。

```shell
# 1. 构建前端静态资源
cd frontend
npm install
npm run build
cd ..

# 2. 一键启动（MySQL 8 + 后端 Spring Boot + 前端 Nginx）
docker compose up -d --build

# 3. 查看服务状态
docker compose ps
```

启动后访问 http://ip 即可，后端 API 默认端口 `9527`，MySQL 映射到宿主机 `3307`（避免与本地 3306 冲突）。

可通过环境变量覆盖默认配置（参见 `.env` 或直接在 shell 中导出）：

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `DB_PASSWORD` | `exam_secret_2024` | MySQL root 密码 |
| `EXAM_JWT_SECRET` | `zh9f8a7s6df5gh4j3k2l1q0wertyuiopASDFGHJKL` | JWT 签名密钥（生产环境务必修改） |
| `EXAM_TOKEN_EXPIRE_HOURS` | `24` | JWT token 过期时间（小时） |

### 1.3 Linux 脚本启动

执行项目根目录下的 `start.sh`，然后访问 http://ip:80。

### 1.4 Windows 手动启动

1. 安装 JDK 8
2. 从 [Releases](https://github.com/fssl168/spring-boot-online-exam/releases) 下载最新 jar 包，或自行 `mvn package` 生成
3. 安装 MySQL 8，创建数据库 `exam`，导入 `doc/sql/exam.sql`
4. 启动：`java -jar exam.jar --spring.profiles.active=prod`
5. 访问 http://localhost:9527

### 1.5 开发模式（前后端分离本地启动）

**后端**：

```shell
cd backend
mvn spring-boot:run
# 默认端口 9527，配置文件 application.yml
```

**前端**：

```shell
cd frontend
npm install
npm run serve
# 默认端口 8000，自动代理 /api 到 http://localhost:9527
```

打开 http://localhost:8000 即可，默认账号 `admin` / `teacher` / `student`，密码均为 `admin123`。

## 2. 介绍

### 2.1 功能简介

**题型支持**：

- 单选题、多选题、判断题
- 题干与选项支持富文本（Summernote / WangEditor）
- 支持 Excel 批量导入题目（Apache POI）

**角色权限**（基于 JWT + `@RoleRequired` 注解 + `RoleInterceptor` 拦截器）：

- **学生**：参加考试、查看我的考试记录、复查答题详情
- **教师**：学生所有权限 + 创建/编辑题目 + 创建/编辑考试 + 查看班级排名 + 主观题批改 + 教师成绩统计分析
- **管理员**：教师所有权限 + 用户管理 + 学科/难度/题型管理 + 系统公告管理

**考试核心能力**：

- 考试计时：前端倒计时 + 后端时限双重校验，超时自动锁定答题并触发自动交卷
- 考试有效期窗口：`examStartDate` / `examEndDate` 服务端校验，未开始/已结束均拒绝进入
- 随机组卷：教师可指定各题型抽取数量，服务端 `Collections.shuffle` 随机出题
- 交卷二次确认：`Modal.confirm` 防止误交
- 草稿自动保存：每 30 秒写入 localStorage，刷新/误关页面可恢复
- 软删除机制：题目和考试使用 `questionVisible` / `examVisible` 字段，不实际删除，保护关联数据完整

**系统功能**：

- 系统公告：管理员发布，首页展示，支持置顶与类型分类（info/success/warning/error）
- 记住我：登录页可选 7 天免登录（token 过期时间动态调整为 24h 或 7d）
- 全局 Loading 状态管理：Vuex 模块统一调度
- 统一错误通知：`ErrorNotification` 工具收口所有 `notification.error` 调用
- XSS 防护：响应数据递归清洗 + v-html 安全渲染
- 请求取消：`CancelToken` 自动取消重复请求

### 2.2 软件架构

**前后端分离，前端组件化，便于二次开发。**

**后端技术栈**（`backend/`，101 个 Java 源文件）：

- Spring Boot 2.7.18 + JPA (Hibernate)
- MySQL 8.0 + Druid 连接池
- JWT 鉴权（`JwtUtils` + `LoginInterceptor` + `RoleInterceptor` + `@RoleRequired` 注解）
- Spring Validation（`@NotBlank` / `@Size` / `@Valid`）
- Apache POI 4.1.2（Excel 批量导入）
- Swagger2（API 文档，按模块分组）
- Hutool（工具类）
- BCrypt 密码加密（`PasswordEncoder`）
- 全局异常处理（`GlobalExceptionHandler` 统一 400/401/403/500 返回）

**前端技术栈**（`frontend/`）：

- Vue 2.7 + Vue Router 3 + Vuex 3
- Ant Design Vue 1.7.8
- Axios 1.6（封装 `request.js`，含拦截器、XSS 清洗、重复请求取消、Loading 调度）
- BootstrapVue 2 + BootstrapTable 1.21（题目/考试表格，支持服务端分页）
- vue-ls（localStorage 封装，存储 token、记住我状态）
- Summernote / WangEditor（富文本编辑）
- viser-vue（数据可视化）

### 2.3 使用教程

1. **下载代码**

   ```shell
   git clone https://github.com/fssl168/spring-boot-online-exam.git
   ```

2. **初始化数据库**

   安装 MySQL 8，新建 `exam` 数据库，导入 `doc/sql/exam.sql`。数据库连接信息在 `backend/src/main/resources/application.yml` 中，可通过环境变量 `DB_HOST` / `DB_USERNAME` / `DB_PASSWORD` / `DB_NAME` 覆盖。

3. **启动后端**

   ```shell
   cd backend
   mvn spring-boot:run
   # 或打包后启动：mvn package && java -jar target/exam-0.0.1-SNAPSHOT.jar
   ```

4. **启动前端**

   ```shell
   cd frontend
   npm install
   npm run serve
   ```

5. **访问**

   打开 http://localhost:8000，默认账号 `admin` / `teacher` / `student`，密码 `admin123`。

## 3. 功能图示

+ 1.管理题目
  + 1.1 题目列表
    > ![题目查看](doc/images/question_list.png)
  + 1.2 题目创建
    > ![题目创建](doc/images/question_create.png)
  + 1.3 题目更新
    > ![题目更新](doc/images/question_update.png)
+ 2.考试管理
  + 2.1 考试列表
    > ![考试查看](doc/images/exam_list.png)
  + 2.2 考试创建
    > ![考试创建](doc/images/exam_create.png)
  + 2.3 考试更新
    > ![考试更新](doc/images/exam_update.png)
+ 3.我的考试
  + 3.1 参加考试
    > 在"考试列表"模块点击自己想参加的考试卡片即可
    > ![参加考试1](doc/images/exam_join.png)
    > ![参加考试2](doc/images/exam_join2.png)
  + 3.2 考试记录查看
    > ![考试记录查看](doc/images/exam_detail.png)

## 4. 项目结构

```
spring-boot-online-exam/
├── backend/                    # 后端 Spring Boot 项目
│   ├── src/main/java/lsgwr/exam/
│   │   ├── annotation/         # @RoleRequired 自定义注解
│   │   ├── config/             # CORS / JWT / Swagger / 拦截器配置
│   │   ├── controller/         # REST 控制器（ExamController/UserController/AnnouncementController...）
│   │   ├── entity/             # JPA 实体（Exam/Question/User/Announcement...）
│   │   ├── enums/              # 枚举（RoleEnum/ExamStatusEnum/ResultEnum...）
│   │   ├── exception/          # GlobalExceptionHandler 全局异常
│   │   ├── interceptor/        # LoginInterceptor + RoleInterceptor
│   │   ├── repository/         # JPA Repository（含 Pageable 分页查询）
│   │   ├── service/            # 业务服务接口与实现
│   │   ├── util/               # IdListBuilder / AnswerParser 工具类
│   │   ├── utils/              # JwtUtils / FileUtils / ResultVOUtil
│   │   └── vo/                 # 视图对象（PageResultVo / ExamVo / AnnouncementVo...）
│   └── src/main/resources/
│       ├── application.yml     # 开发环境配置
│       └── application-prod.yml# 生产环境配置
├── frontend/                   # 前端 Vue 项目
│   └── src/
│       ├── api/                # 接口封装（exam.js / announcement.js / login.js...）
│       ├── components/         # 通用组件（AnnouncementList / Menu / SettingDrawer...）
│       ├── core/               # use.js（全局插件注册）/ bootstrap / directives
│       ├── layouts/            # BasicLayout / UserLayout 布局
│       ├── store/              # Vuex（user / permission / app / loading 模块）
│       ├── utils/              # request.js / errorNotification.js / validators.js / xss.js
│       └── views/              # 页面（list / user / dashboard / home / account...）
├── doc/                        # 文档与资源
│   ├── sql/                    # 数据库初始化脚本
│   ├── deploy/                 # 部署相关（nginx.conf / README）
│   ├── images/                 # 截图
│   ├── UPGRADE-PLAN-From-Codex.md  # 升级实施清单（v2.0 完成 0~7 批）
│   └── 项目分析报告.md
├── Dockerfile                  # 后端多阶段构建
├── docker-compose.yml          # MySQL + Backend + Frontend 一键部署
├── nginx.conf                  # 前端 Nginx 配置
├── build.sh / start.sh         # 构建与启动脚本
└── README.md                   # 本文档
```

## 5. 参与贡献

1. Fork 本仓库
2. 新建 `exam_xxx` 分支
3. 提交代码
4. 新建 Pull Request

## 6. 升级历程（v2.0）

本项目经过 7 个批次的系统性升级，详细清单见 [doc/UPGRADE-PLAN-From-Codex.md](doc/UPGRADE-PLAN-From-Codex.md)。

| 批次 | 主题 | 关键交付 |
|------|------|---------|
| 第 0 批 | 核心安全与业务缺陷 | JWT 角色鉴权（`@RoleRequired` + `RoleInterceptor`）、考试计时（前后端双重校验 + 超时自动交卷）、考试有效期窗口 |
| 第 1 批 | 安全性加固 | BCrypt 密码改造、Spring Validation 输入校验 |
| 第 2 批 | 考试可靠性 | 交卷二次确认、草稿自动保存（30s 周期 + localStorage） |
| 第 3 批 | 教育专业性 | 随机组卷、班级排名、主观题批改、Excel 批量导入、教师成绩统计 |
| 第 4 批 | 核心功能补全 | 软删除机制（`questionVisible` / `examVisible` 字段） |
| 第 5 批 | 代码质量 | `GlobalExceptionHandler` 全局异常、Controller 清理、Service N+1 查询优化、日志规范化、`IdListBuilder` / `AnswerParser` 工具类抽取 |
| 第 6 批 | 可扩展性与基础设施 | 配置外部化（dev/prod profiles + 环境变量）、Docker 多阶段构建、Swagger API 文档分组 |
| 第 7 批 | 前端整体优化 | XSS 响应清洗、表单校验规则、`ExamDetail` 题目列表 windowing、BootstrapTable 服务端分页、系统公告组件、记住我、全局 Loading、统一 `ErrorNotification` 工具、请求取消机制 |

## 7. Todo

+ `√` 0. 修复 issue 提到的 bug：题目创建失败
+ `√` 1. 考试详情编辑
+ `√` 2. 支持题目和考试的删除（采用 `visible` 字段软删除，保护关联数据）
+ `√` 3. 图片改成 base64 存到数据库中
+ `√` 4. 题干和选项支持富文本（Summernote / WangEditor）
+ `√` 5. 支持批量导入题目（Apache POI + `/import-questions` 端点）
+ `√` 6. 新增用户管理、学科管理功能
+ `√` 7. 老师能查到所有学生的成绩以及考试的统计信息（`getExamAllRecords` + `getExamScoreStat` + `ExamRecordStat` 页面）
+ `√` 8. 数据分析功能（班级排名 `ClassRankingVo` + 主观题批改 `EssayGradeVo`）
+ `√` 9. 支持容器化一键部署（`Dockerfile` + `docker-compose.yml` + `nginx.conf`）
+ 10. 支持移动端（uniapp / 适配响应式布局）
+ ......抓紧做吧，争取每周末做一点......
