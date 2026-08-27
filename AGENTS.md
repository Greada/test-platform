# 全功能测试平台

## 项目入口

实际项目根目录是 `test-platform/`（含 `pom.xml`、`backend/`、`frontend/`）。

## 技术栈

Spring Boot 3.3.6 + MyBatis-Plus 3.5.9 + MySQL 5.7 + Vue 3 + Element Plus + Vite 5

## 启动命令

```bash
# 数据库初始化（B2.1 起 Flyway 为准）：
# 【推荐】全新初始化：建空库即可（CREATE DATABASE test_platform DEFAULT CHARSET utf8mb4;），
#         后端启动时 Flyway 自动执行 db/migration/V1__init_schema.sql + V2__seed_data.sql（8 表+种子+账本）
# 【存量库】无 flyway_schema_history 的旧库：启动时自动 baseline（baseline-version=2，不执行 V1/V2，数据不动）
# 【旧手工路径（已降级）】脚本自带 CREATE/USE test_platform，无需指定库名；全新初始化语义——
#         init_v1 建表无 IF NOT EXISTS、init_v3 含 ALTER，重复执行会报错；仅用于维护存量
test-platform/backend/src/main/resources/sql/init_v1.sql
test-platform/backend/src/main/resources/sql/init_v2.sql
test-platform/backend/src/main/resources/sql/init_v3.sql   # V3 分类 + V3.2 用户
test-platform/backend/src/main/resources/sql/init_v4.sql   # V4 CI 构建记录（ci_build 表）
test-platform/backend/src/main/resources/sql/insert_test_case_v1.sql

# 后端 — 在 test-platform/ 下（测试已可正常编译运行，无需跳过）
mvn package -pl backend && java -jar backend/target/test-platform-backend-1.0.0.jar
# 或直接运行 TestPlatformApplication.main()，端口 8080
# Docker 部署：cd test-platform && docker compose up -d（端口 80/8080/3307）

# 前端 — 在 test-platform/frontend/ 下
npm install && npm run dev
# 端口 3000，/api 自动代理到 8080
```

**数据库连接**：`root:1234@localhost:3306/test_platform`（`application.yml` 明文配置，非生产用）。本地 3306 不可用时可用 WSL learn-mysql（`localhost:3309`，root/1234）：IDEA 启动时以环境变量 `SPRING_DATASOURCE_URL` 覆盖 url 即可，不改仓库配置。

## 项目结构

```
test-platform/
├── backend/src/main/java/com/testplatform/
│   ├── config/     — CorsConfig, SecurityConfig, RestTemplateConfig, JwtUtil, JwtAuthFilter, PasswordEncoderConfig
│   ├── common/     — Result, HttpResult, JsonDiffResult, ErrorPatternItem/Result, GlobalExceptionHandler
│   ├── controller/ — Auth, TestCase, Execution, TestSuite, ExecutionReport, Category, Ai（7 个 Controller）
│   ├── entity/     — User, TestCase, ExecutionRecord, TestSuite, TestSuiteCase, ExecutionReport, TestCategory
│   ├── mapper/     — 7 个 Mapper（对应 7 个 Entity）
│   └── service/    — UserService, TestCaseSvc, ExecutionSvc, HttpExecutor, TestSuiteSvc, ExecutionReportSvc,
│                     JsonDiffService, ErrorPatternService, CategoryService, AiService + impl/
└── frontend/src/
    ├── api/index.js, auth.js, ci.js
    ├── router/index.js
    ├── composables/useConfirmDelete.js
    └── views/ — Login, TestCaseList, TestCaseEdit, ExecutionList, TestSuiteList/Detail, ExecutionReportList/Detail, DocView, CiStatus
    └── components/ — CategoryTree, CategoryDialog, JsonDiffViewer, ErrorPatternCard
```

## V4 已完成（全部）

| 阶段 | 内容 |
|---|---|
| V1 | 用例 CRUD + HTTP 执行（GET/POST/PUT/DELETE）+ JSON 子集匹配 + 执行记录/日志 |
| V2.1 | 测试套件 CRUD + 批量执行 + 执行报告（统计卡片/明细/日志） |
| V2.2 | JSON Diff 双栏对比 + 自动修复建议 + 错误模式聚合 + 批执行 4xx/5xx 响应体捕获 |
| V3 | 分类管理（树状 3 层 + 联级 CRUD） |
| V3.1 | AI 智能生成预期结果 + OpenAPI 批量导入 |
| V3.2 | JWT 权限管理（登录/注册/路由守卫 + Axios token 拦截） |
| V3.3 | Jenkins CI/CD 自动化部署（Pipeline + Test 阶段 + 构建结果推送） |
| V3.4 | PR 构建流水线（Gitee API 轮询 + Commit Status 回写） |
| V4 | CI Build 构建记录持久化 + 前端看板 |
| **前端优化** | **代码质量修复 + 性能优化 + 安全加固 + composable 抽取 + 路由懒加载** |

## 执行结果匹配逻辑

1. `expected_result` 是否为合法 JSON？
2. 是 → 递归字段子集匹配（实际响应包含所有预期字段即 PASS，多余字段忽略）
3. 否 → 文本 `contains` 降级匹配

## 已知历史陷阱

