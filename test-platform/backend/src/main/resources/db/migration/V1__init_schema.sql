-- =====================================================================
-- V1__init_schema.sql — 全量 schema 快照（8 张表最终形态）
-- 合并自原 sql/init_v1~v4.sql（历史 ALTER 已折叠进 CREATE，列序与生产一致）
-- 语义：面向空库的全新初始化；存量库走 baseline-on-migrate（见 application.yml）
-- 幂等性：由 Flyway 账本保证只执行一次，脚本本身不重复执行
-- =====================================================================
SET NAMES utf8mb4;

-- ==== V1: 用例与执行记录 ====
CREATE TABLE test_case
(
    id              BIGINT(20)    NOT NULL AUTO_INCREMENT,
    test_no         VARCHAR(20)   NOT NULL COMMENT '测试编号',
    name            VARCHAR(255)  NOT NULL,
    request_url     VARCHAR(1024) NOT NULL,
    request_method  VARCHAR(10)   NOT NULL DEFAULT 'GET',
    request_headers TEXT,
    request_params  TEXT,
    expected_result TEXT    NOT NULL,
    category_id     BIGINT(20)             DEFAULT NULL COMMENT '所属分类ID',
    create_time     DATETIME               DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_test_no (test_no)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 列序 = 原 v1 建表 + v2 ALTER 补列的最终形态（report_id/test_no/case_name/execute_duration）
CREATE TABLE execution_record
(
    id               BIGINT(20)  NOT NULL AUTO_INCREMENT,
    test_case_id     BIGINT(20)  NOT NULL,
    report_id        BIGINT(20)  DEFAULT NULL COMMENT '关联报告ID',
    test_no          VARCHAR(20)  DEFAULT NULL COMMENT '快照-用例编号',
    case_name        VARCHAR(255) DEFAULT NULL COMMENT '快照-用例名称',
    execute_duration BIGINT(20)   DEFAULT NULL COMMENT '执行耗时(ms)',
    status           VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    request_detail   TEXT,
    response_detail  TEXT,
    actual_result    TEXT,
    execute_time     DATETIME              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_test_case_id (test_case_id),
    KEY idx_report_id (report_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ==== V2.1: 套件 + 报告 ====
CREATE TABLE test_suite
(
    id          BIGINT(20)   NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL COMMENT '套件名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE test_suite_case
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

CREATE TABLE execution_report
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

-- ==== V3: 分类 ====
CREATE TABLE test_category (
    id          BIGINT(20)   NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    parent_id   BIGINT(20)   DEFAULT NULL,
    level       INT(11)      DEFAULT 1 COMMENT '层级（1/2/3）',
    sort_order  INT(11)      DEFAULT 0,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==== V3.2: 用户 ====
CREATE TABLE `user` (
  `id`           BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `username`     VARCHAR(50)  NOT NULL,
  `password`     VARCHAR(255) NOT NULL,
  `display_name` VARCHAR(100) DEFAULT NULL,
  `role`         VARCHAR(20)  NOT NULL DEFAULT 'USER',
  `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==== V4: CI/CD 构建记录 ====
CREATE TABLE ci_build (
    id BIGINT(20) AUTO_INCREMENT PRIMARY KEY,
    build_number INT NOT NULL COMMENT 'Jenkins 构建编号',
    total_tests INT DEFAULT 0 COMMENT '总用例数',
    passed INT DEFAULT 0 COMMENT '通过数',
    failed INT DEFAULT 0 COMMENT '失败数',
    pass_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '通过率 %',
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILURE',
    build_url VARCHAR(500) DEFAULT '' COMMENT 'Jenkins 构建链接',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CI/CD 构建记录';
