# Lesson 3: 部署 + 验证（Deploy + Verify stage）

> 目标:在 pipeline 里加 Deploy stage(启动容器)和 Verify stage(健康检查),完成 CI/CD 最后两步。
> 前置:Lesson 2 已完成 Build stage,产出 tp-learn-backend / tp-learn-frontend 镜像。

## 一、概念

### CI/CD 流水线完整四阶段

| 阶段 | 做什么 | L1-L3 对应 |
|------|--------|-----------|
| Test | 跑单元测试 | Lesson 1 |
| Build | 构建 Docker 镜像 | Lesson 1.5 + 2 |
| **Deploy** | **用镜像启动容器** | **Lesson 3a** |
| **Verify** | **健康检查** | **Lesson 3b/3c** |

### docker compose up 做了什么

```bash
docker compose -f docker-compose.learn.yml up -d
```

执行过程:
1. 读 compose 文件,找到所有 service
2. 按 `depends_on` 依赖关系决定启动顺序:
   - mysql(无依赖)→ backend(等 mysql healthy)→ frontend(等 backend 启动)
3. 启动容器(`-d` = 后台运行)
4. 返回(不等待服务就绪)

**关键认知:`up -d` 返回后容器启动了,但服务不一定就绪。** 这是 Verify stage 存在的原因。

### depends_on + healthcheck 的协作

```yaml
backend:
  depends_on:
    mysql:
      condition: service_healthy   # 等 mysql 健康检查通过才启动
```

- `depends_on` 只保证启动顺序,不保证服务就绪
- `condition: service_healthy` = 等 healthcheck 通过才算"就绪"
- mysql 有 healthcheck(mysqladmin ping)→ backend 等 mysql 真正能接受连接才启动

**但 backend 本身没有 healthcheck** → frontend 用简单依赖(`depends_on: - backend`)→ frontend 可能比 backend 的 JVM 先就绪。不过 Nginx 启动快,运行时反代能容错,实际没大问题。

### 服务启动 ≠ 服务就绪（Verify 的核心问题）

```
docker compose up -d  返回
         ↓
    容器已启动,但:
    - MySQL 还在执行 init.sql(首次启动)
    - JVM 还在初始化 Spring Boot(加载 Bean, 连数据库)
    - Nginx 可能还没监听端口
         ↓
    curl http://localhost:8090 → Connection refused
```

**解决方案:**
- 简单版(本课):`sleep 15` 等待,然后单次 curl
- 健壮版(Phase B):轮询重试,最多 N 次,每次间隔 5 秒

### curl 验证什么

| 验证目标 | curl 命令 | 预期 |
|----------|----------|------|
| 后端 API | `POST http://localhost:8090/api/auth/login` | 200 + token |
| 前端页面 | `GET http://localhost:82` | 200 + HTML |

用 `docker run --rm --network host curlimages/curl` 而非直接 curl——因为 Jenkins 容器内可能没有 curl。

## 二、渐进式小步

### 2.1 小步 3a — 更新 compose + 加 Deploy stage

**概念:**
- L1.5 的 learn compose 只有 backend + frontend(聚焦构建)
- L3 需要部署,补齐 mysql + healthcheck + depends_on
- backend 加 `SPRING_DATASOURCE_URL` 覆盖 application.yml 的 `localhost:3306` → 改为 `mysql:3306`

**compose 变更:**
- 新增 mysql 服务(含 healthcheck)
- backend 加 environment(SPRING_DATASOURCE_URL) + depends_on(mysql healthy)
- frontend 加 depends_on(backend)
- 新增 volumes(mysql_learn_data)

**Deploy stage 代码:**
```groovy
stage('Deploy') {
    steps {
        dir("test-platform") {
            sh 'docker compose -f docker-compose.learn.yml up -d'
        }
    }
}
```

**预期结果:** ✅ SUCCESS
- mysql 首次启动(拉镜像 + 执行 init.sql)
- healthcheck 通过后 → backend 启动
- backend 启动后 → frontend 启动

---

### 2.2 小步 3b — 加 Verify stage(天真版,无 sleep,预期失败)

**概念:**
- `docker compose up -d` 立即返回,JVM 还在初始化
- curl 太快 → 连接拒绝

**Verify stage 代码(天真版):**
```groovy
stage('Verify') {
    steps {
        dir("test-platform") {
            sh '''
                echo "===== 验证后端 API ====="
                HTTP_CODE=$(docker run --rm --network host curlimages/curl:latest \
                    -s -o /dev/null -w "%{http_code}" \
                    -X POST http://localhost:8090/api/auth/login \
                    -H 'Content-Type: application/json' \
                    -d '{"username":"admin","password":"admin123"}')
                echo "Backend HTTP code: $HTTP_CODE"
                [ "$HTTP_CODE" = "200" ] || exit 1

                echo "===== 验证前端页面 ====="
                HTTP_CODE=$(docker run --rm --network host curlimages/curl:latest \
                    -s -o /dev/null -w "%{http_code}" \
                    http://localhost:82)
                echo "Frontend HTTP code: $HTTP_CODE"
                [ "$HTTP_CODE" = "200" ] || exit 1
            '''
        }
    }
}
```

