USE test_platform;

-- 清空旧执行记录（避免外键冲突）
TRUNCATE execution_record;
TRUNCATE test_suite_case;
DELETE
FROM test_suite;
TRUNCATE test_case;


-- 插入全部 28 条测试用例
INSERT INTO test_case (test_no, name, request_url, request_method, request_headers, request_params, expected_result)
VALUES
-- ============ 原有 12 条 ============
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
 'origin'),

-- ============ 新增 16 条（覆盖 4xx/5xx/ERROR/超时/中文）============
('TC-013', 'GET 请求-404页面', 'https://httpbin.org/status/404', 'GET', NULL, NULL,
 '{"code":404}'),

('TC-014', 'GET 请求-401未授权', 'https://httpbin.org/status/401', 'GET', NULL, NULL,
 '{"code":401}'),

('TC-015', 'GET 请求-500服务端错误', 'https://httpbin.org/status/500', 'GET', NULL, NULL,
 '{"code":500}'),

('TC-016', '预期成功-实际404', 'https://httpbin.org/status/404', 'GET', NULL, NULL,
 '{"url":"https://httpbin.org/status/404"}'),

('TC-017', 'GET请求-预期不匹配', 'https://httpbin.org/get', 'GET', NULL, NULL,
 '{"headers":{"X-Nonexist":"never"}}'),

('TC-018', 'POST 提交表单', 'https://httpbin.org/post', 'POST',
 '{"Content-Type":"application/x-www-form-urlencoded"}', 'username=admin&password=123456',
 '{"form":{"username":"admin","password":"123456"}}'),

('TC-019', 'DELETE 请求-任意', 'https://httpbin.org/anything', 'DELETE', NULL, NULL,
 '{"method":"DELETE"}'),

('TC-020', 'PATCH 请求-任意', 'https://httpbin.org/anything', 'PATCH',
 '{"Content-Type":"application/json"}', '{"patchField":"newValue"}',
 '{"method":"PATCH"}'),

('TC-021', 'GET 请求-6秒延迟', 'https://httpbin.org/delay/6', 'GET', NULL, NULL,
 '{"url":"https://httpbin.org/delay/6"}'),

('TC-022', 'POST-中文请求体', 'https://httpbin.org/post', 'POST',
 '{"Content-Type":"application/json"}', '{"name":"测试平台","version":"V2.1"}',
 '{"json":{"name":"测试平台","version":"V2.1"}}'),

('TC-023', 'GET-中文参数', 'https://httpbin.org/anything?lang=中文&page=1', 'GET', NULL, NULL,
 '{"args":{"lang":"中文","page":"1"}}'),

('TC-024', 'GET-嵌套JSON匹配', 'https://httpbin.org/anything', 'GET', NULL, NULL,
 '{"method":"GET","headers":{"Host":"httpbin.org"},"args":{}}'),

('TC-025', '部分字段不匹配', 'https://httpbin.org/get', 'GET', NULL, NULL,
 '{"url":"https://httpbin.org/get","nonexist":"value"}'),

('TC-026', 'GET 请求-204无内容', 'https://httpbin.org/status/204', 'GET', NULL, NULL,
 '{"status":204}'),

('TC-027', 'GET-不可达地址（超时）', 'https://192.0.2.1/test', 'GET', NULL, NULL,
 '{"error":"connection timeout"}'),

('TC-028', 'GET-不存在的域名', 'https://this-domain-does-not-exist-12345.com', 'GET', NULL, NULL,
 '{"error":"dns resolution failed"}');


USE test_platform;

-- ============================================
-- 测试本平台自身 REST API
-- 目标URL均为 http://localhost:8080/api/...
-- 用于验证批执行修复后：4xx/5xx 响应体是否完整、ERROR路径字段是否齐全
-- ============================================

INSERT INTO test_case (test_no, name, request_url, request_method, request_headers, request_params, expected_result)
VALUES

-- ====== 用例 CRUD（查询类-幂等） ======

('TC-API-001', '【内部API】GET 查询用例列表',
 'http://localhost:8080/api/testcases', 'GET', NULL, NULL,
 '{"code":200}'),

('TC-API-002', '【内部API】GET 查询单个用例',
 'http://localhost:8080/api/testcases/1', 'GET', NULL, NULL,
 '{"code":200}'),

('TC-API-003', '【内部API】GET 查询不存在的用例（正常返回null）',
 'http://localhost:8080/api/testcases/99999', 'GET', NULL, NULL,
 '{"code":200}'),

-- ====== 用例 CRUD（写入类-每次执行产生副作用） ======

