# Lesson 8 — PR 状态回写

> 目标：把「Jenkins 构建结果」回写到 Gitee PR 页面，形成完整可演示的 PR 闭环。  
> 前置：Lesson 7 已完成 PR 模式、`refs/pull/N/head` 检出和 when 守卫矩阵。  
> 状态：真实成功链路已验收（含 `CHECK_RUN_ID` 贯穿传递与 PR 审核项「测试」回写）；待补 PR 失败回写演练。

---

## 一、这一课解决什么问题

Lesson 7 结束时，Jenkins 已经能区分：

- 普通模式：完整测试、构建、部署、验证
- PR 模式：只测试并构建 backend，不部署

但还有一个缺口：**构建结果只存在于 Jenkins 里**。

如果你在 Gitee 上打开 PR，看不到这个 PR 是否通过 CI。Lesson 8 要补上的就是这条链路：

```text
Gitee PR
  ↓
提交者评论 start build
  ↓
Poller 发现有效评论
  ↓
Jenkins 构建
  ↓
Gitee PR 页面显示 success / failure
```

这就是 Gitee Check Run。

### 0. Check Run 与 PR 审核项「测试」的区别

这两个东西在 Gitee PR 页面上都存在，但含义不同：

| 对象 | 绑定对象 | 谁写入 | 本课含义 |
|---|---|---|---|
| Check Run `ci/jenkins` | commit SHA | Jenkins | 这次代码提交的 CI 结果 |
| PR 审核项「测试」 | PR + 指定测试人 | 被指派的测试人 | PR 流程里的测试确认 |

所以 Jenkins 成功后要做两件事：

1. `pr-report.sh`：把本次 commit 的 Check Run 更新为 `completed(success)`
2. `pr-test-review.sh`：把 token 用户在 PR 审核项「测试」里的状态标记为通过

当前 token 用户是 `greada`，而 PR #2 的测试人也是 `greada`，所以可以直接标记。不需要使用 `force`；`force` 是管理员强制通过用的参数，不该作为常规链路。

---

## 二、总体架构

```text
┌──────────────┐
│   Gitee      │
│  Open PR #2  │
└──────┬───────┘
       │ 1. 查询 open PR、head SHA 和评论
       ▼
┌──────────────────────────┐
│ pr-poller-learn.sh       │
│ - 校验 start build       │
│ - 校验评论人             │
│ - 写 in_progress         │
│ - 触发 Jenkins           │
└──────┬───────────────────┘
       │ 2. buildWithParameters
       ▼
┌──────────────────────────┐
│ Jenkins test-platform-learn │
│ - PR_NUMBER              │
│ - PR_SHA                 │
│ - CHECK_RUN_ID           │
│ - Test / Build backend   │
└──────┬───────────────────┘
       │ 3. post.success / post.failure
       ▼
┌──────────────────────────┐
│ pr-report.sh             │
│ - status                 │
│ - name                   │
│ - details_url            │
│ - output                 │
└──────┬───────────────────┘
       │ 4. Gitee Check Runs API
       ▼
┌──────────────────────────┐
│ Gitee PR 页面            │
│ ci/jenkins ✓ / ✗        │
└──────────────────────────┘

Jenkins 成功时还会额外调用 `pr-test-review.sh`：

```text
Jenkins post.success
  ↓
POST /repos/{owner}/{repo}/pulls/{number}/test
  ↓
Gitee PR 审核项「测试」变为已完成
```
```

### 参与者职责

| 参与者 | 只负责 | 不负责 |
|---|---|---|
| Gitee | 保存 PR、commit、Check Run | 调度 Jenkins |
| Poller | 校验评论、写 in_progress、触发 Jenkins | 执行构建 |
| Jenkins | 测试、构建、判定成功/失败 | 查询 Gitee PR 列表 |
| `pr-report.sh` | 创建/更新一个 Check Run | 改变构建结果 |
| `pr-test-review.sh` | 成功后把当前 token 用户的测试审核项标记为通过 | 判定构建成功或失败 |

---

## 三、Gitee Check Run 是什么

Check Run 是绑定在某个 commit SHA 上的外部系统检查任务。

