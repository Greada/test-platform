# Lesson 0: Jenkinsfile 骨架

> 目标:让 Jenkins 跑通第一个 pipeline,看到绿灯。

## 一、概念

### Jenkinsfile 是什么
Jenkins 是任务执行器,本身不知道要执行什么。Jenkinsfile 是告诉 Jenkins "第一步干啥、第二步干啥"的脚本。放在代码仓库里和代码一起版本管理,叫 **Pipeline as Code**。

### 用什么语言
**Groovy**——跑在 JVM 上的语言。我们用 **声明式(Declarative)** 风格,90% 时候不需要真懂 Groovy,记住固定关键字即可。

### 声明式 pipeline 最小结构

```
pipeline {              ← ① 最外层,声明这是声明式 pipeline
    agent any           ← ② 在哪台 Jenkins 节点跑(any=任意)
    stages {            ← ③ 所有阶段放这里
        stage('阶段名') {  ← ④ 一个阶段
            steps {        ← ⑤ 阶段做什么
                echo '...' ← ⑥ 动作:打印一句话
            }
        }
    }
}
```

### 6 个关键字

| 关键字 | 作用 | 类比 |
|--------|------|------|
| `pipeline` | 声明这是声明式流水线 | "这是一份任务清单" |
| `agent` | 在哪台节点执行 | "在哪间厨房做饭" |
| `stages` | 所有阶段的容器 | "整个做菜流程" |
| `stage` | 一个阶段 | "备菜/炒菜/装盘"之一 |
| `steps` | 阶段内的动作 | "切葱、倒油" |
| `echo` | 打印日志(Groovy 内置) | 最简单的动作 |

> ⚠️ 声明式结构是硬性要求:`pipeline {}` 包住一切,`stages {}` 至少一个 `stage`,`stage` 必须有 `steps`。

### Groovy 内置 step vs shell 命令

```groovy
echo 'hello'        // Groovy 内置 step,Jenkins 直接执行
sh 'echo hello'     // shell 命令,丢给 /bin/bash 执行
```

- Lesson 0 用 `echo`(最简单)
- Lesson 1 起跑 `mvn`/`docker` **必须**用 `sh '...'`

## 二、我写的代码

<!-- 跑完后把 Jenkinsfile-learn 的最终内容贴这里 -->

```groovy
pipeline {
    agent ____
    stages {
        stage('____') {
            steps {
                ____
            }
        }
    }
}
```

填空答案:
1. `____` → 
2. `____` → 
3. `____` → 

## 三、Jenkins 配置

### 3.1 新建 Job

| 配置项 | 值 | 说明 |
|--------|-----|------|
| Job 名 | `test-platform-learn` | 区别于生产用的 `test-platform` Job |
| 类型 | **Pipeline** | ⚠️ 不是 Freestyle project。只有 Pipeline 类型才能用 Jenkinsfile |

> **Pipeline vs Freestyle 区别**:Freestyle 是老式界面配置,所有步骤在 Jenkins 网页上点;Pipeline 用 Jenkinsfile 代码描述,支持复杂流程、可版本管理。现代 CI/CD 一律用 Pipeline。

### 3.2 Pipeline 区块配置(最关键)

进入 Job → 左侧 **Configure** → 滚到最下面 **Pipeline** 区块。这是告诉 Jenkins "去哪读 Jenkinsfile"。

| 配置项 | 值 | 说明 |
|--------|-----|------|
| **Definition** | `Pipeline script from SCM` | "从代码仓库读 Jenkinsfile"。另一个选项 `Pipeline script` 是直接在网页里写脚本,不用 |
| **SCM** | **`Git`** ⚠️ | **源代码管理器**。默认是 `None`(空),必须改成 `Git`,否则报 `NullSCM` 错误 |
| **Repository URL** | `https://gitee.com/greada/test-platform.git` | Git 仓库地址 |
| **Credentials** | (留空) | 公开仓库不用填;私有仓库要加 Gitee 用户名密码凭据 |
| **Branches to build** | `*/main` | 检出哪个分支。`*/main` 表示 main 分支 |
| **Script Path** | `test-platform/Jenkinsfile-learn` | ⚠️ Jenkinsfile 在仓库里的相对路径。默认是 `Jenkinsfile`,我们用 `-learn` 绕开旧文件 |
| **Lightweight checkout** | ✅ 勾上 | 只检出 Jenkinsfile 而非整个仓库,加快首次读取速度 |

