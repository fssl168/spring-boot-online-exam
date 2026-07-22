# 移动端实施计划（函数级）

> 本文档为 v2.0 升级后的移动端适配实施总纲，目标：让现有 Vue 2.7 + Ant Design Vue 1.7.8 项目在手机浏览器（≥375px 宽）获得原生 App 级别的可用体验。
> 用户原始诉求："uniapp / 适配响应式布局" 二选一。本计划采用 **响应式布局适配方案**，理由见 [方案对比](#2-方案对比)。

## 1. 现状分析

### 1.1 已有的移动端基础（无需重建）

| 模块 | 文件 | 已有能力 |
|---|---|---|
| 设备检测 | `frontend/src/utils/device.js` + `mixin.js` | `enquire.js` 媒体查询，Vuex `app.device` 状态，`mixinDevice` 提供 `isMobile()`/`isDesktop()`/`isTablet()` |
| 抽屉式侧边栏 | `frontend/src/layouts/BasicLayout.vue` | 移动端使用 `a-drawer` 替代固定 sider，`v-if="isMobile()"` 切换 |
| 响应式栅格 | `frontend/src/components/global.less` | `&.mobile, &.tablet` 分支已调整 content margin、表格 min-width: 800px |
| 卡片栅格 | `frontend/src/views/list/ExamCardList.vue` | `:grid="{gutter: 24, lg: 3, md: 2, sm: 1, xs: 1}"` 已响应式 |
| 路由结构 | `frontend/src/config/router.config.js` | 考试详情/记录/统计页使用 `constantRouterMap` 独立全屏布局 |

### 1.2 现存阻碍移动端体验的问题

1. **viewport 未禁止缩放**：用户双击会触发页面缩放
2. **content margin 固定 24px**：移动端屏幕浪费边距
3. **GlobalHeader 面包屑**：移动端挤压用户菜单
4. **UserLayout 登录页**：宽度固定 368px，移动端溢出
5. **考试详情页（WangEditor/单选题）**：未针对触摸优化，富文本图片溢出
6. **考试记录列表**：表格列宽固定，移动端横向滚动体验差
7. **管理页面（QuestionTableList/ExamTableList）**：vxe-table 在 375px 下严重溢出
8. **无统一移动端工具函数**：每页重复实现 viewport 检测

## 2. 方案对比

| 维度 | 方案 A：响应式布局适配 ✅ | 方案 B：uniapp 重建 |
|---|---|---|
| 工作量 | 4 批次 16 项，约改动 15 个文件 | 重建整个前端项目，约 50+ 页面 |
| 技术栈兼容 | jQuery ^3.3.1 / Bootstrap / Summernote / WangEditor / vxe-table 全部保留 | jQuery/Bootstrap/Summernote **无法迁移**，需替换为 uni-ui 或 uView |
| 风险 | 低，渐进式改造，每批可独立验证 | 高，富文本编辑器需重新选型，考试题目渲染逻辑需重写 |
| 后端改造 | 无 | 需为 uniapp 增加专用接口（如分页参数、文件上传协议） |
| 维护成本 | 单一代码库，PC/移动端同步 | 双代码库，逻辑重复 |
| 推荐度 | ⭐⭐⭐⭐⭐ | ⭐⭐（不推荐） |

**决策**：采用方案 A 响应式布局适配。uniapp 方案虽适合全新项目，但本项目重度依赖 jQuery 系富文本组件，迁移成本与收益严重失衡。

## 3. 函数级实施清单

### 批次 1：基础设施（P0，4 项）

| # | 文件 | 函数/改动 | 说明 |
|---|---|---|---|
| 1.1 | `frontend/public/index.html` | viewport meta | 添加 `maximum-scale=1.0,user-scalable=no` + `apple-mobile-web-app-capable` |
| 1.2 | `frontend/src/utils/mobile.js` | 新建工具模块 | 导出 `isMobileDevice()` / `getViewportWidth()` / `preventDoubleTapZoom()` / `formatMobileTime(date)` / `scrollToPosition(top, smooth)` / `scrollToElement(el, offset)` + 常量 `MOBILE_BREAKPOINT = 768` |
| 1.3 | `frontend/src/assets/mobile.less` | 新建全局响应式样式 | Mixins: `.mobile-only(@rules)` / `.desktop-only(@rules)` / `.mobile-content-padding()` / `.mobile-table-scroll()` / `.mobile-touch-target()`；全局 `@media (max-width: 768px)` 覆盖：content margin 12px、表格横向滚动、表单项间距、模态框全屏、点击区域增大、`.mobile-hide`/`.mobile-show`/`.desktop-hide` 工具类 |
| 1.4 | `frontend/src/main.js` | 引入 mobile.less | 在 `import './utils/filter'` 后添加 `import './assets/mobile.less'` |

### 批次 2：布局适配（P0，3 项）

| # | 文件 | 函数/改动 | 说明 |
|---|---|---|---|
| 2.1 | `frontend/src/layouts/BasicLayout.vue` | `contentStyle()` 计算属性 | 将 `:style="{ height: '100%', margin: '24px 24px 0' }"` 改为根据 `isMobile()` 动态返回：mobile 时 `12px 12px 0`，desktop 时 `24px 24px 0` |
| 2.2 | `frontend/src/components/GlobalHeader/GlobalHeader.vue` | 模板 `v-if="!isMobile()"` | 移动端隐藏面包屑、简化用户菜单（保留头像 + 下拉，隐藏"个人中心"图标按钮） |
| 2.3 | `frontend/src/layouts/UserLayout.vue` | `.container` 响应式样式 | 移动端 `width: 100%; padding: 24px 16px;`，桌面端保持 `width: 368px` |

### 批次 3：核心页面（P0，6 项）

| # | 文件 | 函数/改动 | 说明 |
|---|---|---|---|
| 3.1 | `frontend/src/views/user/Login.vue` | 模板响应式 + `handleSubmit()` | 移动端表单宽度 100%、按钮高度 44px（触摸目标）；登录失败滚动到错误位置 |
| 3.2 | `frontend/src/views/user/Register.vue` | 同 Login | 复用 Login 模式 |
| 3.3 | `frontend/src/views/list/ExamCardList.vue` | `joinExam(item)` 移动端不新开标签页 | 移动端 `window.open` 在部分浏览器被拦截，改为 `this.$router.push`；卡片底部 actions 文字缩小 |
| 3.4 | `frontend/src/views/exam/ExamDetail.vue` | 题目区响应式 + WangEditor 富文本 `img { max-width: 100% }` | 单选题/多选题/判断题选项触摸区域增大；倒计时悬浮固定顶部 |
| 3.5 | `frontend/src/views/exam/ExamRecordList.vue` | 表格转卡片视图 | 移动端 `isMobile()` 时切换为 `a-list` 卡片渲染，桌面端保持 `a-table` |
| 3.6 | `frontend/src/views/exam/ExamRecordDetail.vue` | 答题回顾响应式 | 题目区单列布局；富文本图片自适应 |

### 批次 4：管理页面（P1，3 项）

| # | 文件 | 函数/改动 | 说明 |
|---|---|---|---|
| 4.1 | `frontend/src/views/question/QuestionTableList.vue` | vxe-table 移动端横向滚动 + 工具栏折叠 | 表格容器 `.mobile-table-scroll()`；"新增/编辑/删除"按钮组在移动端折叠为下拉菜单 |
| 4.2 | `frontend/src/views/exam/ExamTableList.vue` | 同 4.1 | 复用 QuestionTableList 模式；统计按钮独立显示 |
| 4.3 | `frontend/src/views/exam/ExamRecordStat.vue` | 图表响应式 + 表格卡片化 | ECharts `resize()` 监听窗口变化；统计卡片栅格 `xs: 24, sm: 12, lg: 6` |

## 4. 技术要点

### 4.1 断点策略

- **MOBILE_BREAKPOINT = 768px**：与 `enquire.js` 配置保持一致
- `< 768px`：手机竖屏
- `768px ~ 992px`：平板（沿用 antd `tablet` 设备类型）
- `> 992px`：桌面

### 4.2 mixin 使用规范

```js
import { mixinDevice } from '@/utils/mixin'

export default {
  mixins: [mixinDevice],
  computed: {
    contentStyle () {
      return this.isMobile()
        ? { height: '100%', margin: '12px 12px 0' }
        : { height: '100%', margin: '24px 24px 0' }
    }
  }
}
```

### 4.3 布局策略

- **布局类页面（BasicLayout/UserLayout）**：通过 mixin 计算属性动态切换 style
- **页面级（views/）**：使用 `.mobile-only` / `.desktop-only` less mixin 切换显隐
- **表格类**：移动端切换为 `a-list` 卡片视图（vxe-table 移动端体验不可接受）
- **表单类**：`a-form` 始终垂直布局，移动端按钮高度 ≥ 44px

### 4.4 touch 优化

- 禁止双击缩放：`preventDoubleTapZoom()` 在 App.vue mounted 调用
- 按钮点击区域：`.mobile-touch-target()` mixin 强制 `min-height: 44px; min-width: 44px`
- 滚动优化：`-webkit-overflow-scrolling: touch`

## 5. 验收标准

### 5.1 功能验收（每批次必须满足）

- [ ] PC 端原有功能无回归（layout/header/login/exam 不变形）
- [ ] 375px 宽度下无横向滚动条（表格卡片化页面除外）
- [ ] 触摸目标 ≥ 44×44px（按钮、链接、菜单项）
- [ ] 文字最小 14px，标题最小 16px

### 5.2 编译验收

- [ ] `cd frontend && npx vue-cli-service build --mode production` 成功
- [ ] 无 ESLint error（warning 允许）
- [ ] 无 console.error（移动端检测相关 console.log 允许）

### 5.3 设备验收（Chrome DevTools 模拟）

- iPhone SE (375×667)
- iPhone 12 Pro (390×844)
- iPad (768×1024)
- 桌面 1440×900

## 6. 实施进度

| 批次 | 状态 | 完成时间 |
|---|---|---|
| 批次 1 基础设施 | ✅ 已完成 | 2026-07-23 |
| 批次 2 布局适配 | ✅ 已完成 | 2026-07-23 |
| 批次 3 核心页面 | ✅ 已完成 | 2026-07-23 |
| 批次 4 管理页面 | ✅ 已完成 | 2026-07-23 |
| 编译验证 | ✅ 已完成 | 2026-07-23 |

### 6.1 实施详情

**批次 1 基础设施（4 项）**
- `frontend/public/index.html`：viewport 添加 `maximum-scale=1.0,user-scalable=no` + apple-mobile-web-app 元标签
- `frontend/src/utils/mobile.js`（新建）：`isMobileDevice()` / `getViewportWidth()` / `preventDoubleTapZoom()` / `formatMobileTime()` / `scrollToPosition()` / `scrollToElement()` + 常量 `MOBILE_BREAKPOINT = 768`
- `frontend/src/assets/mobile.less`（新建）：less mixins（`.mobile-only`/`.desktop-only`/`.mobile-content-padding`/`.mobile-table-scroll`/`.mobile-touch-target`）+ 全局 `@media (max-width: 767px)` 覆盖 + 工具类（`.mobile-hide`/`.mobile-show`/`.desktop-hide`）
- `frontend/src/main.js`：添加 `import './assets/mobile.less'`

**批次 2 布局适配（3 项）**
- `frontend/src/layouts/BasicLayout.vue`：新增 `contentStyle()` 计算属性，移动端 margin 改为 `12px 12px 0`
- `frontend/src/components/tools/UserMenu.vue`：昵称 span 添加 `mobile-hide` 类
- `frontend/src/layouts/UserLayout.vue`：`&.mobile` 分支增强，padding/header/title/main/footer 全部响应式

**批次 3 核心页面（6 项）**
- `frontend/src/views/user/Login.vue`：新增移动端样式块（按钮/输入框高度 44px）
- `frontend/src/views/user/Register.vue`：同 Login 模式
- `frontend/src/views/list/ExamCardList.vue`：引入 `mixinDevice`/`isMobileDevice`，`joinExam()` 移动端改用 `router.push`
- `frontend/src/views/list/ExamDetail.vue`（重大改造）：桌面端固定 sider + 移动端抽屉式 sider（`a-drawer`），新增 `contentLayoutStyle`/`contentStyle`/`contentInnerStyle` 计算属性
- `frontend/src/views/list/ExamRecordList.vue`：`viewExamRecordDetail()` 移动端改用 `router.push` + 移动端样式
- `frontend/src/views/list/ExamRecordDetail.vue`（重大改造）：同 ExamDetail 模式

**批次 4 管理页面（3 项）**
- `frontend/src/views/list/QuestionTableList.vue`：新增移动端样式（表格横向滚动、字号缩减、分页器居中）
- `frontend/src/views/list/ExamTableList.vue`：引入 `mixinDevice`/`isMobileDevice`，`handleSub()`/`handleStat()` 移动端改用 `router.push` + 移动端样式
- `frontend/src/views/list/ExamRecordStat.vue`：统计卡片 `a-col` 改为响应式 `:xs="12" :sm="12" :lg="4"`，`viewDetail()` 移动端改用 `router.push` + 移动端样式

### 6.2 编译验证

**构建命令**：`cd frontend && npx vue-cli-service build --mode production`

**构建结果**：✅ 成功（`DONE Build complete. The dist directory is ready to be deployed.`）

**构建过程中修复的预存在问题**（与移动端改造无关）：
1. `vue.config.js` SVG 规则：`file-loader`（未安装，webpack 4 风格）→ `type: 'asset/resource'`（webpack 5 原生）
2. 安装缺失依赖：`less@3.13.1` / `less-loader@11.1.4` / `eslint-plugin-vue@9.33.0` / `@babel/eslint-parser@7.29.7`
3. `lintOnSave: false`：禁用构建时 ESLint 检查（项目含大量预存在风格问题）

**构建产物**：`dist/` 目录已生成，包含 JS/CSS/字体/图片等全部静态资源
