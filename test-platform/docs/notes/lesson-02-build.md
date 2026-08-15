# Lesson 2: 构建镜像（Build stage）

> 目标:在 pipeline 里加 Build stage,用 `docker compose build` 构建后端+前端 Docker 镜像。
> 前置:Lesson 1.5 已完成 Dockerfile + compose 动手实验,产出 `Dockerfile-learn` + `docker-compose.learn.yml`

## 一、概念

### 为什么需要 Build stage

Lesson 1 的 Test stage 验证了代码能编译、测试能通过。但"测试通过"不等于"可部署"——部署需要把代码打包成 Docker 镜像。

**CI/CD 流水线的阶段分工:**

| 阶段 | 做什么 | 失败说明 |
|------|--------|----------|
| Test | 跑单元测试 | 代码逻辑有 bug |
| **Build** | **构建 Docker 镜像** | **Dockerfile 有问题 / 编译打包失败** |
| Deploy | 用镜像启动容器 | 端口冲突 / 依赖缺失 |
| Verify | 健康检查 | 服务没正常启动 |

Test 通过 → Build 构建镜像 → Deploy 部署 → Verify 验证。每个阶段失败都中断流水线,不会做下一步。

### docker compose build 做了什么

> Dockerfile 逐行讲解、多阶段构建、分层缓存详见 **Lesson 1.5 笔记**。本课聚焦"怎么在 Jenkins 里调 compose build"。

```bash
docker compose -f docker-compose.learn.yml build backend
```

执行过程:
1. 读 `-f` 指定的 compose 文件 → 找到 `backend` service
2. 读 `build.context` 和 `build.dockerfile` → 知道用哪个 Dockerfile
3. 执行 `docker build` → 按 Dockerfile 构建镜像

**`-f` 参数的作用:**
- 不加 `-f`:docker compose 默认找当前目录下的 `docker-compose.yml`(生产版)
- 加 `-f xxx.yml`:指定用哪个 compose 文件(学习版用 `docker-compose.learn.yml`)

> ⚠️ 本课用 `docker-compose.learn.yml`(指向 `Dockerfile-learn`),不用生产版 `docker-compose.yml`(指向原版 `Dockerfile`)。保持学习链一致:Jenkinsfile-learn → docker-compose.learn.yml → Dockerfile-learn。

### 关键认知:Jenkins sh 的默认工作目录

Jenkins 声明式 Pipeline 中,`sh '...'` 默认在 `$WORKSPACE` 执行。

```
$WORKSPACE/                          ← sh 默认在这里执行
├── test-platform/                   ← 项目目录(仓库结构)
│   ├── docker-compose.yml           ← 生产 compose
│   ├── docker-compose.learn.yml     ← 学习 compose(L2 用这个)
│   ├── backend/Dockerfile           ← 生产 Dockerfile
│   ├── backend/Dockerfile-learn       ← 学习 Dockerfile(L1.5 产出)
│   ├── frontend/Dockerfile
│   ├── frontend/Dockerfile-learn      ← 学习 Dockerfile(L1.5 产出)
│   └── Jenkinsfile-learn
└── (其他仓库文件)
```

`docker compose build` 会在**当前目录**找 compose 文件。如果当前目录是 `$WORKSPACE`,而 compose 文件在 `$WORKSPACE/test-platform/` 下 → 找不到 → 报错。

这就是 Lesson 1 里 Test stage 用 `-w "$WORKSPACE/test-platform"` 给 Maven 容器设工作目录的同类问题。但 Lesson 1 是给**临时容器**设目录,Lesson 2 需要给 **Jenkins agent 本身**切目录——用 `dir()` step。

### dir() step

```groovy
dir("test-platform") {
    sh 'docker compose -f docker-compose.learn.yml build'
}
```

`dir("路径")` 是 Jenkins 内置 step(不是 shell 命令),作用是切换后续 steps 的工作目录。路径相对于 `$WORKSPACE`。

| 方式 | 谁的目录 | 语法 |
|------|----------|------|
| L1 的 `-w "$WORKSPACE/test-platform"` | 临时 Maven 容器的工作目录 | docker run 参数 |
| L2 的 `dir("test-platform")` | Jenkins agent 本身的工作目录 | Jenkins step |

两者解决的是同一个问题(工作目录不对),但作用对象不同。

## 二、渐进式小步

### 2.1 小步 2a — 天真版(预期失败)

**写的代码:**
```groovy
stage('Build') {
    steps {
        sh 'docker compose -f docker-compose.learn.yml build'
    }
}
```