Gitee PR 页面上看到的 `ci/jenkins ✓/✗`，本质上是一条 Check Run 记录。

> 重要坑：Gitee 不支持 GitHub 风格的 `POST /repos/{owner}/{repo}/statuses/{sha}`。
> 本次真实环境验证该接口返回 `405 Not Allowed`，所以必须使用：
> `POST /repos/{owner}/{repo}/check-runs` 创建，`PATCH /repos/{owner}/{repo}/check-runs/{id}` 更新。

### 1. 核心字段

| 字段 | 含义 | 本课取值 |
|---|---|---|
| `name` | 检查任务名称 | `ci/jenkins` |
| `id` | Check Run 唯一 ID | 创建接口返回，Poller 捕获后传给 Jenkins |
| `head_sha` | 绑定的 commit | PR head SHA |
| `pull_request_id` | Gitee PR 内部 ID | Poller 从 PR API 解析 |
| `status` | 执行状态 | `in_progress` / `completed` |
| `conclusion` | 完成结论 | `success` / `failure` |
| `details_url` | 点击后跳转的链接 | Jenkins build URL |
| `output` | 展示报告 | CI 构建中/通过/失败 |

### 2. 状态机

```text
in_progress ──► completed(success)
        └────► completed(failure)
```

| 状态 | 谁写入 | 什么时候写 |
|---|---|---|
| `in_progress` | Poller | 触发 Jenkins 前 |
| `completed + success` | Jenkins `post.success` | PR 构建成功后 |
| `completed + failure` | Jenkins `post.failure` | PR 构建失败后 |

### 3. 为什么必须传递 `CHECK_RUN_ID`

真实环境验证过两个关键事实：

1. `POST /check-runs` 创建 pending 成功后，响应里会返回这条 Check Run 的 `id`。
2. 对 PR 关联的 Check Run，用 `GET /repos/{owner}/{repo}/commits/{sha}/check-runs` 反查可能返回 `total_count=0`，即使页面已经存在这条 pending 记录。

所以完成态不能依赖“按 SHA 反查 ID”，必须走这条确定性链路：

```text
Poller 创建 pending
  ↓ 解析响应中的 check_run_id
buildWithParameters 传入 CHECK_RUN_ID
  ↓
Jenkins post.success / post.failure
  ↓
pr-report.sh 直接 PATCH /check-runs/{check_run_id}
```

手动触发且没有 `CHECK_RUN_ID` 时，`pr-report.sh` 仍保留按 SHA 反查的兜底逻辑；自动触发链路则必须拿到 ID 才会触发 Jenkins。

### 4. 为什么绑定 SHA，而不是 PR 编号

PR 编号不变，但 PR 里的 commit 会变。

如果状态绑定 PR 编号，新 commit 可能复用旧状态，导致误判。

绑定 head SHA 后：

```text
PR #2 @ abc123 → in_progress → completed(success)
PR #2 @ def456 → in_progress → completed(failure)
```

每个 commit 都有独立状态。

---

## 四、Poller 设计

Poller 的职责是：**发现有效 `start build` 评论，并触发一次 Jenkins 构建**。

这里采用方案 B：**评论是唯一触发方式**。Poller 发现新 SHA 时只更新它对当前 PR 的认识，不会自动构建。

### 1. 输入

从 `.env.ci` 读取：

```text
GITEE_OWNER
GITEE_REPO
GITEE_TOKEN
JENKINS_URL
JENKINS_USER
JENKINS_TOKEN
JENKINS_JOB
JENKINS_INTERNAL_URL
```

### 2. 处理流程

1. 调 Gitee API 查询 open PR
2. 解析每个 PR 的 `id`、`number`、`head.sha`、PR 创建者、head 分支提交者
3. 调 Gitee API 查询该 PR 的评论
4. 只保留 `body.trim() == "start build"` 的精确命令评论
5. 校验评论人必须是 PR 创建者或 head 分支提交者
6. 用 `manual:PR_NUMBER:COMMENT_ID` 作为去重键
7. 如果该评论已处理，跳过
8. 如果未处理，先创建或更新 `in_progress` Check Run，并解析响应中的 `check_run_id`
9. `in_progress` 写成功且拿到 `check_run_id` 后，触发 Jenkins
10. Jenkins 返回 `201`，才把评论 key 写入状态文件

