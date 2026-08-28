# 全功能测试平台 V3.3 — Docker + CI/CD

一站式测试管理平台，支持测试用例管理（分类树）、HTTP 执行（GET/POST/PUT/PATCH/DELETE）、JSON 字段子集匹配、测试套件、执行报告、JSON Diff 可视分析与错误模式聚合。

**技术栈**：Java 17 / Spring Boot 3.3.13 / MyBatis-Plus 3.5.9 / MySQL 5.7+ / Flyway 10.10 / Vue 3 + Element Plus / Nginx / Docker / Jenkins CI/CD

## Docker 部署（推荐）

```bash
cp .env.example .env
# 编辑 .env 填入 DB_PASSWORD、JWT_SECRET、CI_API_KEY（必须，缺失会拒绝部署）
docker compose up -d
```

访问 `http://服务器IP`（前端，80 端口），API 地址 `http://服务器IP:8080/api`。

## 本地开发

```bash
# 数据库：建空库即可（CREATE DATABASE test_platform DEFAULT CHARSET utf8mb4;）
#         后端首次启动时 Flyway 自动建表+灌种子（db/migration/V1~V3）
# 后端
cd backend && mvn package && java -jar target/*.jar
# 前端
cd frontend && npm install && npm run dev
```

## 安全特性

- JWT 密钥强制配置（≥32 字符，缺失/过短拒绝启动）
- CORS 精确 origin 白名单
- CI 回写接口独立机器令牌鉴权（X-CI-Token，与用户 JWT 分离）
- SSRF 防护（内网/保留地址黑名单，分环境开关）
- 核心业务表软删除（deleted 字段 + MyBatis-Plus @TableLogic）

## 版本功能

| 版本 | 功能 |
|------|------|
| V1 | 用例 CRUD + HTTP 执行 + JSON 子集匹配 + 日志 |
| V2.1 | 测试套件 + 批量执行 + 执行报告 |
| V2.2 | JSON Diff 对比 + 错误模式聚合 |
| V3 | 分类管理（树状 3 层） |
| V3.1 | AI 生成预期结果 + OpenAPI 批量导入 |
| V3.2 | JWT 权限管理（登录/注册/路由守卫） |
| V3.3 | Jenkins CI/CD 自动化部署（Pipeline + crontab 自动触发） |
| V4 | CI Build 构建记录持久化 + 前端看板 |
| Docker | 容器化部署（Dockerfile + Nginx + docker-compose） |
| 优化工程 | 安全加固（阶段一）+ Flyway 数据治理（阶段二进行中），详见 [docs/优化计划.md](test-platform/docs/优化计划.md) |

---

👉 **完整文档请查阅 [test-platform/docs/PROJECT_INTRO.md](test-platform/docs/PROJECT_INTRO.md)**
