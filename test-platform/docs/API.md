# API 接口文档

Base URL: `http://localhost:8080/api`

---

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | 200 成功，400 业务错误，500 服务端异常 |
| message | string | 提示信息 |
| data | T | 响应数据（泛型） |

---

## 测试用例接口

### 查询全部用例

```
GET /testcases
```

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "testNo": "TC-001",
      "name": "HTTP GET 请求",
      "requestUrl": "https://httpbin.org/get",
      "requestMethod": "GET",
      "requestHeaders": null,
      "requestParams": null,
      "expectedResult": "{\"url\":\"https://httpbin.org/get\"}",
      "createTime": "2026-05-24T12:07:15",
      "updateTime": "2026-05-24T12:07:15"
    }
  ]
}
```

---

### 查询单个用例

```
GET /testcases/{id}
```

**参数**

| 参数 | 位置 | 类型 | 说明 |
|---|---|---|---|
| id | path | Long | 用例 ID |

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "testNo": "TC-001",
    "name": "HTTP GET 请求",
    "requestUrl": "https://httpbin.org/get",
    "requestMethod": "GET",
    "expectedResult": "{\"url\":\"https://httpbin.org/get\"}"
  }
}
```

---

### 新建用例

```
POST /testcases
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| testNo | String | 是 | 测试编号（唯一） |
| name | String | 是 | 用例名称 |
| requestUrl | String | 是 | 请求地址 |
| requestMethod | String | 是 | GET/POST/PUT/DELETE |
| requestHeaders | String | 否 | JSON 格式请求头 |
| requestParams | String | 否 | JSON 格式请求参数 |
| expectedResult | String | 是 | 预期结果（JSON 或文本） |

**请求示例**

```json
{
  "testNo": "TC-013",
  "name": "新建测试用例",
  "requestUrl": "https://httpbin.org/get",
  "requestMethod": "GET",
  "expectedResult": "{\"url\":\"https://httpbin.org/get\"}"
}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**校验失败响应**（expectedResult 为空）

```json
{
  "code": 400,
  "message": "预期结果不能为空",
  "data": null
}
```

---

### 修改用例

```
PUT /testcases/{id}
```

**参数**

| 参数 | 位置 | 类型 | 说明 |
|---|---|---|---|
| id | path | Long | 用例 ID |

**请求体**（同新建接口，需传完整字段）

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

### 删除用例

```
DELETE /testcases/{id}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 执行记录接口

### 执行用例

```
POST /execution-records/{testCaseId}/execute
```

**参数**

| 参数 | 位置 | 类型 | 说明 |
|---|---|---|---|
| testCaseId | path | Long | 要执行的用例 ID |

**执行流程**

```
1. 根据 testCaseId 查询用例
2. HttpExecutor 发起真实 HTTP 请求（携带 headers 和 body）
3. JSON 字段匹配：
   - expectedResult 为 JSON → 递归子集匹配（忽略多余字段）
   - expectedResult 非 JSON → contains 文本降级匹配
4. 记录 execution_record（status = PASS/FAIL/ERROR）
```

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**异常响应**

```json
{
  "code": 500,
  "message": "I/O error on GET request for ...",
  "data": null
}
```

---

### 查询执行记录

```
GET /execution-records?testCaseId={id}
```

**参数**

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| testCaseId | query | Long | 是 | 用例 ID |

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "testCaseId": 1,
      "status": "PASS",
      "requestDetail": "[requestMethod] GET [requestUrl] https://httpbin.org/get \n [requestHeaders] null \n [requestParams] null \n",
      "responseDetail": "[responseBody] \n { \"url\": \"https://httpbin.org/get\", ... }",
      "actualResult": "{ \"url\": \"https://httpbin.org/get\", ... }",
      "executeTime": "2026-05-24T12:13:58"
    }
  ]
}
```

| status | 说明 |
|---|---|
| PASS | 预期结果匹配成功 |
| FAIL | 预期结果不匹配 |
| ERROR | 执行过程发生异常（网络错误、超时等） |

---

## 执行匹配策略

