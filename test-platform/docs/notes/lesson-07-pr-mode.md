# Lesson 7: PR 模式（IS_PR + refs/pull/N/head）

> 目标：让**同一条 pipeline** 支持"PR 构建"——验证 PR 代码（测试 + 编译），但**不部署**
> 前置：L0-L6 已收官，Jenkinsfile-learn 当前为 L6 完成版（#10 绿灯）
> 学习节奏：你讲（本文档）→ 我写（7a）→ 你查（AI 只读审查）→ 跑/审 → 复盘
> 完整生产版参考：~~`test-platform/Jenkinsfile` L20-90~~ **（2026-08-22 裁定停用**——其 PR 检出同缺 refspec，坑⑩大概率同在且从未被真实 PR 构建验证；终局：learn 版优化后替换生产版并删旧文件，详见 2.3 末「生产版裁定」）

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
- **本仓库实证**（2026-08-22 侦查；❌ 同日纠偏见 2.2 补充侦查）：`git ls-remote origin 'refs/pull/*'` →
  - `refs/pull/1/head` = `802f67d`（❌ 不可用作测试 PR——树停在 2026-07，learn 文件全缺，Build 必挂；已另建 smoke PR）
  - `refs/pull/1/MERGE` 同时存在
  - **匿名可访问**（无需 token）——Jenkins 容器里 fetch 不需要配凭据（此结论仍有效）

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
| ⑦ | detached HEAD | Jenkins 隐式 checkout 本就检出 SHA（detached）→ PR 切换是 detached→detached，**无警告**；证据行 = `Previous HEAD position was <SHA>` + `HEAD is now at <SHA>`（#33 实证修正） |
| ⑧ | $ 归属 | 有 shell 层 → 单引号把 `$` 留给 shell（7b）；无 shell 层 → 双引号 `${params.X}` Groovy 自己解析（7b'）；GString 里裸 `$X` = 查 Groovy 变量 → 报错 |
| ⑨ | GitSCM 证据行 | LocalBranch 后 HEAD 落在分支上，**没有** `HEAD is now at`；验收找分支切换痕迹（`Switched to a new branch 'pr-N'` 类） |
| ⑩ | GitSCM refspec | branches 只管"查"不管"拉"——默认 refspec 只拉 refs/heads/*，**不含 refs/pull/*** → `Couldn't find any revision to build`；必须 userRemoteConfigs 显式配 refspec（#35 实证） |

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

### 2.2 小步 7b — 手写 sh checkout（✅ 已完成，#31/#32/#33 实证，复盘见 3.3）

**问题起点：workspace 里的代码是谁检出的？** learn pipeline 没有 Checkout stage，但代码明明在 workspace——#29 日志第一行给出了答案：

```
Obtained test-platform/Jenkinsfile-learn from git https://gitee.com/greada/test-platform.git
```

这是 Job 的 SCM 配置（repo + Script Path）在 pipeline 启动前干的：clone/fetch → 检出默认分支 main → 取出 Jenkinsfile-learn。代码检出是「顺手」完成的——**隐式 checkout**。

**PR 模式的诉求**：隐式 checkout 给的是 main，PR 构建要跑 `refs/pull/N/head` 指向的 commit。Test 跑的是 workspace 里的代码——不切过去，PR 构建测的还是 main，等于白测 → 必须在 Test 之前新增 Checkout stage。

**两条命令拆解（7b 核心）：**

```bash
git fetch origin refs/pull/1/head && git checkout FETCH_HEAD
```

| 命令 | 干什么 | 关键认知 |
|---|---|---|
| `git fetch origin <ref>` | 只拉这一个 ref（最小拉取） | **不创建本地分支**；拉到的 commit SHA 记录在 `.git/FETCH_HEAD` |
| `git checkout FETCH_HEAD` | 切到刚拉到的 commit | `FETCH_HEAD` = 上一次 fetch 的速记指针——运行时只知 PR_NUMBER 不知 SHA，让 git 替我们记住 |

- `&&`：fetch 失败就不 checkout（fail fast，6b 血泪同款思想）
- 侦查实证：`refs/pull/1/head` = `802f67d`，匿名可访问，容器内 fetch 无需凭据

**坑⑦（#33 实证修正版）：Jenkins 里你"永远已经在 detached 上"。** 预告说 checkout FETCH_HEAD 后 git 会警告 detached HEAD——实际**没出现**，日志自己解释了原因：`Previous HEAD position was 6eaef56`。Jenkins 隐式 checkout 本来就检出 main 的 **SHA**（非分支名），HEAD 从一开始就是 detached；detached→detached 切换，git 不重复打警告。CI 里这正是我们要的（只读跑测试，不开发提交）；且 `Previous HEAD position <隐式检出SHA>` + `HEAD is now at <PR head SHA>` 两行同框，比预告的证据还硬——后者与 ls-remote 对上号即铁证检出正确。

**params 注入机制——三层访问路径（7a 病灶再强化）：**

| 层 | 写法 | 用途 |
|---|---|---|
| Groovy | `params.PR_NUMBER` | 本次构建只读快照，分流判断用这层 |
| sh | `$PR_NUMBER` | **string 参数自动注入为 shell 环境变量**，拼 ref 名用这层 |
| sh | `$IS_PR` | 我们自固化的 `env.IS_PR`（7b 不用，7c 守卫用） |

**⚠️ 新坑（引号选择）：sh 步骤用单引号。** `sh 'git fetch origin refs/pull/$PR_NUMBER/head && ...'`——单引号把 `$PR_NUMBER` **留给 shell 解析**（参数已注入环境变量，这正是上一行的机制）；若用双引号，Groovy 会抢先把 `$PR_NUMBER` 当 Groovy 变量解析（脚本上下文没有这个裸名字，报错或解析成空串）。

**任务卡**：在 Resolve Env 之后、Test 之前新增 stage，分流在 **Groovy 层**（script if/else），只有 PR 分支进 sh（sh 默认工作目录 = workspace 根 = 仓库根，无需 dir()）：

```groovy
stage('Checkout') {
    steps {
        script {
            // if PR 模式?(坑②:字符串比较带引号) → sh 'git fetch origin refs/pull/$PR_NUMBER/head && git checkout FETCH_HEAD'
            // else → echo '普通构建,沿用隐式 checkout(main)'
        }
    }
}
```

**约束**：坑⑥（sh 内注释只能用 `#`）；else 分支不写 `checkout scm`（learn 版隐式已检出，echo 说明即可）；不越界碰 Build/Deploy/Verify（7c 的事）。

**验证点**（编号以实际为准，7a 教训：中间可能有穿插构建）：

| build | 面板 | 预期 |
|---|---|---|
| #31 | PR_NUMBER 留空 | Checkout 走 else 分支；全流程与 #29 无差异，双 200 绿 |
| #32 | PR_NUMBER=<smoke PR 编号> | ① fetch 日志 ② `HEAD is now at <ls-remote 实证 SHA>` ③ detached HEAD 警告（坑⑦，预期）；Test 照跑（smoke PR 仅 +1 文档文件，91 用例应绿）；Build/Deploy/Verify 照跑（7c 才跳过） |

补充侦查（2026-08-22，**❌ 同日纠偏——初版结论作废**）：初版写"PR #1 仅改 `AGENTS.md`、后端零改动 → Test 预期同绿"，错在把 **PR 自身补丁**（merge-base 三点 diff）当成了 **PR 树与 main 的距离**。实证纠偏：PR #1 head 树停在 merge-base（2026-07-05），main 已领先 63 提交（72 文件 +8345/-446）；`docker-compose.learn.yml` 等 learn 文件在该树**全部缺失** → #32 若填 1：Checkout 能过（ref 可拉），Build 必挂（compose file not found）。**处置**：从当前 main 新建 smoke PR（仅 +1 占位文档）作本课测试 PR，保持 open 不合并（7c/L8 常驻复用）。

### 2.3 小步 7b' — GitSCM 对照版（已实装 R4 过审；#34 绿 / #35 爆坑⑩，返工中）

**问题起点：7b 两条 git 命令干成的事，为什么生产版要写一大坨 Groovy？** 7b 是**命令式**——告诉 git 每步做什么（fetch 这个 ref、checkout 这个指针）；GitSCM 是**声明式**——只声明"要什么"（哪个远端、哪个 ref、要不要干净、要不要分支），git 插件替你安排每一步。7b' = 把 7b 的 PR 分支换成声明式，两版对照，"谁管什么"一清二楚。

**生产版解剖（`test-platform/Jenkinsfile` L51-59，7b' 参照物；⚠️ 2026-08-22 起停用作参照——见本节末「生产版裁定」）：**

```groovy
checkout([
    $class: 'GitSCM',
    branches: [[name: "refs/pull/${params.PR_NUMBER}/head"]],
    userRemoteConfigs: [[url: 'https://gitee.com/greada/test-platform.git']],
    extensions: [
        [$class: 'LocalBranch', localBranch: "pr-${params.PR_NUMBER}"],
        [$class: 'CleanCheckout']
    ]
])
```

| 块 | 干什么 | 关键认知 |
|---|---|---|
| `$class: 'GitSCM'` | 选定 git 插件的 SCM 实现类 | Groovy map 调 Java 插件——`$class` = "用哪个类"，Jenkins 插件生态的通用互操作语法 |
| `branches: [[name: ...]]` | 声明要检出的 ref | **列表套 map**（两层中括号，可声明多个 refspec；漏一层是经典语法错）；GString 拼 `refs/pull/N/head` |
| `userRemoteConfigs` | 声明远端 | 需要凭据时在这里加 `credentialsId`（7b 已实证匿名可访问，省略） |
| `LocalBranch` | 检出后建**本地分支** `pr-N` | GitSCM 默认检出是 detached（7b 同款）；LocalBranch 让 HEAD 落在分支上——插件对坑⑦是"消除"而非"忍受" |
| `CleanCheckout` | 干净检出（git clean 一族清理未跟踪文件） | workspace 状态 = ref 的纯函数（可重复构建）；learn 版已有 post cleanup deleteDir，属双保险；clean 时机细节 #35 日志现场看 |

**⚠️ 坑⑧（本课核心认知升级）：引号规则的真相是"$ 归谁解析"。** 7b 与 7b' 引号选择**正好相反，但都对**：

| | 写法 | 引号 | `$` 谁解析 | 为什么 |
|---|---|---|---|---|
| 7b | `sh '''...$PR_NUMBER...'''` | 单引号 | **shell** | 参数已注入环境变量；单引号让 `$` 穿过 Groovy 交给 shell |
| 7b' | `branches: [[name: "...${params.PR_NUMBER}..."]]` | 双引号 | **Groovy** | 没有 shell 参与，只能 Groovy 自己拼字符串 |

细节差异：shell 写裸 `$PR_NUMBER`；GString 必须 `${params.PR_NUMBER}`（对象前缀 + 大括号）——裸 `$PR_NUMBER` 在 GString 里是查 Groovy 变量，上下文没有这个名字，直接报错。**"单引号 vs 双引号"是表象，"有没有 shell 层"才是本质。**

**两版能力对照（"谁管什么"总表）：**

| 能力 | 7b 手写 sh | 7b' GitSCM |
|---|---|---|
| 检出指定 ref | ✅ fetch + checkout | ✅ branches 声明 |
| workspace 清洁 | ❌ 自己管（靠 deleteDir 收尾） | ✅ CleanCheckout 显式管 |
| 本地分支 | ❌ detached（坑⑦，无害） | ✅ LocalBranch 建 `pr-N` |
| changelog（Changes 页） | ❌ sh fetch 不产生变更记录（#33 的 changelog 来自**隐式** checkout，build.xml checkouts 段实证） | ✅ 插件自己写 changelog |
| 日志风格 | `+ git fetch ...` 命令回显，grep 友好 | `Fetching upstream changes...` 插件话术，行数多 |
| refspec | ✅ 只 fetch 一个 ref（最小拉取） | 插件构造——**#35 看日志实证它实际 fetch 了什么，别猜** |
| 心智负担 | 低（就是 git） | 中（插件语义 + map 语法） |

**⚠️ 坑⑨：换 GitSCM 后，验收证据行会变。** 7b 的铁证是 `HEAD is now at <SHA>`；加 LocalBranch 后 HEAD 落在**分支**上，不再切 detached → 这行**不会出现**。#35 验收别再找它，要找分支创建/切换的痕迹（形如 `Switched to a new branch 'pr-N'`，具体行长什么样现场看）——找不到 ≠ 失败，找错了才算。

**任务卡**：把 Checkout stage 的 **PR 分支**从手写 sh 换成 GitSCM（对照生产版 L51-59 自己写，url 同仓库），else 分支不动，不越界 7c：

```groovy
stage('Checkout') {
    steps {
        script {
            if (env.IS_PR == 'true') {
                // checkout([ $class: 'GitSCM',
                //            branches: [[name: <GString 拼 ref,坑⑧>]],
                //            userRemoteConfigs: [[url: <同仓库 URL>]],
                //            extensions: [ <LocalBranch pr-N>, <CleanCheckout> ] ])
            } else {
                echo "===== 普通构建: ${env.IS_PR} 沿用隐式checkout(main) ====="  // 不动
            }
        }
    }
}
```

**约束**：坑⑧（双引号 + `${params.PR_NUMBER}`，别写裸 `$`）；branches 列表套 map 两层中括号；7b 旧版 sh 从 Jenkinsfile 删掉但笔记 2.2 留着（对照就是对照两版）；不碰 Build/Deploy/Verify（7c 的事）。

**验证点**（编号以实际为准）：

| build | 面板 | 预期 |
|---|---|---|
| #34 | 留空 | else 分支原样，与 #31 无差异，双 200 绿（回归确认） |
| #35 | `2` | ① 插件式日志（`Fetching upstream changes...` / `> git ...`）② 分支切换痕迹（坑⑨：**没有** `HEAD is now at` 是预期）③ Test 91 绿、全流程绿（7c 未动）④ 三观察点：refspec 实际 fetch 了什么 / Changes 页显示什么（diff 基准是上次构建记录的 revision，跨模式可能有趣）/ 与 #33 的 Checkout 日志逐行对照 |

**跑/审结果（2026-08-22，#34/#35）：**

| build | 面板 | 结果 | 关键证据（log 实证） |
|---|---|---|---|
| #34 | 留空 | SUCCESS ✅ 回归通过 | else 分支 echo（L69）+ 全流程与 #31 无差异，双 200 |
| #35 | `2` | **FAILURE — 坑⑩爆雷** | Checkout stage 挂：`Couldn't find any revision to build`（L81）；Test/Build `skipped due to earlier failure(s)` |

**⚠️ 坑⑩（#35 实证）：branches 只管"查"，不管"拉"。** GitSCM 的 fetch 拉什么由 **refspec** 决定，默认 `+refs/heads/*:refs/remotes/origin/*`（只拉分支命名空间，**不含 refs/pull/***）。#35 日志把整个决策链拍在案上：

```
> git fetch --tags --force --progress -- <url> +refs/heads/*:refs/remotes/origin/*   ← ① 只拉了分支
> git rev-parse refs/remotes/origin/refs/pull/2/head^{commit}                        ← ② 候选1:origin 前缀拼出的地址 → 没有
> git rev-parse refs/pull/2/head^{commit}                                            ← ③ 候选2:裸名 → 也没有(①根本没拉它)
ERROR: Couldn't find any revision to build. Verify the repository and branch configuration for this job.
```

**修复设计（返工任务卡）**：让候选2 命中——refspec 把 PR ref 拉到本地**同名路径**，裸 rev-parse 自然找到。注意 refspec 是 Groovy 字符串且要插值 → **坑⑧再次出场**（双引号 + `${params.PR_NUMBER}`）：

```groovy
userRemoteConfigs: [[
    url: 'https://gitee.com/greada/test-platform.git',
    refspec: "+refs/pull/${params.PR_NUMBER}/head:refs/pull/${params.PR_NUMBER}/head"
]]
```

- 备选：通配 `'+refs/pull/*/head:refs/pull/*/head'`（单引号字面量，拉全部 PR ref）也可——窄版最小拉取且再练一次坑⑧，选窄版
- **branches / extensions 一律不动**（它们没错，错的是没告诉 fetch 去拉）
- **⚠️ 自定义 refspec 会整体替换默认 refspec** → 本次 fetch 不再拉 refs/heads/*——无影响（PR 模式只要 PR ref；且 #36 fetch 行可直接实证这一点）

**#36 验证点**（填 2；留空回归可省——else 分支字节未动）：
① fetch 行出现**我们的** refspec（`+refs/pull/2/head:refs/pull/2/head`，且不再有 `+refs/heads/*`）② rev-parse 裸名候选命中（#35 挂掉的那行复活）③ LocalBranch 痕迹（`pr-2` 分支创建/切换，坑⑨现场）④ 91 用例 + 双 200 全流程绿

**#35 附赠的三个计划外收获：**
1. **上游挂→下游跳的活教材**：Checkout 挂后 Test/Build 全部 `skipped due to earlier failure(s)`——声明式 pipeline 的 stage 短路行为，7c 的 when 守卫还没写，先看到了"不守也自跳"的另一面
2. **retry(2) 双轮目击（第二次）**：L81/L174 两次 `Maximum checkout retry attempts reached`——单轮内 rev-parse 3 次重试 + 整条 pipeline 从头重跑，两层重试各就各位
3. **观察点自打脸修正**：讲义预告"refspec 看日志实证，别猜"→ 实证结果：默认 refspec 不含 refs/pull → 初版"生产版解剖"漏了 refspec 这个关键参数 → 本节已补全

**⚠️ 生产版裁定（2026-08-22，用户指令）：即日起停止参考生产版 Jenkinsfile。** 其 PR 检出同样没配 refspec（坑⑩大概率同在），且从未被真实 PR 构建验证过——参照物本身未经验证。**终局计划**：L9 完成后，learn 版优化升级为新的生产版，删除旧 `test-platform/Jenkinsfile`（docs 中的副本同步更新）。

### 2.4 小步 7c — when 守卫矩阵（待 7b' 完成后展开）

按 7.5 矩阵给各 stage 加 `when` / if 分流，含 Notify 组合条件（坑④）和 Build 写死 compose 文件（坑③）。

## 三、复盘（7a/7b 已回填）

### 3.1 7a — 验证通过（2026-08-22，#29/#30 双绿）

> 计划时预期验证 build 为 #11/#12，实际期间穿插了 #11~#28 的中间构建，坑⑤（参数下次构建才注册）被这批构建自然吸收——#29 触发时参数面板已出现 PR_NUMBER 输入框。

| build | 面板 PR_NUMBER | Resolve Mode 日志 | 行为 | 结果 |
|---|---|---|---|---|
| #29 | 留空 | `IS_PR = false` | Build/Deploy/Verify 照旧全跑，双 HTTP 200 | SUCCESS |
| #30 | `1`（build.xml 参数值实证） | `IS_PR = true` | **同样全跑**（7a 无守卫，Deploy/Verify 也执行——预期行为，7c 才跳过） | SUCCESS |

两条验收标准全部达成：**普通构建与 #10 无行为差异** ✅（#29）；**PR 模式开关固化 env.IS_PR** ✅（#30）。

### 3.2 7a 两轮审查病灶存档（用户手写阶段）

| 轮次 | 病灶 | 根因 |
|---|---|---|
| R1 | ① params/env 两层混淆 ② env.IS_PR 未赋值就被引用 ③ if/else 提前写了 7c 的守卫 | 「params 只读快照 vs env 可写全局」两层心智模型未建立 |
| R2 | ① `params.IS_PR` 幽灵引用复发（应为 `env.IS_PR`）② `== ''` 判空漏 null | 层模型仍不稳；对 Groovy truthiness 天然覆盖 null+'' 双空不熟 |

**教训**：判「非空」首选 truthiness——`params.PR_NUMBER ? 'true' : 'false'` 一个写法同时覆盖 null（参数未注册的首次构建）和 `''`（面板留空）两种"空"，无需手写 `!= null && != ''`。

### 3.3 7b — 验证通过（2026-08-22，#31/#32/#33：正样本 + 负样本 + 真 PR 三连）

> 计划验证 #31/#32；用户实操跑出三连，其中 #32（填老 PR #1）恰成负样本，把侦查纠偏的预言变成了实证。

| build | 面板 | 关键证据（build.xml + log 实证） | 结果 |
|---|---|---|---|
| #31 | 留空 | `IS_PR = false` → Checkout 走 else 分支：`普通构建: false 沿用隐式checkout(main)`（log L69），无 checkout 动作 → 双 200 | SUCCESS ✅ 普通模式验收 |
| #32 | `1`（老 PR） | checkout 成功：`HEAD is now at 802f67d` → Test 过 → **Build 挂**：`docker-compose.learn.yml: no such file or directory`（L1226）——侦查纠偏"Checkout 能过、Build 必挂"逐字命中 | FAILURE ✅ 预期内负样本 |
| #33 | `2`（真 PR） | fetch `refs/pull/2/head` → `HEAD is now at 20df4fb`（与 ls-remote 逐字对号 = 检出铁证）→ junit 91 用例 0 失败 → 双 200，71s | SUCCESS ✅ PR 模式验收 |

**计划外教学收获：**

1. **坑⑦实证修正**（2.2 已同步改写）：detached HEAD 警告从未出现——隐式 checkout 本就检出 SHA（detached），`Previous HEAD position was 6eaef56` 一行同时坐实隐式检出位置 + PR 检出位置。
2. **retry(2) 现场目击**：#32 日志两次出现 `构建模式 IS_PR = true`（L51/L1273）——pipeline 级 retry 把整条流水线**从头重跑**（不是从失败 stage 续跑），两轮挂同一处。6b 的"retry 包整条 pipeline"结论在真实失败场景再现。
3. **单分支 Job 固有行为**：#33 跑的 pipeline 定义仍是 **main 的** Jenkinsfile-learn（Job SCM 启动前从 main 检出脚本），但 Test/Build 跑的是 PR head 的代码——这就是"改 pipeline 必须先合 main 才生效"的根因。
4. **测试载体变更**：AI 备的 smoke 分支（lesson7-smoke-pr + pr-smoke.md）未被用上——用户用自己的分支（docs/update-agents-md-v3.3，merge main + 仅文档改动）建了 PR #2（head=20df4fb），等效满足"main+仅文档"载体要求；smoke 分支已删（本地+远端），PR #2 保持 open 作 7c/L8 常驻测试 PR。

<!-- 7b'/7c 完成后继续回填 -->

## 变更日志

| 日期 | 变更 |
|------|------|
| 2026-08-22 | 初版创建：概念（refs/pull 命名空间 + IS_PR 开关 + when 矩阵 + 7 坑）+ 7a 任务卡与伪代码骨架；7b/7b'/7c 占位 |
| 2026-08-22 | 7a 实装（**节奏破例：用户两轮尝试未过审后明确请求 AI 代写**，AI 逐行讲解）：PR_NUMBER 参数 + `env.IS_PR = params.PR_NUMBER ? 'true' : 'false'`（三元+truthiness，null/'' 双覆盖）+ DEPLOY_TARGET 保持线性；两轮审查病灶（params/env 两层混淆 ×2、`== ''` 漏 null、if/else 越界 7c）待复盘回填；待 #11/#12 构建验证 |
| 2026-08-22 | **7a 验证通过收官**：#29（PR_NUMBER 留空，IS_PR=false，与 #10 无行为差异）+ #30（PR_NUMBER=1，IS_PR=true，其余 stage 照旧全跑双 200）——坑⑤被 #11~#28 中间构建自然吸收；复盘回填（3.1 验证证据 + 3.2 审查病灶存档）→ 进 7b 手写 sh checkout |
| 2026-08-22 | 7b 概念讲解落盘 2.2 节：隐式 checkout（Obtained 日志真相）/ fetch+checkout FETCH_HEAD 拆解 / 坑⑦ detached HEAD=验收证据 / params 三层注入 + **新坑：sh 单引号留 `$` 给 shell** / 任务卡 + #31/#32 验证点；补充侦查 PR #1 仅改 AGENTS.md（后端零改动）。待用户实装 |
| 2026-08-22 | **侦查纠偏**：PR #1 树停在 2026-07-05（merge-base），main 领先 63 提交，learn 文件全缺 → "#32 填 1"方案作废（Build 必挂）；7.2 实证块/2.2 验证点/补充侦查三处同步修正；从 main 新建 smoke PR（分支 lesson7-smoke-pr，仅 +1 占位文档 pr-smoke.md）作测试载体，待用户 Gitee 建 PR |
| 2026-08-22 | **7b 验证通过收官**：#31（留空，else 分支 echo + 双 200）/#32（填 1=老 PR，负样本实证"Checkout 过、Build 挂" + retry(2) 双轮目击）/#33（填 2=真 PR，`HEAD is now at 20df4fb` 对号铁证 + 91 用例 + 双 200，71s）；坑⑦实证修正（隐式 checkout 本就 detached，`Previous HEAD position` 才是证据行）；复盘回填 3.3；smoke 分支未用已删，PR #2 任常驻测试 PR → 进 7b'（GitSCM 对照版） |
| 2026-08-22 | 7b'「你讲」环节落盘 2.3 节：命令式 vs 声明式 / 生产版 GitSCM 逐块解剖（$class/branches/userRemoteConfigs/LocalBranch+CleanCheckout）/ **坑⑧ = $ 归属**（7b 单引号 shell vs 7b' 双引号 Groovy，引号规则对称） / **坑⑨ = GitSCM 证据行变化** / 两版能力对照表（谁管 clean/localBranch/changelog/refspec）+ 任务卡 + #34/#35 验证点。下一步：用户实装 → AI 只读审查 |
| 2026-08-22 | 7b' 实装与审查：用户手写 GitSCM 版（R1 extensions 缺失 → R2 坑⑧复发 localBranch 单引号 + $class 类名小写 → R4 全清过审）+ AI 补头注释随代码提交（c40d839）；跑/审：#34 绿（回归✅）/**#35 FAILURE 爆坑⑩**（branches 只管查不管拉，默认 refspec 不含 refs/pull）→ 返工任务卡（窄版 refspec，坑⑧再练）落盘 2.3；贴墙坑表扩至⑩；**生产版裁定：停用参照，终局 learn 替换生产版并删旧文件**。待返工 → 审查 → #36 |
