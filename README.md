# 全功能测试平台 V2.2

一站式测试管理平台，支持测试用例管理、HTTP 执行（GET/POST/PUT/DELETE）、JSON 字段子集匹配、测试套件、执行报告、JSON Diff 可视分析与错误模式聚合。

**技术栈**：Java 1.8 / Spring Boot 2.7.18 / MyBatis-Plus 3.5.5 / MySQL 5.7+ / Vue 3 + Element Plus

**快速启动**：
- 后端（编译）：`cd test-platform/backend && mvn package -DskipTests && java -jar target/test-platform-backend-1.0.0.jar`
- 前端：`cd test-platform/frontend && npm install && npm run dev`
- 数据库（在 MySQL 中执行）：`test-platform/backend/src/main/resources/sql/init_v1.sql` + `insert_test_case_v1.sql`（V1），`init_v2.sql`（V2.1 升级）

**当前版本**：V2.2（已完成 JSON Diff 可视化、自动修复建议、错误模式聚合）

---

👉 **完整文档请查阅 [test-platform/docs/PROJECT_INTRO.md](test-platform/docs/PROJECT_INTRO.md)**
