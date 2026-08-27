-- [已降级] 见 init_v1.sql 头部说明（存量路径；新库走 db/migration/）
-- V3: 分类管理 + V3.2: JWT 权限管理
-- 执行前请确保已执行 init_v1.sql 和 init_v2.sql
-- 本脚本组为"全新初始化"语义：ALTER 不可重复执行（MySQL 5.7 无 ADD COLUMN IF NOT EXISTS）
USE test_platform;

-- ==== V3: 分类管理 ====
-- 2026-08-26 补齐：此前分类 DDL 仅存在于 docker/init/init.sql，非 Docker 初始化会缺表缺列
-- （两套脚本并存的根治方案见优化计划 B2.x Flyway 迁移）
ALTER TABLE `test_case`
    ADD COLUMN `category_id` BIGINT(20) DEFAULT NULL COMMENT '所属分类ID' AFTER `expected_result`;

CREATE TABLE IF NOT EXISTS `test_category` (
    `id`          BIGINT(20)   NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(100) NOT NULL,
    `parent_id`   BIGINT(20)   DEFAULT NULL,
    `level`       INT(11)      DEFAULT 1 COMMENT '层级（1/2/3）',
    `sort_order`  INT(11)      DEFAULT 0,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==== V3.2: JWT 权限管理 ====
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

INSERT IGNORE INTO `user` (`username`, `password`, `display_name`, `role`)
VALUES ('admin', '$2a$10$ikuTUlx1bNz3j/fr6EB0m.YH9SDLj0k9RmNZPW36AsJ0gccTUbB0K', '管理员', 'ADMIN');
