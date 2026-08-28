# 全功能测试平台 — 前端工程详解

> 本文档详细讲解 `test-platform/frontend/` 目录下前端代码的组成、每个文件的职责，以及用户从打开浏览器到完成业务操作的完整流程。
>
> 技术栈：Vue 3 + Vite 5 + Element Plus + Axios + Vue Router 4
>
> 更新日期：2026-08-05
>
> ⚠️ **时效声明**：本文写于优化工程启动前。此后前端已变化：路由守卫增加 JWT 过期校验（tokenExp）、main.js 生产环境错误脱敏、CiStatus 数据列修复、Element Plus 失效图标替换（详见 `test-platform/docs/优化计划.md` 进展表）。架构性描述（目录结构/请求流程/组件职责）仍然有效，细节以代码为准。

---

## 一、目录总览

```
frontend/
├── index.html                  # HTML 入口（挂载点 #app）
├── package.json                # 依赖清单：vue、vue-router、axios、element-plus、vite
├── vite.config.js              # Vite 构建配置 + /api 开发代理
├── Dockerfile                  # 多阶段镜像：Node 20 构建 → Nginx 部署
├── nginx.conf                  # SPA 路由兜底 + /api 反向代理
└── src/
    ├── main.js                 # 应用入口，注册全局插件/错误处理器/图标
    ├── App.vue                 # 根组件：顶部导航 + 用户信息 + <router-view />
    │
    ├── router/
    │   └── index.js            # 路由表 + 懒加载 + 鉴权守卫
    │
    ├── api/                    # HTTP 请求层
    │   ├── index.js            # Axios 实例 + 拦截器 + 业务 API 分组导出
    │   ├── auth.js             # 登录/注册/me
    │   └── ci.js               # CI 构建记录
    │
    ├── utils/
    │   └── format.js           # formatDate / formatJson 工具函数
    │
    ├── composables/
    │   └── useConfirmDelete.js # 删除确认通用逻辑（DRY）
    │
    ├── components/             # 可复用业务组件
    │   ├── CategoryTree.vue    # 分类树侧边栏
    │   ├── CategoryDialog.vue  # 分类增删改弹窗
    │   ├── JsonDiffViewer.vue  # JSON 双栏对比 + 差异表格 + 修复建议
    │   └── ErrorPatternCard.vue# 错误模式聚合展示
    │
    └── views/                  # 路由级页面组件（10 个）
        ├── Login.vue
        ├── TestCaseList.vue
        ├── TestCaseEdit.vue
        ├── ExecutionList.vue
        ├── TestSuiteList.vue
        ├── TestSuiteDetail.vue
        ├── ExecutionReportList.vue
        ├── ExecutionReportDetail.vue
        ├── DocView.vue
        └── CiStatus.vue
```

### 关键约定

| 约定 | 描述 |
|---|---|
| 单文件组件 | 所有 `.vue` 使用 `<script setup>` + Vue 3 Composition API |
| 状态管理 | 无 Vuex/Pinia，使用 `localStorage` 保存 token + user |
| 样式 | 全部 `<style scoped>`，避免全局污染 |
| 路由 | 懒加载（`() => import(...)`），首屏仅加载 TestCaseList 与 Login |
| Toast 提示 | 统一使用 `ElMessage` |
| 确认弹窗 | 统一使用 `ElMessageBox.confirm` |

---

## 二、核心文件详解

### 2.1 `src/main.js` — 应用入口

**职责**：创建 Vue 实例、注册全局插件、注册全局错误处理器、全局挂载 Element Plus 图标，最后挂载到 `#app`。

**执行顺序（关键）**：

```js
1. createApp(App)
2. app.config.errorHandler = ...   // ⚠️ 必须在 mount 之前注册
3. app.use(ElementPlus)            // 全局组件
4. app.use(router)                 // 路由插件
5. 全局注册 Element Plus Icons（for .. of）
6. app.mount('#app')
```

**关键设计**：