**预期坑:** ❌ FAILED
- `up -d` 返回后 JVM 未就绪
- curl 返回 000(Connection refused)
- `[ "$HTTP_CODE" = "200" ]` 失败 → exit 1

**教学点:服务启动 ≠ 服务就绪**

---

### 2.3 小步 3c — 加 sleep 15(修正版)

**概念:**
- 在 Verify stage 开头加 `sleep 15`,等 JVM 启动完成
- 15 秒是经验值,生产 Jenkinsfile 用的也是这个

**Verify stage 代码(修正版):**
```groovy
stage('Verify') {
    steps {
        dir("test-platform") {
            sh '''
                echo "===== 等待服务启动 ====="
                sleep 15

                echo "===== 验证后端 API ====="
                HTTP_CODE=$(docker run --rm --network host curlimages/curl:latest \
                    -s -o /dev/null -w "%{http_code}" \
                    -X POST http://localhost:8090/api/auth/login \
                    -H 'Content-Type: application/json' \
                    -d '{"username":"admin","password":"admin123"}')
                echo "Backend HTTP code: $HTTP_CODE"
                [ "$HTTP_CODE" = "200" ] || exit 1

                echo "===== 验证前端页面 ====="
                HTTP_CODE=$(docker run --rm --network host curlimages/curl:latest \
                    -s -o /dev/null -w "%{http_code}" \
                    http://localhost:82)
                echo "Frontend HTTP code: $HTTP_CODE"
                [ "$HTTP_CODE" = "200" ] || exit 1

                echo "===== 所有验证通过 ====="
            '''
        }
    }
}
```

**预期结果:** ✅ SUCCESS

## 三、最终 Jenkinsfile-learn 结构(Lesson 3 完成时)

```groovy
pipeline {
    agent any
    stages {
        stage('hello') { ... }
        stage('Test') { ... }
        stage('Build') { ... }
        stage('Deploy') {
            steps {
                dir("test-platform") {
                    sh 'docker compose -f docker-compose.learn.yml up -d'
                }
            }
        }
        stage('Verify') {
            steps {
                dir("test-platform") {
                    sh '''
                        sleep 15
                        ... curl backend ...
                        ... curl frontend ...
                    '''
                }
            }
        }
    }
}
```

## 四、复盘

### 3a 复盘

- **状态**: ✅ SUCCESS
- **踩的坑**:
  - 无（mysql healthcheck + depends_on 自动处理启动顺序）
- **关键认知**:
  - `docker compose up -d` 按 depends_on 依赖关系启动：mysql → backend → frontend
  - healthcheck + `condition: service_healthy` 让 backend 等 mysql 真正就绪才启动
  - `SPRING_DATASOURCE_URL` 必须用 environment 覆盖（localhost → mysql:3306）
  - 首次部署：拉 mysql 镜像 + 执行 init.sql + JVM 启动，耗时较长
  - `up -d` 返回后容器启动了，但服务不一定就绪（3b 会踩这个坑）

### 3b 复盘

- **状态**: ✅ SUCCESS（意外通过）
- **预期**: ❌ FAILED（curl 连接拒绝，JVM 未就绪）
- **实际**: ✅ 通过了
- **为什么意外通过**:
  - 3a 构建时 `up -d` 已经启动了容器，Deploy stage 耗时较长（拉镜像 + init.sql + JVM 启动）
  - 3b 构建时 `up -d` 检测到容器已存在，直接返回（秒级）
  - 从 3a 部署到 3b 构建，中间过了 push 代码的时间，JVM 早已就绪
  - curl 访问时 backend 已经在跑了 → 200 → 验证通过
- **教学点仍然成立**:
  - 如果是首次部署（3a 和 3b 在同一次构建里），Deploy 和 Verify 之间没有时间间隔
  - `up -d` 立即返回，curl 紧跟着执行 → 首次部署会失败
  - 本次因为 3a 已部署过，3b 只是重启已存在的容器 → JVM 已就绪
- **关键认知**:
  - 服务启动 ≠ 服务就绪——这个概念仍然正确
  - `up -d` 对已存在的容器是"重启"，对首次是"新建"，行为不同
  - 首次部署的 Verify 才会踩"JVM 未就绪"的坑
  - 3c 的 sleep 15 仍然有必要（保护首次部署场景）

### 3c 复盘

- **状态**: ✅ SUCCESS（sleep 15 保护首次部署场景）
- **改动**: Verify stage 开头加 `sleep 15`
- **关键认知**:
  - `sleep 15` 是经验值，等 JVM 初始化完成
  - 3b 意外通过是因为 3a 已部署过，JVM 早已就绪
  - 首次部署时 Deploy 和 Verify 之间无时间间隔 → sleep 是必要的
  - 生产 Jenkinsfile 用的也是 sleep 15
  - 更健壮的方案是轮询重试（Phase B Lesson 5 会讲）
- **L3 总结**:
  - Deploy: `docker compose up -d` 按 depends_on 启动容器
  - Verify: curl 验证 API + 前端页面
  - 核心问题: 服务启动 ≠ 服务就绪 → sleep 等待
  - learn compose 完整化: 补齐 mysql + healthcheck + environment + depends_on

## 五、Console Output 关键片段

<!-- 贴关键日志 -->
