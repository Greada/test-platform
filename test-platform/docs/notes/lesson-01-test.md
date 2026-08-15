# Lesson 1: 跑单元测试(Test stage)

> 目标:在 pipeline 里加 Test stage,用临时 Maven 容器跑 91 个单元测试。

## 一、核心问题:为什么不能直接 mvn test

Jenkins 跑在 `jenkins/jenkins:lts-jdk17` 容器里,这个容器**只有 JDK,没有 Maven**。直接 `sh 'mvn test'` 会报 `mvn: command not found`。

**解决方案**:用 `docker run` 起临时 Maven 容器跑测试,跑完销毁(`--rm`)。这是 CI 里"用完即弃"的经典模式。

## 二、渐进式 4 小步

### 2.1 小步 1a — 天真版踩坑

**写的代码**:
```groovy
stage('Test') {
    steps {
        sh 'mvn test -f backend/pom.xml'
    }
}
```

**结果**:❌ FAILED
**报错**:
```
/var/jenkins_home/workspace/test-platform-learn@tmp/durable-4df22bd3/script.sh.copy: 1: mvn: not found
```

**报错拆解**:
- `/var/jenkins_home/workspace/test-platform-learn/` — Jenkins 把代码拉到这里了 ✅(Git 认证已修复)
- `@tmp/durable-xxx/script.sh.copy` — Jenkins 把 `sh '...'` 转成临时脚本执行
- `mvn: not found` — 容器内找不到 mvn 命令

**原因**:
- `jenkins/jenkins:lts-jdk17` 镜像只有 Jenkins 核心 + JDK 17,**没有 Maven**
- Jenkins 容器只负责"调度",真正的构建工具通过临时容器按需启动

**学到的**:
- `sh '...'` 单行 shell 命令(step,不是 Groovy 内置)
- Jenkins 容器 ≠ 装了所有工具的容器
- CI 里要用"容器化构建工具"模式 — Maven/Node 等通过 `docker run` 按需起临时容器,用完即弃
- 好处:Jenkins 容器保持轻量;不同项目可用不同版本工具;工具升级只换镜像 tag

---

### 2.2 小步 1b — docker run 共享 workspace

**概念**:
- `docker run --rm` 临时容器:用完即弃,`--rm` 让容器退出后自动删除,不占磁盘
- `--volumes-from "$(hostname)"`:共享 Jenkins 容器的所有卷,这样 Maven 容器能看到 workspace 里的代码
  - `hostname` 是 Linux 系统自带命令(非 Docker 专有)
  - Docker 给容器分配 hostname = 容器 ID 前 12 位(通过 UTS namespace)
  - `$(hostname)` 命令替换取容器 ID;`${HOSTNAME}` 读环境变量也能取到(两者等价)
- `-w "$WORKSPACE/test-platform"`:设工作目录
  - `$WORKSPACE` 是 Jenkins 注入的环境变量,指向 `/var/jenkins_home/workspace/test-platform-learn`
  - 代码在 `$WORKSPACE/test-platform/` 下(因 Script Path 是 `test-platform/Jenkinsfile.new`)
- `maven:3.9-eclipse-temurin-17`:含 Maven 3.9 + JDK 17(项目要求 Java 17)
- `sh '''...'''` 三引号:多行 shell 命令(对比 Lesson 0 的 `echo` 是 Groovy 内置)

**写的代码**:
```groovy
stage('Test') {
    steps {
        sh '''
            docker run --rm \
                --volumes-from "${HOSTNAME}" \
                -w "$WORKSPACE/test-platform" \
                maven:3.9-eclipse-temurin-17 \
                mvn test -f backend/pom.xml
        '''
    }
}
```

**结果**:✅ SUCCESS
```
Tests run: 91, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 05:08 min
```

**踩的坑**:
- 第一版写 `--volumes-from "${hostname}"`(小写),变量不存在,展开成空字符串
- `${变量名}` 是变量引用,`$(命令)` 是命令替换,两者不同
- 修正为 `${HOSTNAME}`(大写,读环境变量)或 `$(hostname)`(命令替换)均可

