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

## 课程进度（截至 2026-08-16）

### Phase A — 能跑起来(MVP)

- [x] Lesson 0 — Jenkinsfile 骨架
- [x] Lesson 1 — 跑单元测试（Test stage）
- [x] Lesson 1.5 — Dockerfile + Compose 动手实验
- [x] Lesson 2 — 构建镜像（Build stage）
- [x] Lesson 3 — 部署 + 验证（Deploy + Verify）
  - [x] 3a — Deploy stage（docker compose up -d）✅ 成功
  - [x] 3b — Verify stage（天真版，无 sleep）✅ 意外通过（3a 已部署，JVM 已就绪）
  - [x] 3c — 加 sleep 15（修正版）✅ 验证通过（#16，真冷启动，down 后重跑）

### Phase B — 工程化
- [x] Lesson 4 — 参数化 + 双环境
- [x] Lesson 5 — 失败处理 + 产物
- [ ] Lesson 6 — 规范化
  - [x] 6a — options{} 四件套 ✅ Build #5/#6 验证通过(2026-08-21:timestamps/timeout 实证 + 排队 25ms 衔接)
  - [ ] 6b — timeout/retry 演练(retry(2) + 临时 timeout=2min + sleep 600 造超时;#7 揭示两坑:sh 内 //注释污染、retry(1) 不重试)
  - [ ] 6c — 镜像锁版本(4 个浮动 tag)

### Phase C — 高级（未开始）
- [ ] Lesson 7 — PR 模式
- [ ] Lesson 8 — 状态回写
- [ ] Lesson 9 — 并行优化 + 回滚

## 下次从这里继续

- **Lesson 6b** — 验证 timeout/retry(options 加 `retry(2)` + 临时 timeout=2min + Verify `sleep 600` 造超时;验证完 sleep 600 改回 15、timeout 改回 30)

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
