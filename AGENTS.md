# 全功能测试平台

## 项目入口

实际项目根目录是 `test-platform/`（含 `pom.xml`、`backend/`、`frontend/`）。

## 当前状态与续接指南（2026-08-27 更新）

> **跨设备续接先读这里**。完整路线图见 `test-platform/docs/优化计划.md` 的「后续路线图」章节（含每项的内容/预估/验证手段）。

**优化工程进度**（始于 2026-08-25 核查，提交 `4b556a8`→`dcbef9e`）：

- ✅ **阶段一安全加固完成**（后端 B1.1~B1.7/B1.9 + 前端 F1.1~F1.6 + 基建 I1.1~I1.3；验收 12 勾 11）
- 🔄 **阶段二数据治理过半**（Flyway V1/V2/V3 落地双路径验证 + 软删 + 索引 + 冗余脚本清理；B2.3/B2.11-14/B2.7 外键专项待做）
- ➕ 顺带完成：Maven 零告警、CVE 可达高危清零（Boot 3.3.6→3.3.13）、前端 dist 出库

**下次开工入口**（按序）：① B2.3 docker/init/init.sql ALTER 可重入守卫 → ② B2.11 application-prod.yml（注意：compose 已传 `SPRING_PROFILES_ACTIVE=prod` 但 prod 文件不存在，是空引用）+ actuator 成套上（依赖+management 配置+compose healthcheck+SecurityConfig 放行，一次讲完整故事）→ ③ B2.12 HikariCP → ④ B2.7 外键专项（先查存量孤儿数据）→ ⑤ 阶段三 IDOR（V5__creator_id 迁移）。

**环境矩阵（本机现状，换设备需重建的项标注 ✚）**：

| 项 | 值 | 跨设备说明 |
|---|---|---|
| JDK 17+ | `D:\software\ms-21.0.12.1`（shell 的 Maven 默认 JDK8，**必须先设 JAVA_HOME**） | ✚ 任何 JDK 17+ 均可 |
| MySQL | Windows 本地 3306 密码未知（root 猜不出）→ 实际用 WSL `tp-learn-mysql`（localhost:3309，root/1234） | ✚ 任意 MySQL 5.7：建空库即可，Flyway 自动迁移（存量库自动 baseline） |
| 后端启动 | IDEA，Run Configuration 环境变量：`CI_API_KEY=test-ci-token-123` + `SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3309/...` | ✚ 环境变量不进仓库，换机要重配；dev profile 自带 jwt secret/db 密码兜底 |
| WSL Jenkins | 容器 `jenkins`（:8088），job `test-platform-learn` 跑 `Jenkinsfile-learn`，手动触发（DEPLOY_ENV=prod 实际部署 learn 环境 82/8090/3309） | ✚ 换机可跳过：本地 `mvn test` + 手动冒烟即可等价验证 |
| learn 环境 | `docker-compose.learn.yml`：tp-learn-*（82/8090/3309），JWT/CI/SSRF 有兜底值开箱即用 | ✚ 同上 |
| CI 回写契约 | POST `/api/ci/builds` 需 `X-CI-Token` 头；主 Jenkinsfile 的回写**未适配会 401**（`|| true` 兜底不炸构建，丢看板数据）——Jenkinsfile 重写时修 | 契约已定，见 SecurityConfig/CiAuthFilter |

**有意设计勿"顺手修复"**：TC-006 自测用例 FAIL（token 注入已删，无凭证请求本就该失败）；SSRF 不拦域名形式的内网地址（UrlValidator 注释声明取舍）；ci_build 列名 created_at 不改（破坏性 > 命名收益）。

## 技术栈

Spring Boot 3.3.13 + MyBatis-Plus 3.5.9 + MySQL 5.7 + Vue 3 + Element Plus + Vite 5 + Flyway 10.10 + Lombok

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
│   ├── config/     — CorsConfig+CorsProperties, SecurityConfig, RestTemplateConfig, JwtUtil, JwtAuthFilter, CiAuthFilter, AiConfig, PasswordEncoderConfig
│   ├── common/     — Result, HttpResult, JsonDiffResult, ErrorPatternItem/Result, exception/GlobalExceptionHandler
│   ├── controller/ — Auth, TestCase, Execution, TestSuite, ExecutionReport, Category, Ci, Ai（8 个 Controller）
│   ├── entity/     — User, TestCase, ExecutionRecord, TestSuite, TestSuiteCase, ExecutionReport, TestCategory, CiBuild
│   ├── mapper/     — 8 个 Mapper（对应 8 个 Entity）
│   ├── util/       — UrlValidator（SSRF 防护）, OpenApiParser, SchemaToJsonGenerator
│   └── service/    — UserService, TestCaseSvc, ExecutionSvc, HttpExecutor, TestSuiteSvc, ExecutionReportSvc,
│                     JsonDiffService, ErrorPatternService, CategoryService, AiService, CiBuildService + impl/
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
- `sql/` 与 `docker/init/init.sql` 曾双轨不一致（init_v3.sql 缺分类 DDL、init_v4.sql 缺 USE，2026-08-27 已补齐）；**已由 Flyway 根治**（B2.1 起 db/migration 为准，sql/ 手工脚本组降级为存量维护路径）；docker/init/init.sql 的 ALTER 不可重入仍待 B2.3 修
- 判断命令成败看**退出码**而非输出文本：PowerShell 管道后 `$?` 取的是末端命令的，bash 下用 `${PIPESTATUS[0]}`；GBK 乱码会让 grep 误判
- Spring Security 自定义 Filter 挂链：锚点必须用注册表标准类（如 `LogoutFilter`，自定义类报 "does not have a registered order"）；`@Component` Filter 会被 Boot 自动注册进 Servlet 全局链 + Security 链**跑两遍**，需 `FilterRegistrationBean.setEnabled(false)` 关闭自动注册
- JWT secret 相关：`signingKey()` 若先做 SHA-256 预哈希，空/短 secret 也能生成合法密钥——长度校验必须在原始 secret 上做（JwtUtil `@PostConstruct` 已守卫）

## 注意事项

- **有测试可正常编译运行**：`backend/src/test/java/` 下 8 个测试文件共 **98 个单元测试**（含 UrlValidatorTest 7 条），`mvn test` 全部通过
- **Flyway 已接管 schema（B2.1 起）**：db/migration 下 V1（8 表快照）/V2（种子）/V3（软删+索引）；**新变更 = 新建 Vn 脚本，禁改历史脚本**（checksum 校验会拒启动）；存量库自动 baseline（version=2）
- **软删已生效**：TestCase/TestSuite/TestCategory/User 的 deleteById 实际是 `UPDATE deleted=1`，查询自动 `WHERE deleted=0`；手工 SQL 不受保护
- **有 CI/CD**：Jenkinsfile（7 阶段 Pipeline）+ docker-compose.yml（生产）/ docker-compose.test.yml（测试）+ scripts/pr-poller.sh（PR 轮询）+ scripts/pr-report.sh（状态回写）。详见 `test-platform/docs/本地部署与CICD搭建指南.md`
- **无 Maven Wrapper**：本地构建需预装 Maven 3.9+（CI 使用 `maven:3.9-eclipse-temurin-17` 镜像）
- **无 Linter/Formatter**：前端无 ESLint/Prettier，后端无 Checkstyle
- 前端开发时确保后端已启动（vite proxy `/api` → localhost:8080）
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
