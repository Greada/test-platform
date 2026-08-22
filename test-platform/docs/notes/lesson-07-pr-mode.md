# Lesson 7: PR 模式（IS_PR + refs/pull/N/head）

> 目标：让**同一条 pipeline** 支持"PR 构建"——验证 PR 代码（测试 + 编译），但**不部署**
> 前置：L0-L6 已收官，Jenkinsfile-learn 当前为 L6 完成版（#10 绿灯）
> 学习节奏：你讲（本文档）→ 我写（7a）→ 你查（AI 只读审查）→ 跑/审 → 复盘
> 完整生产版参考：`test-platform/Jenkinsfile` L20-90（可对照，但 7a 自己写）

## 一、概念

### 7.1 问题：同一条 pipeline，两种诉求

现在 pipeline 只会"全量走"：Test → Build → Deploy → Verify。但真实场景有第三种触发方式：

| 触发方式 | 诉求 | 该做什么 | 不该做什么 |
|---|---|---|---|
| Crontab / 手动（现有） | 发布 | 测试 + 构建全量镜像 + 部署 + 验证 | — |
| **PR 构建（本课新增）** | **验证 PR 代码质量** | 跑单测 + 验证后端能编译 | **部署它**（PR 没合并，凭什么占用环境？） |

生产版的做法：`pr-poller.sh` 轮询 Gitee PR 列表，发现新 PR 就带着 `PR_NUMBER` 触发 Jenkins Job。pipeline 内部靠一个**模式开关**分流——这就是 `IS_PR`。

### 7.2 refs/pull/N/head — Git 的隐藏命名空间

**ref 是什么**：分支、标签本质都是 ref——`.git/refs/` 下的一个小文件，内容就是一个 commit SHA。`main` 分支 ≈ `refs/heads/main` 这个文件里的 SHA 指针。

| ref | 谁创建 | 指向 |
|---|---|---|
| `refs/heads/main` | 你 push 时 | 你 main 分支最新 commit |
| `refs/tags/v1.0` | 你打 tag 时 | tag 指向的 commit |
| `refs/pull/1/head` | **Gitee 平台** | **PR #1 源分支最新 commit** |
| `refs/pull/1/MERGE` | Gitee 平台 | PR #1 试合并进 base 的结果（Gitee 大写 MERGE，GitHub 是小写 merge） |

**关键认知：**
- `refs/pull/*` 是**平台维护的只读 ref**——PR 存在它就存在，你本地 clone 下来默认**看不到**（不 fetch 它），但 `git fetch origin refs/pull/1/head` 可以直接拉
- `head` vs `MERGE` 怎么选：验证"PR 作者写了什么"用 `head`；验证"合并进 main 会不会坏"用 `MERGE`。CI 常规做法是 `head`（合并冲突留给平台检测）
- **本仓库实证**（2026-08-22 侦查）：`git ls-remote origin 'refs/pull/*'` →
  - `refs/pull/1/head` = `802f67d`（可用作本课测试 PR）
  - `refs/pull/1/MERGE` 同时存在
  - **匿名可访问**（无需 token）——Jenkins 容器里 fetch 不需要配凭据

### 7.3 模式开关：PR_NUMBER 非空 = PR 模式

为什么开关用 `string` 参数的"非空"而不是 `boolean` 勾选框？

| 方案 | 问题 |
|---|---|
| `booleanParam(name: 'IS_PR')` | 手动勾选容易误触发；poller 传参时不直观；布尔只有 2 态，将来无法携带"哪个 PR"的信息 |
| `string(name: 'PR_NUMBER', defaultValue: '')` | **空 = 普通构建；填 `1` = PR #1 构建**——一个参数同时携带"模式 + 数据"，且普通构建什么都不用动 |

生产版实际有 4 个参数（PR_NUMBER / PR_SHA / PR_HEAD_REF / PR_BASE_REF，供状态回写用），**learn 版只取 `PR_NUMBER` 一个**（L8 状态回写时再看要不要加）。

**string 参数语法**（通用示例，非 7a 答案）：

```groovy
parameters {
    string(name: 'SOME_HINT', defaultValue: '', description: '说明文字,留空走默认逻辑')
}
```

### 7.4 env.IS_PR — 为什么固化、为什么只能是字符串

