# Jenkinsfile 从 0 重建 — 学习笔记

> 目标:从空文件开始,10 课重建一份生产级 Jenkinsfile,每行都能解释为什么。

## 课程进度

### Phase A — 能跑起来(MVP)
- [x] Lesson 0 — Jenkinsfile 骨架(最小 pipeline + Job 配置)
- [x] Lesson 1 — 跑单元测试(Test stage + docker run maven)
- [x] Lesson 1.5 — Dockerfile + Compose 动手实验(从零写 Dockerfile + compose build)
- [x] Lesson 2 — 构建镜像(Build stage + docker compose build)
- [x] Lesson 3 — 部署 + 验证(Deploy + Verify sleep 等待)

### Phase B — 工程化 ✅
- [x] Lesson 4 — 参数化 + 双环境(parameters + when)
- [x] Lesson 5 — 失败处理 + 产物(post + junit + CI 看板)
- [x] Lesson 6 — 规范化(options + 镜像锁版本)

### Phase C — 高级（进行中）
- [x] Lesson 7 — PR 模式(IS_PR + refs/pull/N/head) ✅ 整课收官(#37/#38/#39)
- [ ] Lesson 8 — 状态回写(Report PR Status + post.failure)— 讲义/本地实现完成,`start build` 评论唯一触发已定案,待真实环境验收
- [ ] Lesson 9 — 并行优化 + 回滚(parallel + 镜像 tag + 回滚 Job)

## 补充知识

- [Docker Compose 容器编排教程](docker-compose-orchestration.md) — 为 Lesson 2/3 打基础

## 文件约定
- `test-platform/Jenkinsfile-learn` — 本课程逐课构建的新 pipeline
- `test-platform/Jenkinsfile` — ⚠️ 旧生产版,2026-08-22 裁定**停用参照**(其 PR 检出缺 refspec,坑⑩同在且未被验证);终局:L9 后 learn 版升级替换并删除
- `test-platform/docs/notes/lesson-XX-*.md` — 每课笔记

## 学习节奏(每课固定 5 步,2026-08-21 起)
1. **你讲**(AI):问题 + 概念 + 伪代码骨架
2. **我写**(用户):自己在 `Jenkinsfile-learn` 里实现,AI 不代写
3. **你查**(AI):只读检查用户代码,指出问题不代改
4. **跑/审**:Jenkins 跑看绿灯,或读文件审
5. **复盘**:改进点 → 进下一课

## 笔记模板(每课记三块)
- **概念** — 这课讲了什么
- **代码** — 最终 Jenkinsfile 片段(带注释)
- **复盘** — 踩的坑 + 下次注意
