# 全功能测试平台 V3.2

> 📖 文档入口 — 从这里开始访问所有项目文档

## 文档导航

| 文档 | 说明 |
|---|---|
| **[PROJECT_INTRO.md](PROJECT_INTRO.md)** | 🏠 项目介绍（本文档） |
| **[API.md](API.md)** | 📡 API 接口文档（V1 ~ V3.2） |
| **[sql.md](sql.md)** | 🗄️ 数据库 ER 图与表结构 |
| **[进度报告.md](进度报告.md)** | 📊 项目进度总览、里程碑、功能统计 |
| **[开发进度.md](开发进度.md)** | 📝 分阶段详细任务跟踪与修复记录 |
| **[阶段总结报告.md](阶段总结报告.md)** | 🏁 阶段总结、技术决策、经验教训 |
| **[resume.html](resume.html)** | 🔄 开发恢复指南（下次继续用） |

---

## 项目定位

一站式测试管理平台 V3.2，聚焦测试用例管理、执行、报告、日志展示、测试套件、执行报告统计、JSON Diff 分析、错误模式聚合、分类管理、AI 智能生成预期结果、OpenAPI 批量导入与 JWT 权限管理。

## 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.3.6 |
| ORM | MyBatis-Plus | 3.5.9 |
| 数据库 | MySQL | 5.7+ (驱动 8.0.33) |
| 前端 | Vue 3 + Element Plus | — |
| 构建 | Maven (父子模块) | — |

## 项目结构

```
test-platform/
├── pom.xml                              # 父 POM
├── docs/
│   ├── PROJECT_INTRO.md                 # 项目介绍（文档入口）🏠
│   ├── API.md                           # API 接口文档 📡
│   ├── sql.md                           # 数据库 ER 图 🗄️
│   ├── 进度报告.md                       # 项目进度总览 📊
│   ├── 开发进度.md                       # 分阶段任务跟踪 📝
│   ├── 阶段总结报告.md                   # 阶段总结报告 🏁
│   └── resume.html                      # 开发恢复指南 🔄
├── backend/
│   ├── pom.xml
│   ├── Dockerfile                        # 多阶段 Spring Boot 镜像
│   └── src/main/
│       ├── java/com/testplatform/
│       │   ├── TestPlatformApplication.java
│       │   ├── common/ (Result + HttpResult + JsonDiffResult + ErrorPattern + EndpointDef + GlobalExceptionHandler)
│       │   ├── config/ (Cors + Security + RestTemplate + JwtUtil + JwtAuthFilter + PasswordEncoder + AiConfig)
│       │   ├── dto/CategoryNode.java
│       │   ├── controller/ (Auth + Ai + Category + Execution + TestCase + TestSuite + ExecutionReport)
│       │   ├── entity/ (User + TestCase + ExecutionRecord + TestCategory + TestSuite + TestSuiteCase + ExecutionReport)
│       │   ├── mapper/ (7 个 Mapper)
│       │   ├── service/ (10 个接口 + 6 个实现)
│       │   └── util/ (OpenApiParser + SchemaToJsonGenerator)
│       └── resources/
│           ├── application.yml
│           └── sql/ (init_v1 + init_v2 + init_v3 + insert_test_case_v1)
├── docker/
│   └── init/init.sql                     # Docker 入口 SQL（合并 V1+V2+V3+种子数据）
├── docker-compose.yml                    # mysql + backend + frontend 三服务编排
├── .env.example                          # 环境变量模板（DB_PASSWORD / AGNES_API_KEY）
├── .dockerignore                         # 构建排除文件
├── docs/ (7 个文档)
└── frontend/
    ├── Dockerfile                        # Vue 3 + Nginx 多阶段构建
    ├── nginx.conf                        # SPA 路由 + /api 反向代理
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.js + App.vue
        ├── api/index.js + auth.js
        ├── router/index.js
        ├── utils/format.js
        ├── components/ (CategoryTree + CategoryDialog + JsonDiffViewer + ErrorPatternCard)
        └── views/ (Login + TestCaseList + TestCaseEdit + ExecutionList + DocView
                     + TestSuiteList + TestSuiteDetail + ExecutionReportList + ExecutionReportDetail)
```