判断结果为什么不像 L4 那样只在 stage 内用一次，而是**固化到 `env.IS_PR`**？

- `params.*` 是本次构建的**只读快照**，散落各处判断容易写错
- 固化到 env 后：后续每个 stage 的 `when` 守卫、sh 里的 `$IS_PR`、post 块，读的都是**同一个全局开关**
- 这就是生产版 `Resolve Mode` stage 的职责：进 pipeline 第一件事，先定性"这次是 PR 还是普通构建"

**⚠️ 坑①（本课最高发）：`env` 没有布尔类型。** `env.XXX` 只能存字符串，赋值必须带引号：

```groovy
// ❌ 错误示范(不报错但后患无穷):
env.IS_PR = true        // Groovy 布尔,写入 env 时被隐式转成字符串 "true"——碰巧能对,但理解就歪了

// ✅ 正确姿势(生产版同款心智模型):
env.IS_PR = <条件表达式> ? 'true' : 'false'   // 显式字符串,所见即所得
```

**⚠️ 坑②：when 里比较必须带引号。** 与坑①是同一个根：

```groovy
when { expression { env.IS_PR == 'true' } }    // ✅ 字符串比较
when { expression { env.IS_PR == true } }      // ❌ 永远不成立:'true'(字符串) != true(布尔)
```

**判断"非空"的 Groovy 语法**（7a 核心语法点）：

- Groovy truthiness：`null`、空串 `''`、`0` 都是真值判断中的 falsy——`if ('')` 不成立
- Elvis 操作符 `?:`：`A ?: B` = A 为 truthy 取 A，否则取 B
- 所以"PR_NUMBER 非空"的伪代码：`<PR_NUMBER 参数值> ?: ''` 再判断真假；或直接 `if (params.PR_NUMBER)`（null 和空串都 falsy，天然覆盖两种"空"）
- 生产版原句参考：`env.IS_PR = (params.PR_NUMBER ?: '') ? 'true' : 'false'`——看懂即可，7a 自己写

### 7.5 when 守卫矩阵（本课最终形态预览）

7c 完成后，pipeline 行为矩阵（定案 2026-08-22）：

| stage | 普通模式 | PR 模式 | 守卫方式 |
|---|---|---|---|
| Test | ✅ | ✅ | 无守卫(两种模式都跑) |
| Checkout | 默认(scm) | **检出 refs/pull/N/head** | if/else 分支(7b) |
| Resolve Env | ✅ | **跳过** | `when { IS_PR != 'true' }` |
| Build | backend + frontend | **只 backend**（验证编译，与生产版同款） | if/else 分支 |
| Deploy | ✅ | 跳过 | `when` |
| Verify | ✅ | 跳过 | `when` |
| Notify | 仅 prod | 跳过 | `when` 组合条件 |

**⚠️ 坑③：Resolve Env 跳过的连带反应。** PR 模式下 `DEPLOY_TARGET` 从未被赋值 → sh 里 `$DEPLOY_TARGET` 是**空串**。Build 的 PR 分支**绝不能**写 `[ "$DEPLOY_TARGET" = "test" ] && COMPOSE_FILE=...` 这种依赖（空串会让条件静默走错路）→ PR 分支**写死** `docker-compose.learn.yml`。

**⚠️ 坑④：when 多条件默认 AND。**

```groovy
when {
    expression { env.IS_PR != 'true' }   // 条件1
    expression { env.DEPLOY_TARGET == 'prod' }  // 条件2 → 两者 AND
}
```

Notify 要的是"非 PR **且** prod"，恰好就是 AND——默认行为即所需，但必须**知道**它是 AND 才不会在想要 OR 时踩坑。

### 7.6 参数生效时机坑 + 高发坑浓缩清单

**⚠️ 坑⑤：`parameters{}` 的改动，下一次构建才生效。** Jenkins 在构建结束时才把 pipeline 里声明的参数写回 Job 定义。所以 7a 合入后：

- 第一次跑（#11）：参数面板**还没有** PR_NUMBER 输入框，用的是旧参数表 → 行为应与 #10 完全一致（这正是 7a 的第一条验收标准）
- 第二次跑（#12）：面板出现 PR_NUMBER，手动填 `1` 触发 PR 模式

**本课高发坑浓缩（贴墙版）：**

