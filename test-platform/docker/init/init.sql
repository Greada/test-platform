SET NAMES utf8mb4;

-- V1: 核心表
CREATE TABLE IF NOT EXISTS test_case
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

CREATE TABLE IF NOT EXISTS execution_record
(
    id              BIGINT(20)  NOT NULL AUTO_INCREMENT,
    test_case_id    BIGINT(20)  NOT NULL,
    status          VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    request_detail  TEXT,
    response_detail TEXT,
    actual_result   TEXT,
    execute_time    DATETIME             DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_test_case_id (test_case_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- V2.1: 套件 + 报告
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

-- V3.2: 用户 + 分类
CREATE TABLE IF NOT EXISTS `user` (
  `id`           BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `username`     VARCHAR(50)  NOT NULL,
  `password`     VARCHAR(255) NOT NULL,
  `display_name` VARCHAR(100) DEFAULT NULL,
  `role`         VARCHAR(20)  NOT NULL DEFAULT 'USER',
  `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS test_category (
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

-- 默认管理员（密码: admin123）
INSERT IGNORE INTO `user` (`username`, `password`, `display_name`, `role`)
VALUES ('admin', '$2a$10$ikuTUlx1bNz3j/fr6EB0m.YH9SDLj0k9RmNZPW36AsJ0gccTUbB0K', '管理员', 'ADMIN');

-- 12 条种子数据（TC-001 ~ TC-012）
INSERT INTO test_case (test_no, name, request_url, request_method, request_headers, request_params, expected_result)
VALUES
('TC-001', 'HTTP GET 请求', 'https://httpbin.org/get', 'GET', NULL, NULL,
 '{"url":"https://httpbin.org/get"}'),

('TC-002', 'POST 提交 JSON 数据', 'https://httpbin.org/post', 'POST',
 '{"Content-Type":"application/json"}', '{"message":"hello","count":42}',
 '{"json":{"message":"hello","count":42}}'),

('TC-003', 'GET 带查询参数', 'https://httpbin.org/get?keyword=test&page=1', 'GET', NULL, NULL,
 '{"args":{"keyword":"test","page":"1"}}'),

('TC-004', '自定义请求头回显', 'https://httpbin.org/headers', 'GET',
 '{"X-Test-Header":"test123","Authorization":"Bearer demo-token"}', NULL,
 '{"headers":{"X-Test-Header":"test123"}}'),

('TC-005', 'JSON 结构数据', 'https://httpbin.org/json', 'GET', NULL, NULL,
 '{"slideshow":{"author":"Yours Truly"}}'),

('TC-006', '查询全部用例（自测）', 'http://localhost:8080/api/testcases', 'GET', NULL, NULL,
 '{"code":200}'),

('TC-007', '3 秒延迟请求', 'https://httpbin.org/delay/3', 'GET', NULL, NULL,
 '{"url":"https://httpbin.org/delay/3"}'),

('TC-008', 'PUT 请求测试', 'https://httpbin.org/anything', 'PUT', NULL, '{"key":"value"}',
 '{"method":"PUT"}'),

('TC-009', 'DELETE 请求测试', 'https://httpbin.org/anything', 'DELETE', NULL, NULL,
 '{"method":"DELETE"}'),

('TC-010', '预期失败用例', 'https://httpbin.org/get', 'GET', NULL, NULL,
 '{"url":"https://httpbin.org/nonexist"}'),

('TC-011', '多层嵌套匹配', 'https://httpbin.org/anything', 'GET', NULL, NULL,
 '{"method":"GET","headers":{"Host":"httpbin.org"}}'),

('TC-012', '文本降级匹配', 'https://httpbin.org/ip', 'GET', NULL, NULL,
 'origin');
