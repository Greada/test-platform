# 会话规则与进度（下次会话直接生效）

## 角色设定

严谨的高级架构师兼 Code Agent。

## 硬性规则（必须严格遵守）

1. **零猜测**：用户明确确认前，禁止输出任何代码、目录结构、技术选型方案或实现草案
2. **单线程提问**：每次回复只提 1 个核心问题，附带简要选项（A/B/C），不一次性抛多个问题
3. **置信度仪表盘**：每次回答末尾必须更新全局置信度（如：当前置信度：90%），并列出剩余待确认事项概览（仅列标题）
4. **解锁条件**：置信度达 90%-95% 才允许输出第一份完整实施方案或代码
5. **先确认环境**：先确认 OS/版本/包管理器
6. **Interface 先行**：先定义入出参 Interface 再写逻辑
7. **异常分类处理**：异常必须分类，不许空 catch
8. **变更亮红灯**：变更必须亮红灯警告
9. **安全性能自检**：交付时附带安全与性能自检清单
10. **及时更新文档**：每次变更后同步更新相关文档

## 课程进度（截至 2026-08-21）

### Phase A — 能跑起来(MVP)

- [x] Lesson 0 — Jenkinsfile 骨架
- [x] Lesson 1 — 跑单元测试（Test stage）
- [x] Lesson 1.5 — Dockerfile + Compose 动手实验
- [x] Lesson 2 — 构建镜像（Build stage）
- [x] Lesson 3 — 部署 + 验证（Deploy + Verify）
  - [x] 3a — Deploy stage（docker compose up -d）✅ 成功
  - [x] 3b — Verify stage（天真版，无 sleep）✅ 意外通过（3a 已部署，JVM 已就绪）
  - [x] 3c — 加 sleep 15（修正版）✅ 验证通过（#16，真冷启动，down 后重跑）

### Phase B — 工程化 ✅ 全部完成
- [x] Lesson 4 — 参数化 + 双环境
- [x] Lesson 5 — 失败处理 + 产物
- [x] Lesson 6 — 规范化 ✅ 整课收官(2026-08-21)
  - [x] 6a — options{} 四件套 ✅ Build #5/#6 验证通过
  - [x] 6b — timeout/retry 演练 ✅ #7 两坑 → #8 超时定论 → #9 恢复绿灯
  - [x] 6c — 镜像锁版本 ✅ 4 tag 锁定(curl 8.21.0/temurin 17.0.19_10/node 20.19.4/nginx 1.31.4),#10 绿灯

