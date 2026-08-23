# CI/CD 重新设计方案

**状态：** 实施中 — 本地实现完成，待真实环境验收  
**日期：** 2026-08-23  
**范围：** 面向学习/演示的 `test-platform` CI/CD  
**核心决策：** 抛弃旧生产版 CI/CD 假设，基于学习版 Pipeline 重新设计。

## 1. 目标

- 在一台 WSL2 机器上提供完整、可复现的 CI/CD 演示。
- 保留 Jenkins Declarative Pipeline 的学习价值。
- 演示完整 PR 闭环：Gitee PR → 提交者评论触发 → Jenkins 构建 → Gitee Check Run 回写。
- 使用一个 Jenkins Job，清晰区分普通构建和 PR 构建。
- 凭据不进入仓库，也不出现在构建日志中。

## 2. 非目标

- 企业级多租户 CI/CD。
- Kubernetes 部署。
- 云端生产基础设施。
- 继续使用或修复旧 `test-platform/Jenkinsfile`。
- Jenkins 高可用或高频 PR 轮询。

## 3. 已确认决策

| 决策点 | 选择 |
|---|---|
| 首要目标 | 学习与面试演示 |
| 运行环境 | 本机 WSL2 单机演示 |
| 代码平台 | Gitee |
| CI 引擎 | Jenkins Declarative Pipeline |
| 部署运行时 | Docker Compose 学习环境 |
| Job 拓扑 | 一个 Jenkins Job，普通/PR 双模式 |
| PR 触发方式 | 本地轻量 Poller + 唯一命令评论 `start build` |
| 状态回写 | Gitee Check Runs API |
| 旧生产版 Pipeline | 不作为参考 |
| 迁移路线 | 先完成学习版 L8/L9，验收通过后整体替换生产版 |

## 4. 总体架构

```text
Gitee Open PR
    ↓
pr-poller-learn.sh（每 60 秒执行一次）
    ↓
Jenkins Job：test-platform-learn
    ↓
普通模式或 PR 模式
    ↓
Gitee Check Run：in_progress → completed(success/failure)
```

### 4.1 组件职责

| 组件 | 职责 |
|---|---|
| `Jenkinsfile-learn` | 唯一的 Declarative Pipeline，承载普通构建和 PR 构建 |
| `pr-poller-learn.sh` | 校验 PR 评论，并触发 Jenkins |
| `pr-report.sh` | 向 Gitee 创建/更新 Check Run |
| `.env.ci` | 本机运行时凭据与端点配置，不提交仓库 |
| `docker-compose.learn.yml` | 构建/运行学习环境的 backend、frontend、MySQL |

## 5. Jenkins Job 设计

### 5.1 模式判断

- `PR_NUMBER` 为空：普通模式。
- `PR_NUMBER` 非空：PR 模式。
- 模式只解析一次，并固化到 `env.IS_PR`，值为字符串 `true` 或 `false`。
- 模式解析 stage 必须永远执行。
- 环境解析 stage 只在普通模式执行。

### 5.2 普通模式

| Stage | 行为 |
|---|---|
| Checkout | 默认 SCM 检出 `main` |
| Test | 执行后端单元测试并收集 JUnit 报告 |
| Build | 构建 backend 和 frontend 镜像 |
| Deploy | 启动学习版 Docker Compose 服务 |
| Verify | 校验 backend/frontend HTTP 状态 |
| Notify | 仅 `prod` 部署时执行 |

### 5.3 PR 模式

| Stage | 行为 |
|---|---|
| Checkout | 拉取并检出 `refs/pull/N/head` |
| Test | 执行后端单元测试并收集 JUnit 报告 |
| Build | 只构建 backend 镜像 |
| Deploy | 通过 `when` 主动跳过 |
| Verify | 通过 `when` 主动跳过 |
| Notify | 通过 `when` 主动跳过 |
| Report status | 由 Pipeline `post` 块统一执行 |

## 6. Poller 设计

Poller 是本地辅助脚本，不是分布式调度系统。

### 6.1 职责

1. 查询 Gitee 所有 Open PR。
2. 提取 PR 内部 ID、PR 编号、当前 head SHA、PR 创建者和 head 分支提交者。
3. 查询每个 PR 的评论。
4. 只接受 `body.trim() == "start build"` 的精确命令评论。
5. 只允许 PR 创建者或 head 分支提交者触发。
6. 对未处理的有效评论写入 `in_progress` Check Run。
7. 带 PR 参数触发 Jenkins Job。
8. Jenkins 返回 `201` 后，把该评论记录到本地状态文件。

### 6.2 状态记录

- 状态键：`manual:PR_NUMBER:COMMENT_ID`。
- 存储：仓库外的本地文本文件。
- 去重规则：同一条评论不会再次触发。
- 新 commit 行为：不自动构建；提交者必须针对新代码重新评论 `start build`。
- 状态键不包含 SHA：防止 PR 更新后，旧评论被重复用于新 SHA。
- 触发尝试记录：`attempt:PR_NUMBER:COMMENT_ID:HEAD_SHA`，用于失败重试和阻止旧评论打到新 SHA。

### 6.3 为什么选择评论唯一触发

- 新 SHA 只说明代码变了，不说明提交者已经准备好消耗 CI 资源。
- 精确匹配避免普通聊天误触发构建。
- 校验评论人避免任意访客触发构建。
- 评论 ID 去重保证一条确认只触发一次。
- 新 commit 后重新评论，保证旧确认不能代表新代码。

