-- =====================================================================
-- V4__foreign_keys.sql — 核心关系外键约束
-- 策略：ON DELETE RESTRICT，强制业务层先处理子表
-- 前置检查：learn-mysql 存量数据无孤儿（2026-09-01 查询结果均为 0）
-- =====================================================================

ALTER TABLE test_suite_case
    ADD CONSTRAINT fk_test_suite_case_suite_id
    FOREIGN KEY (suite_id) REFERENCES test_suite(id) ON DELETE RESTRICT;

ALTER TABLE test_suite_case
    ADD CONSTRAINT fk_test_suite_case_case_id
    FOREIGN KEY (case_id) REFERENCES test_case(id) ON DELETE RESTRICT;

ALTER TABLE execution_record
    ADD CONSTRAINT fk_execution_record_test_case_id
    FOREIGN KEY (test_case_id) REFERENCES test_case(id) ON DELETE RESTRICT;

ALTER TABLE execution_record
    ADD CONSTRAINT fk_execution_record_report_id
    FOREIGN KEY (report_id) REFERENCES execution_report(id) ON DELETE RESTRICT;
