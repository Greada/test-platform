# Jenkinsfile 从 0 重建 — 学习笔记

> 目标:从空文件开始,10 课重建一份生产级 Jenkinsfile,每行都能解释为什么。

## 课程进度

### Phase A — 能跑起来(MVP)
- [x] Lesson 0 — Jenkinsfile 骨架(最小 pipeline + Job 配置)
- [x] Lesson 1 — 跑单元测试(Test stage + docker run maven)
- [x] Lesson 1.5 — Dockerfile + Compose 动手实验(从零写 Dockerfile + compose build)
- [x] Lesson 2 — 构建镜像(Build stage + docker compose build)
- [x] Lesson 3 — 部署 + 验证(Deploy + Verify sleep 等待)

### Phase B — 工程化
- [x] Lesson 4 — 参数化 + 双环境(parameters + when)
- [x] Lesson 5 — 失败处理 + 产物(post + junit + CI 看板)
- [ ] Lesson 6 — 规范化(options + 镜像锁版本)

### Phase C — 高级
- [ ] Lesson 7 — PR 模式(IS_PR + refs/pull/N/head)
- [ ] Lesson 8 — 状态回写(Report PR Status + post.failure)
- [ ] Lesson 9 — 并行优化 + 回滚(parallel + 镜像 tag + 回滚 Job)

## 补充知识

- [Docker Compose 容器编排教程](docker-compose-orchestration.md) — 为 Lesson 2/3 打基础

## 文件约定
- `test-platform/Jenkinsfile-learn` — 本课程逐课构建的新 pipeline
- `test-platform/Jenkinsfile` — 旧版,保留作参考,不动
- `test-platform/docs/notes/lesson-XX-*.md` — 每课笔记

## 学习节奏(每课固定 4 步)
1. **我讲**:问题 + 概念 + 伪代码骨架
2. **我写**:在 `Jenkinsfile-learn` 里实现
3. **跑/审**:Jenkins 跑看绿灯,或读文件审
4. **复盘**:改进点 → 进下一课

## 笔记模板(每课记三块)
- **概念** — 这课讲了什么
- **代码** — 最终 Jenkinsfile 片段(带注释)
- **复盘** — 踩的坑 + 下次注意