```
expected_result
  ├─ 为合法 JSON → JSON 字段子集匹配
  │    ├─ 基本字段: {"code": 200}
  │    ├─ 嵌套字段: {"headers": {"X-Test-Header": "test123"}}
  │    └─ 多层递归: {"method": "GET", "headers": {"Host": "httpbin.org"}}
  │
  └─ 非 JSON → contains 文本降级
       └─ expected_result 为 "origin"
          actualResult 包含 "origin" → PASS
```

---

## 测试套件接口 (V2.1)

### 查询全部套件

```
GET /test-suites
```

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "冒烟测试",
      "description": "核心功能冒烟",
      "createTime": "2026-05-30T12:00:00",
      "updateTime": "2026-05-30T12:00:00"
    }
  ]
}
```

---

### 查询单个套件

```
GET /test-suites/{id}
```

| 参数 | 位置 | 类型 | 说明 |
|---|---|---|---|
| id | path | Long | 套件 ID |

---

### 新建套件

```
POST /test-suites
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | String | 是 | 套件名称 |
| description | String | 否 | 描述 |

---

### 修改套件

```
PUT /test-suites/{id}
```

| 参数 | 位置 | 类型 | 说明 |
|---|---|---|---|
| id | path | Long | 套件 ID |

**请求体**（同新建）

---

### 删除套件

```
DELETE /test-suites/{id}
```

---

### 查询套件内用例

```
GET /test-suites/{id}/cases
```

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "testCaseId": 1,
      "testNo": "TC-001",
      "caseName": "HTTP GET 请求",
      "requestMethod": "GET",
      "requestUrl": "https://httpbin.org/get",
      "sortOrder": 1
    }
  ]
}
```

---

### 添加单个用例到套件

```
POST /test-suites/{id}/cases
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| caseId | Long | 是 | 用例 ID |

---

### 批量添加用例到套件

```
POST /test-suites/{id}/cases/batch
```

**请求体**

```json
{
  "caseIds": [1, 2, 3]
}
```

---

### 从套件移除用例

```
DELETE /test-suites/{id}/cases/{caseId}
```

---

### 批量执行套件

```
POST /test-suites/{id}/execute
```

**执行流程**

```
1. 查询套件内所有用例
2. 创建 execution_report(status=RUNNING)
3. 逐个执行用例（同 V1 执行逻辑）
4. 每条执行记录关联 report_id，快照 test_no/case_name
5. 更新 execution_report(status=COMPLETED, total/passed/failed/errored/pass_rate)
```

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "reportName": "冒烟测试-2026-05-30 12:00:00",
    "total": 3,
    "passed": 2,
    "failed": 1,
    "errored": 0,
    "passRate": 66.67,
    "status": "COMPLETED",
    "executeTime": "2026-05-30T12:00:00"
  }
}
```

---

## 执行报告接口 (V2.1)

### 查询报告列表

```
GET /execution-reports
```

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "suiteId": 1,
      "suiteName": "冒烟测试",
      "reportName": "冒烟测试-2026-05-30 12:00:00",
      "total": 3,
      "passed": 2,
      "failed": 1,
      "errored": 0,
      "passRate": 66.67,
      "status": "COMPLETED",
      "executeTime": "2026-05-30T12:00:00",
      "createTime": "2026-05-30T12:00:00"
    }
  ]
}
```

---

### 查询报告详情

```
GET /execution-reports/{id}
```

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "suiteId": 1,
    "suiteName": "冒烟测试",
    "reportName": "冒烟测试-2026-05-30 12:00:00",
    "total": 3,
    "passed": 2,
    "failed": 1,
    "errored": 0,
    "passRate": 66.67,
    "status": "COMPLETED",
    "executeTime": "2026-05-30T12:00:00",
    "createTime": "2026-05-30T12:00:00"
  }
}
```

---

### 查询报告明细（执行记录列表）

```
GET /execution-reports/{id}/details
```

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "testCaseId": 1,
      "reportId": 1,
      "testNo": "TC-001",
      "caseName": "HTTP GET 请求",
      "status": "PASS",
      "executeDuration": 150,
      "actualResult": "{ \"url\": \"https://httpbin.org/get\" }",
      "requestDetail": "[requestMethod] GET ...",
      "responseDetail": "[responseBody] ...",
      "executeTime": "2026-05-30T12:00:00"
    }
  ]
}
```


---

## JSON Diff 接口 (V2.2)

### 获取执行记录差异分析