- `errorHandler` 捕获组件树中所有未处理的异常，通过 `ElMessage.error` 弹出提示，避免白屏。
- Element Plus Icons 全局注册后，可在模板中直接以 `<Document />`、`<User />` 方式使用，无需每个组件单独 import。

---

### 2.2 `src/App.vue` — 根组件

**职责**：提供全局布局（顶部导航 + 内容区）+ 用户状态管理。

**结构**：

```
┌─ top-nav ─────────────────────────────────────────────────────┐
│ [测试平台]  [测试用例] [测试套件] [执行报告] [CI 状态] [文档]   │ [用户名 ▾]
└───────────────────────────────────────────────────────────────┘
┌─ el-main (app-main) ── height: calc(100vh - 60px) ─────────┐
│ <router-view />                                              │
└──────────────────────────────────────────────────────────────┘
```

**关键逻辑**：

1. **用户状态冷启动**：`localStorage.getItem('user')` 直接 `JSON.parse` 作为初始显示，让用户刷新后仍能看到名字。
2. **挂载校验**：`onMounted` 时如果存在 token，调用 `authApi.me()` 从后端校验并刷新用户数据，**覆盖**可能被篡改的 localStorage 数据。
3. **登出**：清 token + 清 user + 路由推 `/login`。

**导航菜单**：使用 `el-menu` 组件的 `router` 模式，menu-item 的 `index` 就是路由 path，点击即跳转（无需手动 `router.push`），且 `default-active="$route.path"` 实现高亮同步。

---

### 2.3 `src/router/index.js` — 路由配置

**职责**：定义所有路由、注册懒加载、实现鉴权守卫。

**路由表**：

| Path | Name | Component | 说明 |
|---|---|---|---|
| `/login` | Login | Login.vue | 登录/注册页 |
| `/` | TestCaseList | TestCaseList.vue | 首页 — 用例管理（左右布局） |
| `/executions` | ExecutionList | ExecutionList.vue | 执行记录 |
| `/docs` | DocView | DocView.vue | 开发文档嵌入 |
| `/suites` | TestSuiteList | TestSuiteList.vue | 套件列表 |
| `/suites/:id` | TestSuiteDetail | TestSuiteDetail.vue | 套件详情（含用例+执行报告入口） |
| `/reports` | ExecutionReportList | ExecutionReportList.vue | 报告列表 |
| `/reports/:id` | ExecutionReportDetail | ExecutionReportDetail.vue | 报告详情（Diff+错误模式） |
| `/ci` | CiStatus | CiStatus.vue | CI 构建看板 |
| `/:pathMatch(.*)*` | NotFound | — | 兜底，重定向到 `/` |

**懒加载**：9 个页面组件全部使用 `() => import(...)`，Vite/Rollup 会将每个组件打成独立 chunk（例如 `TestCaseList-BAig9DyE.js`），访问时才下载。

**鉴权守卫**（`router.beforeEach`）：

```js
if (目标不是 /login 且无 token)     → 重定向到 /login
else if (目标是 /login 但已有 token) → 重定向到 /
else                                  → next() 放行
```

---

### 2.4 `src/api/index.js` — Axios 实例与业务 API

**职责**：集中配置 HTTP 客户端、统一错误处理、按业务域分组导出 API 对象。

**Axios 实例配置**：

| 配置项 | 值 | 说明 |
|---|---|---|
| `baseURL` | `/api` | 开发时由 Vite proxy 代理到 `localhost:8080`，生产由 Nginx 代理 |
| `timeout` | 30000 (30s) | 普通请求超时 |

**请求拦截器**：从 localStorage 取 token，注入 `Authorization: Bearer xxx`。

**响应拦截器**（核心）：

1. **401 处理**：检测到 401 + `isRedirecting === false` → 清 token/user + 弹出错误提示 + `router.push('/login')`，然后设置 `isRedirecting = true` 防重复跳转。
2. **其他错误**：提取后端 `message` 或 `error.message`，统一弹出。
3. **成功响应**：透传。

