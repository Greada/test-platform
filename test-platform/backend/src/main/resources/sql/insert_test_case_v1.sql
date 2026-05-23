USE
    test_platform;
-- 先清空之前可能插入的数据
TRUNCATE test_case;
-- 插入测试数据
INSERT INTO test_case (name, request_url, request_method, request_headers, request_params, expected_result)
VALUES
-- 外部可访问的 API（用来测「执行」按钮）
('httpbin GET', 'https://httpbin.org/get', 'GET', NULL, NULL, NULL),
('httpbin POST', 'https://httpbin.org/post', 'POST', '{"Content-Type":"application/json"}', '{"key":"value"}', NULL),
('httpbin 延迟请求', 'https://httpbin.org/delay/2', 'GET', NULL, NULL, NULL),
-- 本项目的 API（测完 Controller 后可用）
('查询所有用例', 'http://localhost:8080/api/testcases', 'GET', NULL, NULL, NULL),
('查询用例详情 (id=1)', 'http://localhost:8080/api/testcases/1', 'GET', NULL, NULL, '{"code":200}'),
('创建新用例', 'http://localhost:8080/api/testcases', 'POST', '{"Content-Type":"application/json"}',
 '{"name":"测","requestUrl":"http://example.com","requestMethod":"GET"}', NULL);

-- 查询数据
SELECT *
FROM test_case;