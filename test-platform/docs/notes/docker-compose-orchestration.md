# Docker Compose 容器编排教程

> 项目实战笔记 — 用 `test-platform/docker-compose.yml` 作例子
> 编写日期:2026-08-09
> 用途:为 Lesson 2/3(构建+部署)打基础

---

## 目录

- [一、为什么需要编排](#一为什么需要编排)
- [二、Docker Compose 核心概念(6 个关键字)](#二docker-compose-核心概念6-个关键字)
- [三、逐块讲解 docker-compose.yml](#三逐块讲解-docker-composeyml)
- [四、prod vs test 两套 compose 的设计](#四prod-vs-test-两套-compose-的设计)
- [五、编排的核心心智模型](#五编排的核心心智模型)
- [六、和 Lesson 2/3 衔接](#六和-lesson-23-衔接)
- [七、常见问题](#七常见问题)

---

## 一、为什么需要编排

### 痛点:手动 docker run 三个容器有多痛苦

假设没有 Docker Compose,要手动启动项目的三个容器:

```bash
# 1. 启动 MySQL
docker run -d --name tp-mysql \
    -e MYSQL_ROOT_PASSWORD=1234 \
    -e MYSQL_DATABASE=test_platform \
    -v mysql_data:/var/lib/mysql \
    -v ./docker/init:/docker-entrypoint-initdb.d \
    -p 3307:3306 \
    mysql:5.7 \
    --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci

# 2. 等 MySQL 就绪(怎么知道就绪?手动 poll?)
sleep 30   # ← 反模式,不可靠

# 3. 启动后端
docker run -d --name tp-backend \
    -e SPRING_DATASOURCE_URL="jdbc:mysql://tp-mysql:3306/test_platform" \
    -e SPRING_DATASOURCE_PASSWORD=1234 \
    -p 8080:8080 \
    tp-backend

# 4. 启动前端
docker run -d --name tp-frontend \
    -p 80:80 \
    tp-frontend
```

### 六大痛点

| 痛点 | 问题 |
|------|------|
| ① 命令冗长 | 4 条长命令,参数多,容易写错 |
| ② 启动顺序 | mysql 必须先起,backend 等 mysql 就绪,frontend 等 backend |
| ③ 就绪检测 | `sleep 30` 不可靠,快了没起来,慢了浪费时间 |
| ④ 容器互联 | backend 怎么访问 mysql?用容器名还是 IP? |
| ⑤ 数据持久化 | mysql 数据要存哪?容器删了数据丢吗? |
| ⑥ 一键启停 | 想全部启动/停止/删除,要敲 4 条命令 |

### Docker Compose 解决了什么

用**一个 YAML 文件**描述所有容器 + 它们的关系,一条命令管理全部:

```bash
docker compose up -d      # 一条命令启动全部
docker compose down        # 一条命令停止全部
docker compose logs -f     # 看所有容器日志
docker compose ps          # 看所有容器状态
```

YAML 文件就是配置的"单一真相源"——版本管理、团队共享、环境一致都靠它。

> 💡 类比:`docker run` 是手动挡(每个操作自己换挡),`docker compose` 是自动挡(设定好规则,一键启动全套)。

---

## 二、Docker Compose 核心概念(6 个关键字)

| 关键字 | 作用 | 解决的痛点 |
|--------|------|-----------|
| `services` | 定义所有容器 | ① 命令冗长:一个文件描述所有容器 |
| `depends_on` | 定义容器依赖 | ② 启动顺序:Compose 按依赖关系启动 |
| `healthcheck` | 健康检查 | ③ 就绪检测:不用 sleep,自动检测 |
| `networks`(隐式) | 容器网络 | ④ 容器互联:service 名作主机名 |
| `volumes` | 数据卷 | ⑤ 数据持久化:容器删了数据不丢 |
| `ports` | 端口映射 | 让外部能访问容器 |
| `environment` | 环境变量 | 配置注入(密码、连接串) |
| `restart` | 重启策略 | 容器挂了自动重启 |

---

## 三、逐块讲解 docker-compose.yml

文件位置:`test-platform/docker-compose.yml`(共 50 行)

### 块 1:顶层结构 + MySQL 服务(1-20 行)

```yaml
services:                    # ① 顶级关键字,下面定义所有容器
  mysql:                     # ② service 名(也是 DNS 主机名,backend 用这个名字访问 mysql)
    image: mysql:5.7         # ③ 用现成镜像(不从 Dockerfile 构建)
    container_name: tp-mysql # ④ 固定容器名(不指定则自动生成 test-platform-mysql-1)
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci
                              # ⑤ 覆盖 MySQL 默认启动参数:字符集=utf8mb4(支持中文/emoji)
    environment:              # ⑥ 环境变量注入(MySQL 官方镜像读取这些变量配置)
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD:-1234}
                              # ⑦ ${变量:-默认值}:从 .env 读 DB_PASSWORD,没有则用 1234
      MYSQL_DATABASE: test_platform
                              # 容器首次启动时自动创建这个数据库
    volumes:                  # ⑧ 数据挂载(两个挂载点)
      - mysql_data:/var/lib/mysql
                              # 命名卷:持久化数据,容器删了数据不丢
      - ./docker/init:/docker-entrypoint-initdb.d
                              # 绑定挂载:首次启动时执行这里的 .sql 初始化脚本
    ports:
      - "3307:3306"           # ⑨ 端口映射:宿主机 3307 → 容器 3306
                              #   用 3307 避开宿主机本地 MySQL 的 3306
    healthcheck:              # ⑩ 健康检查(替代 sleep 的关键机制!)
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
                              # 每 N 秒执行这条命令,返回 0 = 健康
      interval: 5s            # 每 5 秒检查一次
      timeout: 5s             # 命令超时 5 秒算不健康
      retries: 10             # 连续失败 10 次才标记 unhealthy
      start_period: 30s       # 启动后 30 秒内不检查(给 MySQL 启动时间)
    restart: unless-stopped   # ⑪ 异常退出自动重启(手动 stop 不重启)
```

### ⑩ 健康检查——编排里最重要的概念之一

取代 `sleep 30` 的**正确做法**:

```
没健康检查:sleep 30 → 不管起没起,等 30 秒 → 不可靠
有健康检查:Docker 每 5 秒执行 mysqladmin ping
           返回成功 → 标记 healthy
           30 秒内(start_period)不检查
           连续失败 10 次(50 秒)才标记 unhealthy
```

backend 可以**等 mysql healthy 再启动**——这就是下一块的 `depends_on` + `condition`。

### 块 2:后端服务(22-36 行)

```yaml
  backend:                       # service 名(前端用这个名字访问后端)
    build:                       # 不用现成镜像,从 Dockerfile 构建
      context: .                 # 构建上下文 = 当前目录(发送给 Docker daemon)
      dockerfile: backend/Dockerfile
                                 # 用哪个 Dockerfile
    container_name: tp-backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/test_platform?...
                                 # ⭐ 用 service 名 "mysql" 作主机名
                                 #   Docker Compose 内部 DNS 自动解析
                                 #   不是用 IP,不是用 container_name,是用 service 名!
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-1234}
      AGNES_API_KEY: ${AGNES_API_KEY:-}
                                 # AI 服务的 key,从 .env 读,没有则空字符串
    ports:
      - "8080:8080"              # 后端 API 端口
    depends_on:                  # ⭐ 依赖关系(编排核心!)
      mysql:
        condition: service_healthy
                                 # 等 MySQL healthy(健康检查通过)才启动 backend
                                 # 不是等"容器启动",是等"服务就绪"!
    restart: unless-stopped
```

### `depends_on` 的两种写法对比

```yaml
# 写法 A(简单):只等容器启动,不等服务就绪
depends_on:
  - backend              # backend 容器一启动就启动 frontend
                         # 问题:backend 进程可能还没准备好

# 写法 B(精确):等服务就绪
depends_on:
  mysql:
    condition: service_healthy    # 等 MySQL 健康检查通过才启动
                                    # 正确做法,但要求被依赖的有 healthcheck
```

项目里:
- backend → mysql:**写法 B**(等 MySQL healthy)✅
- frontend → backend:**写法 A**(简单依赖)← 因为 backend 没有 healthcheck

### 隐式网络(没写在 compose 文件里,但自动存在)

```yaml
# compose 文件里看不到 networks 配置,因为 Compose 自动创建了一个默认网络
# 同一 compose 项目里的所有 service 自动在同一网络里
# 可以用 service 名互相访问:mysql / backend / frontend
```

这就是为什么 `SPRING_DATASOURCE_URL` 里写 `mysql:3306`——`mysql` 是 service 名,Docker DNS 解析成 MySQL 容器 IP。

```
浏览器 → localhost:8080 → backend 容器 → mysql:3306(内部 DNS 解析)→ mysql 容器
```

### 块 3:前端服务(38-47 行)

```yaml
  frontend:
    build:
      context: ./frontend       # 构建上下文 = frontend 目录
      dockerfile: Dockerfile    # = frontend/Dockerfile
    container_name: tp-frontend
    ports:
      - "80:80"                 # HTTP 默认端口(浏览器直接 localhost 访问)
    depends_on:
      - backend                 # 简单依赖:backend 启动后 frontend 就启动
                                 # 不需要等 backend healthy(backend 没 healthcheck)
    restart: unless-stopped
```

### 块 4:顶层 volumes(49-50 行)

```yaml
volumes:               # 顶级关键字,声明命名卷
  mysql_data:          # 定义一个名为 mysql_data 的卷
                       # 被上面 mysql 服务的 volumes 引用
                       # 卷由 Docker 管理,存在 /var/lib/docker/volumes/mysql_data/_data
```

**为什么放顶层?** 这是声明,不是使用。service 里 `volumes: - mysql_data:/var/lib/mysql` 是"使用",顶层 `volumes: mysql_data:` 是"声明"。分开是为了清晰:声明在顶层,使用在各 service。

---

## 四、prod vs test 两套 compose 的设计

| 配置项 | `docker-compose.yml`(prod) | `docker-compose.test.yml`(test) |
|--------|--------------------------|--------------------------------|
| MySQL 容器名 | `tp-mysql` | `tp-test-mysql` |
| MySQL 端口 | `3307:3306` | `3308:3306` |
| Backend 容器名 | `tp-backend` | `tp-test-backend` |
| Backend 端口 | `8080:8080` | `8081:8080` |
| Frontend 容器名 | `tp-frontend` | `tp-test-frontend` |
| Frontend 端口 | `80:80` | `81:80` |
| 数据卷 | `mysql_data` | `mysql_test_data` |

### 为什么要两套

1. **容器名不冲突** → 两套环境能同时跑在同一台 Docker 上
2. **端口不冲突** → prod 用 80/8080/3307,test 用 81/8081/3308,浏览器同时访问
3. **数据隔离** → 不同卷,prod 和 test 数据库互不干扰

### 一个微妙的点:service 名都叫 `mysql`

```yaml
# docker-compose.yml(prod)
services:
  mysql:               # service 名
    container_name: tp-mysql

# docker-compose.test.yml(test)
services:
  mysql:               # service 名也叫 mysql
    container_name: tp-test-mysql
```

两个文件里 service 名都叫 `mysql`,但容器名不同。Docker Compose 用"项目名"隔离:
- prod 项目名 = `test-platform`(默认=目录名)
- test 项目名 = `test-platform`(同目录,所以默认一样!)← 这里有个坑

实际用法要加 `-p` 指定项目名,或 `COMPOSE_PROJECT_NAME` 环境变量。Lesson 2 时我们真跑 `docker compose -f docker-compose.test.yml up` 时会验证是否冲突。

---

## 五、编排的核心心智模型

记住这张图,所有编排概念都围绕它:

```
┌─ 顶层声明 ──────────────────────────────┐
│  services:  定义"有哪些容器"              │
│  volumes:   定义"有哪些数据卷"            │
│  networks:  定义"有哪些网络"(本项目隐式)  │
└────────────────────────────────────────┘
            ↓ 被引用
┌─ 每个 service ─────────────────────────┐
│  image/build:    容器从哪来              │
│  environment:    环境变量               │
│  ports:          端口映射               │
│  volumes:        数据挂载               │
│  depends_on:     依赖关系(启动顺序)     │
│  healthcheck:    健康检查               │
│  restart:        重启策略               │
└────────────────────────────────────────┘
            ↓ 运行时
┌─ 容器行为 ─────────────────────────────┐
│  Compose 按 depends_on 启动容器          │
│  有 healthcheck 的等 healthy 才启动下游   │
│  同一网络的容器用 service 名互访         │
│  命名卷的数据在容器删除后保留            │
└────────────────────────────────────────┘
```

---

## 六、和 Lesson 2/3 衔接

### Lesson 2 要做的

```bash
docker compose -f docker-compose.yml build backend frontend
```
- `-f` 指定用哪个 compose 文件
- `build` 只构建镜像,不启动容器
- 这会调用 `backend/Dockerfile` 和 `frontend/Dockerfile`

### Lesson 3 要做的

```bash
docker compose -f docker-compose.yml up -d backend frontend
```
- `up -d` 后台启动
- 只启动 backend 和 frontend(MySQL 不动,避免数据丢失)
- depends_on 生效,frontend 等 backend 启动

### 学完编排能回答的问题

1. **为什么 backend 用 `mysql:3306` 而不是 `localhost:3306`?**
   → service 名作 DNS 主机名,Compos 内部网络解析

2. **为什么 MySQL 要配 healthcheck?**
   → 让 backend 等它就绪后再启动(`depends_on` + `condition: service_healthy`)

3. **为什么 prod 和 test 端口不同?**
   → 两套环境不冲突,可同时运行

4. **命名卷和绑定挂载的区别?**
   → 前者 Docker 托管(存在 `/var/lib/docker/volumes/`),后者指定路径

5. **为什么 backend 能等 MySQL 就绪,frontend 不能等 backend 就绪?**
   → backend 依赖 MySQL 用了 `condition: service_healthy`,但 backend 本身没有 healthcheck,所以 frontend 只能用简单依赖

6. **`.env` 文件的作用?**
   → Compose 自动读取同目录的 `.env`,把里面的变量注入 `${VAR}` 引用

---

## 七、常见问题

### Q: `services` 下的名字(如 `mysql`)和 `container_name` 有什么区别?

| 名字 | 作用 | 谁用 |
|------|------|------|
| service 名(`mysql`) | Compose 内部的服务标识 + DNS 主机名 | 同网络容器互访用这个名字 |
| container_name(`tp-mysql`) | Docker 容器的实际名字 | `docker ps`、`docker exec`、`docker logs` 用这个名字 |

backend 的 `SPRING_DATASOURCE_URL` 用 `mysql:3306`(service 名),不是 `tp-mysql:3306`(container_name)。

### Q: 为什么 `ports` 用 `3307:3306` 而不是 `3306:3306`?

避免和宿主机本地 MySQL 冲突。如果宿主机也装了 MySQL(默认 3306),用 `3306:3306` 会报端口占用。用 `3307:3306` = 宿主机访问 3307,Docker 转发到容器内 3306。

### Q: `restart: unless-stopped` 和 `always` 区别?

- `always`:容器退出总是重启,**即使你手动 `docker stop`**
- `unless-stopped`:异常退出重启,**但手动 stop 后不再自启**(尊重人工干预)

生产环境常用 `unless-stopped`。

### Q: 为什么 MySQL 要挂载两个 volume?

```yaml
volumes:
  - mysql_data:/var/lib/mysql           # 命名卷:数据持久化
  - ./docker/init:/docker-entrypoint-initdb.d  # 绑定挂载:初始化脚本
```

- 第一个:数据库数据持久化,容器删了数据不丢
- 第二个:MySQL 容器**首次启动**时会自动执行 `/docker-entrypoint-initdb.d/` 下的 `.sql` 文件,用来初始化表结构和种子数据

### Q: `depends_on` 能保证 backend 等 MySQL **完全就绪**吗?

看用哪种写法:
- 简单写法(`depends_on: - mysql`):只等容器启动,**不保证 MySQL 进程就绪**
- 精确写法(`condition: service_healthy`):等 healthcheck 通过,**保证就绪**

项目里 backend 用精确写法等 MySQL healthy,这是最佳实践。但 frontend 等 backend 只能用简单写法(因为 backend 没 healthcheck),理论上 frontend 可能比 backend 进程先就绪——不过 Nginx 启动快,运行时反代能容错,实际没大问题。

---

## 变更日志

| 日期 | 变更 |
|------|------|
| 2026-08-09 | 初版创建,覆盖编排核心概念、逐块讲解、prod/test 对比、心智模型、FAQ |