**业务 API 分组导出**：

| 对象 | 后端 | 接口清单 |
|---|---|---|
| `testCaseApi` | `/testcases` | `list`/`get`/`save`/`update`/`delete`/`importOpenapi`/`batchSave` |
| `suiteApi` | `/test-suites` | `list`/`get`/`save`/`update`/`delete`/`listCases`/`addCase`/`removeCase`/`execute`/`batchAddCases` |
| `reportApi` | `/execution-reports` | `list`/`get`/`getDetails`/`errorPatterns` |
| `diffApi` | `/execution-records/{id}/diff` | `get` |
| `categoryApi` | `/categories` | `tree`/`list`/`save`/`update`/`delete` |
| `aiApi` | `/ai/expected` | `expected` |

`importOpenapi` 会根据 `useAi` 动态设置 `timeout`：AI 模式 120s，本地模式 30s。

---

### 2.5 `src/api/auth.js` — 认证 API

**职责**：封装 3 个认证接口。

```js
authApi = {
  login:    POST /auth/login    → { token, user }
  register: POST /auth/register
  me:       GET  /auth/me       → { user }（自动携带 Authorization）
}
```

---

### 2.6 `src/api/ci.js` — CI 构建 API

**职责**：封装 CI 看板所需的两个接口（**注意**：此文件使用独立函数导出而非对象风格，与主 index.js 不一致）。

---

### 2.7 `src/utils/format.js` — 工具函数

| 函数 | 用途 | 调用场景 |
|---|---|---|
| `formatDate(row, column, cellValue)` | 日期格式化为 `YYYY-MM-DD HH:mm:ss` | 直接作为 `el-table-column` 的 `:formatter` 回调 |
| `formatJson(text)` | 解析后格式化输出，解析失败原样返回 | `JsonDiffViewer`、日志弹窗 |

---

### 2.8 `src/composables/useConfirmDelete.js` — 通用删除逻辑

**职责**：将"弹确认框 → 调接口 → 提示成功 → 刷新列表"的统一流程封装为 composable，消除 3 个页面（TestCaseList、TestSuiteList、TestSuiteDetail）的重复代码。

**使用示例**：

```js
import { useConfirmDelete } from '../composables/useConfirmDelete'

const remove = useConfirmDelete(suiteApi.delete, fetchList)
// 模板：<el-button @click="remove(row.id)">删除</el-button>
```

**关键设计**：

- `refreshFn()` 只在 `try` 块内删除成功时被调用，避免"用户点击取消后仍触发刷新"的经典 bug。
- `catch` 中区分 `cancel`（用户取消，不报错）和其他错误（弹 error toast）。

---

## 三、页面组件详解

### 3.1 `Login.vue` — 登录/注册

**职责**：用户认证入口。

**结构**：
- 居中卡片（`400px`，圆角+阴影+渐变背景）
- `el-tabs` 切换登录/注册
- 两个 `el-form` 分别绑定独立的 `ref`、`model`、`rules`

**校验规则**：

| 字段 | 登录 | 注册 |
|---|---|---|
| username | 必填 | 必填 + 长度 3-20 |
| password | 必填 | 必填 + 最少 6 字符 |
| displayName | — | 最多 50 字符 |

**登录流程**：
1. `loginForm.validate()` 拦截空输入
2. 调 `authApi.login`，成功后存 `token` + `user` 到 localStorage
3. 显示"登录成功"toast + `router.push('/')`
4. 失败时把后端 message 塞给 `loginError` 红字显示（而非弹 toast，避免和"加载中"叠加混乱）

**注册流程**：
1. 成功后自动切到 login tab + 回填用户名
2. 失败时弹 ElMessage.error

**共享 loading**：登录/注册共用 `loading` ref，避免并发提交。

---

### 3.2 `TestCaseList.vue` — 测试用例管理（核心页面）

**职责**：用例 CRUD + 执行 + 历史日志 + 分类筛选 + OpenAPI 批量导入。这是最复杂的页面（449 行 + 4 个弹窗）。

