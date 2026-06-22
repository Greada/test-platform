# 全功能测试平台 V3.2 — Docker

一站式测试管理平台，支持测试用例管理（分类树）、HTTP 执行（GET/POST/PUT/PATCH/DELETE）、JSON 字段子集匹配、测试套件、执行报告、JSON Diff 可视分析与错误模式聚合。

**技术栈**：Java 17 / Spring Boot 3.3.6 / MyBatis-Plus 3.5.9 / MySQL 5.7+ / Vue 3 + Element Plus / Nginx / Docker

## Docker 部署（推荐）

```bash
cp .env.example .env
# 编辑 .env 填入 DB_PASSWORD 和 AGNES_API_KEY
docker compose up -d
```

访问 `http://服务器IP:3000`（前端），API 地址 `http://服务器IP:8080/api`。

## 本地开发

```bash
# 数据库：依次执行 sql/init_v1.sql → init_v2.sql → init_v3.sql
# 后端
cd backend && mvn package -D"maven.test.skip=true" && java -jar target/*.jar
# 前端
cd frontend && npm install && npm run dev
```

## 版本功能

| 版本 | 功能 |
|------|------|
| V1 | 用例 CRUD + HTTP 执行 + JSON 子集匹配 + 日志 |
| V2.1 | 测试套件 + 批量执行 + 执行报告 |
| V2.2 | JSON Diff 对比 + 错误模式聚合 |
| V3 | 分类管理（树状 3 层） |
| V3.1 | AI 生成预期结果 + OpenAPI 批量导入 |
| V3.2 | JWT 权限管理（登录/注册/路由守卫） |
| Docker | 容器化部署（Dockerfile + Nginx + docker-compose） |

---

👉 **完整文档请查阅 [test-platform/docs/PROJECT_INTRO.md](test-platform/docs/PROJECT_INTRO.md)**