```
GET /execution-records/{id}/diff
```

**参数**

| 参数 | 位置 | 类型 | 说明 |
|---|---|---|---|
| id | path | Long | 执行记录 ID |

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "match": false,
    "differences": [
      {
        "fieldPath": "headers.Host",
        "expectedValue": "httpbin.org",
        "actualValue": "example.com",
        "type": "MISMATCH"
      }
    ],
    "suggestedExpected": "{\"url\":\"https://httpbin.org/get\"}",
    "expectedResult": "{\"url\":\"https://httpbin.org/get\"}",
    "actualResult": "{\"url\":\"https://httpbin.org/get\",\"headers\":{...}}"
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| match | boolean | 是否完全匹配 |
| differences | array | 差异明细 |
| suggestedExpected | string | 修复建议（格式化后的 actualResult） |
| expectedResult | string | 原始预期结果 |
| actualResult | string | 原始实际结果 |

| type | 说明 |
|---|---|
| MISMATCH | 值不匹配 |
| MISSING | 预期字段在实际结果中缺失 |
| EXTRA | 实际结果多余的字段 |

---

## 错误模式聚合接口 (V2.2)

### 获取报告的错误模式分析

```
GET /execution-reports/{id}/error-patterns
```

**参数**

| 参数 | 位置 | 类型 | 说明 |
|---|---|---|---|
| id | path | Long | 报告 ID |

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "requestUrl": "https://httpbin.org/post",
        "requestMethod": "POST",
        "total": 5,
        "pass": 3,
        "fail": 2,
        "error": 0,
        "passRate": "60.00%"
      }
    ],
    "worstEndpoint": "https://httpbin.org/post [POST]"
  }
}
```

---

## JSON Diff 前端组件

前端的 JsonDiffViewer 组件支持：
- 双栏 JSON 对比（左侧预期、右侧实际，递归格式化显示）
- 差异明细表格（字段路径 + 类型 + 预期值 + 实际值）
- 修复建议区域 + 一键应用按钮（存入 localStorage）
- 嵌套 JSON 自动展开（deepUnescape 递归处理）

---

## 分类管理接口 (V3)

### 获取分类树

```
GET /categories/tree
```

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "name": "HTTP基础",
      "level": 1,
      "sortOrder": 1,
      "children": [
        {
          "id": 4,
          "parentId": 1,
          "name": "GET 请求",
          "level": 2,
          "sortOrder": 1,
          "children": []
        }
      ]
    },
    {
      "id": 2,
      "parentId": 0,
      "name": "认证模块",
      "level": 1,
      "sortOrder": 2,
      "children": []
    }
  ]
}
```

---

### 获取分类列表（扁平）

```
GET /categories
```

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "name": "HTTP基础",
      "level": 1,
      "sortOrder": 1
    }
  ]
}
```

---

### 新建分类

```
POST /categories
```

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| parentId | Long | 是 | 父级 ID，0 表示顶级 |
| name | String | 是 | 分类名称（同父级下唯一） |
| sortOrder | Integer | 否 | 排序序号 |

**请求示例**

```json
{
  "parentId": 0,
  "name": "支付模块",
  "sortOrder": 4
}
```

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**校验失败响应**

```json
{
  "code": 400,
  "message": "已达到最大层级（3层）",
  "data": null
}
```

---

### 修改分类

```
PUT /categories/{id}
```

**参数**

| 参数 | 位置 | 类型 | 说明 |
|---|---|---|---|
| id | path | Long | 分类 ID |

**请求体**（同新建接口）

---

### 删除分类

```
DELETE /categories/{id}
```

**参数**

| 参数 | 位置 | 类型 | 说明 |
|---|---|---|---|
| id | path | Long | 分类 ID |

**删除保护**

- 有子分类时返回 400：`"该分类下有 X 个子分类，无法删除"`
- 无子分类时删除成功

---

### 查询分类下的用例

```
GET /testcases?categoryId={id}
```

**参数**

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| categoryId | query | Long | 否 | 分类 ID，不传则返回全部用例 |

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "testNo": "TC-001",
      "name": "HTTP GET 请求",
      "categoryId": 1,
      "requestMethod": "GET",
      "requestUrl": "https://httpbin.org/get"
    }
  ]
}
```
