# 全功能测试平台

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
├── pom.xml                          # 父 POM
├── docs/
│   └── PROJECT_INTRO.md             # 项目介绍
├── backend/
│   ├── pom.xml                      # 子模块 POM
│   └── src/main/java/com/testplatform/
│       ├── TestPlatformApplication.java
│       ├── common/
│       │   ├── Result.java          # 统一响应体
│       │   └── exception/GlobalExceptionHandler.java
│       ├── config/
│       │   └── CorsConfig.java      # CORS 跨域配置
│       ├── controller/              # V1 控制器
│       ├── entity/                  # V1 实体
│       ├── mapper/                  # V1 Mapper
│       └── service/                 # V1 业务层
```

## 当前进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| Phase 1 | 后端骨架 (7 个基础文件) | ✅ 已完成 |
| V1 | 用例管理 + 执行 + 报告 + 日志 | 🔜 进行中（Step 1~6 完成） |

## Phase 1 — 已完成文件

| # | 文件 | 路径 |
|---|---|---|
| 1 | pom.xml (父) | `pom.xml` |
| 2 | pom.xml (子) | `backend/pom.xml` |
| 3 | TestPlatformApplication.java | `backend/src/main/java/com/testplatform/TestPlatformApplication.java` |
| 4 | application.yml | `backend/src/main/resources/application.yml` |
| 5 | Result.java | `backend/src/main/java/com/testplatform/common/Result.java` |
| 6 | GlobalExceptionHandler.java | `backend/src/main/java/com/testplatform/common/exception/GlobalExceptionHandler.java` |
| 7 | CorsConfig.java | `backend/src/main/java/com/testplatform/config/CorsConfig.java` |

## V1 已完成清单

| # | 内容 | 状态 |
|---|---|---|
| 1 | SQL 建表（test_case + execution_record） | ✅ |
| 2 | 后端实体（TestCase + ExecutionRecord） | ✅ |
| 3 | 后端 Mapper（TestCaseMapper + ExecutionRecordMapper） | ✅ |
| 4 | 后端 Service（TestCaseService + ExecutionService + HttpExecutor） | ✅ |
| 5 | 后端 Controller（TestCaseController + ExecutionController）+ SecurityConfig | ✅ |
| 6 | 前端初始化（Vue 3 + Vite + Element Plus + Axios + Router） | ✅ |
| 7 | 前端页面（用例列表/编辑、执行记录/日志） | ✅ |
| — | 全流程联调验证 | ⏳ 待完成 |

## 启动验证

```bash
# 在 IDEA 中
# 打开项目: File → Open → test-platform
# 运行: TestPlatformApplication.main()
# 预期输出: Started TestPlatformApplication in ~2.345 seconds
```
