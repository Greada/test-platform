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

<!-- 跑完后把 Jenkinsfile.new 的最终内容贴这里 -->

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

| 配置项 | 值 |
|--------|-----|
| Job 名 | `test-platform-learn` |
| 类型 | Pipeline |
| Definition | Pipeline script from SCM |
| Repository URL | `https://gitee.com/greada/test-platform.git` |
| Script Path | `test-platform/Jenkinsfile.new` |
| Lightweight checkout | ✅ |

## 四、复盘

- **构建编号**:#
- **状态**:✅ SUCCESS / ❌ FAILED
- **踩的坑**:
  - 
- **下次注意**:
  - 

## 五、Console Output 关键片段

<!-- 贴最后 10-20 行 -->