**布局**：左右结构

```
┌─ side-panel (220px) ─┐ ┌─ content-panel (flex) ─────────────────────────┐
│ CategoryTree          │ │ [标题] [当前分类tag]  [搜索] [分类管理] [导入]   │
│ - HTTP基础            │ │                                                │
│   - GET 请求          │ │ ┌─ 列表模式 ─┐  或  ┌─ 编辑面板 ─┐            │
│   - POST 请求         │ │ │  el-table   │      │TestCaseEdit│            │
│ - 认证模块            │ │ │  + 操作栏   │      │ (内联)     │            │
│ [管理分类]            │ │ └────────────┘      └────────────┘            │
└───────────────────────┘ └────────────────────────────────────────────────┘
```

**核心特性**：

1. **编辑模式切换**：不跳路由，用 `editingCase` / `creating` ref 切换显示列表或内联的 `TestCaseEditPanel`，保存/取消时回到列表。
2. **筛选 + 搜索**：`filteredAndSearchedList` 为 computed，先按 `selectedCategory` 过滤，再按 `testNo` / `name` 关键词搜索。
3. **执行按钮 loading**：每行按钮绑定 `loadingId === row.id`，避免多行同时转圈。
4. **OpenAPI 导入**：粘贴 JSON → 选择 AI/本地 → 解析预览（不入库） → 确认后批量保存。
5. **跳转修复流程**：`onMounted` 检测 `route.query.editId && route.query.fix`，从 localStorage 读 `fix_expected`（来自 Diff 报告），自动打开编辑面板并回填新预期结果。

**弹窗汇总**：

| 弹窗 | 触发 | 内容 |
|---|---|---|
| `CategoryDialog` | "分类管理"按钮 | 分类的增删改 |
| 历史执行记录 | 用例行"历史"按钮 | 该用例所有 execution_record |
| 执行日志 | 历史记录"查看"按钮 | 请求 / 响应 / 实际结果三段日志 |
| 导入 OpenAPI | "导入 OpenAPI"按钮 | 粘贴→解析→预览→批量入库 |

---

### 3.3 `TestCaseEdit.vue` — 用例编辑面板

**职责**：新建/编辑用例的内联面板，被 TestCaseList 引用。

**关键特性**：

- `defineProps` 接收 `editId`（编辑时） / `categories`（分类下拉列表） / `initData`（回填数据） / `selectedCategoryId`。
- `save()` 防重复提交：绑定 `saving` ref 到按钮 `:loading="saving"`，finally 块重置。
- `validateJson(field)` 按钮单独验证 JSON 字段合法性。
- `generateExpected()` 调 `aiApi.expected` 后回填 `expectedResult`，loading 态防重复点击。
- emit `save` 事件给父组件，父组件负责刷新列表和关闭编辑。

---

### 3.4 `ExecutionList.vue` — 执行记录

**职责**：按 `testCaseId` 查询所有执行记录（通常由父级跳转携带 query）。

**关键特性**：

- `onMounted` 读 `route.query.testCaseId`，拉记录并填充表格。
- 日志弹窗展示 `requestDetail` / `responseDetail` / `actualResult`。
- 深色背景 + 等宽字体模拟终端风格。

---

### 3.5 `TestSuiteList.vue` — 测试套件列表

**职责**：套件 CRUD + 跳详情。

**关键特性**：

- `skeleton` 加载骨架屏 + `el-empty` 空状态。
- 保存/新建共用 `dialogVisible` + `formRef`，保存后调 `fetchList` 刷新。
- 删除使用 `useConfirmDelete` composable，统一流程。

---

### 3.6 `TestSuiteDetail.vue` — 套件详情

**职责**：管理套件内的用例 + 批量执行 + 跳报告。

**关键特性**：

- 展示套件基础信息、用例列表。
- "添加用例"弹窗：拉全量用例，过滤掉已存在的，表格支持多选批量添加（`batchAddCases` 一次请求）。
- "执行套件"弹窗：`showExecuting` 在 try/catch 的 `finally` 块统一重置，避免卡死；成功后 `router.push` 到新生成的报告详情页。
- 用例移除使用 `useConfirmDelete`。

