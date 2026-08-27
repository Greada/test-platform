-- =====================================================================
-- V2__seed_data.sql — 系统运转必需的种子数据
-- 内容：默认管理员（admin/admin123）+ 12 条自测用例（TC-001 ~ TC-012）
-- 幂等性：INSERT IGNORE 依赖唯一键（uk_username / uk_test_no），重放不炸不重
-- 注意：本文件是版本化迁移的一部分，一经应用不可修改（checksum 校验）
-- =====================================================================

-- 默认管理员（密码: admin123）
INSERT IGNORE INTO `user` (`username`, `password`, `display_name`, `role`)
VALUES ('admin', '$2a$10$ikuTUlx1bNz3j/fr6EB0m.YH9SDLj0k9RmNZPW36AsJ0gccTUbB0K', '管理员', 'ADMIN');

-- 12 条种子数据（TC-001 ~ TC-012）
INSERT IGNORE INTO test_case (test_no, name, request_url, request_method, request_headers, request_params, expected_result)
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