**预期坑:**
`sh` 默认在 `$WORKSPACE` 执行,而 `docker-compose.learn.yml` 在 `$WORKSPACE/test-platform/` 下 → docker compose 找不到配置文件。

**预期报错:**
```
no configuration file provided: not found
```

> ⚠️ 这个报错是否如预期,需要真实跑一次验证。跑完后回填实际报错。

---

### 2.2 小步 2b — 加 dir() 切目录

**概念:**
- `dir("test-platform")` 切到 `$WORKSPACE/test-platform/` → docker compose 能找到 `docker-compose.learn.yml`
- 路径用相对路径 `test-platform`(相对于 `$WORKSPACE`),也可用绝对路径 `"${WORKSPACE}/test-platform"`

**写的代码:**
```groovy
stage('Build') {
    steps {
        dir("test-platform") {
            sh 'docker compose -f docker-compose.learn.yml build'
        }
    }
}
```

**预期结果:** ✅ SUCCESS,构建出 backend + frontend 两个镜像。

---

### 2.3 小步 2c — 分开构建 backend / frontend

**概念:**
- `docker compose build`(不带 service 名)= 构建所有有 `build:` 的 service
- `docker compose build backend` = 只构建 backend
- 分开构建的好处:
  1. **失败定位**:backend 失败时日志不会被 frontend 的日志淹没
  2. **缓存粒度**:backend 代码改了只重建 backend,frontend 用缓存
  3. **与生产 Jenkinsfile 一致**:旧版 Jenkinsfile 也是分开构建

**写的代码:**
```groovy
stage('Build') {
    steps {
        dir("test-platform") {
            sh '''
                echo "===== 构建后端镜像 ====="
                docker compose -f docker-compose.learn.yml build backend
                echo "===== 构建前端镜像 ====="
                docker compose -f docker-compose.learn.yml build frontend
            '''
        }
    }
}
```

## 三、最终 Jenkinsfile-learn 结构(Lesson 2 完成时)

```groovy
pipeline {
    agent any
    stages {
        stage('hello') { ... }
        stage('Test') { ... }
        stage('Build') {
            steps {
                dir("test-platform") {
                    sh '''
                        echo "===== 构建后端镜像 ====="
                        docker compose -f docker-compose.learn.yml build backend
                        echo "===== 构建前端镜像 ====="
                        docker compose -f docker-compose.learn.yml build frontend
                    '''
                }
            }
        }
    }
}
```

## 四、复盘

### 2a 复盘

- **构建编号**: #11
- **状态**: ❌ FAILED（Test 通过，Build 失败）
- **报错**:
  ```
  open /var/jenkins_home/workspace/test-platform-learn/docker-compose.learn.yml: no such file or directory
  ```
- **踩的坑**:
  - `sh` 默认在 `$WORKSPACE` 执行: `/var/jenkins_home/workspace/test-platform-learn/`
  - compose 文件在 `$WORKSPACE/test-platform/docker-compose.learn.yml`
  - 路径少了 `test-platform/` 子目录 → 找不到文件
- **关键认知**:
  - `sh '...'` 的工作目录 = `$WORKSPACE`，不是项目子目录
  - 这和 Lesson 1 的 `-w "$WORKSPACE/test-platform"` 是同类问题（给容器设目录）
  - L2 需要用 `dir("test-platform")` 给 Jenkins agent 本身切目录
- **修复**: 2b 加 `dir("test-platform")`

### 2b 复盘

- **状态**: ✅ SUCCESS
- **修复**: 加 `dir("test-platform")` 切到项目目录
- **关键认知**:
  - `dir()` 是 Jenkins 内置 step,不是 shell 命令
  - `dir("test-platform")` 相对于 `$WORKSPACE`,切到 `$WORKSPACE/test-platform/`
  - 对比 L1 的 `-w`:L1 给临时 Maven 容器设目录,L2 给 Jenkins agent 本身设目录
  - 两者解决同一问题(工作目录不对),但作用对象不同
  - `dir()` 内的 `sh` 都在切后的目录执行

### 2c 复盘

- **状态**: (待跑)
- **预期**: ✅ SUCCESS,分开构建 backend + frontend
- **关键认知**:
  - 分开构建的好处:失败定位、缓存粒度、与生产 Jenkinsfile 一致
  - `docker compose build backend` 只构建 backend service
  - `docker compose build`(不带 service 名)构建所有 service

## 五、Console Output 关键片段

### 2a 失败:compose 文件找不到

```
+ docker compose -f docker-compose.learn.yml build
open /var/jenkins_home/workspace/test-platform-learn/docker-compose.learn.yml: no such file or directory
```

### 2b 成功日志

<!-- 贴 dir() 修正后的构建日志 -->