## 开发进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| Phase 1 | 后端骨架（7 个基础文件） | ✅ 已完成 |
| V1 | 用例管理 + 执行 + 报告 + 日志 | ✅ 已完成 |
| V2.1 | 测试套件 + 执行报告 + HttpExecutor 升级 | ✅ 已完成 |
| V2.2 | JSON Diff + 错误模式聚合 + 批执行修复 | ✅ 已完成 |
| V3   | 分类管理（树状 3 层） | ✅ 已完成 |
| V3.1 | AI 智能生成预期结果 + OpenAPI 批量导入 | ✅ 已完成 |
| V3.2 | JWT 权限管理（登录/注册/路由守卫） | ✅ 已完成 |
| Docker | Docker 容器化（Dockerfile + Nginx + docker-compose） | ✅ 已完成 |

### Phase 1 — 已完成文件

| # | 文件 | 路径 |
|---|---|---|
| 1 | pom.xml (父) | `pom.xml` |
| 2 | pom.xml (子) | `backend/pom.xml` |
| 3 | TestPlatformApplication.java | `backend/src/main/java/com/testplatform/TestPlatformApplication.java` |
| 4 | application.yml | `backend/src/main/resources/application.yml` |
| 5 | Result.java | `backend/src/main/java/com/testplatform/common/Result.java` |
| 6 | GlobalExceptionHandler.java | `backend/src/main/java/com/testplatform/common/exception/GlobalExceptionHandler.java` |
| 7 | CorsConfig.java | `backend/src/main/java/com/testplatform/config/CorsConfig.java` |

### V1 — 已完成功能

| # | 内容 | 状态 |
|---|---|---|
| 1 | SQL 建表（test_case + execution_record） | ✅ |
| 2 | 后端实体（TestCase.java + ExecutionRecord.java） | ✅ |
| 3 | 后端 Mapper（TestCaseMapper.java + ExecutionRecordMapper.java） | ✅ |
| 4 | 后端 Service（TestCaseService + ExecutionService + HttpExecutor） | ✅ |
| 5 | 后端 Controller（TestCaseController + ExecutionController）+ SecurityConfig | ✅ |
| 6 | 前端初始化（Vue 3 + Vite + Element Plus + Axios + Router） | ✅ |
| 7 | 前端页面（测试用例列表、编辑、执行记录/日志、文档） | ✅ |
| 8 | 测试编号（test_no）支持手动录入 | ✅ |
| 9 | 预期结果 NOT NULL 三层校验（DB + 后端 + 前端） | ✅ |
| 10 | JSON 字段匹配 + 递归嵌套匹配 + 文本降级 | ✅ |
| 11 | 12 条真实种子数据 | ✅ |
| 12 | 全流程联调验证 | ✅ |

### V2.1 — 已完成功能

| # | 内容 | 状态 |
|---|---|---|
| 1 | SQL 建表（test_suite + test_suite_case + execution_report） | ✅ |
| 2 | execution_record 新增 4 字段（report_id, test_no, case_name, execute_duration） | ✅ |
| 3 | HttpResult 包装类 + HttpExecutor 返回耗时和状态码 | ✅ |
| 4 | 测试套件 CRUD（后端 + 前端） | ✅ |
| 5 | 套件-用例管理（添加/移除/批量添加） | ✅ |
| 6 | 批量执行套件 → 自动生成执行报告 | ✅ |
| 7 | 执行报告列表 + 报告详情（统计卡片 + 明细表格 + 日志弹窗） | ✅ |
| 8 | 执行记录增强（显示用例编号、名称、耗时） | ✅ |
| 9 | 批量添加用例接口（一次请求替代 N 次） | ✅ |
| 10 | 批量执行加载对话框（防止用户误以为卡死） | ✅ |
| 11 | V2.1 全流程联调验证 | ✅ |

### V2.2 — 已完成功能

