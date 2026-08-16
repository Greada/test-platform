# Lesson 5: 失败处理 + 产物（post + junit + 工作区清理）

> 目标:让 pipeline 在成功/失败后做不同收尾(通知/清理),并用 `junit` 收集测试报告让 Jenkins 显示测试明细。
> 前置:Lesson 4 已完成参数化 + 双环境,learn pipeline 有 parameters + when + 7 个 stage。

## 一、概念

### 5.1 为什么要 post — 流水线的"收尾"

L1-L4 的 pipeline 只管"跑"——跑完就结束。问题是:

| 场景 | 没 post 时 | 有 post 后 |
|------|-----------|-----------|
| Test 失败 | pipeline 标红,但测试报告丢在工作区,下次构建被覆盖 | `junit` 收集报告 → Jenkins 永久展示测试明细/趋势 |
| 部署失败 | pipeline 标红就完,没人知道 | `post.failure` 发通知 → 人能及时响应 |
| 构建结束 | 工作区残留(占磁盘) | `post.cleanup` `deleteDir()` → 释放空间 |
| 无论成败 | 都要做的清理(如删临时文件) | `post.always` → 必执行 |

**核心认知:`stages{}` 是"顺流而下"(失败就停),`post{}` 是"汇流归海"(无论成败都收尾)。**

### 5.2 post{} 块的两种位置

```groovy
pipeline {
    agent any
    stages {
        stage('Test') {
            steps { sh 'mvn test' }
            post {                          // ← stage 级 post(只管这个 stage)
                always { junit '...' }
            }
        }
    }
    post {                                  // ← pipeline 级 post(管整个 pipeline)
        success { echo '成功' }
        failure { echo '失败' }
        cleanup  { deleteDir() }
    }
}
```

| 位置 | 作用域 | 触发时机 | 典型用途 |
|------|--------|----------|----------|
| stage 内 `post{}` | 只管该 stage | 该 stage 结束后(无论成败) | 收集该 stage 产物(junit 报告) |
| pipeline 级 `post{}` | 整个 pipeline | 所有 stage 跑完后 | 全局通知/清理/PR 回写 |

**关键:stage 级 post 在该 stage 后立即跑;pipeline 级 post 在最后跑(所有 stage 之后)。**

### 5.3 post 的条件块 — 触发顺序

`post{}` 里可以放多个条件块,Jenkins 按固定顺序执行:

| 条件 | 触发时机 | 常用 |
|------|----------|------|
| `always` | 无论成败 | ✅ 最常用(收集产物/通用清理) |
| `success` | pipeline/stage 成功 | ✅ 常用(成功通知) |
| `failure` | pipeline/stage 失败 | ✅ 常用(失败告警) |
| `unstable` | 标黄(如 junit 有失败用例) | 较少用 |
| `aborted` | 被手动取消 | 较少用 |
| `cleanup` | **最后**执行(在所有其他条件后) | ✅ 常用(deleteDir) |

**执行顺序:** `always` → `success`/`failure`/`unstable`/`aborted`(按结果选一) → `cleanup`

```
pipeline 成功:  always → success → cleanup
pipeline 失败:  always → failure → cleanup
```

> ⚠️ `cleanup` 永远最后跑,即使 `always` 里报错也跑——所以 `deleteDir()` 放 `cleanup` 最稳。

### 5.4 junit step — 收集测试报告

```groovy
post {
    always {
        junit 'test-platform/backend/target/surefire-reports/*.xml'
    }
}
```

**作用:**
1. 读 surefire 生成的 XML 测试报告(Maven 跑 test 会产出 `TEST-*.xml`)
2. 把结果上传到 Jenkins 构建记录(永久保存,不随工作区清理丢失)
3. Jenkins 构建页显示 "Test Result" —— 测试总数/通过/失败/跳过 + 具体用例
4. 多次构建后显示**测试趋势图**(通过率随时间变化)

**为什么要放 `always` 不是 `success`:**
- 测试失败时 `mvn test` 返回非零 → stage 失败
- 但报告**已经生成了**(Maven 跑完才返回失败码)
- 放 `always` → 失败时也能收集报告,看哪个用例挂了

**glob 路径规则:**
- 相对于 `$WORKSPACE`(不是 stage 的 dir())
- `**/*.xml` = 递归找所有 xml
- `surefire-reports/*.xml` = 只找该目录下的 xml

> ⚠️ L1 的 Test stage 在 maven 容器里跑,报告生成在 `$WORKSPACE/test-platform/backend/target/surefire-reports/`(因为 `--volumes-from` 共享了工作区)。所以 junit 路径要带 `test-platform/` 前缀。

### 5.5 deleteDir() — 清理工作区

```groovy
post {
    cleanup {
        deleteDir()
    }
}
```