### 3. 为什么只有评论能触发

**新 commit 不自动构建，是为了把“代码更新”和“提交者确认构建”分开。**

PR 里推新 commit 很常见：可能是草稿、临时保存、修复拼写，也可能是别人往分支里推代码。如果 Poller 看到 SHA 变化就直接消耗 Jenkins 资源，提交者反而失去控制权。

改成评论触发后：

```text
新 commit 出现       → 不构建
无评论               → 不构建
非提交者评论          → 不构建
评论 please start... → 不构建
提交者评论 start build → 构建一次
```

### 4. 为什么必须精确匹配

Poller 判断的是：

```text
评论正文去掉首尾空白后 == start build
```

所以下面这些都有效：

```text
start build
 start build 
```

下面这些都无效：

```text
please start build
start build now
START BUILD
```

原因很简单：PR 评论区是聊天区，不是命令行。如果把包含 `start build` 的普通聊天也当命令，就可能因为一句话误触发构建。

### 5. 为什么校验评论人

只有两类人可以触发：

- PR 创建者：`user.login`
- head 分支提交者：`head.user.login`

这可以防止任意访客在公开 PR 里评论一句话就消耗你的 Jenkins 资源。后续如果需要更多维护者，再显式加白名单，而不是默认放开所有人。

### 6. 为什么用评论 ID 去重

Poller 每 60 秒运行一次，但一条有效评论只应该触发一次构建。

状态键是：

```text
manual:PR_NUMBER:COMMENT_ID
```

它刻意不包含 SHA。原因：

- 如果包含 SHA，PR 更新后，同一条旧评论可能对新 SHA 再次触发
- 旧评论只能代表提交者确认过旧代码，不能代表他确认过新代码
- 不包含 SHA 后，一条评论一辈子只处理一次
- 新 commit 后必须再发新的 `start build`，才代表提交者确认新代码

这样规则就闭合了：

```text
PR @ abc123 + 评论 #101 start build → 构建 abc123
PR @ def456 + 仍只有评论 #101        → 不构建
PR @ def456 + 新评论 #102 start build → 构建 def456
```

### 7. 为什么要先写 in_progress

如果先触发 Jenkins，再写 in_progress：

- Jenkins 可能已经开始构建
- PR 页面却还没有任何状态
- 用户会看到“无状态 → success/failure”的跳跃

先写 in_progress，用户能看到：

```text
CI 构建中… → CI 构建通过 ✓
```

### 8. 为什么要本地状态文件

Poller 每 60 秒运行一次，但同一条有效评论只应该触发一次构建。

状态文件里存：

```text
manual:2:101
```

只要这一行存在，下次轮询就跳过这条评论。

Jenkins 没有返回 `201` 时不写这行。这样下一轮会重试，不会把失败的触发误标成已处理。

状态文件还会记录触发尝试：

```text
attempt:2:101:20df4fb2a0e723c42db55954bada569cf39cdb49
```

这条记录的作用是处理竞态：

- Jenkins 触发失败后，同一个 SHA 下一轮可以重试
- 如果重试前 PR 更新到新 SHA，这条旧评论会被跳过
- 因为旧评论只确认过旧 SHA，不能代表提交者确认新代码

---

## 五、Jenkins Pipeline 设计

### 1. 新增参数

| 参数 | 作用 |
|---|---|
| `PR_NUMBER` | PR 编号，非空即 PR 模式 |
| `PR_SHA` | PR head SHA，由 Poller 传入 |
| `CHECK_RUN_ID` | Gitee Check Run ID，由 Poller 传入，完成态直接 PATCH 它 |

手动触发 PR 构建时，`PR_SHA` 可以留空，Pipeline 会从当前 checkout 的 commit 自动获取。
`CHECK_RUN_ID` 也可以留空，此时完成态会走按 SHA 反查的兜底逻辑；自动触发时则必须传入。

