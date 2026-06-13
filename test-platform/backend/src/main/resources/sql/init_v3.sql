USE
test_platform;

-- 创建分类表
CREATE TABLE IF NOT EXISTS test_category (
    id          BIGINT(20)   NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT(20)   DEFAULT 0        COMMENT '父级ID，0=顶级',
    name        VARCHAR(100) NOT NULL         COMMENT '分类名称',
    level       INT(11)      DEFAULT 1        COMMENT '层级(1-3)',
    sort_order  INT(11)      DEFAULT 0        COMMENT '排序',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_parent_name (parent_id, name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 为 test_case 添加 category_id 字段
ALTER TABLE test_case
    ADD COLUMN category_id BIGINT(20) DEFAULT NULL COMMENT '所属分类ID' AFTER name,
    ADD KEY idx_category_id (category_id);

-- 插入默认分类
INSERT INTO test_category (parent_id, name, level, sort_order)
VALUES (0, 'HTTP基础', 1, 1),
       (0, '认证模块', 1, 2),
       (0, '异常场景', 1, 3);

-- 为现有用例分配分类（根据你的种子数据调整）
UPDATE test_case
SET category_id = 1
WHERE test_no LIKE 'TC-00%';
UPDATE test_case
SET category_id = 2
WHERE test_no LIKE 'TC-01%';
UPDATE test_case
SET category_id = 3
WHERE test_no LIKE 'TC-02%';