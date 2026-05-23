# 全功能测试平台

## 项目路径
E:\Java_all\code\test-platform\test-platform\

## 打开方式
IDEA → File → Open → E:\Java_all\code\test-platform\test-platform

## 技术栈
Spring Boot 2.7.18 + MyBatis-Plus 3.5.5 + MySQL 5.7 + Vue 3 + Element Plus

## 当前进度
V1（用例管理 + 执行 + 报告 + 日志）— Step 1~6 已完成，待联调验证

## 已完成的 7 个文件（Phase 1）
| 文件 | 路径 |
|---|---|
| pom.xml (父) | pom.xml |
| pom.xml (子) | backend/pom.xml |
| TestPlatformApplication.java | backend/src/main/java/com/testplatform/TestPlatformApplication.java |
| application.yml | backend/src/main/resources/application.yml |
| Result.java | backend/src/main/java/com/testplatform/common/Result.java |
| GlobalExceptionHandler.java | backend/src/main/java/com/testplatform/common/exception/GlobalExceptionHandler.java |
| CorsConfig.java | backend/src/main/java/com/testplatform/config/CorsConfig.java |

## V1 已完成
1. ✅ SQL 建表（test_case + execution_record）
2. ✅ 后端实体（TestCase.java + ExecutionRecord.java）
3. ✅ 后端 Mapper（TestCaseMapper.java + ExecutionRecordMapper.java）
4. ✅ 后端 Service（TestCaseService + ExecutionService + HttpExecutor）
5. ✅ 后端 Controller（TestCaseController + ExecutionController）+ SecurityConfig
6. ✅ 前端初始化（Vue 3 + Vite + Element Plus + Axios + Router）
7. ✅ 前端页面（用例列表/编辑、执行记录/日志）
8. ⏳ 全流程联调验证

## 启动方式
后端：运行 TestPlatformApplication.main()，控制台输出 Started TestPlatformApplication in ~2.345 seconds
前端：在 frontend/ 目录执行 npm run dev，访问 http://localhost:3000