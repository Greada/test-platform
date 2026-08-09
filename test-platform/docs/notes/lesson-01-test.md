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
**报错**:`mvn: command not found`
**原因**:Jenkins 容器没装 Maven

**学到的**:
- `sh '...'` 单行 shell 命令(step,不是 Groovy 内置)
- Jenkins 容器 ≠ 装了所有工具的容器
- CI 里要用"按需起容器"模式

---

### 2.2 小步 1b — docker run 共享 workspace

**概念**:
- `docker run` 临时容器:用完即弃
- `--volumes-from "$(hostname)"`:共享 Jenkins 容器的所有卷,这样 Maven 容器能看到 workspace 里的代码
- `-w "$WORKSPACE/test-platform"`:设工作目录
- `maven:3.9-eclipse-temurin-17`:含 Maven 3.9 + JDK 17

**写的代码**:
<!-- 跑通后填 -->

**结果**:
**报错/现象**:
**学到的**:

---

### 2.3 小步 1c — 加 Maven 依赖缓存

**概念**:
- 1b 慢的原因:每次重新下载所有 Maven 依赖
- Docker 命名卷 `maven-repo`:跨构建复用本地仓库
- 第一次还是慢(下载并存入缓存),第二次飞快(用缓存)

**写的代码**:
<!-- 跑通后填 -->

**结果**:
**对比**(两次构建速度):
- 第一次:
- 第二次:
**学到的**:

---

### 2.4 小步 1d — 补全健壮性参数

**概念**:
- `-e HOME=/tmp`:Maven 写缓存需要 HOME 目录,避免权限报错
- `--user 1000:1000`:非 root 运行,匹配 Jenkins workspace 文件权限
- `--network host`:用宿主机网络(为 Lesson 5 推送测试结果做准备)
- `-B`:Batch 模式,不显示下载进度条
- `-s backend/settings.xml`:用阿里云镜像加速

**最终 Test stage 代码**:
<!-- 跑通后填最终版 -->

**结果**:✅ SUCCESS,91 个测试通过
**学到的**:

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

- **构建编号**:
- **状态**:
- **踩的坑**:
  - 
- **关键认知**:
  - 
- **下次注意**:
  - 

## 五、Console Output 关键片段

<!-- 贴关键日志 -->