### 6.4 错误处理

| 失败场景 | 行为 |
|---|---|
| Gitee API 不可用 | 记录日志，下一轮重试 |
| 无法取得 head SHA | 跳过该 PR，下一轮重试 |
| 写 in_progress 失败 | 不触发 Jenkins，下一轮重试 |
| 触发 Jenkins 失败 | 保持 in_progress 状态，下一轮重试 |
| 重复评论 | 静默跳过 |
| 非提交者评论 | 不触发，不记录已处理 |
| 非精确命令 | 不触发，不记录已处理 |
| 新 SHA 但无新评论 | 不触发 |

## 7. Gitee Check Run 设计

Gitee 不支持 GitHub 风格的 `POST /repos/{owner}/{repo}/statuses/{sha}`，真实环境返回 `405 Not Allowed`。因此状态回写使用官方 Check Runs API：

- 创建：`POST /repos/{owner}/{repo}/check-runs`
- 查询：`GET /repos/{owner}/{repo}/commits/{ref}/check-runs`
- 更新：`PATCH /repos/{owner}/{repo}/check-runs/{check_run_id}`

### 7.1 状态机

```text
in_progress → completed(success)
in_progress → completed(failure)
```

| 状态 | 写入者 | 含义 |
|---|---|---|
| `in_progress` | Poller | 构建已触发 |
| `completed + success` | Jenkins `post.success` | PR 构建成功 |
| `completed + failure` | Jenkins `post.failure` | PR 构建失败 |

### 7.2 Check Run 字段

| 字段 | 值 |
|---|---|
| `name` | `ci/jenkins` |
| `head_sha` | PR head commit |
| `pull_request_id` | Gitee PR 内部 ID |
| `status` | `in_progress` 或 `completed` |
| `conclusion` | `success` 或 `failure` |
| `details_url` | Jenkins 构建详情链接 |
| `output` | 人类可读的构建结果描述 |

Check Run 绑定 PR head SHA，而不是 PR 编号。这样每个 commit 都能拿到独立状态。

## 8. 凭据设计

### 8.1 必需本地配置

```text
GITEE_OWNER
GITEE_REPO
GITEE_TOKEN
JENKINS_URL
JENKINS_USER
JENKINS_TOKEN
```

### 8.2 规则

- 配置保存在本地 `.env.ci` 文件。
- `.env.ci` 永远不提交仓库。
- 日志永远不输出 token。
- 优先使用只具备最小必要权限的 Gitee/Jenkins 凭据。
- Poller 和状态回写脚本在必需变量缺失时必须显式失败。

## 9. 失败语义

| 场景 | Pipeline 结果 | Gitee 状态 | 说明 |
|---|---|---|---|
| PR 测试失败 | FAILURE | failure | `post.failure` 必须执行 |
| PR 构建失败 | FAILURE | failure | `post.failure` 必须执行 |
| 普通构建失败 | FAILURE | 不写 PR 状态 | 普通模式不回写 Check Run |
| 状态 API 失败 | 保持原构建结果 | 停留在上一状态 | 记录 warning，不掩盖构建结果 |
| Poller 失败 | 不产生 Jenkins 构建 | in_progress 或无状态 | 下一轮重试，失败评论不标记已处理 |
| 新 SHA 无新评论 | 不产生 Jenkins 构建 | 无新状态 | 新 SHA 不是触发信号 |

## 10. 验证计划

| 验证项 | 模式 | 预期结果 |
|---|---|---|
| 回归构建 | 普通模式 | 现有完整流水线保持绿灯 |
| PR 成功 | PR #2 | Gitee 显示 `ci/jenkins` success |
| PR 失败演练 | PR #2 | Gitee 显示 `ci/jenkins` failure |
| 重复轮询 | 同一条有效评论 | 不触发第二次 Jenkins 构建 |
| 新 SHA 无新评论 | 新 commit | 不触发 Jenkins 构建 |
| 新 SHA + 新 `start build` | 新 commit | 触发一次新构建并更新状态 |
| 非提交者/非精确命令 | PR 评论 | 不触发 Jenkins 构建 |

## 11. 落地顺序

1. 在版本控制外创建本地 `.env.ci`。
2. 定义状态回写接口和必需变量。
3. 在学习版 Pipeline 中捕获 PR head SHA。
4. 通过 Pipeline `post` 块实现 success/failure 回写。
5. 实现轻量 PR 评论 Poller。
6. 运行普通模式回归。
7. 运行 PR 成功验证。
8. 运行 PR 失败验证。
9. 归档证据并更新课程笔记。

## 12. 迁移策略

用户已确认采用「学习版先行，最终整体替换」路线：

1. 继续以 `Jenkinsfile-learn` 作为唯一演进主线。
2. 在学习版中完成 L8 状态回写闭环。
3. 在学习版中完成 L9 并行优化与回滚设计。
4. 通过普通构建、PR 成功、PR 失败、重复评论、新 commit 不自动触发、新评论触发六类验证。
5. 全部验收通过后，将 `Jenkinsfile-learn` 升级为新的正式 `Jenkinsfile`。
6. 删除旧生产版 Pipeline 及过时文档副本，避免两套体系并存。

切换前不直接修改旧生产版；切换后学习版成为唯一事实来源。

## 13. 待审阅问题

无。方案已准备好进入最终实施确认。
