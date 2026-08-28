# 废弃文件登记表

> 生成于 2026-08-28（优化工程阶段二期间）。本表登记所有已废弃/被取代的文件。
> 废弃文件保留在仓库中（历史可考），但**不应再被引用或更新**；新内容请写到对应的替代文档。

| 废弃文件 | 原用途 | 替代者 | 废弃原因 |
|---|---|---|---|
| `优化建议.html` | V2.2 时期（2026-06）的代码优化建议清单 | `test-platform/docs/优化计划.md` | 优化工程已全面取代：计划从 60+ 项发展到 135 项核查 + 分阶段执行 + 实测验证体系 |
| `test-platform/README.md` | 内层项目 README（与仓库根重复） | 仓库根 `README.md` | 双份维护必然漂移：根版已同步至当前（Boot 3.3.13/Flyway），内层停在 2026-08-05 |
| `test-platform/frontend/module-preview.html` | 开发期 UI 原型静态预览页 | 无（真实页面即文档） | 零引用、不参与构建、内容与实际 UI 已脱节（2026-06-13 后未更新） |
| `test-platform/docs/resume.html` | V3.2 时期开发恢复指南（HTML） | `AGENTS.md`（根） | 续接职能已被 AGENTS.md「当前状态与续接指南」继承（含环境矩阵/开工入口） |
| `test-platform/backend/src/main/resources/sql/`（init_v1~v4 + insert_test_case_v1） | 手工 SQL 初始化脚本组 | `backend/src/main/resources/db/migration/`（Flyway） | B2.1 起 Flyway 为 schema 权威来源；脚本头部已各自标注"已降级"，仅维护存量库时使用 |
| `test-platform/docs/进度报告.md`、`开发进度.md`、`阶段总结报告.md` | V1~V4 原始功能开发期的进度/总结文档 | `test-platform/docs/优化计划.md`（进展更新表） | 史料保留：描述的版本号与初始化方式已过时，AGENTS.md 已声明其时效性；不做内容修改 |

## 标记约定

- 上表前 4 项（独立文件）：文件头部插入 `[DEPRECATED]` 声明块
- sql/ 脚本组：已有头部降级标注（2026-08-27），无需重复
- 历史文档（进度报告等）：不改内容，靠 AGENTS.md 的时效声明覆盖

维护约定：**新的废弃决策请追加到本表**，并同步在各文件头部加声明——"废弃"是一个需要留痕的决策，不是悄悄烂掉。
