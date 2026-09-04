# 本地部署与 CI/CD 搭建指南

> 项目：全功能测试平台 V3.3
> 目标：在 Windows 本机通过 WSL2 完整部署项目，并搭建 Jenkins CI/CD 流水线
> 适用场景：面试演示、学习 CI/CD 全流程
> 编写日期：2026-08-09

---

## 目录

- [零、CI/CD 核心概念（小白必读）](#零cicd-核心概念小白必读)
  - [0.1 什么是 CI/CD？](#01-什么是-cicd)
  - [0.2 什么是 Jenkins 和 Jenkinsfile？](#02-什么是-jenkins-和-jenkinsfile)
  - [0.3 什么是 Docker 和 Docker Compose？](#03-什么是-docker-和-docker-compose)
  - [0.4 什么是多阶段构建？](#04-什么是多阶段构建)
  - [0.5 什么是 PR 状态回写？](#05-什么是-pr-状态回写)
  - [0.6 一次完整的 CI/CD 流程演示](#06-一次完整的-cicd-流程演示)
- [一、环境准备](#一环境准备)
  - [1.1 安装 WSL2 + Ubuntu](#11-安装-wsl2--ubuntu)
  - [1.2 WSL2 资源限制（可选）](#12-wsl2-资源限制可选)
  - [1.3 在 WSL2 内安装 Docker](#13-在-wsl2-内安装-docker)
  - [1.4 搭建 Jenkins 容器](#14-搭建-jenkins-容器)
  - [1.4.1 docker run 命令逐行详解](#141-docker-run-命令逐行详解)
  - [1.5 Jenkins 初始化配置](#15-jenkins-初始化配置)
- [二、项目部署](#二项目部署)
  - [2.1 获取代码](#21-获取代码)
  - [2.2 配置环境变量](#22-配置环境变量)
  - [2.3 Docker Compose 启动](#23-docker-compose-启动)
  - [2.4 验证服务](#24-验证服务)
  - [2.5 常用运维命令](#25-常用运维命令)
- [三、CI/CD 流水线配置](#三cicd-流水线配置)
  - [3.1 安装 Jenkins 插件](#31-安装-jenkins-插件)
  - [3.2 创建部署 Pipeline Job](#32-创建部署-pipeline-job)
  - [3.3 创建 PR 构建 Job](#33-创建-pr-构建-job)
  - [3.4 配置 Jenkins Credentials](#34-配置-jenkins-credentials)
  - [3.5 手动触发首次构建](#35-手动触发首次构建)
  - [3.6 查看构建结果](#36-查看构建结果)
- [四、PR 状态回写配置](#四pr-状态回写配置)
  - [4.1 申请 Gitee 私人令牌](#41-申请-gitee-私人令牌)
  - [4.2 配置 .env.ci 凭据文件](#42-配置-envci-凭据文件)
  - [4.3 配置 Crontab 定时轮询](#43-配置-crontab-定时轮询)
  - [4.4 提交 PR 验证全流程](#44-提交-pr-验证全流程)
- [五、源文件完整逐行注释](#五源文件完整逐行注释)
  - [5.1 Jenkinsfile 完整逐行注释](#51-jenkinsfile-完整逐行注释)
  - [5.2 docker-compose.yml 完整逐行注释](#52-docker-composeyml-完整逐行注释)
  - [5.3 docker-compose.test.yml 与 prod 的差异](#53-docker-composeymltest-与-prod-的差异)
  - [5.4 backend/Dockerfile 完整逐行注释](#54-backenddockerfile-完整逐行注释)
  - [5.5 frontend/Dockerfile 完整逐行注释](#55-frontenddockerfile-完整逐行注释)
  - [5.6 frontend/nginx.conf 完整逐行注释](#56-frontendnginxconf-完整逐行注释)
  - [5.7 scripts/pr-poller.sh 完整逐行注释](#57-scriptspr-pollersh-完整逐行注释)
  - [5.8 scripts/pr-report.sh 完整逐行注释](#58-scriptspr-reportsh-完整逐行注释)
  - [5.9 架构总览图](#59-架构总览图)
- [六、面试演示指南](#六面试演示指南)
  - [6.1 演示流程清单](#61-演示流程清单)
  - [6.2 高频面试问题与回答](#62-高频面试问题与回答)
  - [6.3 画图讲解要点](#63-画图讲解要点)
  - [6.4 可延伸的改进话题](#64-可延伸的改进话题)
- [七、附录](#七附录)
  - [A. 命令速查表](#a-命令速查表)
  - [B. 端口与容器对照表](#b-端口与容器对照表)
  - [C. 故障排查 FAQ](#c-故障排查-faq)

---

## 零、CI/CD 核心概念（小白必读）

> 如果你是 CI/CD 新手，请务必先读完这一章，再看后面的配置和源文件注释。
> 这一章用最通俗的语言解释所有核心概念，不求严谨，只求"能懂"。

### 0.1 什么是 CI/CD？

**CI（Continuous Integration，持续集成）**

你写完代码后，不是直接合并到主分支，而是先让机器自动帮你做两件事：

1. **拉取你的代码** — 从 Git 仓库下载到构建服务器
2. **跑单元测试** — 确保你的代码没有破坏现有功能

如果测试通过 → 允许合并；如果测试失败 → 阻止合并，让你修。

> **类比：** 就像考试前先做一次模拟卷，模拟卷过了才允许你参加正式考试。

**CD（Continuous Deployment/Delivery，持续部署/交付）**

CI 测试通过后，自动把代码部署到服务器上，不需要人工 SSH 上去手动部署。

- **Continuous Delivery（持续交付）**：自动构建+测试，但部署需要人点一下"发布"按钮
- **Continuous Deployment（持续部署）**：全自动，测试通过就自动部署，无需人工介入

> **本项目是 Continuous Deployment**：测试通过后自动 `docker compose up` 部署。

### 0.2 什么是 Jenkins 和 Jenkinsfile？

**Jenkins** 是一个开源的 CI/CD 工具，说白了就是一个"自动化任务执行器"。你给它一段脚本，它就会按照脚本一步一步执行。

**Jenkinsfile** 就是这段脚本，用 Groovy 语言编写，放在代码仓库里和代码一起版本管理。这叫"Pipeline as Code"（流水线即代码）。

**Jenkinsfile 的核心结构（声明式 Pipeline）：**

```
pipeline {           ← 最外层，所有内容包在里面
    agent any        ← 在哪个节点执行（any = 任意可用节点）

    parameters { }   ← 定义构建参数（用户可选择的选项）

    stages {         ← 定义多个阶段，按顺序执行
        stage('名字') {
            steps { ← 这个阶段具体做什么
                sh '命令'      ← 执行 shell 命令
                script { }     ← 执行 Groovy 脚本
            }
            when { }  ← 条件判断，满足才执行这个阶段
            post { }  ← 阶段结束后做的事
        }
    }

    post { }  ← 整个流水线结束后做的事
}
```

**关键概念对照表：**

| 概念 | 作用 | 类比 |
|------|------|------|
| `pipeline` | 声明这是一个流水线 | "这是一个任务清单" |
| `agent any` | 在哪台机器上执行 | "在哪间厨房做饭" |
| `stage` | 一个执行阶段 | "第一步备菜、第二步炒菜、第三步装盘" |
| `steps` | 阶段内的具体操作 | "切葱、切姜、倒油" |
| `when` | 条件判断 | "如果客人要辣的，才放辣椒" |
| `post` | 阶段/流水线后的收尾 | "做完饭后洗碗" |
| `sh '...'` | 执行 shell 命令 | "执行一个终端命令" |
| `script { }` | 执行 Groovy 逻辑 | "if/else 判断" |
| `env.变量名` | 环境变量 | "全局变量，所有阶段都能用" |
| `params.参数名` | 用户传入的参数 | "用户下单时选的口味" |

### 0.3 什么是 Docker 和 Docker Compose？

**Docker** 就是"集装箱"——把你的应用和它的运行环境打包成一个"镜像"（Image），然后在任何机器上都能运行这个镜像，不用担心环境差异。

| Docker 概念 | 类比 | 说明 |
|------------|------|------|
| **镜像（Image）** | 菜谱 | 一份只读的模板，包含运行应用所需的一切 |
| **容器（Container）** | 做好的菜 | 根据镜像启动的运行实例 |
| **Dockerfile** | 菜谱的写法 | 描述如何从零构建镜像的脚本 |
| **docker build** | 照菜谱做菜 | 根据 Dockerfile 构建镜像 |
| **docker run** | 把菜端上桌 | 根据镜像启动一个容器 |

**Docker Compose** 是 Docker 的"批量管理工具"。一个项目通常需要多个容器（数据库+后端+前端），Compose 让你用一个 YAML 文件定义所有容器，一条命令启动全部。

```yaml
# docker-compose.yml 的核心结构
services:        ← 定义所有容器
  mysql:         ← 第一个容器（数据库）
    image: mysql:5.7       ← 用现成的 MySQL 镜像
    ports: ["3307:3306"]   ← 端口映射：宿主机3307 → 容器3306
  backend:       ← 第二个容器（后端）
    build: ./backend       ← 用本地 Dockerfile 构建
    depends_on: [mysql]    ← 等 mysql 启动后才启动
  frontend:      ← 第三个容器（前端）
    build: ./frontend
    depends_on: [backend]
```

**一条命令控制全部：**

| 命令 | 作用 |
|------|------|
| `docker compose up -d` | 后台启动所有容器 |
| `docker compose down` | 停止并删除所有容器 |
| `docker compose logs -f` | 查看所有容器日志 |
| `docker compose build` | 重新构建所有镜像 |
| `docker compose ps` | 查看容器状态 |

### 0.4 什么是多阶段构建？

**问题：** 后端 Java 应用需要 Maven + JDK 17 来编译，但运行时只需要 JRE 17。如果把 Maven 和 JDK 都打包进最终镜像，镜像会有 600MB；如果只放 JRE，只有 200MB。

**多阶段构建** 就是在一个 Dockerfile 里写多个 `FROM`，第一阶段用大镜像编译，第二阶段只把编译产物复制到小镜像里：

```dockerfile
# 第一阶段：用 Maven+JDK 编译（~400MB）
FROM maven:3.9-jdk17 AS build
COPY src/ src/
RUN mvn package              # 编译出 .jar 文件

# 第二阶段：只用 JRE 运行（~200MB）
FROM eclipse-temurin:17-jre
COPY --from=build /app/target/*.jar app.jar  # 只复制 jar，不复制 Maven
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**好处：**
1. 镜像小（600MB → 200MB）→ 拉取快、占用磁盘少
2. 安全（不含编译工具）→ 攻击面小
3. 缓存友好（pom.json 单独一层，改代码不重下依赖）

### 0.5 什么是 PR 状态回写？

**场景：** 你在 Gitee 上提了一个 Pull Request（PR），想把 `feature/login` 分支合并到 `main`。但是怎么保证你的代码不会破坏 main 分支？

**PR 状态回写** 就是 CI 自动在你的 PR 上打一个标记：

```
PR #42: feature/login → main
  ✅ ci/jenkins: success   ← CI 测试通过，可以合并
  ❌ ci/jenkins: failure   ← CI 测试失败，禁止合并
  ⏳ ci/jenkins: pending   ← CI 正在运行中，请等待
```

这个标记叫 **Commit Status**，通过 Gitee API 写入。Gitee 可以设置"CI 失败的 PR 不允许合并"，这就是 **PR 门禁**。

**本项目的实现方式（因为 Gitee 免费版不支持 Webhook）：**

```
开发者提 PR → Gitee
                ↑
    pr-poller.sh 每 2 分钟轮询 Gitee API
        ↓
    发现新 PR → 设置 pending 状态 → 触发 Jenkins 构建
        ↓
    Jenkins 跑完测试 → 调用 pr-report.sh → 回写 success/failure
```

> **Gitee 企业版可以用 Webhook**：PR 创建时 Gitee 主动推送通知给 Jenkins，无需轮询，延迟从 2 分钟降到秒级。

### 0.6 一次完整的 CI/CD 流程演示

以"开发者修改了一个 bug 并部署到生产环境"为例：

```
第 1 步：开发者在本地修复 bug
  - 修改 Java 代码
  - git commit -m "fix: 修复登录失败问题"
  - git push origin main

第 2 步：Jenkins 自动触发（或手动点 Build Now）
  - Jenkins 检测到 main 分支有新提交
  - 开始执行 Jenkinsfile

第 3 步：Resolve Mode（模式判断）
  - IS_PR = false（不是 PR 构建）
  - 走普通部署流程

第 4 步：Checkout（拉代码）
  - Jenkins 从 Gitee 拉取 main 分支最新代码到工作空间

第 5 步：Resolve Env（环境判断）
  - 分支是 main → DEPLOY_TARGET = prod（生产环境）

第 6 步：Test（单元测试）
  - 启动 Maven 容器
  - 执行 mvn test，跑 91 个单元测试
  - 全部通过 → 继续
  - 有失败 → 流水线中断，不会部署

第 7 步：Docker Build（构建镜像）
  - docker compose build backend  → 用 Dockerfile 构建后端镜像
  - docker compose build frontend → 用 Dockerfile 构建前端镜像

第 8 步：Deploy（部署）
  - docker compose up -d backend frontend
  - 停止旧容器 → 用新镜像启动新容器
  - MySQL 不动（避免数据丢失）

第 9 步：Verify（验证）
  - sleep 15（等 JVM 启动）
  - curl 调用登录 API → 返回 200？✅
  - curl 访问前端页面 → 返回 200？✅
  - 任一失败 → exit 1，流水线标记 FAILURE

第 10 步：完成
  - 用户访问 http://localhost → 看到新版本
  - 前端 CI 看板显示最新构建记录
```

> **如果是 PR 构建**（第 3-9 步不同）：
> - 第 3 步：IS_PR = true
> - 第 5 步：跳过（PR 不部署）
> - 第 7 步：只构建后端镜像（验证编译通过即可）
> - 第 8 步：跳过（不部署）
> - 第 9 步：跳过（不验证）
> - 新增第 9.5 步：Report PR Status → 回写 Gitee commit status

---



### 1.1 安装 WSL2 + Ubuntu

**在 Windows PowerShell（管理员）中执行：**

```powershell
# 安装 WSL2 + Ubuntu 22.04
wsl --install -d Ubuntu-22.04

# 验证安装
wsl -l -v
# 应显示：
#   NAME            STATE           VERSION
#   Ubuntu-22.04    Running         2
```

安装完成后，首次启动会提示创建 Linux 用户名和密码（记住这个密码，后续 `sudo` 要用）。

> **为什么用 WSL2 而非 Docker Desktop？**
> WSL2 更轻量、更可控，不占用额外系统托盘进程，且与 Jenkins/Docker 的 Linux 生态完全兼容。

### 1.2 WSL2 资源限制（可选）

如果本机内存充足（16GB+），可跳过此步。如需限制 WSL2 占用：

**在 Windows 中创建文件 `C:\Users\<你的用户名>\.wslconfig`：**

```ini
[wsl2]
memory=8GB
processors=4
swap=2GB
localhostForwarding=true
```

**应用配置：**

```powershell
wsl --shutdown
# 等待 8 秒后重新打开 Ubuntu 终端
```

### 1.3 在 WSL2 内安装 Docker

**打开 Ubuntu 终端，执行以下命令：**

```bash
# 更新包索引
sudo apt update && sudo apt upgrade -y

# 安装 Docker 依赖
sudo apt install -y ca-certificates curl gnupg lsb-release

# 添加 Docker 官方 GPG key
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# 添加 Docker 仓库
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装 Docker Engine + Compose 插件
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 将当前用户加入 docker 组（免 sudo）
sudo usermod -aG docker $USER

# 启动 Docker 服务
sudo systemctl enable docker
sudo systemctl start docker

# 验证（需要重新打开终端让 docker 组生效）
docker --version
docker compose version
```

**如果 `systemctl` 不可用（WSL2 旧版本）：**

```bash
# 编辑 /etc/wsl.conf 启用 systemd
sudo tee /etc/wsl.conf > /dev/null << 'EOF'
[boot]
systemd=true
EOF

# 在 Windows PowerShell 中重启 WSL
# wsl --shutdown
# 然后重新打开 Ubuntu 终端
```

**验证 Docker 运行：**

```bash
docker run hello-world
# 看到 "Hello from Docker!" 即成功
```

### 1.4 搭建 Jenkins 容器

```bash
# 创建持久化卷
docker volume create jenkins_home

# 启动 Jenkins 容器
# 关键：挂载 docker.sock 让 Jenkins 能在容器内操作宿主机 Docker
#       挂载 docker 二进制让容器内可调用 docker 命令
docker run -d --name jenkins \
  --restart unless-stopped \
  -p 8088:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $(which docker):/usr/bin/docker \
  -v /usr/libexec/docker:/usr/libexec/docker \
  -v /etc/docker:/etc/docker \
  -u root \
  jenkins/jenkins:lts-jdk17

# 查看日志，等待启动完成
docker logs -f jenkins
# 看到 "Jenkins is fully up and running" 即可 Ctrl+C 退出
```

**端口说明：**

| 端口 | 用途 |
|------|------|
| 8088 | Jenkins Web 界面 |
| 50000 | Jenkins Agent 通信（本机用不到，但保留） |

> **为什么挂载 docker.sock？**
> Jenkins 容器需要执行 `docker build`、`docker compose` 等命令。通过挂载宿主机的 `/var/run/docker.sock`，Jenkins 容器内的 docker 命令会直接调用宿主机的 Docker daemon，这种方式叫 Docker-outside-of-Docker（DooD），比 DinD 更简单且性能更好。

#### 1.4.1 docker run 命令逐行详解

这条命令的总体作用：从 `jenkins/jenkins:lts-jdk17` 镜像启动一个名为 `jenkins` 的容器，后台运行，对外暴露 Jenkins Web 和 Agent 端口，并把宿主机的 Docker 能力"借给"容器，让 Jenkins 在容器内能执行 `docker build/compose`。

下面逐段拆解每个参数的作用与"为什么需要它"。

**一、基础运行控制**

| 参数 | 作用 | 说明 |
|------|------|------|
| `docker run` | 从镜像启动新容器 | Docker 最核心命令 |
| `-d` | 后台模式（detached） | 容器在后台运行，日志用 `docker logs -f jenkins` 查看；不加 `-d` 会占用前台终端，Ctrl+C 即停容器 |
| `--name jenkins` | 固定容器名 | 后续命令可直接用 `docker logs jenkins`、`docker exec jenkins ...`；不加则生成随机名 |
| `--restart unless-stopped` | 自动重启策略 | WSL/Docker 重启、Jenkins 崩溃 → 自动拉起；但手动 `docker stop` 不会烦人地自启 |

Docker 有 4 种重启策略：

| 策略 | 行为 |
|------|------|
| `no`（默认） | 容器退出就退出，不自动重启 |
| `on-failure` | 仅进程崩溃（非 0 退出码）才重启 |
| `always` | 任何退出都重启，含手动 `docker stop` 也会重启 |
| `unless-stopped` | 异常都重启，**但手动 stop 后不再自启** |

生产环境最常用 `unless-stopped`，既保证可用性又尊重人工干预。

**二、端口映射 `-p`（格式：宿主机端口:容器端口）**

| 参数 | 映射 | 作用 |
|------|------|------|
| `-p 8088:8080` | 宿主 8088 → 容器 8080 | Jenkins Web 界面。用 8088 是因为 8080 要留给项目后端，避免冲突 |
| `-p 50000:50000` | 宿主 50000 → 容器 50000 | Jenkins Agent 通信端口。本项目单节点用不到，官方建议保留以备扩展 |

> 如果宿主机端口被占用，`docker run` 会报 `bind: address already in use`，可换成未占用端口。

**三、卷挂载 `-v`（格式：宿主机路径:容器内路径）—— 本命令核心**

| 参数 | 类型 | 作用 |
|------|------|------|
| `-v jenkins_home:/var/jenkins_home` | 命名卷 | Jenkins 数据持久化。Job 配置、构建历史、插件、凭据全存这里；容器删了重建数据不丢 |
| `-v /var/run/docker.sock:/var/run/docker.sock` | 绑定挂载 | ⭐ 最关键。把宿主机 Docker daemon 的 socket 借给容器 |
| `-v $(which docker):/usr/bin/docker` | 绑定挂载 | 把宿主机 docker CLI 二进制挂进容器，否则容器内报 `docker: command not found` |
| `-v /usr/libexec/docker:/usr/libexec/docker` | 绑定挂载 | 挂载 CLI 插件目录（含 compose、buildx），否则 `docker compose` 子命令不可用 |
| `-v /etc/docker:/etc/docker` | 绑定挂载 | 挂载 Docker 配置（含 `daemon.json` 镜像加速），保持宿主机与容器配置一致 |

**命名卷 vs 绑定挂载的区别**

| 方式 | 宿主机位置 | 特点 |
|------|-----------|------|
| 命名卷（`jenkins_home`） | Docker 决定（`/var/lib/docker/volumes/...`） | Docker 托管、权限自动正确、方便备份迁移 |
| 绑定挂载（`/var/run/docker.sock` 等） | 你指定的路径 | 直接借用宿主机特定文件/目录，精确控制 |

> **关于 `$(which docker)`**：命令里写的是 `-v $(which docker):/usr/bin/docker`，`$(which docker)` 在执行时展开为 docker 二进制的实际路径（本机是 `/usr/bin/docker`）。已确认本机路径就是 `/usr/bin/docker`，也可直接写死路径避免 shell 转义问题。

**DooD 机制详解（为什么挂载 docker.sock 是核心）**

`/var/run/docker.sock` 是 Docker daemon 的 Unix socket，所有 `docker xxx` 命令本质上都通过它和 daemon 通信。挂载后，容器内的 docker 命令经 socket 传给**宿主机**的 Docker daemon 执行——这种技术叫 **DooD（Docker-outside-of-Docker）**，对比 DinD（Docker-in-Docker，容器内再跑一个 daemon）更简单、轻量、共享镜像缓存。

Jenkinsfile 里大量调用 docker 命令，所以 Jenkins 容器必须能调 docker：

| Jenkinsfile 阶段 | 用到的 docker 命令 |
|------------------|-------------------|
| Test | `docker run maven:3.9... mvn test`（起 Maven 容器跑测试） |
| Docker Build | `docker compose build backend/frontend` |
| Deploy | `docker compose up -d backend frontend` |
| Verify | `docker run curlimages/curl ...`（起 curl 容器做健康检查） |

**四、用户与镜像**

| 参数 | 作用 | 说明 |
|------|------|------|
| `-u root` | 以 root 运行 | docker.sock 属主是 root，jenkins 用户无权访问会报 permission denied；用 root 最省事（生产环境建议用 docker 组专用用户） |
| `jenkins/jenkins:lts-jdk17` | 镜像名 | `jenkins/jenkins` 官方仓库，`lts` 长期支持版，`jdk17` 预装 JDK 17（Spring Boot 3.x 要求 Java 17） |

本地没有此镜像时 Docker 会自动 `docker pull`，走已配置的镜像加速源。

**整体数据流图**

```
┌─────────────────────────────────────────────────────┐
│  WSL2 (Ubuntu) 宿主机                                 │
│                                                       │
│  Docker Daemon ──── /var/run/docker.sock             │
│       │                      │ (挂载)                 │
│       │                      ▼                        │
│       │              ┌──────────────────────────┐    │
│       │              │  jenkins 容器 (root)      │    │
│       │   ◄──────────│ docker 命令经 socket 回传 │    │
│       │              │ /usr/bin/docker (挂载)   │    │
│       │              │ /usr/libexec/docker(挂载)│    │
│       │              │ /etc/docker (挂载)       │    │
│       │              │ :8080 Jenkins Web        │    │
│       │              └──────────────────────────┘    │
│       │                          │                    │
│       │              jenkins_home 卷 → /var/jenkins_home
│       │                          (持久化 Job/插件/凭据)│
│       ▼                                                │
│   按 Jenkinsfile 起临时容器：                           │
│   maven 容器跑测试 / docker compose 部署三容器         │
└─────────────────────────────────────────────────────┘
        ↑
   浏览器 localhost:8088 → 经 -p 8088:8080 进容器
```

### 1.5 Jenkins 初始化配置

**第一步：获取初始密码**

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
# 复制输出的 UUID
```

**第二步：访问 Jenkins Web 界面**

浏览器打开 `http://localhost:8088`

1. 粘贴初始密码 → 点击 "继续"
2. 选择 "安装推荐插件"（Install suggested plugins）
3. 等待插件安装完成（约 3-5 分钟）
4. 创建管理员用户（记住用户名和密码）

**第三步：记录 Jenkins API Token（PR 轮询要用）**

1. 登录 Jenkins → 右上角用户名 → 设置
2. 找到 "API Token" → "添加新 Token" → 命名 "ci-poller"
3. 复制生成的 Token，保存备用

---

## 二、项目部署

### 2.1 获取代码

你的代码已经在 Windows 的 `D:\Java_all\code\test-platform\test-platform\`。

在 WSL2 中可以直接访问 Windows 文件系统：

```bash
# 进入项目目录
cd /mnt/d/Java_all/code/test-platform/test-platform

# 验证文件存在
ls -la docker-compose.yml Jenkinsfile backend/Dockerfile frontend/Dockerfile
```

> **提示：** 也可以在 WSL2 内 `git clone` 一份独立副本，构建速度更快（Linux 文件系统比 NTFS 挂载快）：
> ```bash
> cd ~
> git clone https://gitee.com/greada/test-platform.git
> cd test-platform/test-platform
> ```

### 2.2 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env
nano .env
```

`.env` 文件内容：

```bash
# MySQL root 密码（改为你的强密码）
DB_PASSWORD=your_strong_password_here

# JWT 签名密钥（必须 ≥32 字符；缺失会拒绝部署——B1.1 起强制）
JWT_SECRET=change_me_32_chars_minimum

# CI 回写接口机器令牌（必须；缺失会拒绝部署——B1.4 起强制）
CI_API_KEY=change_me_32_chars_minimum

# AI 服务（可选，不设则 AI 功能降级，启动不报错——B2.13 起）
AGNES_API_KEY=

# CORS 允许的前端来源（可选，逗号分隔；不设走 application.yml 默认值——B2.14 起）
CORS_ALLOWED_ORIGINS=http://localhost:80
```

> **注意**：以上必填项与 `.env.example` 模板一致；`DB_PASSWORD`/`JWT_SECRET`/`CI_API_KEY` 三项缺失时 `docker compose up` 会直接报错拒绝插值（`:?` 强制语法），属于**有意设计**的 fail-fast。

### 2.3 Docker Compose 启动

```bash
# 在项目根目录执行
docker compose up -d

# 查看启动过程
docker compose logs -f
# 看到 "Started TestPlatformApplication" 即后端启动成功，Ctrl+C 退出日志
```

**首次启动会经历：**

1. 拉取 MySQL 5.7 镜像（~150MB）
2. 执行 `docker/init/init.sql` 初始化数据库表
3. 多阶段构建后端镜像（Maven 编译 + 打包 JAR，约 3-5 分钟）
4. 多阶段构建前端镜像（npm install + npm build，约 2-3 分钟）
5. 启动三个容器

### 2.4 验证服务

```bash
# 1. 检查容器状态（三个容器都应该是 Up）
docker compose ps

# 2. 测试后端 API
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | head -c 200

# 应返回类似：{"code":200,"message":"success","data":{"token":"eyJ...","user":{...}}}

# 3. 测试前端页面
curl -s -o /dev/null -w "%{http_code}" http://localhost:80
# 应返回 200
```

**浏览器访问：**

| 服务 | 地址 |
|------|------|
| 前端界面 | `http://localhost:80` |
| 后端 API | `http://localhost:8080/api` |
| Swagger 文档 | `http://localhost:8080/swagger-ui.html` |

**登录：** 用户名 `admin`，密码 `admin123`

### 2.5 常用运维命令

```bash
# 查看日志
docker compose logs -f backend    # 后端日志
docker compose logs -f mysql      # 数据库日志

# 重启服务
docker compose restart backend
docker compose restart

# 停止所有服务
docker compose down

# 停止并删除数据卷（⚠️ 清空数据库）
docker compose down -v

# 重新构建镜像（代码更新后）
docker compose build backend
docker compose up -d backend

# 进入容器
docker compose exec mysql mysql -u root -p test_platform
docker compose exec backend sh
```

---

## 三、CI/CD 流水线配置

### 3.1 安装 Jenkins 插件

Jenkins → 管理 Jenkins → Plugins → Available plugins

**必装插件：**

| 插件名 | 用途 |
|--------|------|
| Pipeline | 声明式流水线执行 |
| Git | Git 仓库集成 |
| Docker Pipeline | Pipeline 中使用 docker 命令 |
| Docker | Docker 集成 |
| JUnit | 测试报告展示 |

推荐插件安装时已包含大部分。检查：Jenkins → 管理 Jenkins → Plugins → Installed plugins，搜索上述名称。

### 3.2 创建部署 Pipeline Job

**Jenkins → 新建任务：**

1. **任务名称：** `test-platform`
2. **类型：** Pipeline
3. 点击 "确定"

**配置：**

| 配置项 | 值 |
|--------|-----|
| Definition | Pipeline script from SCM |
| SCM | Git |
| Repository URL | `https://gitee.com/greada/test-platform.git` |
| Credentials | （如需私有仓库，添加 Gitee 用户名/密码凭据） |
| Branches to build | `*/main` |
| Script Path | `test-platform/Jenkinsfile` |
| 轻量级检出 | ✅ 勾选 |

4. 点击 "保存"

### 3.3 创建 PR 构建 Job

**Jenkins → 新建任务：**

1. **任务名称：** `test-platform-pr-build`
2. **类型：** Pipeline
3. 点击 "确定"

**配置：**

- **General → 参数化构建过程：**
  - 添加 4 个字符串参数：

  | 名称 | 默认值 | 描述 |
  |------|--------|------|
  | `PR_NUMBER` | （空） | PR 编号 |
  | `PR_SHA` | （空） | PR 最新 commit SHA |
  | `PR_HEAD_REF` | （空） | PR 源分支名 |
  | `PR_BASE_REF` | （空） | PR 目标分支名 |

  - 添加 1 个选择参数 `DEPLOY_ENV`：
    - 选项：`auto`、`test`、`prod`
    - 默认值：`auto`

- **Pipeline → Definition：** Pipeline script from SCM
- **SCM：** 同上（`https://gitee.com/greada/test-platform.git`）
- **Script Path：** `test-platform/Jenkinsfile`
- **轻量级检出：** ✅ 勾选

4. 点击 "保存"

> **为什么需要两个 Job？**
> - `test-platform`：主 Job，由 Jenkins Web 触发或 Crontab 调用，执行完整部署流程
> - `test-platform-pr-build`：PR 构建 Job，由 `pr-poller.sh` 脚本通过 API 触发，只跑测试+构建，不部署

### 3.4 配置 Jenkins Credentials

流水线 Verify 阶段需要登录凭据（已改为从 Jenkins Credentials 读取）。

**Jenkins → 管理 Jenkins → Credentials → System → Global credentials：**

1. 点击 "添加凭据"
2. 选择类型：**Username with password**
3. 填写：
   - Username: `admin`
   - Password: `admin123`
   - ID: `tp-login-cred`（**必须与 Jenkinsfile 中一致**）
   - 描述: `测试平台登录凭据`
4. 点击 "创建"

### 3.5 手动触发首次构建

1. Jenkins → 点击 `test-platform` 任务
2. 点击 "Build Now"
3. 等待构建，点击构建编号 → "Console Output" 查看日志

**预期阶段执行（普通构建）：**

```
✅ Resolve Mode    — 普通分支
✅ Checkout        — 拉取代码
✅ Resolve Env     — 部署目标: prod
✅ Test            — 91 个测试全部通过
✅ Docker Build    — 构建后端+前端镜像
✅ Deploy          — docker compose up -d
✅ Verify          — 登录 API 正常 + 前端正常
```

**如果使用 `test` 环境：**

1. 点击任务 → "Build with Parameters"
2. `DEPLOY_ENV` 选择 `test`
3. 点击 "Build"

这会使用 `docker-compose.test.yml`，部署到测试环境（端口 81/8081/3308）。

### 3.6 查看构建结果

构建完成后，可以在以下位置查看结果：

| 位置 | 内容 |
|------|------|
| Jenkins 构建页面 | 各阶段状态、Console 日志 |
| Jenkins Test Report | 单元测试通过/失败明细 |
| 前端 CI 看板 | `http://localhost:80` → 左侧菜单 "CI 状态" |
| 后端 API | `GET /api/ci/builds` 获取构建记录 JSON |

前端 CI 看板展示每次构建的：构建编号、总测试数、通过数、失败数、通过率、构建状态（SUCCESS/FAILURE）、Jenkins 链接。

---

## 四、PR 状态回写配置

### 4.1 申请 Gitee 私人令牌

1. 登录 Gitee → 右上角头像 → 设置
2. 左侧菜单 → 私人令牌 → 生成新令牌
3. 勾选权限：`projects`、`pull_requests`、`commits`
4. 生成后 **立即复制**（只显示一次）

### 4.2 配置 .env.ci 凭据文件

```bash
# 创建凭据文件
sudo mkdir -p /opt
sudo nano /opt/.env.ci
```

**文件内容（替换为你的真实值）：**

```bash
# Gitee 配置
GITEE_OWNER=greada
GITEE_REPO=test-platform
GITEE_TOKEN=你的Gitee私人令牌

# Jenkins 配置
JENKINS_URL=http://localhost:8088
JENKINS_USER=admin
JENKINS_TOKEN=你的Jenkins_API_Token
# pr-poller.sh 在 WSL 内部调用 Jenkins，用 localhost:8080
# （Jenkins 容器内部端口，不需要经过 Windows 端口映射）
JENKINS_INTERNAL_URL=http://localhost:8080
```

**设置权限：**

```bash
sudo chmod 600 /opt/.env.ci
```

> **注意 `JENKINS_INTERNAL_URL`：**
> pr-poller.sh 在 WSL2 内运行，通过 `curl` 调用 Jenkins API 触发构建。
> - `JENKINS_URL`（8088）：给 Gitee 回写的链接，用户从浏览器访问
> - `JENKINS_INTERNAL_URL`（8080）：pr-poller.sh 内部调用 Jenkins 用，直接访问容器端口

### 4.3 配置 Crontab 定时轮询

```bash
# 编辑 crontab
crontab -e
```

**添加以下内容（每 2 分钟轮询一次）：**

```bash
# test-platform PR 轮询
*/2 * * * * /bin/bash /mnt/d/Java_all/code/test-platform/test-platform/scripts/pr-poller.sh >> /tmp/pr-poller-cron.log 2>&1
```

**验证 crontab 已添加：**

```bash
crontab -l
# 应显示上面的轮询任务
```

**手动测试 pr-poller.sh：**

```bash
# 先手动运行一次看是否有报错
bash /mnt/d/Java_all/code/test-platform/test-platform/scripts/pr-poller.sh

# 查看日志
cat /tmp/pr-poller.log
```

### 4.4 提交 PR 验证全流程

**完整演示流程：**

1. **在 Gitee 上创建分支：**
   ```bash
   # 在本地创建一个 feature 分支
   git checkout -b feature/test-ci
   # 做一个小改动（比如修改 README）
   echo "CI test" >> README.md
   git add . && git commit -m "test CI pipeline"
   git push origin feature/test-ci
   ```

2. **在 Gitee 上创建 Pull Request：**
   - 源分支：`feature/test-ci`
   - 目标分支：`main`
   - 点击 "创建 Pull Request"

3. **等待 2 分钟**（crontab 间隔）

4. **观察 Gitee PR 页面：**
   - PR 底部出现 `ci/jenkins` 状态标识
   - 状态从 `pending`（黄色圆点）→ `success`（绿色对勾）或 `failure`（红色叉号）
   - 点击状态可跳转到 Jenkins 构建详情页

5. **观察 Jenkins：**
   - `test-platform-pr-build` 任务被自动触发
   - 执行 Resolve Mode → Checkout → Test → Docker Build → Report PR Status

6. **查看 pr-poller 日志：**
   ```bash
   cat /tmp/pr-poller.log
   # 应显示：发现 1 个 open PR → 设置 pending → 触发 Jenkins 构建
   ```

---

## 五、源文件完整逐行注释

> 本章对项目中所有 CI/CD 相关文件进行**完整的逐行注释**。
> 每个文件先展示带注释的完整源码，再总结关键设计点。
> 建议对照源文件 `test-platform/Jenkinsfile` 等一起阅读。

### 5.1 Jenkinsfile 完整逐行注释

**文件位置：** `test-platform/Jenkinsfile`

这是整个 CI/CD 的核心文件。Jenkins 读取这个文件，按顺序执行其中的阶段。

```groovy
// ============================================================================
// Jenkinsfile — 全功能测试平台 CI/CD Pipeline
//
// 【这是什么？】
// 这是一个"声明式流水线"脚本，用 Groovy 语言编写。
// Jenkins 读取这个文件后，会按顺序执行里面定义的各个 stage（阶段）。
// 如果某个 stage 失败（exit 非零），后续 stage 不会执行。
//
// 【运行环境】
// Jenkins 运行在 Docker 容器里，容器挂载了宿主机的 docker.sock，
// 所以 Jenkins 内部可以直接调用 docker 命令。
//
// 【双环境支持】
//   - 测试环境（test）：:81/:8081/:3308，容器名 tp-test-*
//   - 生产环境（prod）：:80/:8080/:3307，容器名 tp-*
//
// 【三种触发方式】
//   - Crontab 自动触发 → 默认 prod
//   - Jenkins 手动触发 → 可选 auto/test/prod
//   - PR 构建 → 由 pr-poller.sh 触发，只跑 Test + 后端 Build + 回写状态
// ============================================================================

// pipeline {} — 声明式流水线的最外层结构，所有内容必须包在里面
pipeline {

    // agent any — 在任意可用的 Jenkins 节点上执行
    // 本项目只有一个 Jenkins 实例（跑在 Docker 容器里），所以用 any
    // 如果有多节点集群，可以指定 agent label
    agent any

    // parameters {} — 定义构建参数
    // 用户在 Jenkins 界面点 "Build with Parameters" 时会看到这些选项
    // 参数值在流水线中通过 params.参数名 访问
    parameters {
        // choice — 下拉选择框
        // DEPLOY_ENV：用户选择部署到哪个环境
        //   auto: 根据分支名自动判断（main→prod，其他→test）
        //   test: 强制测试环境
        //   prod: 强制生产环境
        choice(name: 'DEPLOY_ENV', choices: ['auto', 'test', 'prod'],
               description: '部署环境：auto 根据分支自动判断，test 强制测试环境，prod 强制生产环境')

        // string — 文本输入框
        // 以下 4 个参数用于 PR 构建（pr-poller.sh 触发时传入）
        // 普通构建时这些参数为空
        string(name: 'PR_NUMBER', defaultValue: '', description: 'PR 编号（非 PR 构建时留空）')
        string(name: 'PR_SHA', defaultValue: '', description: 'PR 最新 commit SHA')
        string(name: 'PR_HEAD_REF', defaultValue: '', description: 'PR 源分支名')
        string(name: 'PR_BASE_REF', defaultValue: '', description: 'PR 目标分支名')
    }

    // stages {} — 定义所有执行阶段，按从上到下的顺序执行
    stages {

        // ====================================================================
        // Stage 0: Resolve Mode — 模式判断
        //
        // 【为什么要判断模式？】
        // 同一个 Jenkinsfile 服务于两种场景：
        //   1. 普通构建：push 到 main 或手动触发 → 跑测试 + 构建 + 部署
        //   2. PR 构建：有人提了 Pull Request → 只跑测试 + 构建后端，不部署
        // 通过判断 PR_NUMBER 是否为空来区分。
        // ====================================================================
        stage('Resolve Mode') {
            steps {
                script {
                    // params.PR_NUMBER ?: '' — 如果 PR_NUMBER 为 null，转为空字符串
                    // (...) ? 'true' : 'false' — 三元表达式，非空则 IS_PR=true
                    env.IS_PR = (params.PR_NUMBER ?: '') ? 'true' : 'false'

                    // echo — 在 Jenkins 控制台输出日志
                    // ${env.IS_PR == 'true' ? 'PR #' + params.PR_NUMBER : '普通分支'}
                    // 如果是 PR 构建就显示 "PR #42"，否则显示 "普通分支"
                    echo "===== 构建模式: ${env.IS_PR == 'true' ? 'PR #' + params.PR_NUMBER : '普通分支'} ====="
                }
            }
        }

        // ====================================================================
        // Stage 1: Checkout — 代码检出
        //
        // 【为什么要分两种检出方式？】
        // 普通构建：检出 Job 配置中指定的分支（比如 main）
        // PR 构建：必须检出 PR 的源代码，而不是目标分支
        //          比如你从 feature/login 向 main 提 PR，
        //          Jenkins 要测试的是 feature/login 的代码，不是 main 的
        //
        // Gitee/GitHub 用 refs/pull/N/head 这个特殊引用指向 PR 的源代码
        // ====================================================================
        stage('Checkout') {
            steps {
                script {
                    if (env.IS_PR == 'true' && params.PR_NUMBER) {
                        // PR 模式：检出 PR 源代码
                        // checkout([...]) — Jenkins 的 Git 检出步骤
                        checkout([
                            // $class: 'GitSCM' — 使用 Git SCM 插件
                            $class: 'GitSCM',
                            // branches — 指定要检出的分支/引用
                            // refs/pull/42/head 指向 PR #42 的最新源代码
                            branches: [[name: "refs/pull/${params.PR_NUMBER}/head"]],
                            // userRemoteConfigs — 远程仓库地址
                            userRemoteConfigs: [[url: 'https://gitee.com/greada/test-platform.git']],
                            // extensions — 扩展配置
                            extensions: [
                                // LocalBranch — 把检出的代码放到本地分支 pr-42
                                [$class: 'LocalBranch', localBranch: "pr-${params.PR_NUMBER}"],
                                // CleanCheckout — 检出前先清理工作空间，避免上次构建的残留文件干扰
                                [$class: 'CleanCheckout']
                            ]
                        ])
                    } else {
                        // 普通模式：检出 Job 配置中指定的分支
                        // scm 是一个快捷方式，自动使用 Job 配置里的 Git 仓库和分支设置
                        checkout scm
                    }
                }
            }
        }

        // ====================================================================
        // Stage 1.5: Resolve Env — 环境判断
        //
        // 【为什么要判断环境？】
        // 项目有两套环境：
        //   prod（生产）：端口 80/8080/3307，用 docker-compose.yml
        //   test（测试）：端口 81/8081/3308，用 docker-compose.test.yml
        //
        // auto 模式下根据分支名自动判断：
        //   main 分支 → prod（主分支部署到生产）
        //   其他分支 → test（特性分支部署到测试）
        //
        // when 条件：只有 IS_PR != 'true' 时才执行（PR 构建跳过此阶段）
        // ====================================================================
        stage('Resolve Env') {
            // when { expression { ... } } — 条件判断，不满足则跳过整个 stage
            when { expression { env.IS_PR != 'true' } }
            steps {
                script {
                    if (params.DEPLOY_ENV == 'auto') {
                        // auto 模式：根据分支名判断
                        // env.BRANCH_NAME — 当前构建的分支名
                        // ?: '' — 如果为 null，转为空字符串
                        def branch = env.BRANCH_NAME ?: ''
                        if (branch == 'main' || branch == '') {
                            // main 分支 → 生产环境
                            env.DEPLOY_TARGET = 'prod'
                        } else {
                            // 其他分支 → 测试环境
                            env.DEPLOY_TARGET = 'test'
                        }
                    } else {
                        // 手动模式：直接用用户选择的值（test 或 prod）
                        env.DEPLOY_TARGET = params.DEPLOY_ENV
                    }
                    echo "===== 部署目标: ${env.DEPLOY_TARGET} ====="
                }
            }
        }

        // ====================================================================
        // Stage 2: Test — 运行单元测试
        //
        // 【这一步做什么？】
        // 在一个临时的 Maven 容器里运行项目的 91 个单元测试。
        // 测试通过 → 继续；测试失败 → 流水线中断，不会构建和部署。
        //
        // 【为什么不直接在 Jenkins 里跑 mvn test？】
        // Jenkins 容器里没有 Maven 和 JDK。
        // 所以启动一个临时的 Maven 容器来跑测试，跑完就删除（--rm）。
        // 这样 Jenkins 容器本身保持轻量。
        // ====================================================================
        stage('Test') {
            steps {
                // sh '''...''' — 执行多行 shell 脚本（三引号支持多行）
                sh '''
                    echo "===== 运行单元测试 ====="

                    # docker run — 启动一个临时容器来跑测试
                    # 各参数含义：
                    #   --rm                          容器退出后自动删除，不留残留
                    #   --volumes-from "$(hostname)"  共享 Jenkins 容器的所有卷
                    #                                 $(hostname) 在容器内返回容器ID
                    #                                 这样 Maven 容器能访问 Jenkins 工作空间的代码
                    #   -w "$WORKSPACE/test-platform" 设置工作目录为 Jenkins 工作空间下的项目目录
                    #                                 $WORKSPACE 是 Jenkins 内置变量，指向检出代码的位置
                    #   -v "maven-repo:/tmp/.m2"      把 Docker 卷 maven-repo 挂载到容器的 Maven 本地仓库
                    #                                 这样每次跑测试不用重新下载依赖（跨构建缓存）
                    #   -e HOME=/tmp                  设置 HOME 环境变量，避免权限问题
                    #   --network host                使用宿主机网络，方便后续推送测试结果到后端
                    #   --user 1000:1000              以 UID 1000 用户运行，避免 root 权限问题
                    #   maven:3.9-eclipse-temurin-17  使用的镜像（含 Maven 3.9 + JDK 17）
                    #   mvn test -f backend/pom.xml   要执行的命令
                    #     -B                          Batch 模式（不显示下载进度条）
                    #     -s backend/settings.xml     使用阿里云 Maven 镜像加速下载
                    docker run --rm \
                        --volumes-from "$(hostname)" \
                        -w "$WORKSPACE/test-platform" \
                        -v "maven-repo:/tmp/.m2" \
                        -e HOME=/tmp \
                        --network host \
                        --user 1000:1000 \
                        maven:3.9-eclipse-temurin-17 \
                        mvn test -f backend/pom.xml -B -s backend/settings.xml
                '''
            }
            // post {} — 阶段结束后的收尾操作
            post {
                // always — 无论测试成功还是失败都执行
                always {
                    // junit — Jenkins 的 JUnit 插件，解析 surefire 报告
                    // surefire-reports/*.xml 是 Maven 生成的测试报告
                    // 解析后在 Jenkins 界面展示测试通过/失败明细
                    junit 'test-platform/backend/target/surefire-reports/*.xml'

                    // 下面这段 shell 脚本的作用：
                    // 解析测试报告，统计通过/失败数，推送到后端 CI 看板
                    sh '''
                        # 初始化计数器
                        total=0; passed=0; failed=0

                        # 遍历所有测试报告 XML 文件
                        for f in test-platform/backend/target/surefire-reports/TEST-*.xml; do
                            [ ! -f "$f" ] && continue

                            # grep -oE — 用正则提取测试数量
                            # XML 中的格式是 tests="91" failures="0" errors="0"
                            # awk '{s+=$1} END{print s}' — 累加所有文件的数字
                            t=$(grep -oE 'tests="[0-9]+' "$f" | grep -oE '[0-9]+' | awk '{s+=$1} END{print s}')
                            f_cnt=$(grep -oE 'failures="[0-9]+' "$f" | grep -oE '[0-9]+' | awk '{s+=$1} END{print s}')
                            e=$(grep -oE 'errors="[0-9]+' "$f" | grep -oE '[0-9]+' | awk '{s+=$1} END{print s}')

                            # ${t:-0} — 如果 t 为空，默认为 0
                            t=${t:-0}; f_cnt=${f_cnt:-0}; e=${e:-0}

                            # 累加
                            total=$((total + t))
                            failed=$((failed + f_cnt + e))
                            passed=$((passed + t - f_cnt - e))
                        done

                        # 如果有测试结果，推送到后端 CI 看板
                        if [ "$total" -gt 0 ]; then
                            # 根据部署目标选择端口
                            CI_PORT=8080
                            [ "$DEPLOY_TARGET" = "test" ] && CI_PORT=8081

                            # awk 计算通过率（保留2位小数）
                            rate=$(awk 'BEGIN {printf "%.2f", '"$passed"' * 100 / '"$total"' }')

                            # 判断构建状态
                            status="SUCCESS"; [ "$failed" -gt 0 ] && status="FAILURE"

                            # printf 构造 JSON 字符串
                            # $BUILD_NUMBER — Jenkins 内置变量，当前构建编号
                            # $BUILD_URL — Jenkins 内置变量，当前构建的 URL
                            printf '{"buildNumber":%s,"totalTests":%s,"passed":%s,"failed":%s,"passRate":%s,"status":"%s","buildUrl":"%s"}' \
                                "$BUILD_NUMBER" "$total" "$passed" "$failed" "$rate" "$status" "$BUILD_URL" | \

                            # 用 curl 容器把 JSON 推送到后端的 CI 构建记录接口
                            # --data-binary @- — 从 stdin 读取数据
                            # || true — 即使推送失败也不影响流水线（非关键步骤）
                            docker run --rm -i --network host curlimages/curl:latest \
                                -s -X POST http://localhost:$CI_PORT/api/ci/builds \
                                -H 'Content-Type: application/json' \
                                --data-binary @- || true
                        fi
                    '''
                }
            }
        }

        // ====================================================================
        // Stage 3: Docker Build — 构建 Docker 镜像
        //
        // 【这一步做什么？】
        // 根据 Dockerfile 构建后端和前端的 Docker 镜像。
        //
        // PR 模式只构建后端镜像（验证代码能编译通过），
        // 普通模式构建后端+前端两个镜像。
        // ====================================================================
        stage('Docker Build') {
            steps {
                // dir("...") — 切换到指定目录执行后续步骤
                // $WORKSPACE 是 Jenkins 检出代码的根目录
                dir("${WORKSPACE}/test-platform") {
                    script {
                        if (env.IS_PR == 'true') {
                            // PR 模式：只构建后端镜像
                            // 目的不是部署，只是验证 PR 的代码能编译通过
                            sh '''
                                echo "===== [PR 模式] 仅构建后端镜像 ====="
                                docker compose -f docker-compose.yml build backend
                            '''
                        } else {
                            // 普通模式：构建后端+前端
                            sh '''
                                # 根据部署目标选择 compose 文件
                                COMPOSE_FILE="docker-compose.yml"
                                [ "$DEPLOY_TARGET" = "test" ] && COMPOSE_FILE="docker-compose.test.yml"

                                echo "===== 使用 $COMPOSE_FILE 构建 ====="

                                # docker compose build — 根据 Dockerfile 构建镜像
                                # -f 指定使用哪个 compose 文件
                                echo "===== 构建后端镜像 ====="
                                docker compose -f "$COMPOSE_FILE" build backend

                                echo "===== 构建前端镜像 ====="
                                docker compose -f "$COMPOSE_FILE" build frontend
                            '''
                        }
                    }
                }
            }
        }

        // ====================================================================
        // Stage 4: Deploy — 部署
        //
        // 【这一步做什么？】
        // 用新构建的镜像替换正在运行的旧容器。
        //
        // docker compose up -d backend frontend 会：
        //   1. 检测到镜像已更新
        //   2. 停止旧的 backend 和 frontend 容器
        //   3. 用新镜像启动新容器
        //   4. MySQL 不受影响（不在参数列表中）
        //
        // PR 模式跳过此阶段（when 条件）
        // ====================================================================
        stage('Deploy') {
            when { expression { env.IS_PR != 'true' } }
            steps {
                dir("${WORKSPACE}/test-platform") {
                    sh '''
                        COMPOSE_FILE="docker-compose.yml"
                        [ "$DEPLOY_TARGET" = "test" ] && COMPOSE_FILE="docker-compose.test.yml"

                        echo "===== 使用 $COMPOSE_FILE 部署 ====="

                        # docker compose up -d — 后台启动/更新容器
                        # 只指定 backend frontend，不重启 mysql（避免数据库重启）
                        echo "===== 更新容器 ====="
                        docker compose -f "$COMPOSE_FILE" up -d backend frontend

                        # 清理悬空镜像（旧的、没有标签的镜像层）
                        # 释放磁盘空间，避免磁盘被旧镜像填满
                        echo "===== 清理旧镜像 ====="
                        docker image prune -f
                    '''
                }
            }
        }

        // ====================================================================
        // Stage 5: Verify — 健康检查
        //
        // 【这一步做什么？】
        // 部署后验证服务是否真的可用。
        // 不能假设"容器启动了就等于服务可用了"——JVM 可能需要 10-15 秒才能启动。
        //
        // 验证两件事：
        //   1. 后端 API 能登录（curl 调用 /api/auth/login）
        //   2. 前端页面能访问（curl 访问首页）
        // 任一失败则 exit 1，流水线标记为 FAILURE。
        //
        // 【凭据安全】
        // 登录用的用户名密码不硬编码在脚本里，
        // 而是通过 withCredentials 从 Jenkins Credentials 安全注入。
        // PR 模式跳过此阶段。
        // ====================================================================
        stage('Verify') {
            when { expression { env.IS_PR != 'true' } }
            steps {
                // withCredentials — 从 Jenkins Credentials Store 读取凭据
                // credentialsId: 'tp-login-cred' — 凭据的 ID（在 Jenkins 界面创建）
                // 注入两个环境变量：
                //   TP_LOGIN_USER — 用户名（admin）
                //   TP_LOGIN_PASS — 密码（admin123）
                // 这样在 shell 脚本里可以用 $TP_LOGIN_USER 而不暴露明文
                withCredentials([
                    usernamePassword(credentialsId: 'tp-login-cred',
                        usernameVariable: 'TP_LOGIN_USER',
                        passwordVariable: 'TP_LOGIN_PASS')
                ]) {
                    sh '''
                        echo "===== 部署目标: $DEPLOY_TARGET ====="

                        # 根据环境设置容器名前缀
                        CONTAINER_FILTER="tp-"
                        [ "$DEPLOY_TARGET" = "test" ] && CONTAINER_FILTER="tp-test-"

                        # docker ps — 列出运行中的容器
                        # --filter — 按名称过滤
                        # --format — 自定义输出格式
                        echo "===== 容器状态 ====="
                        docker ps --filter "name=$CONTAINER_FILTER" \
                            --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

                        # 等待 JVM 启动
                        # Spring Boot 首次启动需要 10-15 秒
                        echo "===== 等待服务启动 ====="
                        sleep 15

                        # 根据环境选择端口
                        if [ "$DEPLOY_TARGET" = "test" ]; then
                            BACKEND_PORT=8081
                            FRONTEND_URL="http://localhost:81"
                        else
                            BACKEND_PORT=8080
                            FRONTEND_URL="http://localhost:80"
                        fi

                        # 测试后端登录 API
                        # printf 构造 JSON 请求体，从环境变量读取凭据
                        echo "===== 测试登录 API (端口 $BACKEND_PORT) ====="
                        LOGIN_BODY=$(printf '{"username":"%s","password":"%s"}' "$TP_LOGIN_USER" "$TP_LOGIN_PASS")

                        # 用 curl 容器发 HTTP 请求
                        # -s — silent 模式
                        # -o /dev/null — 丢弃响应体（只关心状态码）
                        # -w "%{http_code}" — 只输出 HTTP 状态码
                        # -X POST — 使用 POST 方法
                        # -d "$LOGIN_BODY" — 请求体
                        HTTP_CODE=$(docker run --rm --network host curlimages/curl:latest \
                            -s -o /dev/null -w "%{http_code}" \
                            -X POST http://localhost:$BACKEND_PORT/api/auth/login \
                            -H 'Content-Type: application/json' \
                            -d "$LOGIN_BODY")

                        # 检查状态码
                        if [ "$HTTP_CODE" = "200" ]; then
                            echo "后端 API 正常，HTTP 状态码: $HTTP_CODE"
                        else
                            echo "后端 API 异常，HTTP 状态码: $HTTP_CODE"
                            exit 1  # 非0退出码 → 流水线标记为 FAILURE
                        fi

                        # 测试前端页面
                        echo "===== 测试前端 ($FRONTEND_URL) ====="
                        HTTP_CODE=$(docker run --rm --network host curlimages/curl:latest \
                            -s -o /dev/null -w "%{http_code}" \
                            $FRONTEND_URL)

                        # 200 = 正常；302 = 重定向（也正常）
                        if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "302" ]; then
                            echo "前端正常，HTTP 状态码: $HTTP_CODE"
                        else
                            echo "前端异常，HTTP 状态码: $HTTP_CODE"
                            exit 1
                        fi

                        echo ""
                        echo "部署全部通过（$DEPLOY_TARGET 环境）"
                    '''
                }
            }
        }

        // ====================================================================
        // Stage 6: Report PR Status — PR 状态回写
        //
        // 【这一步做什么？】
        // 只有 PR 构建才执行。
        // 把构建结果（success/failure）通过 Gitee API 回写到 PR 的 commit status。
        //
        // success → Gitee PR 显示绿色对勾 ✓
        // failure → Gitee PR 显示红色叉号 ✗
        //
        // 这样团队可以在 Gitee 上直接看到 CI 结果，CI 失败的 PR 不允许合并。
        // ====================================================================
        stage('Report PR Status') {
            // 只有 PR 构建才执行
            when { expression { env.IS_PR == 'true' } }
            steps {
                script {
                    // currentBuild.result — 当前构建的结果
                    // null 表示还在运行中（还没失败），等同于 SUCCESS
                    def ciStatus = currentBuild.result == 'SUCCESS' || currentBuild.result == null ? 'success' : 'failure'

                    // withEnv — 设置临时环境变量，传递给 shell 脚本
                    withEnv(["CI_STATUS=${ciStatus}"]) {
                        sh '''
                            echo "===== 回写 PR #${PR_NUMBER} 构建状态: ${CI_STATUS} ====="
                            # 调用 pr-report.sh 脚本回写 Gitee commit status
                            # || true — 即使回写失败也不影响流水线（非关键步骤）
                            bash test-platform/scripts/pr-report.sh || true
                        '''
                    }
                }
            }
        }
    }

    // ========================================================================
    // post {} — 流水线结束后的处理
    //
    // 和 stage 里的 post 不同，这里的 post 是整个流水线级别的。
    // ========================================================================
    post {
        // always — 无论成功还是失败都执行
        always {
            script {
                if (env.IS_PR == 'true') {
                    echo "===== PR #${params.PR_NUMBER} 构建完成 ====="
                }
            }
        }

        // failure — 流水线失败时执行
        failure {
            script {
                if (env.IS_PR == 'true') {
                    // PR 构建失败时，也要回写 failure 状态到 Gitee
                    withEnv(["CI_STATUS=failure"]) {
                        sh '''
                            echo "===== [失败] 回写 PR #${PR_NUMBER} 构建状态 ====="
                            bash test-platform/scripts/pr-report.sh || true
                        '''
                    }
                }
            }
        }

        // success — 流水线成功时执行
        success {
            echo "构建/部署成功完成"
        }

        // cleanup — 最后始终执行（在 always/failure/success 之后）
        cleanup {
            // deleteDir — 删除 Jenkins 工作空间
            // 清理这次构建下载的代码和生成的文件，释放磁盘空间
            deleteDir()
        }
    }
}
```

---

### 5.2 docker-compose.yml 完整逐行注释

**文件位置：** `test-platform/docker-compose.yml`

这是**生产环境**的容器编排文件。定义了三个容器（MySQL、后端、前端）以及它们之间的关系。

```yaml
# services: — 顶级关键字，下面定义所有容器（服务）
services:

  # ============================================================
  # MySQL 数据库容器
  # ============================================================
  mysql:
    image: mysql:5.7          # 使用 MySQL 5.7 官方镜像（从 Docker Hub 拉取）
    container_name: tp-mysql  # 容器名称（固定，方便引用和查看）
                              # 不指定的话 Docker 会自动生成类似 test-platform-mysql-1 的名字

    # command — 覆盖 MySQL 启动命令的默认参数
    # --character-set-server=utf8mb4     默认字符集设为 utf8mb4（支持中文和 emoji）
    # --collation-server=utf8mb4_general_ci  排序规则（ci = case insensitive，不区分大小写）
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci

    environment:
      # ${DB_PASSWORD:-1234} — 从环境变量读取密码
      # 语法：${变量名:-默认值}
      # 如果 .env 文件或系统环境变量中有 DB_PASSWORD，就用那个值
      # 否则用默认值 1234（仅用于开发，生产应该强制设置）
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD:-1234}

      # 自动创建数据库（容器首次启动时执行）
      MYSQL_DATABASE: test_platform

    volumes:
      # 数据持久化：把 Docker 命名卷 mysql_data 挂载到 MySQL 数据目录
      # 这样即使容器被删除重建，数据库数据也不会丢失
      # 格式：卷名:容器内路径
      - mysql_data:/var/lib/mysql

      # 初始化脚本：把本地的 docker/init/ 目录挂载到 MySQL 的初始化目录
      # MySQL 容器首次启动时会自动执行 /docker-entrypoint-initdb.d/ 下的 .sql 文件
      - ./docker/init:/docker-entrypoint-initdb.d

    ports:
      # 端口映射：宿主机端口:容器端口
      # "3307:3306" — 宿主机的 3307 端口映射到容器的 3306 端口
      # 为什么用 3307 而不是 3306？避免和宿主机本地安装的 MySQL 冲突
      # 应用连接数据库时用 localhost:3307
      - "3307:3306"

    healthcheck:
      # 健康检查：Docker 定期执行 test 命令，判断容器是否健康
      # mysqladmin ping — MySQL 提供的健康检查命令，返回 "mysqld is alive"
      # CMD 表示在容器内执行命令
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 5s        # 每 5 秒检查一次
      timeout: 5s         # 超时时间 5 秒（超过则视为不健康）
      retries: 10         # 连续失败 10 次才标记为 unhealthy
      start_period: 30s   # 启动后 30 秒内不检查（给 MySQL 启动时间）

    restart: unless-stopped  # 容器异常退出时自动重启（除非手动 docker stop）

  # ============================================================
  # 后端容器（Spring Boot Java 应用）
  # ============================================================
  backend:
    build:
      # context: . — 构建上下文为当前目录（docker-compose.yml 所在目录）
      # Docker 会把这个目录下的所有文件发送给 Docker daemon
      context: .
      # dockerfile: backend/Dockerfile — 使用 backend/Dockerfile 构建镜像
      dockerfile: backend/Dockerfile

    container_name: tp-backend

    environment:
      # 覆盖 Spring Boot 的数据库连接 URL
      # mysql:3306 — 用容器名 "mysql" 作为主机名
      #   Docker 内部 DNS 会自动把 "mysql" 解析为 MySQL 容器的 IP
      #   所以后端容器不需要知道 MySQL 的具体 IP 地址
      # 3306 — 容器内部端口（不是宿主机的 3307）
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/test_platform?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8

      # 数据库密码（同 MySQL 容器）
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-1234}

      # AI 服务的 API Key（可选，不设则 AI 功能不可用）
      # ${AGNES_API_KEY:-} — 默认为空字符串
      AGNES_API_KEY: ${AGNES_API_KEY:-}

    ports:
      - "8080:8080"  # 后端 API 端口

    depends_on:
      # 依赖关系：backend 依赖 mysql
      # Docker Compose 会先启动 mysql，再启动 backend
      mysql:
        # condition: service_healthy — 等 MySQL 健康检查通过后才启动 backend
        # 这很关键：如果 MySQL 还没启动好，backend 连接数据库会失败
        # 如果只用 depends_on: mysql（不带 condition），只是等 mysql 容器启动，
        # 但 MySQL 进程可能还没准备好接受连接
        condition: service_healthy

    restart: unless-stopped

  # ============================================================
  # 前端容器（Nginx 静态文件服务 + 反向代理）
  # ============================================================
  frontend:
    build:
      context: ./frontend    # 构建上下文为 frontend 目录
      dockerfile: Dockerfile  # 使用 frontend/Dockerfile 构建

    container_name: tp-frontend

    ports:
      - "80:80"  # HTTP 默认端口（浏览器直接 http://localhost 访问）

    depends_on:
      # 依赖 backend（但不需要 condition: service_healthy）
      # 因为 Nginx 启动时不需要 backend 已就绪
      # Nginx 会在运行时通过反向代理访问 backend
      - backend

    restart: unless-stopped

# volumes: — 顶级关键字，定义命名卷
# 命名卷由 Docker 管理，存储在宿主机的 /var/lib/docker/volumes/ 下
volumes:
  mysql_data:  # 定义名为 mysql_data 的卷（上面 mysql 服务引用了它）
```

---

### 5.3 docker-compose.test.yml 与 prod 的差异

**文件位置：** `test-platform/docker-compose.test.yml`

测试环境的 compose 文件与生产环境**结构完全相同**，只有以下 4 处不同：

| 配置项 | 生产环境 (docker-compose.yml) | 测试环境 (docker-compose.test.yml) |
|--------|------------------------------|-----------------------------------|
| MySQL 容器名 | `tp-mysql` | `tp-test-mysql` |
| MySQL 端口 | `3307:3306` | `3308:3306` |
| 后端容器名 | `tp-backend` | `tp-test-backend` |
| 后端端口 | `8080:8080` | `8081:8080` |
| 前端容器名 | `tp-frontend` | `tp-test-frontend` |
| 前端端口 | `80:80` | `81:80` |
| 数据卷名 | `mysql_data` | `mysql_test_data` |

**为什么这样设计？**

1. **容器名不同** — 两套环境可以同时运行在同一个 Docker 实例上，互不冲突
2. **端口不同** — prod 用 80/8080/3307，test 用 81/8081/3308，浏览器可以同时访问两个环境
3. **数据卷不同** — 测试环境和生产环境的数据库数据完全隔离，互不影响

> **注意：** 后端的 `SPRING_DATASOURCE_URL` 中的主机名仍然是 `mysql:3306`，因为 Docker Compose 内部 DNS 把当前 compose 项目中的 `mysql` 服务名解析为对应容器。两个 compose 文件是独立的项目，各自的 `mysql` 指向各自的 MySQL 容器。

---

### 5.4 backend/Dockerfile 完整逐行注释

**文件位置：** `test-platform/backend/Dockerfile`

这是一个**多阶段构建**的 Dockerfile。第一阶段用 Maven 编译 Java 代码，第二阶段只用 JRE 运行编译产物。

```dockerfile
# ============================================================================
# 阶段 1：构建阶段（AS build）
#
# 使用 Maven + JDK 17 的 Alpine 镜像来编译代码
# Alpine 是精简版 Linux，体积比标准版小很多
# 这个阶段的镜像约 400MB，但最终镜像不会包含它
# ============================================================================

# FROM ... AS build — 定义第一阶段，命名为 "build"
# maven:3.9-eclipse-temurin-17-alpine 包含：
#   - Maven 3.9（构建工具）
#   - JDK 17（Java 开发工具包，含编译器 javac）
#   - Alpine Linux（精简操作系统）
FROM maven:3.9-eclipse-temurin-17-alpine AS build

# WORKDIR — 设置工作目录（容器内的路径）
# 后续的 COPY、RUN 命令都在这个目录下执行
WORKDIR /app

# ---- 依赖缓存层 ----
# 这几步的目的是：把 pom.xml 单独复制进去并下载依赖，
# 这样只要 pom.xml 没变，Docker 就会缓存这一层，下次构建不用重新下载依赖。
# 源码改动时只会重新编译，不会重新下载依赖（省 2-3 分钟）。

# COPY backend/settings.xml ... — 复制 Maven 配置文件
# settings.xml 配置了阿里云 Maven 镜像，加速依赖下载（国内访问 Maven 中央仓库很慢）
COPY backend/settings.xml /root/.m2/settings.xml

# COPY pom.xml . — 复制父 POM
COPY pom.xml .

# COPY backend/pom.xml backend/ — 复制后端模块的 POM
COPY backend/pom.xml backend/

# RUN mvn dependency:go-offline -pl backend
#   mvn — Maven 命令
#   dependency:go-offline — 预下载所有依赖到本地仓库
#   -pl backend — 只处理 backend 模块
#   -B — Batch 模式（不显示下载进度条，减少日志）
# 执行完后，/root/.m2/repository 下会有所有依赖 jar 包
RUN mvn -B dependency:go-offline -pl backend

# ---- 编译阶段 ----
# COPY backend/src backend/src — 复制源代码
# 这一步在依赖下载之后，所以源码改了只会触发重新编译，不会重新下载依赖
COPY backend/src backend/src

# RUN mvn package -DskipTests -pl backend
#   mvn package — 编译代码并打包成 JAR 文件
#   -DskipTests — 跳过单元测试（CI 流水线里有专门的 Test 阶段跑测试）
#   -pl backend — 只构建 backend 模块
#   -B — Batch 模式
# 产出：/app/backend/target/test-platform-backend-1.0.0.jar
RUN mvn -B package -DskipTests -pl backend

# ============================================================================
# 阶段 2：运行阶段
#
# 只包含 JRE 17（不含 Maven、不含编译器），镜像约 200MB
# 从阶段 1 复制编译好的 JAR 文件，然后运行它
# ============================================================================

# eclipse-temurin:17-jre-alpine — 只包含 JRE 17（Java 运行时环境）
# 没有 javac 编译器，没有 Maven，体积小
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# COPY --from=build ... — 从阶段 1（build）复制文件到当前阶段
# 把编译好的 JAR 文件复制过来，重命名为 app.jar
# 只有这一个文件从阶段 1 带过来，Maven 和 JDK 都不带
COPY --from=build /app/backend/target/*.jar app.jar

# EXPOSE 8080 — 声明容器监听 8080 端口
# 这只是文档声明，不会自动映射端口
# 真正的端口映射在 docker-compose.yml 的 ports 中定义
EXPOSE 8080

# ENTRYPOINT — 容器启动时执行的命令
# JSON 数组格式：["java", "-jar", "app.jar"]
# 等同于在容器内执行 java -jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**构建缓存原理图：**

```
Dockerfile 层次（从上到下）：

┌─────────────────────────────────┐
│ FROM maven:3.9... AS build      │ ← 基础镜像层（不变，有缓存）
├─────────────────────────────────┤
│ COPY settings.xml               │ ← Maven 配置（很少变，有缓存）
├─────────────────────────────────┤
│ COPY pom.xml                    │ ← POM 文件（偶尔变）
├─────────────────────────────────┤
│ RUN mvn dependency:go-offline   │ ← 下载依赖（pom.xml 不变就用缓存，省2-3分钟）
├─────────────────────────────────┤
│ COPY backend/src                │ ← 源代码（经常变）← 改代码后从这里开始重建
├─────────────────────────────────┤
│ RUN mvn package                 │ ← 编译打包（每次改代码都要重新执行）
├─────────────────────────────────┤
│ FROM eclipse-temurin:17-jre     │ ← 运行镜像（不变，有缓存）
├─────────────────────────────────┤
│ COPY --from=build *.jar app.jar │ ← 复制 JAR（每次都执行）
├─────────────────────────────────┤
│ ENTRYPOINT java -jar app.jar    │ ← 启动命令
└─────────────────────────────────┘
```

---

### 5.5 frontend/Dockerfile 完整逐行注释

**文件位置：** `test-platform/frontend/Dockerfile`

同样是多阶段构建。第一阶段用 Node.js 编译 Vue 前端，第二阶段只用 Nginx 托管静态文件。

```dockerfile
# ============================================================================
# 阶段 1：构建阶段
# 用 Node.js 20 编译 Vue 3 前端代码，产出静态 HTML/JS/CSS 文件
# ============================================================================

# node:20-alpine — 包含 Node.js 20 + npm 的 Alpine 镜像
FROM node:20-alpine AS build

WORKDIR /app

# COPY package*.json ./ — 复制 package.json 和 package-lock.json
# 先复制依赖描述文件，再 npm ci 安装依赖
# 这样源码改动时不用重新 npm install（省 30-60 秒）
COPY package*.json ./

# RUN npm ci — 安装依赖
#   npm ci — 比 npm install 更严格：
#     - 必须有 package-lock.json
#     - 完全按照 lock 文件安装，不修改版本
#     - 安装前先删除 node_modules
#     - 速度更快，适合 CI 环境
RUN npm ci

# COPY . . — 复制所有源代码到容器
# .dockerignore 会排除 node_modules、dist 等
COPY . .

# RUN npm run build — 执行 vite build 编译前端
#   Vue 3 + Vite 会把 .vue 文件编译成普通 HTML/JS/CSS
#   产出在 /app/dist/ 目录下
RUN npm run build

# ============================================================================
# 阶段 2：运行阶段
# 用 Nginx 托管静态文件，不需要 Node.js
# ============================================================================

# nginx:alpine — Nginx 的 Alpine 镜像（约 20MB，非常轻量）
FROM nginx:alpine

# COPY --from=build /app/dist ... — 从阶段 1 复制编译产物
# /app/dist — Vite 构建输出的目录
# /usr/share/nginx/html — Nginx 默认的静态文件目录
COPY --from=build /app/dist /usr/share/nginx/html

# COPY nginx.conf ... — 复制 Nginx 配置文件
# 配置反向代理：/api/ 请求转发到后端容器
COPY nginx.conf /etc/nginx/conf.d/default.conf

# EXPOSE 80 — 声明监听 80 端口（HTTP 默认端口）
EXPOSE 80

# CMD — 容器启动命令
# nginx -g "daemon off;" — 以前台模式运行 Nginx
#   默认 Nginx 以守护进程（后台）运行，但这会让 Docker 以为容器已退出
#   daemon off; 让 Nginx 在前台运行，容器就不会停止
CMD ["nginx", "-g", "daemon off;"]
```

**为什么前端也需要多阶段构建？**

| | 构建阶段 | 运行阶段 |
|---|---------|---------|
| 镜像 | node:20-alpine (~400MB) | nginx:alpine (~20MB) |
| 包含 | Node.js + npm + 源码 | Nginx + 静态文件 |
| 作用 | 编译 .vue → .html/.js/.css | 托管静态文件 + 反向代理 |

前端编译后只是一堆静态文件（HTML/JS/CSS），运行时不需要 Node.js。用 Nginx 托管即可，镜像从 400MB 降到 20MB。

---

### 5.6 frontend/nginx.conf 完整逐行注释

**文件位置：** `test-platform/frontend/nginx.conf`

Nginx 配置文件，定义了两个功能：静态文件托管 + 反向代理。

```nginx
# server { } — 定义一个虚拟服务器
server {
    # listen 80 — 监听 80 端口（HTTP 默认端口）
    listen 80;

    # server_name localhost — 匹配请求头中的 Host 字段
    # localhost 表示只处理访问 localhost 的请求
    server_name localhost;

    # charset utf-8 — 设置响应的字符集为 UTF-8（支持中文）
    charset utf-8;

    # ---- Gzip 压缩配置 ----
    # gzip on — 开启 gzip 压缩
    # 压缩后传输的数据量减小 50-70%，加快页面加载速度
    gzip on;

    # gzip_types — 指定哪些类型的响应需要压缩
    # text/plain        — 纯文本
    # application/json  — JSON 响应（后端 API 返回的数据）
    # text/css          — CSS 样式表
    # application/javascript — JavaScript 文件
    gzip_types text/plain application/json text/css application/javascript;

    # ---- 静态文件托管 ----
    # location / — 匹配所有路径（兜底规则）
    location / {
        # root /usr/share/nginx/html — 静态文件根目录
        # Nginx 会在这个目录下查找请求的文件
        root /usr/share/nginx/html;

        # index index.html — 默认首页文件
        # 访问 http://localhost/ 时自动返回 index.html
        index index.html;

        # try_files — 按顺序尝试查找文件
        # $uri — 请求的路径（如 /about → 查找 /usr/share/nginx/html/about）
        # $uri/ — 尝试作为目录（如 /about/ → 查找 /usr/share/nginx/html/about/index.html）
        # /index.html — 都找不到就返回 index.html
        #
        # 这是 Vue Router 的 history 模式必需的配置：
        # 前端路由（如 /login、/testcases）在服务器上没有对应的文件，
        # 如果不配置 try_files，刷新页面会返回 404。
        # 配置后，所有找不到的路径都回退到 index.html，
        # 由 Vue Router 在前端处理路由。
        try_files $uri $uri/ /index.html;
    }

    # ---- 反向代理配置 ----
    # location /api/ — 匹配所有以 /api/ 开头的请求
    # 前端代码中 axios 请求 /api/testcases，
    # Nginx 会把这个请求转发到后端容器的 8080 端口
    location /api/ {
        # resolver 127.0.0.11 valid=10s
        #   127.0.0.11 — Docker 内置的 DNS 服务器
        #   Docker 容器通过这个 DNS 解析其他容器名（如 "backend"）的 IP
        #   valid=10s — DNS 缓存 10 秒（10 秒后重新解析，适应容器 IP 变化）
        resolver 127.0.0.11 valid=10s;

        # set $backend_upstream http://backend:8080
        #   把后端地址存到变量里（用变量是因为 Nginx 启动时 backend 容器可能还没就绪）
        #   backend — 容器名（Docker Compose 中定义的 service 名）
        #   8080 — 后端容器内部端口
        set $backend_upstream http://backend:8080;

        # proxy_pass — 把请求转发到上面的地址
        # 用变量而不是直接写 proxy_pass http://backend:8080
        # 是为了利用 resolver 动态解析（如果 backend 容器重启，IP 会变）
        proxy_pass $backend_upstream;

        # proxy_set_header — 设置转发请求的 HTTP 头
        # Host — 原始请求的 Host
        # X-Real-IP — 客户端真实 IP
        # X-Forwarded-For — 代理链（记录经过的每一跳 IP）
        # X-Forwarded-Proto — 原始协议（http 或 https）
        # 这些头让后端能获取客户端的真实信息（不设置的话后端只能看到 Nginx 的 IP）
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**反向代理的工作流程：**

```
浏览器请求: http://localhost/api/testcases
                    │
                    ▼
          ┌─────────────────┐
          │  Nginx (前端容器) │
          │  匹配 /api/      │
          │  proxy_pass     │
          └────────┬────────┘
                   │ 转发请求
                   ▼
          ┌─────────────────┐
          │  Backend 容器    │
          │  http://backend:8080/api/testcases
          └─────────────────┘

浏览器请求: http://localhost/login
                    │
                    ▼
          ┌─────────────────┐
          │  Nginx (前端容器) │
          │  匹配 /          │
          │  try_files      │
          │  返回 index.html │
          └─────────────────┘
```

---

### 5.7 scripts/pr-poller.sh 完整逐行注释

**文件位置：** `test-platform/scripts/pr-poller.sh`

这个脚本由 crontab 每 2 分钟调用一次，轮询 Gitee API 检查是否有新的 PR 需要构建。

```bash
#!/bin/bash
# =============================================================================
# pr-poller.sh — Gitee Pull Request 轮询 & 触发 Jenkins 构建
#
# 【这个脚本做什么？】
# 1. 调 Gitee API 获取所有 open（未合并）的 PR
# 2. 对每个 PR，检查是否已经触发过 CI 构建
# 3. 没构建过 → 设置 pending 状态 → 触发 Jenkins 构建
#
# 【为什么要轮询？】
# Gitee 免费版不支持 Webhook（PR 创建时主动通知 Jenkins），
# 所以只能定时轮询。每 2 分钟检查一次，最多有 2 分钟延迟。
#
# 【被谁调用？】
# crontab 定时任务：*/2 * * * * bash pr-poller.sh
# =============================================================================

# set -euo pipefail — 严格模式
#   -e — 任何命令失败立即退出脚本（不会忽略错误继续执行）
#   -u — 使用未定义的变量时报错
#   -o pipefail — 管道中任一命令失败则整个管道失败
set -euo pipefail

# SCRIPT_DIR — 获取脚本所在目录
# $(cd "$(dirname "$0")" && pwd) — 先 cd 到脚本目录再 pwd，获取绝对路径
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ---- 加载配置文件 ----
# .env.ci 文件包含 Gitee Token、Jenkins Token 等敏感信息
# 按优先级查找：/opt/.env.ci > 脚本同目录 > Jenkins 容器内
if [ -f /opt/.env.ci ]; then
    source /opt/.env.ci
elif [ -f "${SCRIPT_DIR}/.env.ci" ]; then
    source "${SCRIPT_DIR}/.env.ci"
elif [ -f /var/jenkins_home/.env.ci ]; then
    source /var/jenkins_home/.env.ci
else
    echo "[ERROR] .env.ci 未找到，请创建在 /opt/ 或脚本同目录"
    exit 1
fi

# : "${VAR:?msg}" — 检查必需的环境变量
# 如果变量未设置或为空，打印 msg 并退出
# 这是一种防御性编程，确保所有必需的凭据都已配置
: "${GITEE_OWNER:?未设置 GITEE_OWNER}"
: "${GITEE_REPO:?未设置 GITEE_REPO}"
: "${GITEE_TOKEN:?未设置 GITEE_TOKEN}"
: "${JENKINS_URL:?未设置 JENKINS_URL}"
: "${JENKINS_USER:?未设置 JENKINS_USER}"
: "${JENKINS_TOKEN:?未设置 JENKINS_TOKEN}"

# Gitee API 基础地址
GITEE_API="https://gitee.com/api/v5"

# Jenkins PR 构建 Job 的名称
JENKINS_JOB="test-platform-pr-build"

# Jenkins 内部访问地址（在 WSL/Docker 内部用 localhost:8080 访问 Jenkins 容器）
# 如果没设置，默认用 localhost:8080
JENKINS_INTERNAL_URL="${JENKINS_INTERNAL_URL:-http://localhost:8080}"

# 已处理记录文件 — 记录已经触发过的 PR:SHA 组合，避免重复触发
DONE_LOG="/tmp/pr-poller-done.log"

# 日志文件
LOG_FILE="/tmp/pr-poller.log"

# log() — 日志函数，带时间戳写入日志文件
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$LOG_FILE"
}

log "===== PR Poller 开始 ====="

# ---- Step 1: 获取所有 open PR ----
# curl -sS — 静默模式但显示错误
# Gitee API: GET /repos/{owner}/{repo}/pulls?state=open
# access_token 通过 URL query 传递（Gitee API 的认证方式）
PR_LIST=$(curl -sS "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/pulls?state=open&access_token=${GITEE_TOKEN}")

# 统计 PR 数量
# grep -o '"number":[0-9]*' — 提取所有 "number":数字 的匹配
# wc -l — 计数
PR_COUNT=$(echo "$PR_LIST" | grep -o '"number":[0-9]*' | wc -l) || true
log "发现 ${PR_COUNT} 个 open PR"

# 没有 open PR 就直接退出
if [ "$PR_COUNT" -eq 0 ]; then
    log "没有 open PR，退出"
    exit 0
fi

# ---- Step 2: 遍历每个 PR ----
# 提取所有 PR 编号
PR_NUMBERS=$(echo "$PR_LIST" | grep -o '"number":[0-9]*' | grep -o '[0-9]*') || true

for PR_NUMBER in $PR_NUMBERS; do
    # 获取单个 PR 的详细信息（包含 head SHA、分支名等）
    PR_DETAIL=$(curl -sS "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/pulls/${PR_NUMBER}?access_token=${GITEE_TOKEN}")

    # 从 JSON 中提取字段（用 grep + sed 解析，因为可能没有 jq 工具）
    # HEAD_SHA — PR 最新提交的 SHA（用于回写 commit status）
    HEAD_SHA=$(echo "$PR_DETAIL" | grep -o '"sha":"[^"]*"' | head -1 | sed 's/"sha":"//;s/"//') || true

    # HEAD_REF — PR 源分支名
    HEAD_REF=$(echo "$PR_DETAIL" | grep -P -o '"head":\{"label":"[^"]*","ref":"[^"]*"' | sed 's/.*"ref":"//;s/"//') || true

    # BASE_REF — PR 目标分支名
    BASE_REF=$(echo "$PR_DETAIL" | grep -P -o '"base":\{"label":"[^"]*","ref":"[^"]*"' | sed 's/.*"ref":"//;s/"//') || true

    # PR_TITLE — PR 标题
    PR_TITLE=$(echo "$PR_DETAIL" | grep -o '"title":"[^"]*"' | head -1 | sed 's/"title":"//;s/"//') || true

    log "PR #${PR_NUMBER}: ${PR_TITLE} (${HEAD_REF} → ${BASE_REF}) SHA=${HEAD_SHA}"

    # 无法获取 SHA 就跳过
    if [ -z "$HEAD_SHA" ]; then
        log "  ⚠️  无法获取 SHA，跳过"
        continue
    fi

    # ---- Step 3: 检查是否已处理过 ----
    # 用 "PR编号:SHA" 作为唯一键
    # 如果这个 SHA 已经处理过，说明 Jenkins 已经构建过了，跳过
    KEY="${PR_NUMBER}:${HEAD_SHA}"
    if [ -f "$DONE_LOG" ] && grep -qF "$KEY" "$DONE_LOG" 2>/dev/null; then
        log "  已处理过，跳过"
        continue
    fi

    # ---- Step 4: 检查 Gitee commit status ----
    # 查询这个 commit 是否已经有 ci/jenkins 的状态
    STATUS_JSON=$(curl -sS "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/commits/${HEAD_SHA}/status?access_token=${GITEE_TOKEN}")
    EXISTING_STATUS=$(echo "$STATUS_JSON" | grep -o '"ci/jenkins"' | head -1) || true

    if [ -n "$EXISTING_STATUS" ]; then
        # 已有状态，提取具体状态值
        CI_STATE=$(echo "$STATUS_JSON" | grep -o '"ci/jenkins","state":"[^"]*"' | sed 's/.*"state":"//;s/"//') || true
        if [ "$CI_STATE" = "pending" ]; then
            log "  构建进行中（pending），跳过"
            echo "$KEY" >> "$DONE_LOG" || true
            continue
        fi
        log "  已有状态: ${CI_STATE}，跳过"
        echo "$KEY" >> "$DONE_LOG" || true
        continue
    fi

    # ---- Step 5: 设置 pending 状态 ----
    # 在触发 Jenkins 之前，先在 Gitee 上设置 pending 状态
    # 这样开发者在 Gitee 上能看到 "CI 构建中..." 的提示
    PENDING_PAYLOAD=$(printf '{"state":"pending","target_url":"%s/job/%s/","description":"CI 构建中…","context":"ci/jenkins"}' \
        "$JENKINS_URL" "$JENKINS_JOB")

    # POST 请求设置 commit status
    curl -sS -X POST "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/statuses/${HEAD_SHA}?access_token=${GITEE_TOKEN}" \
        -H 'Content-Type: application/json' \
        -d "$PENDING_PAYLOAD" > /dev/null || true
    log "  已设置 pending 状态"

    # ---- Step 6: 触发 Jenkins 构建 ----
    # 调用 Jenkins REST API 触发构建
    # buildWithParameters — 触发参数化构建
    TRIGGER_URL="${JENKINS_INTERNAL_URL}/job/${JENKINS_JOB}/buildWithParameters"

    # -u user:token — HTTP Basic 认证
    # --data-urlencode — URL 编码参数
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -u "${JENKINS_USER}:${JENKINS_TOKEN}" \
        -X POST "$TRIGGER_URL" \
        --data-urlencode "PR_NUMBER=${PR_NUMBER}" \
        --data-urlencode "PR_SHA=${HEAD_SHA}")

    # Jenkins 成功触发构建返回 201 Created
    if [ "$HTTP_CODE" = "201" ]; then
        log "  ✅ Jenkins 构建已触发 (HTTP 201)"
    else
        log "  ❌ Jenkins 触发失败 (HTTP ${HTTP_CODE})"
    fi

    # 记录已处理
    echo "$KEY" >> "$DONE_LOG" || true
done

log "===== PR Poller 结束 ====="
exit 0
```

---

### 5.8 scripts/pr-report.sh 完整逐行注释

**文件位置：** `test-platform/scripts/pr-report.sh`

这个脚本由 Jenkinsfile 的 Report PR Status 阶段调用，把构建结果回写到 Gitee。

```bash
#!/bin/bash
# =============================================================================
# pr-report.sh — 回写 Gitee Pull Request Commit Status
#
# 【这个脚本做什么？】
# 调用 Gitee API，把 Jenkins 构建结果（success/failure）写到 PR 的 commit status。
# 这样开发者在 Gitee PR 页面就能看到 CI 是否通过。
#
# 【被谁调用？】
# Jenkinsfile 的 "Report PR Status" 阶段
# 通过环境变量传入参数：PR_NUMBER、PR_SHA、CI_STATUS、BUILD_URL
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# 加载配置（同 pr-poller.sh）
if [ -f /opt/.env.ci ]; then
    source /opt/.env.ci
elif [ -f "${SCRIPT_DIR}/.env.ci" ]; then
    source "${SCRIPT_DIR}/.env.ci"
elif [ -f /var/jenkins_home/.env.ci ]; then
    source /var/jenkins_home/.env.ci
else
    echo "[ERROR] .env.ci 未找到"
    exit 1
fi

# 检查必需的变量
: "${GITEE_OWNER:?未设置 GITEE_OWNER}"
: "${GITEE_REPO:?未设置 GITEE_REPO}"
: "${GITEE_TOKEN:?未设置 GITEE_TOKEN}"
: "${PR_SHA:?未设置 PR_SHA}"          # PR 的 commit SHA（回写到哪个 commit）
: "${CI_STATUS:?未设置 CI_STATUS}"    # success / failure / pending

GITEE_API="https://gitee.com/api/v5"
JENKINS_JOB="test-platform-pr-build"

# 可选参数的默认值
PR_NUMBER="${PR_NUMBER:-unknown}"
BUILD_URL="${BUILD_URL:-${JENKINS_URL}/job/${JENKINS_JOB}/}"

# 根据状态生成描述文字（Gitee 上显示的文字）
if [ "$CI_STATUS" = "success" ]; then
    CI_DESC="${CI_DESC:-CI 构建通过 ✓}"
elif [ "$CI_STATUS" = "failure" ]; then
    CI_DESC="${CI_DESC:-CI 构建失败 ✗}"
elif [ "$CI_STATUS" = "pending" ]; then
    CI_DESC="${CI_DESC:-CI 构建中…}"
else
    CI_DESC="${CI_DESC:-CI 状态: ${CI_STATUS}}"
fi

echo "[pr-report] PR #${PR_NUMBER} SHA=${PR_SHA} → ${CI_STATUS} (repo=${GITEE_OWNER}/${GITEE_REPO})"

# 构造 Gitee API 的请求体（JSON 格式）
# state — 状态：success / failure / pending
# target_url — 点击状态后跳转的链接（Jenkins 构建详情页）
# description — 显示的文字描述
# context — 状态标识名（同一个 commit 可以有多个不同 context 的状态）
STATUS_PAYLOAD=$(printf '{"state":"%s","target_url":"%s","description":"%s","context":"ci/jenkins"}' \
    "$CI_STATUS" "$BUILD_URL" "$CI_DESC")

# 调用 Gitee API 回写 commit status
# POST /repos/{owner}/{repo}/statuses/{sha}
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "${GITEE_API}/repos/${GITEE_OWNER}/${GITEE_REPO}/statuses/${PR_SHA}?access_token=${GITEE_TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "$STATUS_PAYLOAD")

# Gitee API 成功返回 200 或 201
if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "200" ]; then
    echo "[pr-report] ✅ Commit status 回写成功"
else
    # 回写失败不影响流水线（|| true 在 Jenkinsfile 中处理了）
    echo "[pr-report] ⚠️  Commit status 回写失败 (HTTP ${HTTP_CODE}) — 不阻塞流水线"
fi
```

---

### 5.9 架构总览图

```
┌─────────────────────────────────────────────────────────────────┐
│                        Windows 本机                              │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    WSL2 (Ubuntu)                         │    │
│  │                                                          │    │
│  │  ┌──────────────┐    ┌──────────────────────────────┐   │    │
│  │  │   Crontab    │    │     Docker Engine             │   │    │
│  │  │  (每2分钟)   │    │                               │   │    │
│  │  └──────┬───────┘    │  ┌────────────┐              │   │    │
│  │         │            │  │  Jenkins   │              │   │    │
│  │         ▼            │  │  (:8088)  │              │   │    │
│  │  ┌──────────────┐    │  │  Pipeline │              │   │    │
│  │  │pr-poller.sh │    │  │  7 阶段    │              │   │    │
│  │  │  轮询 Gitee  │    │  └─────┬──────┘              │   │    │
│  │  │  API         │    │        │ docker compose      │   │    │
│  │  └──────┬───────┘    │        ▼                      │   │    │
│  │         │            │  ┌────────────────────────┐  │   │    │
│  │         │ 触发构建    │  │  tp-mysql  (:3307)    │  │   │    │
│  │         └───────────►│  │  tp-backend(:8080)    │  │   │    │
│  │                      │  │  tp-frontend (:80)    │  │   │    │
│  │  ┌──────────────┐    │  └────────────────────────┘  │   │    │
│  │  │ /opt/.env.ci │    │                               │   │    │
│  │  │  Gitee Token │    └───────────────────────────────┘   │    │
│  │  │  Jenkins Token│                                     │    │
│  │  └──────────────┘                                       │    │
│  └─────────────────────────────────────────────────────────┘    │
│                          │                                       │
│              浏览器访问   │ localhost:80 / :8088                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  http://localhost:80    → 前端界面                       │    │
│  │  http://localhost:8080  → 后端 API                       │    │
│  │  http://localhost:8088  → Jenkins                        │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
          │
          │ Gitee API (HTTPS)
          ▼
┌─────────────────────┐
│   Gitee 仓库         │
│  gitee.com/greada/  │
│   test-platform     │
│                     │
│  PR commit status:  │
│  ci/jenkins ✓/✗    │
└─────────────────────┘
```

**数据流向说明：**

| 步骤 | 方向 | 说明 |
|------|------|------|
| ① | Crontab → pr-poller.sh | 每 2 分钟触发轮询 |
| ② | pr-poller.sh → Gitee API | 查询 open PR 列表 |
| ③ | pr-poller.sh → Gitee API | 设置 pending 状态 |
| ④ | pr-poller.sh → Jenkins API | 触发 PR 构建 |
| ⑤ | Jenkins → Docker | 执行 Test + Build 阶段 |
| ⑥ | Jenkins → pr-report.sh | 构建完成后调用回写脚本 |
| ⑦ | pr-report.sh → Gitee API | 回写 success/failure 状态 |
| ⑧ | 开发者 → Gitee PR 页面 | 看到 CI 结果 ✓/✗ |

---

## 六、面试演示指南

### 6.1 演示流程清单

**建议按以下顺序演示（约 10 分钟）：**

1. **展示项目功能（1 分钟）**
   - `http://localhost:80` 打开前端
   - 登录 → 展示测试用例列表 → 执行一个用例 → 查看结果

2. **展示 Jenkins 流水线（3 分钟）**
   - `http://localhost:8088` 打开 Jenkins
   - 展示 Jenkinsfile 的 7 个阶段
   - 手动触发一次构建，展示各阶段执行

3. **展示 CI 看板（1 分钟）**
   - 前端 → CI 状态页面
   - 展示构建记录（构建号、通过率、状态）

4. **展示 PR 流程（3 分钟）**
   - Gitee 上创建一个 PR
   - 展示 Gitee PR 页面的 `ci/jenkins` 状态
   - 点击状态跳转到 Jenkins 构建详情

5. **讲解架构（2 分钟）**
   - 画出架构总览图（见 5.5）
   - 讲解 Docker Compose 编排
   - 讲解多阶段构建

### 6.2 高频面试问题与回答

**Q: 你的 CI/CD 流水线包含哪些阶段？**

> 流水线分为 7 个阶段：模式判断、代码检出、环境判断、单元测试、Docker 镜像构建、容器部署、健康检查验证。PR 构建时还会额外执行状态回写到 Gitee。其中 PR 构建只跑测试和编译验证，不做部署。

**Q: PR 构建和普通构建有什么区别？**

> 三点区别：
> 1. PR 构建检出 `refs/pull/N/head`，普通构建检出目标分支
> 2. PR 构建只构建后端镜像验证编译通过，不部署
> 3. PR 构建完成后回写 Gitee commit status，普通构建有 Verify 健康检查

**Q: 为什么用轮询而不是 Webhook？**

> Gitee 免费版不支持 Webhook 推送，所以用 crontab 每 2 分钟轮询 Gitee API。轮询方案有约 2 分钟延迟，但实现简单且可靠。生产环境可以用 Gitee 企业版 Webhook 或迁移到 GitHub Actions 实现推送式触发。

**Q: Docker Compose 如何保证服务启动顺序？**

> 三个机制：
> 1. `depends_on` 定义启动顺序：frontend → backend → mysql
> 2. MySQL 配置了 `healthcheck`（`mysqladmin ping`）
> 3. backend 的 `depends_on` 使用 `condition: service_healthy`，等 MySQL 健康检查通过后才启动

**Q: 多阶段构建有什么好处？**

> 三个好处：
> 1. 镜像体积小——后端从 600MB（含 Maven+JDK）降到 200MB（仅 JRE）
> 2. 安全性好——运行镜像不含编译工具，减小攻击面
> 3. 构建缓存——pom.xml 和 package.json 单独一层，源码改了不重下依赖

**Q: 如何避免测试阶段重复下载 Maven 依赖？**

> 两个手段：
> 1. Dockerfile 中先 `COPY pom.xml` 再 `RUN mvn dependency:go-offline`，依赖层独立缓存
> 2. Jenkinsfile Test 阶段挂载 `maven-repo` 持久化卷到 `/tmp/.m2`，跨构建复用本地仓库

**Q: 部署后如何验证服务可用？**

> Verify 阶段执行两个检查：
> 1. 调用 `/api/auth/login` 验证后端 API 返回 200
> 2. 访问前端首页验证 Nginx 返回 200
> 任一失败则 `exit 1` 阻断流水线，标记为 FAILURE。凭据通过 Jenkins Credentials 注入，不硬编码。

**Q: 如何管理多环境（test/prod）？**

> 通过两套 docker-compose 文件实现：
> - `docker-compose.yml`：生产环境，端口 80/8080/3307，容器名 tp-*
> - `docker-compose.test.yml`：测试环境，端口 81/8081/3308，容器名 tp-test-*
> 流水线根据分支自动选择：main → prod，其他分支 → test。两套环境数据卷独立，互不干扰。

### 6.3 画图讲解要点

面试时在白板上画以下三层架构：

```
第一层：代码托管
  Gitee 仓库 ← pr-poller.sh 轮询

第二层：CI/CD 引擎
  Jenkins Pipeline (7 阶段)
  ├── Test (Maven 容器)
  ├── Build (Docker 多阶段构建)
  ├── Deploy (Docker Compose)
  └── Verify (curl 健康检查)

第三层：运行时
  Docker Compose 编排
  ├── MySQL (数据持久化)
  ├── Backend (Spring Boot JVM)
  └── Frontend (Nginx 静态托管 + 反代)
```

**讲解顺序：** 从代码提交开始 → CI 触发 → 构建 → 部署 → 验证，形成闭环。

### 6.4 可延伸的改进话题

如果面试官追问"还有什么可以改进的"，可以说：

1. **安全加固**——JWT 密钥从环境变量注入、CORS 限制白名单、SSRF 防护
2. **蓝绿部署**——当前是滚动替换，可改为双容器组 + Nginx upstream 切换，实现零停机部署
3. **监控告警**——引入 Prometheus + Grafana 监控容器和应用指标
4. **Flyway**——数据库版本管理，替代手工 SQL 脚本
5. **代码质量**——引入 SonarQube 静态分析、ESLint 前端规范
6. **Webhook 替代轮询**——Gitee 企业版或迁移 GitHub Actions

---

## 七、附录

### A. 命令速查表

```bash
# ===== Docker =====
docker compose up -d                      # 启动所有服务
docker compose down                       # 停止所有服务
docker compose down -v                    # 停止并删除数据卷
docker compose build backend              # 重新构建后端镜像
docker compose up -d backend              # 只重启后端
docker compose logs -f backend            # 查看后端日志
docker compose ps                         # 查看容器状态
docker compose exec mysql mysql -u root -p test_platform  # 进入 MySQL

# ===== Jenkins =====
docker logs -f jenkins                    # 查看 Jenkins 日志
docker restart jenkins                    # 重启 Jenkins
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword  # 初始密码

# ===== PR 轮询 =====
bash /mnt/d/Java_all/code/test-platform/test-platform/scripts/pr-poller.sh  # 手动执行
cat /tmp/pr-poller.log                   # 查看轮询日志
cat /tmp/pr-poller-done.log              # 查看已处理的 PR
crontab -l                               # 查看定时任务
crontab -e                               # 编辑定时任务

# ===== 测试 =====
cd /mnt/d/Java_all/code/test-platform/test-platform
mvn test -pl backend -B -s backend/settings.xml  # 本地运行单元测试
```

### B. 端口与容器对照表

| 服务 | 生产环境 | 测试环境 | 容器名（prod） | 容器名（test） |
|------|---------|---------|---------------|---------------|
| MySQL | 3307 | 3308 | tp-mysql | tp-test-mysql |
| Backend | 8080 | 8081 | tp-backend | tp-test-backend |
| Frontend | 80 | 81 | tp-frontend | tp-test-frontend |
| Jenkins | 8088 | — | jenkins | — |

### C. 故障排查 FAQ

**Q: `docker compose up` 构建后端报 "Cannot connect to the Docker daemon"**

```bash
# 检查 Docker 服务是否运行
sudo systemctl status docker
# 如果未运行，启动它
sudo systemctl start docker
# 确认当前用户在 docker 组
groups $USER
# 如果没有 docker 组，重新添加并重启终端
sudo usermod -aG docker $USER
```

**Q: Jenkins 构建报 "docker: command not found"**

Jenkins 容器内找不到 docker 命令。检查挂载：

```bash
# 确认 docker 二进制路径
which docker
# 如果是 /usr/bin/docker，确保启动时挂载了
docker inspect jenkins | grep -A5 Mounts | grep docker
```

如果路径不同，重新创建 Jenkins 容器时调整 `-v $(which docker):/usr/bin/docker`。

**Q: Jenkins 构建报 "permission denied while trying to connect to docker.sock"**

```bash
# 给 docker.sock 读写权限
sudo chmod 666 /var/run/docker.sock
# 或更安全的方式：将 jenkins 容器以 root 运行（当前配置已是 -u root）
```

**Q: pr-poller.sh 报 ".env.ci 未找到"**

```bash
# 确认文件存在
ls -la /opt/.env.ci
# 如果不存在，参考 4.2 节创建
```

**Q: pr-poller.sh 报 "JENKINS_TOKEN 未设置"**

`.env.ci` 中缺少 `JENKINS_TOKEN`。获取方式：Jenkins → 用户名 → 设置 → API Token → 添加新 Token。

**Q: Gitee PR 没有出现 ci/jenkins 状态**

1. 检查 pr-poller 是否在运行：`cat /tmp/pr-poller.log`
2. 检查 GITEE_TOKEN 是否有效：`curl -s "https://gitee.com/api/v5/user?access_token=你的token" | head -c 100`
3. 检查 crontab：`crontab -l`
4. 手动执行一次：`bash scripts/pr-poller.sh`

**Q: 后端容器启动后立即退出**

```bash
# 查看退出日志
docker compose logs backend | tail -50

# 常见原因：
# 1. MySQL 未就绪 → 检查 mysql 健康检查
# 2. 密码不匹配 → 检查 .env 中 DB_PASSWORD 与 init.sql 一致
# 3. 端口冲突 → lsof -i :8080 检查端口占用
```

**Q: 前端页面空白或 502**

```bash
# 1. 检查后端是否正常
curl -s http://localhost:8080/api/auth/login -X POST \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'

# 2. 检查 Nginx 反代配置
docker compose exec frontend cat /etc/nginx/conf.d/default.conf

# 3. 检查 Nginx 能否解析 backend 容器名
docker compose exec frontend ping -c1 backend
```

**Q: Maven 测试阶段报 "Could not resolve dependencies"**

网络问题导致 Maven 无法下载依赖。确认 `backend/settings.xml` 配置了阿里云镜像：

```bash
cat backend/settings.xml
# 应包含 https://maven.aliyun.com/repository/public
```

如果网络不通，检查 WSL2 DNS 配置：

```bash
cat /etc/resolv.conf
# 如果 DNS 不对，手动设置：
echo "nameserver 8.8.8.8" | sudo tee /etc/resolv.conf
```

---

## 变更日志

| 日期 | 变更 |
|------|------|
| 2026-08-09 | 初版创建，覆盖 WSL2 环境搭建、项目部署、CI/CD 配置、PR 状态回写、流水线原理、面试指南 |
| 2026-08-09 | 新增"零、CI/CD 核心概念"入门章节；第五节全面重写为完整源文件逐行注释版（Jenkinsfile、docker-compose.yml、两个 Dockerfile、nginx.conf、pr-poller.sh、pr-report.sh） |
| 2026-08-09 | 1.4 节新增 1.4.1 docker run 命令逐行详解（参数表、DooD 机制、命名卷 vs 绑定挂载对比、数据流图） |
