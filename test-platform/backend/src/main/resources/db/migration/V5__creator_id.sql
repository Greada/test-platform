-- =====================================================================
-- V5__creator_id.sql — 数据隔离：业务表添加 creator_id
-- 存量数据默认 creator_id=1（admin）
-- =====================================================================

ALTER TABLE test_case
    ADD COLUMN creator_id BIGINT NOT NULL DEFAULT 1 COMMENT '创建人ID';
ALTER TABLE test_suite
    ADD COLUMN creator_id BIGINT NOT NULL DEFAULT 1 COMMENT '创建人ID';
ALTER TABLE execution_report
    ADD COLUMN creator_id BIGINT NOT NULL DEFAULT 1 COMMENT '创建人ID';
ALTER TABLE test_category
    ADD COLUMN creator_id BIGINT NOT NULL DEFAULT 1 COMMENT '创建人ID';

ALTER TABLE test_case
    ADD KEY idx_creator_id (creator_id);
ALTER TABLE test_suite
    ADD KEY idx_creator_id (creator_id);
ALTER TABLE execution_report
    ADD KEY idx_creator_id (creator_id);
ALTER TABLE test_category
    ADD KEY idx_creator_id (creator_id);