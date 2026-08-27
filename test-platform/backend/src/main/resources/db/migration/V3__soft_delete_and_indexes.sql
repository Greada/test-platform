-- =====================================================================
-- V3__soft_delete_and_indexes.sql — 软删字段 + 时间戳补齐 + 索引
-- 对应优化计划 B2.5（部分）/B2.6/B2.9
-- 配套：application.yml logic-delete 配置 + 实体 @TableLogic（两者缺一不可）
--
-- 【明确排除项及原因】（勿在后续版本中"顺手补上"，需先做专项评估）：
-- 1. 外键约束（B2.7）：存量数据存在引用孤儿（execution_record 引用已硬删用例），
--    加 FK 会在迁移时校验失败；需先清孤儿 + 定删除策略（RESTRICT/级联）+ 业务配套
-- 2. 字符集统一 unicode_ci（B2.8）：存量表 general_ci 重建索引锁表成本高，收益趋近零
-- 3. ci_build created_at→create_time 改名（B2.5 部分）：破坏性改名，实体映射/前端字段
--    全要联动，成本 > 命名一致性收益
-- =====================================================================

-- 软删字段（@TableLogic 配套，代码侧下一半）
ALTER TABLE test_case
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删标记';
ALTER TABLE test_suite
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;
ALTER TABLE test_category
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `user`
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;

-- 时间戳补齐（不改名，只补缺）
ALTER TABLE `user`
    ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE execution_report
    ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 索引补齐（计划 B2.9 的三个缺口）
CREATE INDEX idx_category_id ON test_case (category_id);
CREATE INDEX idx_build_number ON ci_build (build_number);
CREATE INDEX idx_execute_time ON execution_record (execute_time);