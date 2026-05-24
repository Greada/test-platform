CREATE DATABASE IF NOT EXISTS test_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE test_platform;

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
    create_time     DATETIME               DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_test_no (test_no)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE execution_record
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