**5 分钟花在哪**:
- 大量时间花在 `Downloaded from central: https://repo.maven.apache.org/...`
- 下载的依赖存在临时 Maven 容器内部,`--rm` 退出后全丢
- 下次构建又要重新下载全部依赖 → 这就是 1c 要解决的问题

**学到的**:
- `sh '...'` 单行 vs `sh '''...'''` 多行的区别
- `docker run --rm` 临时容器模式(用完即弃)
- `--volumes-from` 跨容器共享文件
- `$WORKSPACE` 是 Jenkins 注入的环境变量
- `${变量}` vs `$(命令)` 的区别(shell 语法)
- `hostname` 是 Linux 命令,Docker 给容器分配 hostname=容器 ID 前 12 位

---

### 2.3 小步 1c — 加 Maven 依赖缓存

**概念**:
- 1b 慢的原因:依赖下载到容器内部,`--rm` 退出后全丢,下次又重下
- Docker 命名卷 `maven-repo`:永久存储,跨构建复用依赖
- 第一次还是慢(下载并存入卷),第二次飞快(用卷里的缓存)
- 坑:Maven 默认仓库在 `$HOME/.m2`,容器 root 的 HOME=/root,但卷挂载在 /tmp/.m2 → 路径不一致缓存不生效
- 解法:加 `-Dmaven.repo.local=/tmp/.m2/repository` 强制 Maven 用挂载路径(为 1d 的 `-e HOME=/tmp` 衔接做准备)

**写的代码**:
```groovy
sh '''
    docker run --rm \
        --volumes-from "${HOSTNAME}" \
        -w "$WORKSPACE/test-platform" \
        -v "maven-repo:/tmp/.m2" \
        maven:3.9-eclipse-temurin-17 \
        mvn test -f backend/pom.xml -Dmaven.repo.local=/tmp/.m2/repository
'''
```

