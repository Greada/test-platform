USE test_platform;

TRUNCATE test_case;

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

SELECT * FROM test_case;