- `SecurityConfig` 路径匹配曾为 `"*/**"` → 应为 `"/**"`（导致 403）
- 全等匹配已改为 JSON 子集匹配，旧逻辑已被覆盖
- `JwtUtil.parseToken` 必须是 `parseClaimsJws`（带 `s`），不是 `parseClaimsJwt`（该 API 不校验签名）
- Spring Boot 3: `antMatchers` → `requestMatchers`；`HttpComponentsClientHttpRequestFactory` → `SimpleClientHttpRequestFactory`（Duration 替代毫秒）
- jjwt 0.9.1 → 0.12.6: `SigningKeyResolverAdapter` → 移除了，直接 `Keys.hmacShaKeyFor`；`parseClaimsJws` → `parseSignedClaims`
- MyBatis-Plus 3.5.9: `BaseMapper.insert(T)` 与 `insert(Collection<T>)` 重载冲突 → **已解决**，测试文件使用 `any(TestCase.class)` 等显式类型匹配，`mvn test` 可正常编译运行（91 个测试全部通过）
- PowerShell 下 Maven `-D` 属性需引号包裹（如 `-D"maven.test.skip=true"`）否则被解析为生命周期阶段
- 未用 `spring-boot-starter-parent` 时，`spring-boot-maven-plugin` 需显式声明 `<goal>repackage</goal>` 否则生成普通 JAR 而非 fat JAR
- Shell 里 Maven 默认走 JDK 8（报"无效目标发行版: 17"），需先 `$env:JAVA_HOME = "D:\software\ms-21.0.12.1"`；IDEA 启动不受影响
- GlobalExceptionHandler 会把业务异常吞成统一 500（B3.17 待修）——**排查 500 先看控制台真实堆栈**，响应体里没有线索
- `sql/` 与 `docker/init/init.sql` 曾双轨不一致（init_v3.sql 缺分类 DDL、init_v4.sql 缺 USE，2026-08-27 已补齐）；两套并存问题待 B2.x Flyway 根治
- 判断命令成败看**退出码**而非输出文本：PowerShell 管道后 `$?` 取的是末端命令的，bash 下用 `${PIPESTATUS[0]}`；GBK 乱码会让 grep 误判
- Spring Security 自定义 Filter 挂链：锚点必须用注册表标准类（如 `LogoutFilter`，自定义类报 "does not have a registered order"）；`@Component` Filter 会被 Boot 自动注册进 Servlet 全局链 + Security 链**跑两遍**，需 `FilterRegistrationBean.setEnabled(false)` 关闭自动注册
- JWT secret 相关：`signingKey()` 若先做 SHA-256 预哈希，空/短 secret 也能生成合法密钥——长度校验必须在原始 secret 上做（JwtUtil `@PostConstruct` 已守卫）

## 注意事项

- **有测试可正常编译运行**：`backend/src/test/java/` 下 7 个测试文件共 91 个单元测试，`mvn test` 全部通过（MyBatis-Plus insert 重载冲突已解决）
- **有 CI/CD**：Jenkinsfile（7 阶段 Pipeline）+ docker-compose.yml（生产）/ docker-compose.test.yml（测试）+ scripts/pr-poller.sh（PR 轮询）+ scripts/pr-report.sh（状态回写）。详见 `test-platform/docs/本地部署与CICD搭建指南.md`
- **无 Maven Wrapper**：本地构建需预装 Maven 3.9+（CI 使用 `maven:3.9-eclipse-temurin-17` 镜像）
- **无 Linter/Formatter**：前端无 ESLint/Prettier，后端无 Checkstyle
- 前端开发时确保后端已启动（vite proxy `/api` → localhost:8080）
- SQL 初始化必须 `init_v1 → v2 → v3 → v4 → insert_test_case_v1` 顺序执行（增量 DDL；漏 v4 会导致 CI 看板接口 500）
- 12 条种子数据（TC-001 ~ TC-012）用于自测验证
- **V3.2 开始所有接口需要 JWT token**，先访问 `/login` 用 `admin/admin123` 登录

## 文档

完整文档在 `test-platform/docs/`：PROJECT_INTRO, API, sql, 进度报告, 开发进度, 阶段总结, 优化计划, 本地部署与CICD搭建指南, resume.html


<!-- open-mem-context -->
## Project Activity (auto-generated by open-mem)

### test-platform/
| ID | Type | Title | Date |
|----|------|-------|------|
| 76cee9d7-3b5c-42e7-a4b2-01b16cf3c03d | 🟣 feature | Docker 容器化测试工程 | 2026-06-22 |
| f31040b3-7ab3-458c-aa15-23e42457e8d8 | 🟣 feature | CI/CD 流水线落地集成 | 2026-06-22 |

**Key concepts:** docker, containerization, docker-compose, multi-stage-build, nginx, mysql, spa, future-roadmap, ci-cd, github-actions

### test-platform\backend/
| ID | Type | Title | Date |
|----|------|-------|------|
| 76cee9d7-3b5c-42e7-a4b2-01b16cf3c03d | 🟣 feature | Docker 容器化测试工程 | 2026-06-22 |
| f31040b3-7ab3-458c-aa15-23e42457e8d8 | 🟣 feature | CI/CD 流水线落地集成 | 2026-06-22 |

**Key concepts:** docker, containerization, docker-compose, multi-stage-build, nginx, mysql, spa, future-roadmap, ci-cd, github-actions

### test-platform\frontend/
| ID | Type | Title | Date |
|----|------|-------|------|
| 76cee9d7-3b5c-42e7-a4b2-01b16cf3c03d | 🟣 feature | Docker 容器化测试工程 | 2026-06-22 |
| f31040b3-7ab3-458c-aa15-23e42457e8d8 | 🟣 feature | CI/CD 流水线落地集成 | 2026-06-22 |

**Key concepts:** docker, containerization, docker-compose, multi-stage-build, nginx, mysql, spa, future-roadmap, ci-cd, github-actions

💡 *Use `mem-find` to search full details. Use `mem-create` to save important decisions.*
<!-- /open-mem-context -->