---

### 3.7 `ExecutionReportList.vue` — 执行报告列表

**职责**：展示所有执行报告 + 通过率统计卡片。

**关键特性**：

- 通过 `route.query.suiteId` 可选按套件过滤。
- 顶部统计卡片（total/passed/failed/passRate）根据选中报告自动计算。
- 点击报告行跳转 `/reports/:id`。

---

### 3.8 `ExecutionReportDetail.vue` — 执行报告详情（核心页面）

**职责**：单报告详情 + 执行明细 + JSON Diff + 错误模式聚合 + 修复建议应用。

**关键特性**：

1. **并行加载**：`Promise.all([ get(), getDetails(), errorPatterns() ])` 三个独立请求并行，响应时间减半。
2. **统计卡片**：total / passed / failed / errored / passRate / 执行耗时。
3. **明细表格**：每行一个 execution_record，点击"日志"打开 Diff 弹窗。
4. **ErrorPatternCard**：组件展示 URL+Method 聚合的错误分布。
5. **Diff 弹窗**：嵌入 `JsonDiffViewer`，传 expectedResult / actualResult / diff 结果。
6. **修复建议应用**：点击 "一键应用" 将 suggestedExpected 存入 `localStorage.fix_expected`，`window.open` 新标签页跳 `/testcases?editId=X&fix=1`，在 TestCaseList 自动打开编辑面板预填。

---

### 3.9 `DocView.vue` — 文档页

**职责**：嵌入后端 `/docs` 目录下的 markdown 文档（通过 iframe 或静态资源），供用户在线查阅。

---

### 3.10 `CiStatus.vue` — CI 构建看板

**职责**：展示 Jenkins Pipeline 构建历史 + 最新状态。

**关键特性**：

- 使用 `ci.js` 的 `getLatestBuild` / `getBuildList` 独立函数。
- 顶部统计卡片 + 构建历史表格（buildNumber / passRate / status / Jenkins URL 可跳转）。

---

## 四、公共组件详解

### 4.1 `CategoryTree.vue` — 分类树

**职责**：树形分类导航，点击节点筛选用例。

**Props / Events**：
- emit `categoryChange(data)`：节点点击向外广播
- emit `manage`：右上角齿轮按钮
- expose `fetchTree()`：父组件可主动刷新

**关键逻辑**：
- `el-tree` 配置 `node-key="id"` + `highlight-current` + `current-node-key` 控制高亮同步。
- 父组件通过 ref 调 `fetchTree()` 在分类增删改后刷新。

---

### 4.2 `CategoryDialog.vue` — 分类弹窗

**职责**：分类 CRUD 弹窗（级联父级选择 + 自动计算层级）。

**关键逻辑**：
- 编辑时从 `categoryApi.list()` 拿全量分类，前端组装级联下拉。
- 删除用 `ElMessageBox.confirm`，删除成功后 emit `saved`，父组件刷新树 + 列表。

---

### 4.3 `JsonDiffViewer.vue` — JSON Diff 查看器

**职责**：双栏对比 + 差异表格 + 修复建议。

**Props**：
- `result`：后端 diff 计算结果（match / differences / suggestedExpected / 原始串）
- `expectedJson` / `actualJson`：原始 JSON 字符串

**渲染流程**：
1. `result` 为空时显示 loading（`<Loading />` 旋转图标）
2. `match=true` 且无差异时显示绿色"完全匹配"（`<CircleCheckFilled />`）
3. 有差异时分左右两栏展示格式化 JSON + 差异明细表格（fieldPath / type / 预期值 / 实际值）
4. 如果有 `suggestedExpected`，底部绿色卡片展示 + "一键应用"按钮

**事件**：`applyFix` emit 给父组件处理。

---

### 4.4 `ErrorPatternCard.vue` — 错误模式卡片

