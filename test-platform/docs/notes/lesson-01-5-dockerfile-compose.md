# Lesson 1.5: Dockerfile + Compose 动手实验

> 目标:从零写 Dockerfile,从零写 docker-compose 文件,建立"构建镜像"的直觉。
> 方式:命令行手动 docker build / docker compose build,不碰 Jenkins(那是 Lesson 2 的事)。
> 前置阅读:`docker-compose-orchestration.md`(Compopse 概念参考)、主指南 5.4/5.5(Dockerfile 逐行注释参考)

---

## 一、概念

### Dockerfile 是什么

Dockerfile 是一份"构建镜像的脚本"——告诉 Docker 从哪个基础镜像开始、复制什么文件、执行什么命令、暴露什么端口。

**类比:菜谱。**

| Dockerfile | 菜谱 |
|------------|------|
| `FROM maven:3.9` | "用这个牌子的锅" |
| `COPY src /app` | "把食材放进去" |
| `RUN mvn package` | "炒熟" |
| `EXPOSE 8080` | "装盘,标注这道菜配几号碟" |
| `CMD ["java", "-jar"]` | "上桌时这样端" |

### Dockerfile 的 6 个核心指令

| 指令 | 作用 | 类比 |
|------|------|------|
| `FROM` | 基础镜像(从哪开始) | 选锅 |
| `WORKDIR` | 设置工作目录 | 在哪个灶台做 |
| `COPY` | 从宿主机复制文件到镜像 | 放食材 |
| `RUN` | 在镜像内执行命令 | 炒菜 |
| `EXPOSE` | 声明端口(文档性质,不真正开端口) | 标注配几号碟 |
| `CMD` | 容器启动时默认执行的命令 | 上桌怎么端 |

> `EXPOSE` 只是声明,真正映射端口要靠 `docker run -p` 或 compose 的 `ports:`。

### build context(构建上下文)——最重要的概念

```bash
docker build -f backend/Dockerfile -t myimage .
#                                         ↑ 这个点就是 context
```

**build context = `docker build` 命令最后一个参数指定的目录。**

Docker 会把 context 目录整个发给 Docker daemon,Dockerfile 里的 `COPY` 路径都是**相对于 context** 的。

```
test-platform/              ← 如果 context 是这里(.)
├── pom.xml
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
└── frontend/
```

```dockerfile
# 如果 context = test-platform/(项目根)
COPY pom.xml .              # ✅ 能找到 test-platform/pom.xml
COPY backend/pom.xml backend/  # ✅ 能找到 test-platform/backend/pom.xml

# 如果 context = test-platform/backend/
COPY pom.xml .              # ✅ 能找到 backend/pom.xml
COPY backend/pom.xml backend/  # ❌ 找不到!backend/ 下没有 backend/pom.xml
```

**这就是最容易踩的坑**:你在 `backend/` 目录下跑 `docker build .`,context 就是 `backend/`,但 Dockerfile 里的 `COPY backend/pom.xml` 是按"项目根"写的 → 路径对不上 → 报错。

### 分层缓存(Layer Cache)

Dockerfile 的每一行指令生成一个"层"(layer)。Docker 构建时会缓存每一层。下次构建时,如果某一层的输入没变,就直接用缓存(跳过执行),这叫 **cache hit(缓存命中)**。

```dockerfile
COPY pom.xml .           # 层 1:pom.xml 没变 → CACHED
RUN mvn dependency:go-offline  # 层 2:上层没变 → CACHED
COPY backend/src backend/src   # 层 3:源码改了 → 重新执行
RUN mvn package           # 层 4:上层变了 → 重新执行
```

**关键设计:把"不常变的"(依赖)放前面,"常变的"(源码)放后面。** 这样改代码时,前面的层都命中缓存,只重新编译,不重新下载依赖。

### 多阶段构建(Multi-stage Build)

一个 Dockerfile 里写多个 `FROM`,每个 `FROM` 开始一个新阶段。最终镜像只保留最后一个阶段。