### Phase C — 高级（进行中）
- [ ] Lesson 7 — PR 模式(IS_PR + refs/pull/N/head)— 7a ✅(#29/#30 双绿),7b 进行中
- [ ] Lesson 8 — 状态回写(Report PR Status + post.failure)
- [ ] Lesson 9 — 并行优化 + 回滚(parallel + 镜像 tag + 回滚 Job)

## 下次从这里继续

- **Lesson 7 — 7b 跑/审环节** — 7a 收官(#29/#30 双绿);7b 代码已提交推送(cf7218d+c3ea125),AI 只读审查过(坑②✅ 单引号✅ 无越界✅)
- 学习节奏(5 步):你讲✅→我写✅→你查✅→**跑/审(当前环节)**→复盘
- ⚠️ 侦查纠偏(2026-08-22):PR #1 树停在 2026-07(learn 文件全缺,Build 必挂),原"#32 填 1"方案作废 → 已备 smoke PR(分支 lesson7-smoke-pr,仅 +1 占位文档);**待用户在 Gitee 建 PR(base: main ← compare: lesson7-smoke-pr)**,建成后 ls-remote 取编号+SHA 回填验证点
- 7b 验证点:#31(留空)→ Checkout 走 else 分支 echo,全流程与 #29 无差异,双 200(**#31 不依赖 PR,随时可跑**);#32(PR_NUMBER=smoke PR 编号)→ ①fetch 日志 ②`HEAD is now at <SHA>`(与 ls-remote 对号=检出铁证) ③detached HEAD 警告(坑⑦预期) ④Test 91 用例绿 ⑤Build/Deploy/Verify 照跑(7c 才跳过)
- 验证双绿后:复盘回填 lesson-07(三、复盘 + 2.2「待实装」状态) + SESSION_CONTEXT → 进 7b'(GitSCM 对照版)
- 重启后快速续接:对 AI 说「读 SESSION_CONTEXT 继续 L7」即可

### L7 定案计划(2026-08-22,零猜测侦查完成)

| 决策点 | 定案 |
|---|---|
| 模式开关 | 新参数 `PR_NUMBER`(string,默认空),非空=PR 模式;Resolve Mode stage 固化 `env.IS_PR`(⚠️ env 无布尔,when 比较必须字符串 `'true'` 带引号) |
| Checkout | **7b 手写 sh**(fetch refs/pull/N/head + checkout FETCH_HEAD)→ **7b' 换 GitSCM** 对比两版 |
| Build(PR 模式) | 只构建 backend 镜像(与生产 Jenkinsfile 同款) |
| Deploy/Verify | PR 模式 when 跳过 |
| Notify | 组合 when:非 PR 且 prod |
| Resolve Env | PR 模式跳过 → ⚠️ Build 的 PR 分支不能依赖 `$DEPLOY_TARGET`(空串),compose 写死 learn 版 |

- 小步:7a 参数+Resolve Mode → 7b 手写 checkout → 7b' GitSCM → 7c when 守卫
- 侦查实证:`git ls-remote origin 'refs/pull/*'` → PR #1 head=`802f67d`(另有 `refs/pull/1/MERGE` 试合并 ref),匿名可访问,PR #1 可当测试 PR
- 高发坑:when 字符串比较带引号 / sh 内注释用 `#` / detached HEAD 可接受 / Notify 组合 when 默认 AND

## 关键文件清单

| 文件 | 说明 |
|---|---|
| `test-platform/Jenkinsfile-learn` | 学习版 pipeline（当前到 L5 完结） |
| `test-platform/docker-compose.learn.yml` | 学习版 compose（含 mysql/backend/frontend） |
| `test-platform/backend/Dockerfile-learn` | 多阶段 backend Dockerfile（L1.5 产出） |
| `test-platform/frontend/Dockerfile-learn` | 多阶段 frontend Dockerfile（L1.5 产出） |
| `test-platform/docs/notes/lesson-03-deploy-verify.md` | L3 笔记 |
| `test-platform/docs/notes/README.md` | 课程进度总览 |

## 环境信息

- OS: WSL2 Ubuntu (Running/2)
- Docker: 29.7.2
- Docker Compose: v5.4.0
- Jenkins: jenkins/jenkins:lts-jdk17（端口 8088）
- Jenkins Job: `test-platform-learn`（Script Path: `test-platform/Jenkinsfile-learn`）
- 镜像源: daocloud + 1ms.run + xuanyuan（已修复失效源）
- learn 环境端口: mysql=3309, backend=8090, frontend=82

## 变更日志

| 日期 | 变更 |
|------|------|
| 2026-08-15 | L1 复盘回填 + L1.5 完整 + L2 完整 + L3 3a/3b 完成 |
| 2026-08-16 | L3 3c 验证通过（#16，真冷启动）+ lesson-03 第五节日志回填 |
| 2026-08-16 | L4 参数化+双环境完成（4a/4b/4c 验证通过）+ lesson-04 笔记回填 |
| 2026-08-16 | L5 失败处理+产物完成（5a/5b/5c 验证通过）+ lesson-05 笔记回填 |
| 2026-08-21 | L6a 验证通过（Build #5/#6 双绿，排队 25ms 衔接实证）+ lesson-06 笔记回填；新证据通道：`docker exec jenkins` 直读 builds/build.xml + log（绕过 API 403） |
| 2026-08-21 | **学习节奏调整**（自 6b 起）：你讲(AI)→我写(用户)→你查(AI 只读检查)→跑/审→复盘，5 步 |
| 2026-08-21 | 6b #7 无效轮：sh 内 //注释污染 + retry(1) 不重试（两处已修正，retry 值应为 2）；Jenkinsfile 注释与笔记源头同步修正 |
| 2026-08-21 | **查明并卸载 Windows 侧 git-ai**（幽灵改写提交的根因）：杀 daemon + 删 opencode 插件 + 清 PATH/.bash_profile + 清仓库 refs/notes/ai 与 .git/ai；WSL 侧按用户要求保留（无进程运行）。注意：本次 opencode 会话关闭后需手动删一次 `~/.git-ai` 残留空日志目录 |
| 2026-08-21 | L6b 验证完成（#8：timeout 包 retry，单轮 137.2s ABORTED；超时=ABORTED≠FAILURE，post.failure 不触发）+ lesson-06 笔记回填；恢复代码（sleep 600→15、timeout→30）待用户下次执行 |
| 2026-08-21 | L6c 完成（4 tag 锁定：curl 8.21.0 / temurin 17.0.19_10 / node 20.19.4 / nginx 1.31.4，#10 绿灯 78.9s）→ **L6 整课收官，Phase B 全部完成** → 下次进 L7（Phase C） |
| 2026-08-22 | L7 开课：AI 讲概念（refs/pull/N/head + IS_PR + when 矩阵）；三决策定案（checkout 先手写后 GitSCM / PR 模式只建 backend / when 守卫矩阵）；侦查实证 Gitee 暴露 refs/pull/1/head=802f67d 匿名可访问。另：创建全局 opencode strict 模式 agent（~/.config/opencode/agents/strict.md，零猜测+置信度≥90%+edit/bash ask 硬约束），需重启生效 |
| 2026-08-22 | L7「你讲」环节产出：创建 lesson-07-pr-mode.md（概念：refs/pull 命名空间 + IS_PR 开关设计 + when 守卫矩阵 + 7 坑浓缩清单；7a 任务卡+伪代码骨架，7b/7b'/7c 占位）。下一步：用户写 7a → AI 只读审查 |
| 2026-08-22 | L7 7a 两轮只读审查（用户写）:R1 发现 params/env 两层混淆+env.IS_PR 未赋值+if/else 越界 7c;R2 赋值块修对但 params.IS_PR 幽灵引用复发 + `== ''` 漏 null(坑⑤联动);答疑 PR_NUMBER 数据流(poller→buildWithParameters→params)。用户请求代写 → AI 实装 7a(三元+truthiness 版)并逐行讲解,待 push + #11/#12 验证 |
| 2026-08-22 | 节奏裁定:用户选 A(维持 5 步节奏,7a 代写属特例);AI 代提交 7a 代码 + lesson-07 笔记 + SESSION_CONTEXT 并推送;待跑 #11/#12 验证 |
| 2026-08-22 | **7a 验证收官**:实际验证 build 为 #29(PR_NUMBER 留空,IS_PR=false,与 #10 无行为差异)/#30(填 PR_NUMBER=1,IS_PR=true,其余 stage 照旧全跑双 200),双绿;坑⑤被 #11~#28 中间构建自然吸收;复盘回填(验证证据 + 两轮审查病灶存档)→ 7b 手写 sh checkout 待开讲 |
| 2026-08-22 | 7b「你讲」环节完成并落盘 lesson-07 2.2 节(隐式 checkout 真相/fetch+FETCH_HEAD 拆解/坑⑦=验收证据/params 三层注入/新坑:sh 单引号留 $ 给 shell/任务卡+验证点);侦查补充:本地 fetch PR #1(802f67d)实证仅改 AGENTS.md 后端零改动;下一步:用户写 Checkout stage → AI 只读审查 |
| 2026-08-21 | L6b 彻底闭环（#9 绿灯 89.3s，双 HTTP 200，91 用例，检出 7c55751）→ 进 L6c 镜像锁版本 |
| 2026-08-22 | 7b「我写→你查」完成：用户实装 Checkout stage（含 else 分支 echo）并提交推送（cf7218d，随附 lesson-07 2.2 文档）；AI 只读审查过——代码本体过审，1 个一致性问题（头注释缺 7b 登记行）+ SESSION_CONTEXT 尾巴过时；按裁定 B：先补头注释登记 + SESSION_CONTEXT 状态同步成一个自洽提交，再跑 #31/#32 验证 |
| 2026-08-22 | **侦查纠偏**（用户指出 PR #1 太老会报错，实证属实）：PR #1 树停在 2026-07-05（merge-base），main 领先 63 提交，learn 文件全缺 → "#32 填 1"方案作废（Build 必挂）；lesson-07 三处 + SESSION_CONTEXT 错误侦查记载同步修正；按裁定 A：AI 备好 smoke PR 分支（lesson7-smoke-pr + 占位文档 pr-smoke.md，已推送），待用户 Gitee 建 PR |
