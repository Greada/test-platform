# 全功能测试平台

## 项目路径
E:\Java_all\code\test-platform\test-platform\

## 打开方式
IDEA → File → Open → E:\Java_all\code\test-platform\test-platform

## 技术栈
Spring Boot 2.7.18 + MyBatis-Plus 3.5.5 + MySQL 5.7 + Vue 3 + Element Plus

## 当前进度
Phase 1（后端骨架）已完成，进入 Phase 2（用户认证模块）

## 已完成的 7 个文件
| 文件 | 路径 |
|---|---|
| pom.xml (父) | pom.xml |
| pom.xml (子) | backend/pom.xml |
| TestPlatformApplication.java | backend/src/main/java/com/testplatform/TestPlatformApplication.java |
| application.yml | backend/src/main/resources/application.yml |
| Result.java | backend/src/main/java/com/testplatform/common/Result.java |
| GlobalExceptionHandler.java | backend/src/main/java/com/testplatform/common/exception/GlobalExceptionHandler.java |
| CorsConfig.java | backend/src/main/java/com/testplatform/config/CorsConfig.java |

## Phase 2 待创建（下一步）
1. SQL 建 user 表
2. User.java 实体
3. UserMapper.java
4. UserService.java + UserServiceImpl.java
5. JwtUtil.java
6. JwtAuthFilter.java
7. SecurityConfig.java
8. LoginController.java

## 验证
启动 TestPlatformApplication.main()，控制台输出 Started TestPlatformApplication in ~2.345 seconds
