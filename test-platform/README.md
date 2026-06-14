# 全功能测试平台 V3.1

一站式测试管理平台，支持测试用例管理、HTTP 执行、JSON 匹配、测试套件、执行报告、JSON Diff 分析与错误模式聚合。

## 快速开始

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

- 前端：http://localhost:3000
- 后端：http://localhost:8080

## 技术栈

Java 8 / Spring Boot 2.7.18 / MyBatis-Plus 3.5.5 / MySQL 5.7+ / Vue 3 + Element Plus

## AI 集成

支持接入 Agnes AI 实现自动生成预期结果。启动后端前设置：

```bash
export AGNES_API_KEY=your_api_key
```

详见 [docs/PROJECT_INTRO.md](docs/PROJECT_INTRO.md) AI 接入章节。

## 文档

| 文档 | 说明 |
|------|------|
| [docs/PROJECT_INTRO.md](docs/PROJECT_INTRO.md) | 项目详细介绍、结构、进度 |
| [docs/API.md](docs/API.md) | API 接口文档（V1 ~ V3.1） |
| [docs/sql.md](docs/sql.md) | 数据库 ER 图与表结构 |
| [docs/阶段总结报告.md](docs/阶段总结报告.md) | 阶段总结与经验教训 |
| [docs/resume.html](docs/resume.html) | 开发恢复指南 |