```dockerfile
# 阶段 1:build(用 Maven 编译,~400MB)
FROM maven:3.9 AS build
COPY src/ src/
RUN mvn package

# 阶段 2:run(只用 JRE 运行,~200MB)
FROM eclipse-temurin:17-jre
COPY --from=build /app/target/*.jar app.jar  # 只从阶段 1 复制 jar
CMD ["java", "-jar", "app.jar"]
```

好处:最终镜像不含 Maven/编译工具,又小又安全。

### docker compose build vs docker build

| 命令 | 怎么找 Dockerfile | context 在哪 |
|------|------------------|-------------|
| `docker build -f Dockerfile .` | `-f` 手动指定 | 命令最后的 `.` |
| `docker compose build backend` | 读 compose 文件的 `build.dockerfile` | 读 compose 文件的 `build.context` |

`docker compose build` 的好处:Dockerfile 路径和 context 都在 compose 文件里定义好了,不用记。

---

## 二、渐进式小步

### 2.1 小步 1.5a — 天真版 Dockerfile + build context 踩坑

**概念:**
- 先写一个最简单的单阶段 Dockerfile(不做缓存优化,不做多阶段)
- 用错误的 context 构建 → 看报错
- 再用正确的 context 构建 → 成功

**天真版 Dockerfile.new(已写好):**

```dockerfile
FROM maven:3.9-eclipse-temurin-17
WORKDIR /app
COPY backend/settings.xml /root/.m2/settings.xml
COPY . .
RUN mvn package -DskipTests -f backend/pom.xml -B
EXPOSE 8080
CMD ["java", "-jar", "backend/target/test-platform-backend-1.0.0.jar"]
```

逐行解读:
- `FROM maven:3.9-eclipse-temurin-17` — 用 Maven+JDK17 镜像(非 alpine,~400MB)
- `WORKDIR /app` — 设工作目录为 /app(后续命令都在这里执行)
- `COPY backend/settings.xml /root/.m2/settings.xml` — 把阿里云 Maven 源配置放到全局路径
  - Maven 默认读取 `/root/.m2/settings.xml`,放这里不需要 `-s` 参数就自动生效
  - 对比 Lesson 1 的 Test stage 用 `-s backend/settings.xml` 显式指定路径——两种方式都可以
- `COPY . .` — 把 context 目录的所有文件复制到 /app
  - ⚠️ 如果 context 是 `backend/`,这里只复制了 backend 目录的内容
- `RUN mvn package -DskipTests -f backend/pom.xml -B` — 编译打包,跳过测试
  - `-f backend/pom.xml` 暗示 context 是项目根(因为从 /app 看,backend/pom.xml 在子目录)
- `EXPOSE 8080` — 声明端口
- `CMD ["java", "-jar", "..."]` — 启动命令

**踩坑:用错 context**

```bash
cd /mnt/d/Java_all/code/test-platform/test-platform/backend
docker build -f Dockerfile.new -t tp-backend-learn .
#                                              ↑ context = backend/
```

预期报错:`COPY . .` 复制了 backend/ 的内容,但 `RUN mvn package -f backend/pom.xml` 找不到 `backend/pom.xml`(因为 context 里没有 `backend/` 子目录)。

**修正:用项目根作 context**

```bash
cd /mnt/d/Java_all/code/test-platform/test-platform
docker build -f backend/Dockerfile.new -t tp-backend-learn .
#                                                ↑ context = 项目根
```

这样 `COPY . .` 复制整个项目,`backend/pom.xml` 路径正确。

> ⚠️ 首次构建会拉取 `maven:3.9-eclipse-temurin-17`(~400MB)+ 编译,预计 5-10 分钟。

---

### 2.2 小步 1.5b — 分层缓存优化

**概念:**
- 1.5a 的 `COPY . .` 把所有文件放一层 → 改任何文件都重新执行 `RUN mvn package`(含下载依赖)
- 优化:把 `COPY pom.xml` 和 `COPY src` 分开,依赖层(pom)在前,代码层(src)在后
- 改代码时:pom 没变 → 依赖层 CACHED → 只重新编译,不重下依赖