### 3.3 SCM 是什么(踩坑详解)

**SCM = Source Code Management(源代码管理)**,就是告诉 Jenkins "代码在哪、怎么拉"。

Jenkins 支持多种 SCM:
- **Git** ← 我们用这个
- Subversion(SVN)— 老牌版本控制
- Mercurial — 另一个版本控制工具
- None — 没有 SCM(默认值,会报错!)

**踩的坑:SCM 默认是 None**

新建 Pipeline Job 时,Definition 选 `Pipeline script from SCM` 后,**SCM 下拉框默认停留在 `None`**。如果直接填 Script Path 就保存,Jenkins 会报:

```
Checking out hudson.scm.NullSCM into ...
```

`NullSCM` 是 Jenkins 的"空 SCM"兜底实现——表示"没有配置源代码管理"。Jenkins 不知道去哪拉代码,自然找不到 `Jenkinsfile-learn`。

**解决**:SCM 必须手动改成 `Git`,改完后 Repository URL / Branches / Script Path 等 Git 字段才会出现。

### 3.4 构建时 Jenkins 做了什么(理解 SCM 的工作流程)

点 **Build Now** 后,Jenkins 内部执行顺序:

```
1. 读 Job 配置 → 发现 Definition = Pipeline script from SCM
2. 读 SCM = Git → 去 Repository URL 拉代码
   ↓
   git fetch https://gitee.com/greada/test-platform.git
   git checkout main
   → 代码下载到 /var/jenkins_home/workspace/test-platform-learn/
3. 读 Script Path = test-platform/Jenkinsfile-learn
   → 找到仓库里的 Jenkinsfile-learn
4. 解析 Jenkinsfile-learn 的 Groovy 语法
5. 按 pipeline{} 里的定义逐个 stage 执行
6. 全部成功 → 标记 SUCCESS(绿色)
```

**关键认知**:
- Jenkins **自动拉代码**,你不需要手动 git clone/pull
- Jenkins 拉的是 **Gitee 远程仓库**的代码,不是你本地 Windows/WSL 里的代码
- 所以 `Jenkinsfile-learn` 必须先 **git push 到 Gitee**,Jenkins 才能读到

> ⚠️ **工作流提醒**:改 `Jenkinsfile-learn` 后,必须 `git add && git commit && git push` 到 Gitee,再点 Build Now。否则 Jenkins 拉的还是旧版本。

## 四、复盘

- **构建编号**:#1
- **状态**:✅ SUCCESS
- **踩的坑**:
  - 新建 Job 后 SCM 默认是 `None`,导致报 `hudson.scm.NullSCM` 错误
  - 原因:Definition 选了 `Pipeline script from SCM`,但 SCM 下拉框没手动改成 `Git`
  - 解决:SCM 改成 `Git`,填好 Repository URL 和 Script Path 后保存
- **关键认知**:
  - Jenkins 自动从 Gitee 拉代码,不需要本地手动 git clone
  - 改 `Jenkinsfile-learn` 后必须 `git push` 到 Gitee,Jenkins 才能读到新版本
  - `Jenkinsfile-learn` 是相对仓库根目录的路径,不是绝对路径
- **下次注意**:
  - 建新 Pipeline Job 时,Definition 改完后立刻检查 SCM 下拉框是不是 Git
  - 配置完先 push 代码再 Build Now

## 五、Console Output 关键片段

<!-- 贴最后 10-20 行 -->