| # | 内容 | 状态 |
|---|---|---|
| 1 | JSON Diff 服务（递归对比 + 嵌套 Map/List + 数值安全比较） | ✅ |
| 2 | 自动修复建议（差异分析 → 生成修复方案 → 一键应用） | ✅ |
| 3 | 双栏 JSON 对比组件（预期 vs 实际 + 差异表格 + 修复建议） | ✅ |
| 4 | 批执行 ERROR 修复（HttpStatusCodeException 捕获 4xx/5xx 响应体） | ✅ |
| 5 | ExecutionServiceImpl 重构（抽取 5 个辅助方法 + ObjectMapper 注入复用） | ✅ |
| 6 | 错误模式聚合（按 URL+Method 分组统计通过率 + 最差端点提示） | ✅ |

### V3 — 已完成：分类管理

| # | 内容 | 状态 |
|---|---|---|
| 1 | SQL 建表（test_category）+ 为 test_case 添加 category_id | ✅ |
| 2 | TestCategory 实体类 + CategoryNode DTO | ✅ |
| 3 | TestCategoryMapper | ✅ |
| 4 | CategoryService + CategoryServiceImpl（树形构建、层级校验≤3、同名唯一、删除保护） | ✅ |
| 5 | CategoryController（5 个 REST 端点） | ✅ |
| 6 | TestCase 集成 category_id 筛选（后端 + 前端可分类筛选） | ✅ |
| 7 | 前端：CategoryTree 侧边栏组件（树形导航，点击筛选） | ✅ |
| 8 | 前端：CategoryDialog 弹窗编辑（新增/编辑/删除，级联父级） | ✅ |
| 9 | 前端：TestCaseList 左右布局重构 + TestCaseEdit 内联编辑面板 + 分类选择 | ✅ |
| 10 | 导航栏从侧边栏改为顶部导航 | ✅ |
| 11 | 移除独立"执行记录"页面，改为 TestCaseList 内联执行历史弹窗 | ✅ |

### V3.1 — 已完成：AI 智能生成预期结果 + OpenAPI 批量导入

| # | 内容 | 状态 |
|---|---|---|
| 1 | Agnes AI 接入（配置类 + AiService 调 chat/completions） | ✅ |
| 2 | AiController：POST /api/ai/expected 预测预期结果 | ✅ |
| 3 | 编辑面板"AI 生成"按钮，自动回填 expectedResult | ✅ |
| 4 | OpenApiParser 手动解析 OpenAPI JSON | ✅ |
| 5 | 批量导入：POST /import-openapi 解析 + AI/本地填充预期结果（useAi 开关） | ✅ |
| 6 | SchemaToJsonGenerator：JSON Schema → 模板 JSON（本地生成，默认模式） | ✅ |
| 7 | 批量入库：POST /batch-save | ✅ |
| 8 | 前端 OpenAPI 导入对话框（粘贴→解析→AI/本地切换→预览→确认） | ✅ |

### V3.2 — 已完成：JWT 权限管理