('TC-API-004', '【内部API】POST 新建用例',
 'http://localhost:8080/api/testcases', 'POST',
 '{"Content-Type":"application/json"}',
 '{"testNo":"TC-AUTO-1","name":"API自动创建","requestUrl":"http://localhost:8080/api/testcases","requestMethod":"GET","expectedResult":"{}"}',
 '{"code":200}'),

('TC-API-005', '【内部API】POST 新建用例-缺少必填字段（校验失败）',
 'http://localhost:8080/api/testcases', 'POST',
 '{"Content-Type":"application/json"}', '{}',
 '{"code":400}'),

('TC-API-006', '【内部API】PUT 更新不存在的用例',
 'http://localhost:8080/api/testcases/99999', 'PUT',
 '{"Content-Type":"application/json"}',
 '{"testNo":"TC-NONE","name":"不存在","requestUrl":"http://localhost:8080/api/testcases","requestMethod":"GET","expectedResult":"{}"}',
 '{"code":200}'),

('TC-API-007', '【内部API】DELETE 删除不存在的用例',
 'http://localhost:8080/api/testcases/99999', 'DELETE', NULL, NULL,
 '{"code":200}'),

-- ====== 套件 CRUD ======

('TC-API-008', '【内部API】GET 套件列表',
 'http://localhost:8080/api/test-suites', 'GET', NULL, NULL,
 '{"code":200}'),

('TC-API-009', '【内部API】GET 套件详情',
 'http://localhost:8080/api/test-suites/1', 'GET', NULL, NULL,
 '{"code":200}'),

('TC-API-010', '【内部API】POST 新建套件',
 'http://localhost:8080/api/test-suites', 'POST',
 '{"Content-Type":"application/json"}',
 '{"name":"API自动创建套件","description":"由测试用例自动创建-可手动删除"}',
 '{"code":200}'),

-- ====== 套件用例管理 ======

('TC-API-011', '【内部API】GET 套件用例列表',
 'http://localhost:8080/api/test-suites/1/cases', 'GET', NULL, NULL,
 '{"code":200}'),

-- ====== 执行记录 ======

('TC-API-012', '【内部API】GET 查询用例执行历史',
 'http://localhost:8080/api/execution-records?testCaseId=1', 'GET', NULL, NULL,
 '{"code":200}'),

('TC-API-013', '【内部API】GET 查询不存在的用例执行历史（空列表）',
 'http://localhost:8080/api/execution-records?testCaseId=99999', 'GET', NULL, NULL,
 '{"code":200}'),

('TC-API-014', '【内部API】POST 单次执行用例（用例1）',
 'http://localhost:8080/api/execution-records/1/execute', 'POST', NULL, NULL,
 '{"code":200}'),

('TC-API-015', '【内部API】POST 单次执行不存在的用例',
 'http://localhost:8080/api/execution-records/99999/execute', 'POST', NULL, NULL,
 '{"code":400}'),

-- ====== 执行报告 ======

('TC-API-016', '【内部API】GET 报告列表',
 'http://localhost:8080/api/execution-reports', 'GET', NULL, NULL,
 '{"code":200}'),

('TC-API-017', '【内部API】GET 报告详情',
 'http://localhost:8080/api/execution-reports/1', 'GET', NULL, NULL,
 '{"code":200}'),

('TC-API-018', '【内部API】GET 报告执行明细',
 'http://localhost:8080/api/execution-reports/1/details', 'GET', NULL, NULL,
 '{"code":200}'),

-- ====== HTTP 异常场景（修复后验证重点） ======

('TC-API-019', '【内部API】POST 到只读接口（405 Method Not Allowed）',
 'http://localhost:8080/api/testcases', 'POST',
 '{"Content-Type":"application/x-www-form-urlencoded"}',
 'id=1',
 '{"status":405}'),

('TC-API-020', '【内部API】GET 不存在的接口路径（404 Not Found）',
 'http://localhost:8080/api/nonexistent-path', 'GET', NULL, NULL,
 '{"status":404}'),

('TC-API-021', '【内部API】GET 非法ID参数（类型转换异常→500）',
 'http://localhost:8080/api/testcases/abc', 'GET', NULL, NULL,
 '{"code":500}');

SELECT id, test_no, name, request_method, request_url
FROM test_case
WHERE test_no LIKE 'TC-API%'
ORDER BY id;

-- 查看结果
SELECT id, test_no, name, request_method, request_url
FROM test_case
ORDER BY id;

DELETE
FROM test_case
WHERE test_no = 'TC-AUTO-1';

DELETE
FROM test_suite
WHERE name LIKE 'API自动创建%';