**职责**：渲染 ErrorPatternService 返回的聚合数据，URL+Method 分组 + 通过率色标。

---

## 五、前端完整运行流程

### 5.1 首次访问流程

```
1. 浏览器访问 http://localhost:3000
   ↓
2. Vite dev server 返回 index.html
   ↓
3. index.html 加载 src/main.js
   ↓
4. main.js 执行：
   - createApp(App)
   - 注册 errorHandler
   - app.use(ElementPlus)
   - app.use(router)
   - 全局注册 Element Plus Icons
   - mount('#app')
   ↓
5. App.vue 渲染：
   - localStorage 读 user，初始化顶部用户名
   - 异步调 authApi.me() 校验
   ↓
6. router.beforeEach 触发：
   - 无 token → 重定向到 /login
   - Login.vue 按路由懒加载被下载并渲染
```

### 5.2 登录流程

```
1. 用户填写用户名 + 密码，点击"登录"
   ↓
2. el-form.validate() 校验必填项
   ↓
3. 点击按钮 :loading="true"，防重复
   ↓
4. authApi.login({ username, password })
   ↓
5. Axios 请求拦截器：此时无 token，跳过 Authorization
   ↓
6. 后端 POST /api/auth/login：校验密码 + BCrypt + 签发 JWT
   ↓
7. 成功：
   - 存 localStorage.token、localStorage.user
   - ElMessage.success('登录成功')
   - router.push('/') 跳首页
   ↓
8. 首页 beforeEach 放行 → 懒加载 TestCaseList.vue
```

### 5.3 用例列表加载流程

```
1. TestCaseList onMounted 设置 loading=true → 显示 el-skeleton 骨架屏
   ↓
2. Promise.all 并行：
   - fetchList()        → GET /api/testcases
   - fetchCategories()  → GET /api/categories
   ↓
3. 请求拦截器：自动注入 Bearer token
   ↓
4. 响应拦截器：
   - 200 → 透传
   - 401 → 清 local + 弹错误 + 跳登录（防重）
   - 其他 → 弹 ElMessage.error
   ↓
5. 两个请求都完成后：
   - list.value = 用例数组
   - allCategories.value = 分类数组
   ↓
6. loading=false，骨架屏隐藏，el-table 渲染
   ↓
7. CategoryTree 内部也独立调用 fetchTree()，渲染左侧树
```

### 5.4 执行用例流程

```
1. 用户点击某行"执行"按钮
   ↓
2. loadingId.value = row.id（仅该行转圈）
   ↓
3. api.post(`/execution-records/${id}/execute`)
   ↓
4. 后端：
   - 拉用例 → HttpExecutor 发真实 HTTP
   - JSON 子集匹配 / 文本降级匹配
   - 写 execution_record（status + 日志）
   ↓
5. 成功：ElMessage.success + fetchList() 刷新列表
   失败/异常：弹 error
   ↓
6. finally：loadingId.value = null（按钮恢复正常）
```

### 5.5 Diff + 修复建议流程（跨页面协作）

```
1. 用户在执行报告详情页看某条 FAIL 记录的日志
   ↓
2. JsonDiffViewer 显示 suggestedExpected + "一键应用"按钮
   ↓
3. 用户点击 → emit('applyFix', suggestedExpected)
   ↓
4. ExecutionReportDetail.receiveApplyFix：
   - localStorage.setItem('fix_expected', value)
   - window.open('/?editId=X&fix=1', '_blank')
   ↓
5. 新标签页加载 TestCaseList
   ↓
6. TestCaseList onMounted：
   - 检测 route.query.editId && route.query.fix
   - localStorage.getItem('fix_expected')
   - 拉取 /testcases/X 数据
   - 覆盖 expectedResult = suggested
   - 自动打开编辑面板
   - 清理 localStorage.fix_expected
   ↓
7. 用户确认无误 → 保存 → PUT /testcases/X → 列表刷新
```

### 5.6 401 跳转流程

