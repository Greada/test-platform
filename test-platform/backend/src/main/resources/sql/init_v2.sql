USE test_platform;

CREATE TABLE IF NOT EXISTS test_suite
(
    id          BIGINT(20)   NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL COMMENT '套件名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS test_suite_case
(
    id         BIGINT(20) NOT NULL AUTO_INCREMENT,
    suite_id   BIGINT(20) NOT NULL COMMENT '套件ID',
    case_id    BIGINT(20) NOT NULL COMMENT '用例ID',
    sort_order INT(11) DEFAULT 0 COMMENT '排序序号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_suite_case (suite_id, case_id),
    KEY idx_suite_id (suite_id),
    KEY idx_case_id (case_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS execution_report
(
    id           BIGINT(20)    NOT NULL AUTO_INCREMENT,
    suite_id     BIGINT(20)             DEFAULT NULL COMMENT '关联套件ID（为空表示单用例报告）',
    report_name  VARCHAR(255)  NOT NULL COMMENT '报告名称',
    total        INT(11)       NOT NULL DEFAULT 0,
    passed       INT(11)       NOT NULL DEFAULT 0,
    failed       INT(11)       NOT NULL DEFAULT 0,
    errored      INT(11)       NOT NULL DEFAULT 0,
    pass_rate    DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT '通过率 %',
    status       VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED' COMMENT 'RUNNING / COMPLETED',
    execute_time DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    create_time  DATETIME               DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_report_suite (suite_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

ALTER TABLE execution_record
    ADD COLUMN report_id        BIGINT(20)   DEFAULT NULL COMMENT '关联报告ID' AFTER test_case_id,
    ADD COLUMN test_no          VARCHAR(20)  DEFAULT NULL COMMENT '快照-用例编号' AFTER report_id,
    ADD COLUMN case_name        VARCHAR(255) DEFAULT NULL COMMENT '快照-用例名称' AFTER test_no,
    ADD COLUMN execute_duration BIGINT(20)   DEFAULT NULL COMMENT '执行耗时(ms)' AFTER case_name,
    ADD KEY idx_report_id (report_id);