| # | 内容 | 状态 |
|---|---|---|
| 1 | SQL 建表（user）+ 默认管理员 admin/admin123 | ✅ |
| 2 | User 实体 + UserMapper | ✅ |
| 3 | JwtUtil（HS256 签发/解析） | ✅ |
| 4 | JwtAuthFilter（Token 校验过滤器） | ✅ |
| 5 | PasswordEncoderConfig（BCrypt） | ✅ |
| 6 | UserService（登录/注册/查用户） | ✅ |
| 7 | AuthController（POST /api/auth/login, register, GET /me） | ✅ |
| 8 | SecurityConfig 更新（放行 /api/auth/**，其余需认证） | ✅ |
| 9 | 前端 Login.vue 登录/注册页面 | ✅ |
| 10 | Axios 请求拦截器自动带 Bearer Token + 401 跳登录 | ✅ |
| 11 | 路由导航守卫（未登录跳 /login） | ✅ |
| 12 | App.vue 导航栏显示用户名 + 退出 | ✅ |
| 13 | JwtAuthFilter + SecurityConfig 增加 401/403 JSON 响应 | ✅ |
| 14 | 前端 401 时显示后端错误提示后跳转登录页 | ✅ |

### V1 修复与增强记录

| # | 问题 | 修复 |
|---|---|---|
| 1 | HttpExecutor 忽略请求头和请求体 | 解析 headers JSON 并利用 HttpEntity 传入 body |
| 2 | 全部用例执行结果为 FAIL | 全等匹配改为 JSON 字段子集匹配 + 递归嵌套支持 |
| 3 | requestDetail / responseDetail 为空 | 执行时记录完整的请求和响应日志 |
| 4 | SecurityConfig 路径匹配错误导致 403 | `"*/**"` 改为 `"/**"` 并启用 `.cors()` |

### V2.1 修复与增强记录

| # | 问题 | 修复 |
|---|---|---|
| 1 | ExecutionController 返回值类型不匹配 | `Result<Void>` → `Result<ExecutionRecord>` |
| 2 | JSON 匹配逻辑中 allMatch 被 contains 覆盖 | try 块用 allMatch，catch 块用 contains 文本降级 |
| 3 | TestSuiteController 空壳 | 补全 CRUD + 用例管理 + 批量执行共 8 个端点 |
| 4 | ExecutionReportController 不存在 | 新建文件，补全 3 个端点 |
| 5 | ExecutionReportServiceImpl 全部 return null | 注入 Mapper + 完整实现 |
| 6 | ExecutionReport 字段名与数据库不匹配 | `executionTime` → `executeTime` |
| 7 | 批量添加用例慢（循环 N 次 API） | 新增 batchAddCases 批量接口，前端一次调用 |

### V3.2 修复与增强记录

| # | 问题 | 修复 |
|---|---|---|
| 1 | token 过期后后端返回 403，前端无法判断 | JwtAuthFilter 在 token 无效时直接返回 401 JSON，不再放行 |
| 2 | 无自定义认证入口点，未认证请求无明确错误响应 | SecurityConfig 添加 AuthenticationEntryPoint（401）和 AccessDeniedHandler（403） |
| 3 | 前端 401 跳转时不显示原因 | 前端拦截器先弹后端错误提示（如"token过期或无效"），1.5 秒后跳转登录页 |

## 数据库设计

### test_case（测试用例表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT(20) PK | 自增主键 |
| test_no | VARCHAR(20) UNIQUE | 测试编号（如 TC-001） |
| name | VARCHAR(255) | 用例名称 |
| category_id | BIGINT(20) | **V3** 所属分类 ID |
| request_url | VARCHAR(1024) | 请求地址 |
| request_method | VARCHAR(10) | 请求方法（GET/POST/PUT/PATCH/DELETE） |
| request_headers | TEXT | 请求头（JSON 格式） |
| request_params | TEXT | 请求参数（JSON 格式） |
| expected_result | TEXT NOT NULL | 预期结果（JSON 或文本） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### user（用户表）— V3.2 新增

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT(20) PK | 自增主键 |
| username | VARCHAR(50) UNIQUE | 用户名 |
| password | VARCHAR(255) | BCrypt 加密密码 |
| display_name | VARCHAR(100) | 显示名称 |
| role | VARCHAR(20) | 角色（USER/ADMIN） |
| create_time | DATETIME | 创建时间 |

初始管理员：admin / admin123

### test_category（分类表）— V3 新增

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT(20) PK | 自增主键 |
| parent_id | BIGINT(20) | 父级 ID（0=顶级） |
| name | VARCHAR(100) | 分类名称（同父级下唯一） |
| level | INT(11) | 层级（1-3） |
| sort_order | INT(11) | 排序序号 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### execution_record（执行记录表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT(20) PK | 自增主键 |
| test_case_id | BIGINT(20) | 关联用例 ID |
| report_id | BIGINT(20) | **V2.1** 关联报告 ID |
| test_no | VARCHAR(20) | **V2.1** 快照-用例编号 |
| case_name | VARCHAR(255) | **V2.1** 快照-用例名称 |
| execute_duration | BIGINT(20) | **V2.1** 执行耗时(ms) |
| status | VARCHAR(10) | PASS / FAIL / ERROR |
| request_detail | TEXT | 请求详情（日志） |
| response_detail | TEXT | 响应详情（日志） |
| actual_result | TEXT | 实际响应结果 |
| execute_time | DATETIME | 执行时间 |

### test_suite（测试套件表）— V2.1 新增

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT(20) PK | 自增主键 |
| name | VARCHAR(255) | 套件名称 |
| description | VARCHAR(500) | 描述 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### test_suite_case（套件-用例关联表）— V2.1 新增

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT(20) PK | 自增主键 |
| suite_id | BIGINT(20) | 套件 ID |
| case_id | BIGINT(20) | 用例 ID |
| sort_order | INT(11) | 排序序号 |

UNIQUE KEY on (suite_id, case_id)

### execution_report（执行报告表）— V2.1 新增

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT(20) PK | 自增主键 |
| suite_id | BIGINT(20) | 关联套件 ID |
| report_name | VARCHAR(255) | 报告名称 |
| total | INT(11) | 总计用例数 |
| passed | INT(11) | 通过数 |
| failed | INT(11) | 失败数 |
| errored | INT(11) | 错误数 |
| pass_rate | DECIMAL(5,2) | 通过率 % |
| status | VARCHAR(20) | RUNNING / COMPLETED |
| execute_time | DATETIME | 执行时间 |
| create_time | DATETIME | 创建时间 |

## API 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/testcases` | 查询全部用例（支持 `?categoryId=` 筛选） |
| GET | `/api/testcases/{id}` | 查询单个用例 |
| POST | `/api/testcases` | 新建用例 |
| PUT | `/api/testcases/{id}` | 修改用例 |
| DELETE | `/api/testcases/{id}` | 删除用例 |
| POST | `/api/execution-records/{testCaseId}/execute` | 执行用例 |
| GET | `/api/execution-records?testCaseId={id}` | 查询执行记录 |
| **GET** | **`/api/test-suites`** | **V2.1** 查询全部套件 |
| **GET** | **`/api/test-suites/{id}`** | **V2.1** 查询单个套件 |
| **POST** | **`/api/test-suites`** | **V2.1** 新建套件 |
| **PUT** | **`/api/test-suites/{id}`** | **V2.1** 修改套件 |
| **DELETE** | **`/api/test-suites/{id}`** | **V2.1** 删除套件 |
| **GET** | **`/api/test-suites/{id}/cases`** | **V2.1** 查询套件内用例 |
| **POST** | **`/api/test-suites/{id}/cases`** | **V2.1** 添加单个用例 |
| **POST** | **`/api/test-suites/{id}/cases/batch`** | **V2.1** 批量添加用例 |
| **DELETE** | **`/api/test-suites/{id}/cases/{caseId}`** | **V2.1** 移除用例 |
| **POST** | **`/api/test-suites/{id}/execute`** | **V2.1** 批量执行套件 |
| **GET** | **`/api/execution-reports`** | **V2.1** 查询报告列表 |
| **GET** | **`/api/execution-reports/{id}`** | **V2.1** 查询报告详情 |
| **GET** | **`/api/execution-reports/{id}/details`** | **V2.1** 查询报告明细 |
| **GET** | **`/api/categories/tree`** | **V3** 查询分类树 |
| **GET** | **`/api/categories`** | **V3** 查询分类列表 |
| **POST** | **`/api/categories`** | **V3** 新建分类 |
| **PUT** | **`/api/categories`** | **V3** 修改分类 |
| **DELETE** | **`/api/categories/{id}`** | **V3** 删除分类 |
| **POST** | **`/api/ai/expected`** | **V3.1** AI 生成预期结果 |
| **POST** | **`/api/testcases/import-openapi`** | **V3.1** 导入 OpenAPI 解析+AI 填充 |
| **POST** | **`/api/testcases/batch-save`** | **V3.1** 批量保存用例 |
| **POST** | **`/api/auth/login`** | **V3.2** 用户登录（返回 token + user） |
| **POST** | **`/api/auth/register`** | **V3.2** 用户注册 |
| **GET** | **`/api/auth/me`** | **V3.2** 获取当前用户信息（需 token） |

详见 [API.md](API.md)（完整请求/响应示例）。

> 其他文档：[sql.md](sql.md) · [进度报告.md](进度报告.md) · [开发进度.md](开发进度.md) · [阶段总结报告.md](阶段总结报告.md) · [resume.html](resume.html)

## 执行结果匹配逻辑

```
expected_result 是否为合法 JSON？
  ├─ 是 → JSON 字段子集匹配（支持递归嵌套）
  │        实际响应里找预期中的所有字段，值一致则 PASS
  │        多余字段忽略，嵌套对象递归比较
  └─ 否 → 文本 contains 降级匹配
             实际响应包含 expected_result 字符串则 PASS
```

## 种子数据

| 编号 | 名称 | 方法 | 预期结果 | 验证目的 |
|---|---|---|---|---|
| TC-001 | HTTP GET 请求 | GET | `{"url":"..."}` | 基础 JSON 匹配 |
| TC-002 | POST 提交 JSON 数据 | POST | `{"json":{...}}` | POST + 嵌套匹配 |
| TC-003 | GET 带查询参数 | GET | `{"args":{...}}` | URL 参数透传 |
| TC-004 | 自定义请求头回显 | GET | `{"headers":{...}}` | 请求头 + 递归嵌套 |
| TC-005 | JSON 结构数据 | GET | `{"slideshow":{...}}` | 深层嵌套 |
| TC-006 | 查询全部用例（自测） | GET | `{"code":200}` | 自引用测试 |
| TC-007 | 3 秒延迟请求 | GET | `{"url":"..."}` | 延迟响应 |
| TC-008 | PUT 请求测试 | PUT | `{"method":"PUT"}` | PUT 方法 |
| TC-009 | DELETE 请求测试 | DELETE | `{"method":"DELETE"}` | DELETE 方法 |
| TC-010 | 预期失败用例 | GET | `{"url":"..."}`（故意写错） | 验证 FAIL 正确触发 |
| TC-011 | 多层嵌套匹配 | GET | `{"method":"GET","headers":{...}}` | 多层递归 |
| TC-012 | 文本降级匹配 | GET | `origin` | 非 JSON 降级 |

## 启动方式

### Docker 部署（推荐）

```bash
cp .env.example .env
# 编辑 .env 填入 DB_PASSWORD
docker compose up -d
```

访问 `http://服务器IP:80`（前端）/ `http://服务器IP:8080`（后端 API）

数据库端口映射 `3307:3306`（避免本地 MySQL 冲突），MySQL 字符集 `utf8mb4`。

### 本地开发

**后端**：运行 `TestPlatformApplication.main()`，监听 `http://localhost:8080`

**前端**：在 `frontend/` 目录执行 `npm run dev`，访问 `http://localhost:3000`

**数据库**：依次执行 `init_v1.sql` → `init_v2.sql` → `init_v3.sql`，然后 `insert_test_case_v1.sql`（可选种子数据）。MySQL 连接默认 `root:1234@localhost:3306`。

## AI 接入（V3.1）

平台接入 [Agnes AI](https://platform.agnes-ai.com) 的 `agnes-2.0-flash` 模型，用于自动生成测试用例预期结果。

### 配置方式

```yaml
ai:
  agnes:
    api-key: ${AGNES_API_KEY}
    base-url: https://apihub.agnes-ai.com/v1
    model: agnes-2.0-flash
```

启动前需设置环境变量 `AGNES_API_KEY`。

### 使用场景

1. **单用例 AI 生成**：编辑页面填写请求地址/方法/参数后，点击"AI 生成"按钮，自动填充预期结果
2. **OpenAPI 批量导入**：粘贴 Swagger/OpenAPI JSON，后端解析所有端点并调用 AI 生成预期结果，预览后批量入库