- `deleteDir()` 是 Jenkins 内置 step,递归删除当前工作区(`$WORKSPACE`)
- 放 `cleanup`(最后执行) → 确保其他 post 块(如 junit)跑完才删
- 作用:释放磁盘空间,避免下次构建受上次残留影响

**生产 Jenkinsfile 第 313-315 行就在用:**
```groovy
cleanup {
    deleteDir()
}
```

### 5.6 失败处理模式 — post.failure 能做什么

```groovy
post {
    failure {
        echo '===== 构建失败 ====='
        // 发通知(邮件/钉钉/企业微信)
        // 回写 PR 状态(L7 会讲)
        // 归档失败现场(日志/截图)
    }
}
```

**失败处理的核心:** pipeline 失败不代表"什么都不做"——`post.failure` 正是"失败时要做什么"的钩子。

**生产 Jenkinsfile 的失败处理(第 296-307 行):**
```groovy
failure {
    script {
        if (env.IS_PR == 'true') {
            withEnv(["CI_STATUS=failure"]) {
                sh 'bash test-platform/scripts/pr-report.sh || true'
            }
        }
    }
}
```
→ PR 构建失败时回写 Gitee 状态(L7/L8 会细讲)。

## 二、渐进式小步

### 2.1 小步 5a — pipeline 级 post(成功/失败通知 + 清理)

**概念:**
- 在 pipeline 末尾(pipeline{} 内、stages{} 之后)加 `post{}` 块
- 三个条件:`success`(echo 成功)/`failure`(echo 失败)/`cleanup`(deleteDir)
- 演示成败分支不同收尾

**代码骨架:**
```groovy
pipeline {
    agent any
    // parameters...
    stages { ... }
    post {
        success {
            echo '===== 流水线成功完成 ====='
        }
        failure {
            echo '===== 流水线失败 ====='
        }
        cleanup {
            deleteDir()
        }
    }
}
```

**验证点:** 成功跑打印 "流水线成功完成";故意失败打印 "流水线失败";工作区被删。

---

### 2.2 小步 5b — Test stage 加 post.always + junit

**概念:**
- 在 Test stage 的 `steps{}` 之后加 stage 级 `post{}`
- `always{}` 里放 `junit` 收集测试报告
- 失败时也能收集(放 always 不是 success)

**代码骨架:**
```groovy
stage('Test') {
    steps { sh '...' }
    post {
        always {
            junit 'test-platform/backend/target/surefire-reports/*.xml'
        }
    }
}
```

**验证点:** 构建结果页出现 "Test Result" 链接,显示 91 个测试用例的明细。

---

### 2.3 小步 5c — 失败演练(验证 post.failure 真触发)

**概念:**
- 故意在 Verify stage 加一行失败(如 `exit 1` 或 `false`)
- 观察 pipeline 失败时,`post.failure` 是否触发、"流水线失败"是否打印
- 验证后**删掉失败行**,恢复正常

**代码骨架(临时失败行):**
```groovy
stage('Verify') {
    steps {
        dir("test-platform") {
            sh '''
                exit 1   // ← 故意失败,验证 post.failure
                ...
            '''
        }
    }
}
```

> ⚠️ `exit 1` 放在 sh 开头,后面的代码不执行(因为 sh 已退出)。验证完后**必须删掉这行**,否则后续 L6+ 全挂。

**验证点:**
- pipeline 标红(FAILURE)
- Console 末尾打印 `===== 流水线失败 =====`(证明 post.failure 触发)
- `cleanup` 的 deleteDir 也跑了(在 failure 之后)

## 三、最终 Jenkinsfile-learn 结构(Lesson 5 完成时)

```groovy
pipeline {
    agent any
    parameters { ... }
    stages {
        stage('hello') { ... }
        stage('Print Params') { ... }
        stage('Resolve Env') { ... }
        stage('Test') {
            steps { sh '...' }
            post {
                always { junit 'test-platform/backend/target/surefire-reports/*.xml' }
            }
        }
        stage('Build') { ... }
        stage('Deploy') { ... }
        stage('Verify') { ... }
        stage('Notify') { when { ... } steps { ... } }
    }
    post {
        success { echo '===== 流水线成功完成 =====' }
        failure { echo '===== 流水线失败 =====' }
        cleanup  { deleteDir() }
    }
}
```

## 四、复盘

### 5a 复盘

- **状态**: ✅ SUCCESS
- **改动**: pipeline 级 `post{}`（success/failure/cleanup）
- **关键认知**:
  - `post{}` 放在 `stages{}` 之后、`pipeline{}` 闭合之前（pipeline 级，不是 stage 级）
  - 执行顺序：success → cleanup（无 always 时）
  - `cleanup` 永远最后跑，即使 success/failure 块里报错也跑 → `deleteDir()` 放这里最稳
  - 在 `cleanup` 里加 `echo "===== 流水线开始清理 ====="` 是好习惯，Console 留痕迹方便追踪
  - 用户实际文字：success="流水线成功完成"、failure="流水线构建失败"、cleanup="流水线开始清理"