**优化版 Dockerfile.new(1.5b 跑时更新):**

```dockerfile
FROM maven:3.9-eclipse-temurin-17
WORKDIR /app

# 依赖层(不常变)——放前面
COPY backend/settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY backend/pom.xml backend/
RUN mvn -B dependency:go-offline -pl backend

# 代码层(常变)——放后面
COPY backend/src backend/src
RUN mvn -B package -DskipTests -pl backend

EXPOSE 8080
CMD ["java", "-jar", "backend/target/test-platform-backend-1.0.0.jar"]
```

**练习:**
1. 第一次 build(全量执行,慢)
2. 改一行代码(如 `backend/src/.../TestPlatformApplication.java` 加个注释)
3. 再 build → 看日志里依赖层显示 `CACHED`,只有代码层重新执行

---

### 2.3 小步 1.5c — 多阶段构建

**概念:**
- 1.5b 是单阶段,最终镜像含 Maven(~400MB),又大又不安全
- 拆成两阶段:阶段 1 用 Maven 编译,阶段 2 只用 JRE 运行
- `COPY --from=build` 从阶段 1 复制产物到阶段 2

**多阶段版 Dockerfile.new(1.5c 跑时更新):**

```dockerfile
# 阶段 1:build(用 Maven+JDK 编译)
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY backend/settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY backend/pom.xml backend/
RUN mvn -B dependency:go-offline -pl backend
COPY backend/src backend/src
RUN mvn -B package -DskipTests -pl backend

# 阶段 2:run(只用 JRE 运行)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**对比:**
```bash
# 看镜像大小
docker images tp-backend-learn
# 1.5b 单阶段:~800MB(含 Maven + JDK + 源码 + jar)
# 1.5c 多阶段:~300MB(只有 JRE + jar)
```

**两个变化点:**
1. `AS build` — 给阶段 1 起名,阶段 2 用 `COPY --from=build` 引用
2. `ENTRYPOINT` 替代 `CMD` — ENTRYPOINT 是"固定命令",CMD 是"默认参数可覆盖"。生产镜像用 ENTRYPOINT 更安全(不会被 docker run 后面的参数误覆盖)

---

### 2.4 小步 1.5d — frontend Dockerfile(你来写)

**概念:**
- frontend 和 backend 结构类似:阶段 1 用 Node 编译,阶段 2 用 Nginx 托管
- ⚠️ context 不同:backend 的 context 是项目根(`.`),frontend 的 context 是 `./frontend`
  - 所以 frontend Dockerfile 里的 COPY 路径**不带 `frontend/` 前缀**

**你的任务:参照 1.5c 的 backend Dockerfile,写 `frontend/Dockerfile.new`**

提示:
- 阶段 1:`FROM node:20-alpine AS build`,`COPY package*.json ./`,`RUN npm ci`,`COPY . .`,`RUN npm run build`
- 阶段 2:`FROM nginx:alpine`,`COPY --from=build /app/dist /usr/share/nginx/html`,`COPY nginx.conf /etc/nginx/conf.d/default.conf`
- context 是 `./frontend`,所以 COPY 路径相对 frontend 目录

写完后贴给我审,我对比原版 `frontend/Dockerfile` 给反馈。

---

### 2.5 小步 1.5e — docker-compose.learn.yml

**概念:**
- 1.5a-d 都是手动 `docker build -f ...`
- compose 文件把"Dockerfile 路径 + context + 端口"全定义好,一条命令构建
- learn 版只含 backend + frontend(不含 mysql,L1.5 不部署,mysql 留到 L3)

**docker-compose.learn.yml(1.5e 时创建):**

```yaml
services:
  backend:
    build:
      context: .
      dockerfile: backend/Dockerfile.new
    container_name: tp-learn-backend
    ports:
      - "8090:8080"    # 用 8090 避开生产 8080

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile.new
    container_name: tp-learn-frontend
    ports:
      - "82:80"         # 用 82 避开生产 80
```

**练习:**
```bash
# 一条命令构建所有
docker compose -f docker-compose.learn.yml build