| # | 坑 | 正确姿势 |
|---|---|---|
| ① | env 无布尔 | `env.IS_PR = ... ? 'true' : 'false'` 显式字符串 |
| ② | when 比较布尔 | `env.IS_PR == 'true'` 带引号 |
| ③ | DEPLOY_TARGET 空串 | PR 分支的 compose 文件写死 learn 版，不依赖 `$DEPLOY_TARGET` |
| ④ | when 多条件 | 默认 AND；Notify 恰好需要 AND |
| ⑤ | 参数下次生效 | 改完 parameters 至少跑两次才见新参数 |
| ⑥ | sh 内注释 | 必须用 `#`，`//` 会被 shell 当参数（6b 血泪） |
| ⑦ | detached HEAD | 7b 检出 FETCH_HEAD 后 git 会警告"detached HEAD"——**这是预期行为**，Jenkins 不在乎，别慌 |

## 二、渐进式小步

### 2.1 小步 7a — PR_NUMBER 参数 + Resolve Mode

**做什么**（只埋开关，不改任何 stage 行为）：

1. `parameters{}` 新增 `PR_NUMBER`（string，默认空，非空 = PR 模式）
2. `Resolve Env` stage 扩展为「Resolve Mode」职责：根据 PR_NUMBER 是否非空，固化 `env.IS_PR`（字符串 'true'/'false'），并在日志里 echo 出来（日志是后面验收的证据）

**伪代码骨架**（翻译成 Groovy 是你的作业）：

```groovy
parameters {
    choice(...)          // 现有 DEPLOY_ENV,不动
    string(...)          // ← PR_NUMBER,参照 7.3 通用示例
}

stage('Resolve Env') {
    steps {
        script {
            // 现有 DEPLOY_TARGET 逻辑保留
            // ↑ 新增:计算 IS_PR(注意坑①:字符串),echo 打印(验收证据)
        }
    }
}
```

**不做什么**（留给 7b/7c）：checkout 分流、Build 只建 backend、Deploy/Verify/Notify/Resolve Env 的 when 守卫——7a 跑完应该和 #10 一模一样绿。

**验证点：**
- #11（改完后第一次跑）：确认坑⑤——面板无 PR_NUMBER；日志出现 `IS_PR` 值为 false 的证据；全流程与 #10 无行为差异
- #12（第二次跑，面板填 PR_NUMBER=1）：日志出现 `IS_PR='true'` 证据；**其余 stage 照旧全跑**（7a 还没有守卫，Deploy/Verify 也会跑——这是预期的，别意外）

### 2.2 小步 7b — 手写 sh checkout（待 7a 完成后展开）

先手写 `sh` 版：`git fetch origin refs/pull/N/head && git checkout FETCH_HEAD`，体验"底层命令"和 detached HEAD（坑⑦）。细节骨架轮到这步时再讲。

### 2.3 小步 7b' — GitSCM 对照版（待 7b 完成后展开）

换成 `checkout([$class: 'GitSCM', branches: [[name: 'refs/pull/N/head']], ...])` 声明式写法，两版对比：谁管 clean、谁管 localBranch、日志差异。生产版用 GitSCM（L47-59 可提前围观）。

### 2.4 小步 7c — when 守卫矩阵（待 7b' 完成后展开）

按 7.5 矩阵给各 stage 加 `when` / if 分流，含 Notify 组合条件（坑④）和 Build 写死 compose 文件（坑③）。

## 三、复盘（待回填）

<!-- 7a/7b/7b'/7c 完成后回填:踩坑、验证结果、build 号 -->

## 变更日志

| 日期 | 变更 |
|------|------|
| 2026-08-22 | 初版创建：概念（refs/pull 命名空间 + IS_PR 开关 + when 矩阵 + 7 坑）+ 7a 任务卡与伪代码骨架；7b/7b'/7c 占位 |
| 2026-08-22 | 7a 实装（**节奏破例：用户两轮尝试未过审后明确请求 AI 代写**，AI 逐行讲解）：PR_NUMBER 参数 + `env.IS_PR = params.PR_NUMBER ? 'true' : 'false'`（三元+truthiness，null/'' 双覆盖）+ DEPLOY_TARGET 保持线性；两轮审查病灶（params/env 两层混淆 ×2、`== ''` 漏 null、if/else 越界 7c）待复盘回填；待 #11/#12 构建验证 |
