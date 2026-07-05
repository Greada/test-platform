# 全功能测试平台 V3.3

一站式测试管理平台，支持测试用例管理、HTTP 执行、JSON 匹配、测试套件、执行报告、JSON Diff 分析、错误模式聚合、分类管理、AI 智能生成预期结果、OpenAPI 批量导入、JWT 权限管理、CI/CD 自动化部署。

## 快速开始

### Docker 部署（推荐）

```bash
cp .env.example .env
# 编辑 .env 填入 DB_PASSWORD
docker compose up -d
```

访问 http://localhost:80 （前端） / http://localhost:8080 （后端 API）

### 本地开发

```bash
# 后端
cd backend
mvn package -DskipTests
java -jar target/test-platform-backend-1.0.0.jar

# 前端
cd frontend
npm install
npm run dev
```

## 访问地址

- Docker 部署前端：http://localhost:80
- 本地开发前端：http://localhost:3000
- 后端 API：http://localhost:8080
- 数据库（Docker）：localhost:3307

## 技术栈

Java 17 / Spring Boot 3.3.6 / MyBatis-Plus 3.5.9 / MySQL 5.7+ / Vue 3 + Element Plus / Docker / Jenkins CI/CD

## AI 集成

支持接入 Agnes AI 实现自动生成预期结果。启动前设置环境变量：

```bash
# 本地开发
export AGNES_API_KEY=your_api_key

# Docker 部署（在 .env 中添加）
AGNES_API_KEY=your_api_key
```

详见 [docs/PROJECT_INTRO.md](docs/PROJECT_INTRO.md) AI 接入章节。

## CI/CD 自动化部署

项目使用 Jenkins Pipeline 实现自动构建部署，代码 push 后触发流程：

```bash
# 流程：Checkout → Test → Docker Build → Deploy → Verify
#
# 自动执行：
#   1. 从 Gitee 拉取最新代码
#   2. Maven 容器执行单元测试，解析结果推送后端
#   3. docker compose build backend frontend
#   4. docker compose up -d backend frontend
#   5. 验证 API 和前端可用性
```

详见 [docs/PROJECT_INTRO.md](docs/PROJECT_INTRO.md) CI/CD 接入章节。

## 文档

| 文档 | 说明 |
|------|------|
| [docs/PROJECT_INTRO.md](docs/PROJECT_INTRO.md) | 项目详细介绍、结构、进度 |
| [docs/API.md](docs/API.md) | API 接口文档（V1 ~ V3.3） |
| [docs/sql.md](docs/sql.md) | 数据库 ER 图与表结构 |
| [docs/进度报告.md](docs/进度报告.md) | 项目进度总览与里程碑 |
| [docs/开发进度.md](docs/开发进度.md) | 分阶段详细任务跟踪 |
| [docs/阶段总结报告.md](docs/阶段总结报告.md) | 阶段总结与经验教训 |
| [docs/resume.html](docs/resume.html) | 开发恢复指南 |
| [Jenkinsfile](Jenkinsfile) | CI/CD Pipeline 配置 |
