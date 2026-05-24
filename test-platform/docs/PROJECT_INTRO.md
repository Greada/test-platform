# 全功能测试平台 V1

## 项目定位

一站式测试管理平台 V1，聚焦测试用例管理、执行、报告与日志展示。

## 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 语言 | Java | 1.8 |
| 框架 | Spring Boot | 2.7.18 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 5.7+ (驱动 8.0.33) |
| 前端 | Vue 3 + Element Plus | — |
| 构建 | Maven (父子模块) | — |

## 项目结构

```
test-platform/
├── pom.xml                              # 父 POM
├── docs/
│   ├── PROJECT_INTRO.md                 # 项目介绍（本文档）
│   └── API.md                           # API 接口文档
├── backend/
│   ├── pom.xml                          # 子模块 POM
│   └── src/main/
│       ├── java/com/testplatform/
│       │   ├── TestPlatformApplication.java
│       │   ├── common/
│       │   │   ├── Result.java          # 统一响应体
│       │   │   └── exception/GlobalExceptionHandler.java
│       │   ├── config/
│       │   │   ├── CorsConfig.java      # CORS 跨域配置
│       │   │   ├── SecurityConfig.java  # Spring Security 配置
│       │   │   └── RestTemplateConfig.java
│       │   ├── controller/
│       │   │   ├── TestCaseController.java
│       │   │   └── ExecutionController.java
│       │   ├── entity/
│       │   │   ├── TestCase.java
│       │   │   └── ExecutionRecord.java
│       │   ├── mapper/
│       │   │   ├── TestCaseMapper.java
│       │   │   └── ExecutionRecordMapper.java
│       │   └── service/
│       │       ├── TestCaseService.java
│       │       ├── ExecutionService.java
│       │       ├── HttpExecutor.java
│       │       └── impl/
│       │           ├── TestCaseServiceImpl.java
│       │           └── ExecutionServiceImpl.java
│       └── resources/
│           ├── application.yml
│           └── sql/
│               ├── init_v1.sql
│               └── insert_test_case_v1.sql
└── frontend/
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.js
        ├── App.vue
        ├── api/
        │   └── index.js
        ├── router/
        │   └── index.js
        └── views/
            ├── TestCaseList.vue
            ├── TestCaseEdit.vue
            ├── ExecutionList.vue
            └── DocView.vue
```

## 开发进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| Phase 1 | 后端骨架（7 个基础文件） | ✅ 已完成 |
| V1 | 用例管理 + 执行 + 报告 + 日志 | ✅ 已完成 |
| — | 全流程联调验证 | ✅ 已通过 |

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

### V1 修复与增强记录

| # | 问题 | 修复 |
|---|---|---|
| 1 | HttpExecutor 忽略请求头和请求体 | 解析 headers JSON 并利用 HttpEntity 传入 body |
| 2 | 全部用例执行结果为 FAIL | 全等匹配改为 JSON 字段子集匹配 + 递归嵌套支持 |
| 3 | requestDetail / responseDetail 为空 | 执行时记录完整的请求和响应日志 |
| 4 | SecurityConfig 路径匹配错误导致 403 | `"*/**"` 改为 `"/**"` 并启用 `.cors()` |

## 数据库设计

### test_case（测试用例表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT(20) PK | 自增主键 |
| test_no | VARCHAR(20) UNIQUE | 测试编号（如 TC-001） |
| name | VARCHAR(255) | 用例名称 |
| request_url | VARCHAR(1024) | 请求地址 |
| request_method | VARCHAR(10) | 请求方法（GET/POST/PUT/DELETE） |
| request_headers | TEXT | 请求头（JSON 格式） |
| request_params | TEXT | 请求参数（JSON 格式） |
| expected_result | TEXT NOT NULL | 预期结果（JSON 或文本） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### execution_record（执行记录表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT(20) PK | 自增主键 |
| test_case_id | BIGINT(20) | 关联用例 ID |
| status | VARCHAR(10) | PASS / FAIL / ERROR |
| request_detail | TEXT | 请求详情（日志） |
| response_detail | TEXT | 响应详情（日志） |
| actual_result | TEXT | 实际响应结果 |
| execute_time | DATETIME | 执行时间 |

## API 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/testcases` | 查询全部用例 |
| GET | `/api/testcases/{id}` | 查询单个用例 |
| POST | `/api/testcases` | 新建用例 |
| PUT | `/api/testcases/{id}` | 修改用例 |
| DELETE | `/api/testcases/{id}` | 删除用例 |
| POST | `/api/execution-records/{testCaseId}/execute` | 执行用例 |
| GET | `/api/execution-records?testCaseId={id}` | 查询执行记录 |

详见 [API.md](API.md)。

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

**后端**：运行 `TestPlatformApplication.main()`，监听 `http://localhost:8080`

**前端**：在 `frontend/` 目录执行 `npm run dev`，访问 `http://localhost:3000`

**数据库**：执行 `init_v1.sql` + `insert_test_case_v1.sql`，MySQL 连接默认 `root:1234@localhost:3306`
