# 全功能测试平台

## 项目定位
一站式测试管理平台，支持测试用例管理、测试计划执行、测试报告生成等全流程功能。

## 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 语言 | Java | 1.8 |
| 框架 | Spring Boot | 2.7.18 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 5.7+ (驱动 8.0.33) |
| 安全 | Spring Security + JWT (jjwt 0.9.1) | — |
| 前端 *(规划中)* | Vue 3 + Element Plus | — |
| 构建 | Maven (父子模块) | — |

## 项目结构

```
test-platform/
├── pom.xml                          # 父 POM
├── docs/
│   └── PROJECT_INTRO.md             # 本文件
├── backend/
│   ├── pom.xml                      # 子模块 POM
│   └── src/main/java/com/testplatform/
│       ├── TestPlatformApplication.java
│       ├── auth/                    # JWT / 认证 (Phase 2)
│       ├── common/
│       │   ├── Result.java          # 统一响应体
│       │   └── exception/GlobalExceptionHandler.java
│       ├── config/
│       │   └── CorsConfig.java      # CORS 跨域配置
│       ├── project/                 # (预留)
│       ├── report/                  # (预留)
│       ├── testcase/                # (预留)
│       ├── testplan/                # (预留)
│       └── user/                    # 用户模块 (Phase 2)
```

## 当前进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| Phase 1 | 后端骨架 (7 个基础文件) | ✅ 已完成 |
| Phase 2 | 用户认证模块 (用户表 + JWT + 登录注册) | 🔜 进行中 |
| Phase 3 | 测试用例管理 | 📋 待规划 |
| Phase 4 | 测试计划执行 | 📋 待规划 |
| Phase 5 | 测试报告 | 📋 待规划 |
| Phase 6 | 前端开发 (Vue 3 + Element Plus) | 📋 待规划 |

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

## Phase 2 — 待创建

| # | 文件 | 包 |
|---|---|---|
| 1 | `init_user.sql` | `resources/` |
| 2 | `User.java` 实体 | `user` |
| 3 | `UserMapper.java` | `user` |
| 4 | `UserService.java` + `UserServiceImpl.java` | `user` |
| 5 | `JwtUtil.java` | `auth` |
| 6 | `JwtAuthFilter.java` | `auth` |
| 7 | `SecurityConfig.java` | `config` |
| 8 | `LoginController.java` | `auth` |
| 9 | 补充 `@MapperScan` 注解 | `TestPlatformApplication.java` |

## 启动验证

```bash
# 在 IDEA 中
# 打开项目: File → Open → test-platform
# 运行: TestPlatformApplication.main()
# 预期输出: Started TestPlatformApplication in ~2.345 seconds
```