### 5b 复盘

- **状态**: ✅ SUCCESS
- **改动**: Test stage 加 stage 级 `post{ always{ junit ... } }`
- **关键认知**:
  - stage 级 `post{}` 放在 `steps{}` 之后、`stage{}` 内（和 pipeline 级 post 位置不同）
  - `junit` 放 `always` 不是 `success`：测试失败时 `mvn test` 返回非零 → stage 失败，但报告已生成 → `always` 能收集
  - `junit` 路径相对 `$WORKSPACE`（不是相对 `dir()`），要带 `test-platform/` 前缀
  - 路径 `test-platform/backend/target/surefire-reports/*.xml` 匹配 maven 在 `$WORKSPACE/test-platform/backend/` 下产出的 XML
  - Console 出现 `Recording test results` → junit 读到 XML 成功
  - `No suitable checks publisher found` → 无害警告（Jenkins Checks API 找 GitHub Checks publisher，Gitee 用不到，忽略）
  - Jenkins 构建结果页出现 "Test Result" 链接，显示 91 个测试用例明细
  - 多次构建后显示测试趋势图（首次只显示本次结果，无趋势）
  - 在 `junit` 前加 `echo "===== Test 收集执行日志 ====="` 方便 Console 定位

### 5c 复盘

- **状态**: ✅ FAILURE（预期红灯）
- **改动**: Verify stage 的 sh 开头临时加 `exit 1`（故意失败）
- **踩的坑**:
  - 第一次写法：拆成两个 sh 块（第一个 `exit 1`，第二个原验证逻辑）→ 第二个 sh 成死代码，结构不清晰
  - 修正写法：一个 sh 块，`exit 1` 放最开头，原验证逻辑保留（死代码但不删，验证完只删 `exit 1` 一行恢复）
- **关键认知**:
  - `exit 1` 放 sh 最开头 → sh 立即返回非零 → Verify stage 失败 → pipeline 失败 → `post.failure` 触发
  - Console 末尾顺序：`流水线构建失败`（post.failure）→ `流水线开始清理`（cleanup）→ `deleteDir` → `Finished: FAILURE`
  - 证明：pipeline 失败时 `post.failure` 真触发，`cleanup` 仍在 failure 之后执行（执行顺序 always → failure → cleanup）
  - Verify 失败后 Notify stage **不会执行**（pipeline 在失败 stage 处中断，`when` 也救不了）
  - 验证完**必须删掉 `exit 1`** → 恢复正常版 commit
- **L5 总结**:
  - `post{}` 两种位置：stage 级（stage 内 steps 后，收集该 stage 产物）vs pipeline 级（stages 后，全局通知/清理）
  - post 条件块执行顺序：`always` → `success`/`failure`/`unstable`/`aborted`（按结果选一）→ `cleanup`
  - `junit` 收集 surefire XML → Jenkins Test Result 页面 + 测试趋势图
  - `deleteDir()` 放 `cleanup`（最后跑，确保 junit 等先完成）
  - 失败演练验证：故意 `exit 1` → 确认 `post.failure` 真触发 + `cleanup` 仍执行

## 五、Console Output 关键片段

### 5a — pipeline 级 post（成功跑）

```
[Pipeline] echo
===== 流水线成功完成 =====
[Pipeline] echo
===== 流水线开始清理 =====
[Pipeline] deleteDir
```

执行顺序：success（echo 成功）→ cleanup（echo 清理 + deleteDir）。

### 5b — Test stage post.always + junit

**Maven 测试结果（Console）：**
```
[INFO] Tests run: 91, Failures: 0, Errors: 0, Skipped: 0
```

**junit 收集（Console）：**
```
[Pipeline] echo
===== Test 收集执行日志 =====
[Pipeline] junit
Recording test results
[Checks API] No suitable checks publisher found.
```

`Recording test results` → junit 读到 XML 成功。`No suitable checks publisher found` → 无害警告（Gitee 不用 GitHub Checks）。Jenkins 构建结果页出现 "Test Result" 链接，显示 91 个用例明细。

### 5c — 失败演练（故意 exit 1）

**Verify stage 的 sh 开头临时加 `exit 1`：**
```groovy
sh '''
    exit 1 // 临时测试
    echo "===== 等待服务启动 ====="   // ← 不执行（sh 已退出）
    ...
'''
```

**Console 末尾（pipeline 失败时 post 触发）：**
```
[Pipeline] echo
===== 流水线构建失败 =====
[Pipeline] echo
===== 流水线开始清理 =====
[Pipeline] deleteDir
ERROR: script returned exit code 1
Finished: FAILURE
```

执行顺序：failure（echo 构建失败）→ cleanup（echo 清理 + deleteDir）→ `Finished: FAILURE`。证明 `post.failure` 真触发，`cleanup` 在 failure 之后仍执行。验证后删掉 `exit 1` 恢复正常。