```
1. 任意页面发起业务请求
   ↓
2. 后端返回 401（token 过期或无效）
   ↓
3. Axios 响应拦截器：
   - error.response.status === 401
   - isRedirecting 为 false（首次）
   ↓
4. 执行：
   - isRedirecting = true（后续 401 不再触发，避免重复跳转）
   - localStorage.removeItem('token')
   - localStorage.removeItem('user')
   - ElMessage.error(后端返回的 message || '登录已过期')
   - router.push('/login')
   ↓
5. App.vue 顶栏 v-if="user" 不再渲染用户区
   ↓
6. beforeEach 接管：其他页面访问全跳 /login
```

### 5.7 App.vue 冷启动校验流程

```
1. 用户刷新页面
   ↓
2. App.vue script setup 执行：
   - localStorage.user → JSON.parse → 初始显示（避免闪烁"未登录"）
   ↓
3. onMounted 中：
   - 若有 localStorage.token → 调 GET /api/auth/me
   ↓
4. 200 + user：更新 user ref（覆盖 localStorage 可能被篡改的数据）
           → localStorage.setItem('user', ...)
   401: 拦截器自动跳登录
   error: console.warn（不影响当前显示）
```

---

## 六、状态流转图

```
localStorage
├─ token        ← 登录写入，登出/401 清除
└─ user (JSON)  ← 登录写入；App.vue 冷启动读取 + 校验覆盖

ref 状态（页面级）
├─ list / loading / loadingId             TestCaseList
├─ editingCase / creating                 TestCaseList → 控制是否切换编辑面板
├─ searchQuery / selectedCategory         TestCaseList → 过滤条件
├─ importJson / importPreview / useAi     TestCaseList → OpenAPI 导入
├─ suite / cases / showExecuting          TestSuiteDetail
├─ report / details / errorPatterns       ExecutionReportDetail
├─ diffResult / logVisible               ExecutionReportDetail → Diff 弹窗
└─ user（App.vue）                        全局顶部栏用户显示

跨页面数据传递
├─ localStorage.fix_expected              Diff 页面 → 用例列表
└─ route.query.editId + fix               ExecutionReportDetail → TestCaseList
```

---

## 七、Vite 开发与生产部署

### 7.1 开发模式

`vite.config.js` 配置 `/api` 代理到 `http://localhost:8080`，前端 `npm run dev` 起在 `localhost:3000`，后端 `mvn spring-boot:run` 在 `8080`，所有 `/api/*` 请求自动转发。

### 7.2 生产模式

Dockerfile 多阶段：

```
阶段 1（node:20）：npm install && npm run build  → 生成 dist/
阶段 2（nginx:alpine）：复制 dist/ + nginx.conf   → 暴露 80
```

`nginx.conf` 关键点：

- `try_files $uri $uri/ /index.html`：SPA 路由兜底
- `/api` 用 `$backend_upstream` + `resolver 127.0.0.11` 反代到 backend 容器（运行时解析主机名，避免 backend 未就绪时 nginx 启动崩溃）
- `charset utf-8`：中文响应不乱码

---

## 八、性能与优化措施

| 优化项 | 涉及文件 | 效果 |
|---|---|---|
| 路由懒加载 | `router/index.js` | 首屏只加载 Login + TestCaseList |
| Promise.all 并行 | `ExecutionReportDetail`、`TestCaseList` | 独立请求并行，响应时间减半 |
| 防重复提交 | `TestCaseEdit`（save）、`TestSuiteDetail`（execute） | 按钮 loading 期间禁用 |
| `isRedirecting` 防重跳 | `api/index.js` | 多个 401 响应只触发一次登录页跳转 |
| finally 重置状态 | 所有含 loading 的函数 | 异常路径也不会卡死按钮 |
| composable 抽取 | `useConfirmDelete` | 3 个文件复用同一逻辑，减少 ~60 行 |
| 死代码清理 | `CategoryTree.selectedCategory`、`JsonDiffViewer.deepUnescape` | 减少包体积 |
| Element Plus Icons 全局注册 | `main.js` | 登录页字符串图标正常渲染 |
| errorHandler 注册时机 | `main.js` | 捕获挂载阶段错误 |
| localStorage 用户校验 | `App.vue onMounted` | 防篡改 |