> ⚠️ Jenkins 参数声明有一个历史坑：新增参数通常要到下一次读取新版 Jenkinsfile 后才注册。推送本次改动后，需要先让 Job 读取一次新版 `Jenkinsfile-learn`，再触发 PR 构建，避免 `CHECK_RUN_ID` 被当成未声明参数丢弃。

### 2. 模式判断

```groovy
env.IS_PR = (params.PR_NUMBER ?: '') ? 'true' : 'false'
env.PR_SHA = params.PR_SHA ?: ''
env.CHECK_RUN_ID = params.CHECK_RUN_ID ?: ''
```

这两行在 `Resolve Mode` stage 中执行，保证后续所有 stage 都能使用同一份模式判断结果。

### 3. Checkout 后校验 SHA

PR 模式检出 `refs/pull/N/head` 后，Pipeline 会校验：

```groovy
sh 'test "$PR_SHA" = "$(git rev-parse HEAD)"'
```

目的：

- 确保 Poller 传入的 SHA 和实际检出的 commit 一致
- 防止 PR 在构建期间更新后，状态写错 commit

### 4. 为什么用 Pipeline 级 post

构建失败时，后续 stage 会被跳过。

如果状态回写放在普通 stage 里，失败时可能没有机会执行。

因此回写放在：

```groovy
post {
  success { ... }
  failure { ... }
}
```

这保证：

- 成功构建一定会尝试写 success
- 失败构建一定会尝试写 failure

---

## 六、`pr-report.sh` 设计

`pr-report.sh` 只做一件事：把一个状态写到 Gitee。

### 1. 输入

| 变量 | 必填 | 说明 |
|---|---|---|
| `PR_SHA` | 是 | 写到哪个 commit |
| `CI_STATUS` | 是 | `pending/success/failure` |
| `CHECK_RUN_ID` | 自动触发时是 | 指定后直接 PATCH，不再按 SHA 反查 |
| `PR_ID` | 创建时必填 | Gitee PR 内部 ID，不是 PR number |
| `PR_NUMBER` | 否 | 日志展示 |
| `BUILD_URL` | 否 | Gitee 状态跳转链接 |
| `ENV_FILE` | 否 | 测试/本地运行时指定配置文件 |
| `CURL_BIN` | 否 | 测试时替换为 mock curl |
| `PR_REPORT_STRICT` | 否 | Poller 使用，失败即退出 1 |

### 2. 严格模式为什么存在

有两种失败语义：

| 场景 | 期望 |
|---|---|
| Jenkins 构建后回写 success/failure 失败 | 不改变原构建结果 |
| Poller 写 in_progress 失败 | 不应该触发 Jenkins |

所以：

- Jenkins 调用：默认非严格，回写失败不掩盖构建结果
- Poller 调用：`PR_REPORT_STRICT=1`，in_progress 写失败就不触发

### 3. 成功后的测试审核项回写

`pr-test-review.sh` 使用：

```text
POST /api/v5/repos/{owner}/{repo}/pulls/{number}/test?access_token={token}
```

这个接口没有 `state` 参数。它的语义是：**当前 token 用户完成自己的 PR 测试项，并把结果标记为通过**。

因此它的前提是：

- token 用户必须是这个 PR 的测试人，或者有权限操作测试项
- Jenkins 构建已经成功，才会进入 `post.success`
- 不使用 `force=true`，避免把「管理员强制通过」混进常规 CI 链路

如果这个 API 调用失败，脚本只打 warning，不改变 Jenkins 已经成功的事实。

---

## 七、失败语义

| 场景 | Jenkins 结果 | Gitee 状态 | 原因 |
|---|---|---|---|
| PR 测试失败 | FAILURE | failure | `post.failure` 回写 |
| PR 构建失败 | FAILURE | failure | `post.failure` 回写 |
| 普通构建失败 | FAILURE | 不写 | 普通模式没有 PR SHA |
| 回写 API 失败 | 保持原结果 | 停留上一状态 | 状态系统不能反过来改构建结论 |
| Poller 触发失败 | 不产生构建 | in_progress | 下一轮重试，且不记录评论为已处理 |
| 无有效评论 | 不产生构建 | 无状态 | 新 SHA 不是触发信号 |

---

## 八、安全设计

