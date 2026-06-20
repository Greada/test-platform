-- V3.2: JWT 权限管理
-- 执行前请确保已执行 init_v1.sql 和 init_v2.sql

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