---

## 九、已知历史陷阱与修复

| 陷阱 | 原因 | 修复 |
|---|---|---|
| errorHandler 无效 | 在 `mount` 之后注册 | 移到 `mount` 之前 |
| 删除取消后列表刷新 | `fetchList()` 写在 `catch` 之外 | 移入 `try` 成功路径 |
| 保存按钮重复提交 | 无 loading 绑定 | `:loading="saving"` + finally |
| 报告详情加载慢 | 三个 API 串行 await | `Promise.all` 并行 |
| 登录页图标不显示 | Element Plus Icons 未全局注册 | `main.js` 全局注册 |
| 旧版图标语法 | `<i class="el-icon-loading">` 弃用 | 替换为 `<Loading />` 组件 |
| `window.location.href` 跳登录 | 整页刷新 + 多次跳转 | `router.push` + 防重标记 |
| localStorage 用户数据可篡改 | 直接读取无校验 | `authApi.me()` 挂载时校验 |
| 注册无密码强度 | 规则只校验 required | 增加 `min:6` |
| JsonDiffViewer 死代码 | deepUnescape 从未调用 | 删除 |

---

## 十、文件行数与职责速查表

| 文件 | 行数 | 职责 |
|---|---|---|
| `main.js` | 24 | 应用入口 + 全局插件 |
| `App.vue` | 155 | 根布局 + 用户管理 |
| `router/index.js` | 41 | 路由表 + 懒加载 + 守卫 |
| `api/index.js` | 89 | Axios + 拦截器 + 业务 API |
| `api/auth.js` | 7 | 认证 3 接口 |
| `api/ci.js` | 9 | CI 构建 2 接口 |
| `utils/format.js` | 16 | date/json 格式化 |
| `composables/useConfirmDelete.js` | 20 | 删除确认 composable |
| `components/CategoryTree.vue` | 110 | 分类树 |
| `components/CategoryDialog.vue` | 156 | 分类弹窗 |
| `components/JsonDiffViewer.vue` | 134 | Diff 查看器 |
| `components/ErrorPatternCard.vue` | 40 | 错误模式卡片 |
| `views/Login.vue` | 147 | 登录 / 注册 |
| `views/TestCaseList.vue` | 449 | 用例管理（最复杂） |
| `views/TestCaseEdit.vue` | 221 | 用例编辑面板 |
| `views/ExecutionList.vue` | 122 | 执行记录 |
| `views/TestSuiteList.vue` | 149 | 套件列表 |
| `views/TestSuiteDetail.vue` | 168 | 套件详情 |
| `views/ExecutionReportList.vue` | 136 | 报告列表 |
| `views/ExecutionReportDetail.vue` | 181 | 报告详情 |
| `views/DocView.vue` | 294 | 文档嵌入 |
| `views/CiStatus.vue` | 184 | CI 看板 |

合计约 **2652 行**前端源代码（不含 dist）。

---

## 十一、后续建议（未在本次优化范围内）

1. 引入 ESLint + Prettier（全仓库无代码规范工具）。
2. Element Plus 全量引入改为按需引入（unplugin-vue-components），减少主 chunk 357 kB。
3. 引入 Pinia 管理跨页面状态（目前依赖 localStorage，无法触发组件响应式）。
4. 表格加分页（`TestCaseList`、`TestSuiteDetail` 当前全量加载，随用例增长会有性能问题）。
5. `api/ci.js` 导出风格与 `api/index.js` 统一为对象导出。
6. 引入 Vitest + Vue Test Utils 增加单元测试覆盖。
7. Token 改用 httpOnly cookie（需后端配合），消除 XSS 窃取风险。

---

*本文档基于项目实际代码生成，反映 2026-08-05 的前端工程全貌。*