1. `.env.ci` 不进仓库
2. token 不写日志
3. Jenkins 容器内配置文件权限 `600`
4. 测试使用 mock curl，不访问真实 Gitee
5. 真实验收前，先检查日志中没有 token

---

## 九、当前实现状态

| 文件 | 状态 | 说明 |
|---|---|---|
| `scripts/pr-report.sh` | 已完成 | 支持显式 `CHECK_RUN_ID`、状态校验、配置注入、严格模式 |
| `scripts/pr-test-review.sh` | 已完成 | 成功后把 token 用户在 PR 审核项「测试」标记为通过 |
| `scripts/pr-poller-learn.sh` | 已完成 | 支持评论查询、权限校验、in_progress、ID 捕获、触发、评论去重 |
| `scripts/tests/test-pr-report.sh` | 已通过 | 验证 Check Runs payload、显式 ID PATCH、PR 内部 ID 和 token 不泄露 |
| `scripts/tests/test-pr-test-review.sh` | 已通过 | 验证 test API、不使用 force、失败只告警、token 不泄露 |
| `scripts/tests/test-pr-poller-learn.sh` | 已通过 | 验证评论唯一触发、权限、精确匹配、ID 传递、去重和失败语义 |
| `Jenkinsfile-learn` | 已接入 | `PR_SHA` + `CHECK_RUN_ID` + `post.success/failure` + 成功后测试项回写 |

### 真实验收记录（2026-08-23）

- Jenkins `#43`：普通模式构建 `SUCCESS`，日志中没有 `[pr-report]`
- Jenkins `#44`：PR head `36f37155...` 构建 `SUCCESS`
- Poller 创建 pending Check Run 成功，返回 `CHECK_RUN_ID=26887886`
- Jenkins 完成态直接 `PATCH /check-runs/26887886`，返回 `HTTP 200`
- Gitee 查询结果：`id=26887886`、`status=completed`、`conclusion=success`、`name=ci/jenkins`
- Jenkins `#45`：PR head `17e5bc7...` 构建 `SUCCESS`
- 最新 Check Run：`id=26887892`、`status=completed`、`conclusion=success`、`name=ci/jenkins`

- Jenkins `#47`：PR head `810ef0e...` 构建 `SUCCESS`
- Poller 新评论 `50943859` 触发 Check Run `26887948`
- Jenkins `post.success` 依次回写 Check Run success 与 PR 审核项「测试」通过
- Gitee 查询结果：测试人 `greada` 的 `accept=true`

尚未完成：

1. PR #2 失败回写验证

---

## 十、验收清单

- [x] 普通模式构建不出现 `[pr-report]`（Jenkins #43）
- [x] PR #2 构建成功后，Gitee 显示 `ci/jenkins` success（Jenkins #44 / Check Run 26887886）
- [x] PR #2 构建成功后，Gitee PR 审核项「测试」显示已完成（Jenkins #47 / tester `greada` accept=true）
- [ ] PR #2 构建失败后，Gitee 显示 `ci/jenkins` failure
- [ ] 同一条 `start build` 评论第二次轮询不会触发新构建
- [ ] 新 commit 出现但没有新评论时，不会触发新构建
- [ ] 新 commit 后提交者再评论 `start build`，会触发新构建并生成新状态
- [ ] 非提交者或非精确命令评论不会触发构建
- [ ] 日志不包含 Gitee token 或 Jenkins token

---

## 十一、面试讲解顺序

1. 先讲目标：让 PR 页面能看到 CI 结果
2. 再讲状态模型：in_progress → completed(success/failure)
3. 然后讲触发：只有提交者的精确 `start build` 评论才触发
4. 接着讲 Jenkins：PR 模式只验证不部署
5. 再区分两层状态：Check Run 表示 CI 结果，PR 审核项「测试」表示流程确认
6. 最后讲失败语义：post.failure 保证失败也能回写；测试项只在成功后通过

---

## 十二、参考文档

- 设计方案：`../superpowers/specs/2026-08-23-cicd-redesign-design.md`
- 实施计划：`../superpowers/plans/2026-08-23-cicd-l8-implementation.md`
- Gitee Check Runs API：`https://help.gitee.com/base/pullrequest/ci-check`