**结果**:✅ SUCCESS
```
Tests run: 91, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**对比(两次构建速度)**:
- 第一次(Build #7):5 分 51 秒(下载依赖存入 maven-repo 卷,仍走中央仓库)
- 第二次(Build #8):**16.2 秒** 🚀(maven-repo 卷有缓存,跳过下载)
- 提速:**21 倍**!

**踩的坑**:
- 第一版漏写 `-Dmaven.repo.local=/tmp/.m2/repository`,缓存不生效(Maven 写 /root/.m2,卷在 /tmp/.m2 空着)
- 拼写错误:`-Daemon` → `-Dmaven`(D 和 maven 之间不能丢字符);`respository` → `repository`

**学到的**:
- Docker 命名卷:永久存储,容器销毁后数据保留
- Maven 仓库路径默认在 `$HOME/.m2/repository`,可通过 `-Dmaven.repo.local` 覆盖
- 挂载路径和 Maven 实际写入路径必须一致,否则缓存不生效
- 缓存的价值:21 倍提速,生产环境 N 次构建省下的时间是巨大的

**Jenkins 目录访问**:
- 宿主机路径:`/var/lib/docker/volumes/jenkins_home/_data`(需 sudo)
- 容器内路径:`/var/jenkins_home`
- 推荐用 `docker exec jenkins ls /var/jenkins_home` 访问(免 sudo,路径短)
- 构建日志在 `jobs/test-platform-learn/builds/N/log`

---

### 2.4 小步 1d — 补全健壮性参数

**概念**:
- `-e HOME=/tmp`:Maven 写 `.m2` 需要 HOME 目录。容器默认 HOME=/root,但 `--user 1000:1000` 后没 /root 写权限 → 强制 HOME=/tmp,Maven 写 /tmp/.m2(就是挂载的卷)
- `--network host`:用宿主机网络栈(为 Lesson 5 推送测试结果到 localhost:8080 做准备)
- `--user 1000:1000`:非 root 运行,匹配 Jenkins workspace 文件权限(UID 1000)。若 root 跑,target/ 目录属主变 root,Jenkins 清理权限报错
- `-B`:Batch 模式,不显示下载进度条(日志干净)
- `-s backend/settings.xml`:用项目里的阿里云 Maven 镜像,不走中央仓库(国外慢)
- 参数依赖关系:`--user 1000:1000` → /root 无权限 → 必须 `-e HOME=/tmp` → 必须 `-v maven-repo:/tmp/.m2`,三个参数绑定,缺一报错
- 1c → 1d 衔接:1c 的 `-Dmaven.repo.local=/tmp/.m2/repository` 可以去掉(因为 `-e HOME=/tmp` 后 Maven 默认路径自动变成 /tmp/.m2/repository),路径不变,缓存保留

**最终 Test stage 代码**:
```groovy
sh '''
    docker run --rm \
        --volumes-from "${HOSTNAME}" \
        -w "$WORKSPACE/test-platform" \
        -v "maven-repo:/tmp/.m2" \
        -e HOME=/tmp \
        --network host \
        --user 1000:1000 \
        maven:3.9-eclipse-temurin-17 \
        mvn test -f backend/pom.xml -B -s backend/settings.xml
'''
```

**结果**:✅ SUCCESS,91 个测试通过
**学到的**:
- 三个参数有依赖关系(user/HOME/volume),必须一起加
- `settings.xml` 已经配好阿里云源,但要用 `-s` 参数指定才生效
- 1c 的缓存路径设计(挂 /tmp/.m2 + 指定路径)让 1d 衔接时缓存不丢
- Batch 模式让日志可读

---

## 三、最终 Jenkinsfile.new 结构(Lesson 1 完成时)

```groovy
pipeline {
    agent any
    stages {
        stage('hello') {
            steps { echo 'hello' }
        }
        stage('Test') {
            steps {
                sh '''... docker run ... mvn test ...'''
            }
        }
    }
}
```

## 四、复盘

- **构建编号**: #1–#8（#5 失败 = 1a 天真版；#7 = 1c 首次下载 5:51；#8 = 1c 缓存命中 16.2s = 最终成功构建）
- **状态**: #8 ✅ SUCCESS（91 个测试全部通过）
- **踩的坑**:
  - 1a: Jenkins 容器只有 JDK 无 Maven → `mvn: not found`；解决：用 `docker run` 起临时 Maven 容器
  - 1b: `${hostname}` 小写不展开（shell 变量区分大小写）→ 改 `${HOSTNAME}` 或 `$(hostname)`
  - 1c: 漏写 `-Dmaven.repo.local` 导致缓存不生效（Maven 写 /root/.m2，卷在 /tmp/.m2）；拼写错误 `respository` → `repository`
  - 1d: `--user 1000:1000` → /root 无写权限 → 必须 `-e HOME=/tmp` → 必须 `-v maven-repo:/tmp/.m2`，三个参数绑定
- **关键认知**:
  - Jenkins 容器 ≠ 全功能构建环境；用"临时容器 + --rm"按需启动工具，Jenkins 保持轻量
  - 命名卷跨构建缓存依赖：第一次 5:51 → 第二次 16.2s（**21 倍提速**）
  - `${变量}` 引用 vs `$(命令)` 替换 — shell 基础但容易混
  - 参数依赖链：--user → HOME 无权限 → -e HOME → -v 卷路径，缺一报错
  - 挂载路径与 Maven 实际写入路径必须一致，否则缓存空转
- **下次注意**:
  - shell 变量大小写（HOSTNAME vs hostname）
  - 卷挂载路径 vs 应用默认路径要对齐
  - 有依赖关系的参数必须一起加，不能逐个试

## 五、Console Output 关键片段

### 1a 失败（Build #5）

```
/var/jenkins_home/workspace/test-platform-learn@tmp/durable-xxx/script.sh.copy: 1: mvn: not found
```

### 1b 成功

```
Tests run: 91, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time:  05:08 min
```

### 1c 缓存命中（Build #8）

```
Tests run: 91, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time:  16.20 s
```

> 1b/1d 的具体构建编号未在正文中记录；#5 为 lastFailedBuild，#8 为 lastSuccessfulBuild。