# 只构建 backend
docker compose -f docker-compose.learn.yml build backend
```

对比手动 `docker build`:不用记 `-f` 路径和 context,全在 compose 文件里。

---

## 三、L1.5 完成时的文件状态

| 文件 | 状态 |
|------|------|
| `backend/Dockerfile.new` | 多阶段版(1.5c 最终版) |
| `frontend/Dockerfile.new` | 你写的多阶段版(1.5d 审过后) |
| `docker-compose.learn.yml` | backend + frontend 两服务(1.5e) |

这些文件在 Lesson 2 会被 Jenkinsfile.new 引用:`docker compose -f docker-compose.learn.yml build`

---

## 四、复盘

### 1.5a 复盘

- **状态**: ✅ 踩坑成功 + 修正成功
- **镜像**: `tp-backend-learn:latest` = **1.26GB**(单阶段,含 Maven+JDK+源码+jar)
- **踩的坑**:
  - **build context 错误**:在 `backend/` 目录跑 `docker build -f Dockerfile.new .`
    - context = `backend/`,`COPY . .` 只复制了 backend/ 目录的内容到 /app
    - `RUN mvn package -f backend/pom.xml` 找不到 `backend/pom.xml`(因为 /app 里没有 backend/ 子目录,只有 pom.xml)
    - 报错:`POM file backend/pom.xml specified with the -f/--file command line argument does not exist`
  - **修正**:回项目根跑 `docker build -f backend/Dockerfile.new .`
    - context = 项目根,`COPY . .` 复制整个项目,`backend/pom.xml` 路径正确
  - **意外发现**:构建用的是编辑前的旧版 Dockerfile(无 settings.xml 行)
    - 日志显示 `[1/4]` 到 `[4/4]`(4 步),新版应有 5 步(多了 COPY settings.xml)
    - Maven 走中央仓库(国外)→ 耗时 459.4s(~7.5 分钟)
    - 当前 Dockerfile.new 已修复(加了 settings.xml),1.5b 重建时走阿里云会快很多
- **关键认知**:
  - **build context 是 `docker build` 最后一个参数**,Dockerfile 里所有 COPY 路径都相对于它
  - `-f` 只指定 Dockerfile 位置,**不改变 context**
  - context = 项目根时,Dockerfile 里的 `COPY backend/xxx` 才能找到文件
  - context = `backend/` 时,Dockerfile 里的 `COPY backend/xxx` 会失败(backend/ 下没有 backend/ 子目录)
  - Maven 不配国内源 → 走中央仓库 → 慢(7 分钟 vs 配源后预计 2-3 分钟)
  - 单阶段镜像 1.26GB,包含编译工具(Maven+JDK),1.5c 多阶段后会缩到 ~300MB
- **下次注意**:
  - `docker build` 命令最后的 `.` 不能随便写,要和 Dockerfile 里的 COPY 路径匹配
  - 改了 Dockerfile 后要确认构建用的是新版(看日志步数对不对)

### 1.5b 复盘

- **状态**: ✅ 分层缓存验证成功
- **数据**:
  | 构建 | 耗时 | 说明 |
  |------|------|------|
  | 1.5a 首次(无源+单层) | 459s | 走中央仓库(国外) + COPY . . 无缓存优化 |
  | 1.5b 首次(有源+分层) | 88s | 走阿里云(国内) → 提速 5.2 倍 |
  | 1.5b 第二次(缓存命中) | 11s | 依赖层 CACHED,只重编译代码层 → 提速 8 倍 |
  | **综合** | **459s → 11s** | **41 倍提速** |
- **两个提速来源拆解**:
  - 阿里云源:459s → 88s(5.2 倍) — 国外→国内的网络提速
  - 分层缓存:88s → 11s(8 倍) — 依赖层命中缓存,跳过下载
- **关键认知**:
  - 分层缓存的核心:**不常变的(pom/依赖)放前面,常变的(src)放后面**
  - Docker 按行生成层,每层有 cache key(基于输入文件内容哈希)
  - `COPY pom.xml` 和 `COPY backend/src` 分开 → 改代码时 pom 没变 → 依赖层 CACHED
  - 1.5a 的 `COPY . .` 把所有文件放一层 → 改任何文件都导致全量重执行(依赖重下)
  - 11s 的构建只执行了 `COPY backend/src` + `RUN mvn package` 两层,其余全 CACHED
- **下次注意**:
  - Dockerfile 里 COPY 顺序很重要:依赖文件(pom.xml)在前,源码在后
  - 缓存命中看时间即可验证(11s vs 88s),不必纠结日志里的 CACHED 标记

### 1.5c 复盘

- **状态**: ✅ 多阶段构建成功
- **镜像大小对比**:
  | 版本 | 镜像大小 | 包含什么 |
  |------|----------|----------|
  | 1.5b 单阶段 | 1.26GB | Maven + JDK + 源码 + jar |
  | 1.5c 多阶段 | **330MB** | JRE + jar |
  | **缩减** | **3.8 倍** | 砍掉了 Maven、JDK、源码 |
- **关键认知**:
  - 多阶段构建的核心:`COPY --from=build` 只复制产物(jar),不带编译工具
  - 阶段 1(build)用 Maven+JDK 编译,镜像大但不进最终镜像
  - 阶段 2(run)只用 JRE 运行,镜像小且安全(无编译工具=攻击面小)
  - `AS build` 给阶段命名,阶段 2 用 `COPY --from=build` 引用
  - `ENTRYPOINT` 替代 `CMD`:ENTRYPOINT 是固定命令,不会被 docker run 参数误覆盖,生产更安全
  - jar 路径从 `backend/target/*.jar` 改为 `app.jar`(COPY 时重命名,路径更干净)
- **三层优化总结**(1.5a → 1.5c):
  | 优化 | 提速/缩减 | 手段 |
  |------|-----------|------|
  | 阿里云源 | 459s → 88s(5.2 倍) | `COPY settings.xml /root/.m2/` |
  | 分层缓存 | 88s → 11s(8 倍) | COPY pom 与 COPY src 分开 |
  | 多阶段 | 1.26GB → 330MB(3.8 倍) | 双 FROM + COPY --from |
  | **综合** | **41 倍提速 + 3.8 倍瘦身** | |

### 1.5d 复盘

- **状态**: ✅ 构建成功(frontend Dockerfile 自己写 + 审阅修正)
- **数据**:
  | 项 | 值 |
  |---|---|
  | 构建耗时 | 96.8s |
  | 镜像大小 | 94.9MB(对比 backend 330MB,小 3.5 倍) |
  | 构建步数 | 15 步 |
- **踩的坑**:
  - `COPY ..` 语法错误 → `COPY . .`(COPY 需要源+目标两个参数)
  - `From` 大小写 → `FROM`(Docker 官方约定指令全大写)
  - 缺 `EXPOSE 80` + `CMD` → 补上(虽然 nginx 镜像有默认 CMD,但显式写出更清晰)
  - 缺 `frontend/.dockerignore` → node_modules(114MB)被复制进 context → 创建 .dockerignore 排除
- **关键认知**:
  - **context 不同是最大差异**:
    - backend context = 项目根(`.`)→ COPY 路径带 `backend/` 前缀
    - frontend context = `./frontend` → COPY 路径**不带** `frontend/` 前缀
  - **.dockerignore 在 context 目录下查找,不是全局**:
    - 项目根的 `.dockerignore` 对 frontend 构建无效
    - 需单独创建 `frontend/.dockerignore`
  - .dockerignore 效果:context 116MB → ~2MB(排除 node_modules 114MB + dist 1.7MB)
  - frontend 比 backend 小的原因:
    - nginx:alpine(~40MB) < eclipse-temurin:17-jre-alpine(~200MB)
    - 静态文件 dist < jar 包
- **下次注意**:
  - 写 Dockerfile 时 COPY 必须两个参数(源 + 目标)
  - 指令全大写(FROM/COPY/RUN/EXPOSE/CMD)
  - 每个 context 目录都要有自己的 .dockerignore

## 五、Console Output 关键片段

### 1.5a 踩坑:错误 context(在 backend/ 目录跑)

```
=> ERROR [4/4] RUN mvn package -DskipTests -f backend/pom.xml -B    0.7s
------
 > [4/4] RUN mvn package -DskipTests -f backend/pom.xml -B:
0.319 POM file backend/pom.xml specified with the -f/--file command line argument does not exist
0.611 POM file backend/pom.xml specified with the -f/--file command line argument does not exist
```

分析:context = `backend/`,`COPY . .` 只复制了 backend/ 内容,`/app` 里没有 `backend/` 子目录 → `backend/pom.xml` 找不到。

### 1.5a 成功:正确 context(在项目根跑)

```
[+] Building 459.4s (9/9) FINISHED
 => [internal] load build context                     29.0s
 => => transferring context: 147.29MB                 28.9s
 => [1/4] FROM docker.io/library/maven:3.9-eclipse-temurin-17
 => CACHED [2/4] WORKDIR /app
 => [3/4] COPY . .
 => [4/4] RUN mvn package -DskipTests -f backend/pom.xml -B    416.8s
 => exporting to image                                10.6s
 => => naming to docker.io/library/tp-backend-learn:latest
```

注意:
- `[1/4]` 到 `[4/4]` = 4 步,说明跑的是旧版(无 settings.xml 行);新版应为 5 步
- RUN mvn 耗时 416.8s(~7 分钟)= 走中央仓库(无国内源)
- 镜像大小 1.26GB(单阶段,含 Maven+JDK)

### 1.5b 缓存命中日志

<!-- 贴改代码后再 build 的日志,看 CACHED -->

### 1.5e 复盘

- **状态**: ✅ compose build 成功
- **数据**:
  | 项 | 值 |
  |---|---|
  | 构建耗时 | 3.5s |
  | 构建步数 | 35 步(backend 19 + frontend 15 + compose 编排层 1) |
  | 镜像 | tp-learn-backend(330MB) + tp-learn-frontend(94.9MB) |
- **为什么 3.5s 这么快**:
  - 两个镜像之前手动 `docker build` 时已构建过,Docker 缓存了每一层
  - `docker compose build` 复用这些缓存,全部 CACHED,无需重新编译
  - 对比:1.5c backend 首次 88s + 1.5d frontend 首次 96.8s = 184.8s → compose build 3.5s
- **关键认知**:
  - `docker compose build` 本质就是调用 `docker build`,共享同一个 Docker daemon 的缓存
  - 手动 `docker build` 和 `docker compose build` 产出的镜像**完全相同**(tag 不同而已)
  - compose 的价值:把 Dockerfile 路径 + context + 端口定义在 YAML 里,不用记命令参数
  - 一条命令构建所有 service:`docker compose -f xxx.yml build`
  - 也可以只构建一个:`docker compose -f xxx.yml build backend`
- **compose build vs docker build 对比**:
  | 命令 | 怎么找 Dockerfile | context 在哪 | 镜像名 |
  |------|------------------|-------------|--------|
  | `docker build -f backend/Dockerfile.new -t tp-backend-learn .` | `-f` 手动指定 | 命令最后的 `.` | `-t` 手动指定 |
  | `docker compose -f docker-compose.learn.yml build backend` | 读 compose 文件 | 读 compose 文件 | compose 自动命名 |
- **L1.5 三层优化全程总结**:
  | 优化 | 效果 | 手段 | 哪一步 |
  |------|------|------|--------|
  | 阿里云源 | 459s → 88s(5.2 倍) | settings.xml 放 /root/.m2/ | 1.5a→1.5b |
  | 分层缓存 | 88s → 11s(8 倍) | COPY pom 与 COPY src 分开 | 1.5b |
  | 多阶段构建 | 1.26GB → 330MB(3.8 倍) | 双 FROM + COPY --from | 1.5c |
  | .dockerignore | context 116MB → 2MB | 排除 node_modules/dist | 1.5d |
  | compose 编排 | 一条命令构建全部 | YAML 定义所有 service | 1.5e |
