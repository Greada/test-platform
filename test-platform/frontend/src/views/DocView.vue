<template>
  <div style="padding: 20px; max-width: 900px; margin: 0 auto; line-height: 1.8">
    <h1>全功能测试平台 V1 — 文档</h1>

    <el-divider/>

    <h2>项目定位</h2>
    <p>一站式测试管理平台 V1，聚焦测试用例管理、执行、报告与日志展示。</p>

    <h2>技术栈</h2>
    <el-table :data="techStack" border stripe size="small" style="width: 100%">
      <el-table-column prop="layer" label="层" width="120"/>
      <el-table-column prop="tech" label="技术"/>
      <el-table-column prop="version" label="版本" width="200"/>
    </el-table>

    <h2>项目结构</h2>
    <pre style="background: #f5f7fa; padding: 16px; border-radius: 6px; overflow: auto"><code>{{ projectStructure }}</code></pre>

    <h2>开发进度</h2>
    <el-table :data="progress" border stripe size="small" style="width: 100%">
      <el-table-column prop="phase" label="阶段"/>
      <el-table-column prop="content" label="内容"/>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === '已完成' ? 'success' : 'warning'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <h2>API 接口</h2>
    <el-table :data="apiList" border stripe size="small" style="width: 100%">
      <el-table-column prop="method" label="方法" width="80">
        <template #default="{ row }">
          <el-tag :type="row.method === 'GET' ? '' : row.method === 'POST' ? 'success' : row.method === 'PUT' ? 'warning' : 'danger'" size="small">{{ row.method }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路径"/>
      <el-table-column prop="desc" label="说明"/>
    </el-table>

    <h2>执行匹配策略</h2>
    <p><code>expected_result</code> 为合法 JSON → <el-tag type="success">JSON 字段子集匹配</el-tag>（支持递归嵌套，多余字段忽略）</p>
    <p><code>expected_result</code> 非 JSON → <el-tag type="info">contains 文本降级</el-tag></p>

    <h3>匹配示例</h3>
    <el-table :data="matchExamples" border stripe size="small" style="width: 100%">
      <el-table-column prop="expected" label="预期结果"/>
      <el-table-column prop="actual" label="实际响应"/>
      <el-table-column prop="result" label="结果" width="80">
        <template #default="{ row }">
          <el-tag :type="row.result === 'PASS' ? 'success' : 'danger'" size="small">{{ row.result }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <h2>种子数据</h2>
    <el-table :data="seedData" border stripe size="small" style="width: 100%">
      <el-table-column prop="testNo" label="编号" width="80"/>
      <el-table-column prop="name" label="名称"/>
      <el-table-column prop="method" label="方法" width="70"/>
      <el-table-column prop="purpose" label="验证目的"/>
    </el-table>

    <h2>启动方式</h2>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="后端">运行 <code>TestPlatformApplication.main()</code>，监听 <code>http://localhost:8080</code></el-descriptions-item>
      <el-descriptions-item label="前端">在 <code>frontend/</code> 目录执行 <code>npm run dev</code>，访问 <code>http://localhost:3000</code></el-descriptions-item>
      <el-descriptions-item label="数据库">执行 <code>init_v1.sql</code> + <code>insert_test_case_v1.sql</code>，默认连接 <code>root:1234@localhost:3306</code></el-descriptions-item>
    </el-descriptions>

    <div style="margin-top: 40px; text-align: center; color: #999; font-size: 13px">
      测试平台 V1 &copy; 2026
    </div>
  </div>
</template>

<script setup>
const techStack = [
  { layer: '语言', tech: 'Java', version: '1.8' },
  { layer: '框架', tech: 'Spring Boot', version: '2.7.18' },
  { layer: 'ORM', tech: 'MyBatis-Plus', version: '3.5.5' },
  { layer: '数据库', tech: 'MySQL', version: '5.7+ (驱动 8.0.33)' },
  { layer: '前端', tech: 'Vue 3 + Element Plus', version: '-' },
  { layer: '构建', tech: 'Maven (父子模块)', version: '-' },
]

const progress = [
  { phase: 'Phase 1', content: '后端骨架（7 个基础文件）', status: '已完成' },
  { phase: 'V1', content: '用例管理 + 执行 + 报告 + 日志', status: '已完成' },
  { phase: '联调验证', content: '全流程集成验证', status: '已完成' },
]

const apiList = [
  { method: 'GET', path: '/api/testcases', desc: '查询全部用例' },
  { method: 'GET', path: '/api/testcases/{id}', desc: '查询单个用例' },
  { method: 'POST', path: '/api/testcases', desc: '新建用例' },
  { method: 'PUT', path: '/api/testcases/{id}', desc: '修改用例' },
  { method: 'DELETE', path: '/api/testcases/{id}', desc: '删除用例' },
  { method: 'POST', path: '/api/execution-records/{testCaseId}/execute', desc: '执行用例' },
  { method: 'GET', path: '/api/execution-records?testCaseId={id}', desc: '查询执行记录' },
]

const matchExamples = [
  { expected: '{"code": 200}', actual: '{"code":200,"data":[...]}', result: 'PASS' },
  { expected: '{"headers":{"X-Test-Header":"test123"}}', actual: '{"headers":{"X-Test-Header":"test123","Host":"..."}}', result: 'PASS' },
  { expected: '{"url":"https://httpbin.org/get"}', actual: '{"url":"https://httpbin.org/get","args":{}}', result: 'PASS' },
  { expected: '{"url":"https://httpbin.org/wrong"}', actual: '{"url":"https://httpbin.org/get"}', result: 'FAIL' },
]

const seedData = [
  { testNo: 'TC-001', name: 'HTTP GET 请求', method: 'GET', purpose: '基础 JSON 匹配' },
  { testNo: 'TC-002', name: 'POST 提交 JSON 数据', method: 'POST', purpose: 'POST + 嵌套匹配' },
  { testNo: 'TC-003', name: 'GET 带查询参数', method: 'GET', purpose: 'URL 参数透传' },
  { testNo: 'TC-004', name: '自定义请求头回显', method: 'GET', purpose: '请求头 + 递归嵌套' },
  { testNo: 'TC-005', name: 'JSON 结构数据', method: 'GET', purpose: '深层嵌套' },
  { testNo: 'TC-006', name: '查询全部用例（自测）', method: 'GET', purpose: '自引用测试' },
  { testNo: 'TC-007', name: '3 秒延迟请求', method: 'GET', purpose: '延迟响应' },
  { testNo: 'TC-008', name: 'PUT 请求测试', method: 'PUT', purpose: 'PUT 方法' },
  { testNo: 'TC-009', name: 'DELETE 请求测试', method: 'DELETE', purpose: 'DELETE 方法' },
  { testNo: 'TC-010', name: '预期失败用例', method: 'GET', purpose: '验证 FAIL 正确触发' },
  { testNo: 'TC-011', name: '多层嵌套匹配', method: 'GET', purpose: '多层递归' },
  { testNo: 'TC-012', name: '文本降级匹配', method: 'GET', purpose: '非 JSON 降级' },
]

const projectStructure = `test-platform/
├── pom.xml
├── docs/
│   ├── PROJECT_INTRO.md
│   └── API.md
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/testplatform/
│       │   ├── TestPlatformApplication.java
│       │   ├── common/
│       │   ├── config/
│       │   ├── controller/
│       │   ├── entity/
│       │   ├── mapper/
│       │   └── service/
│       └── resources/
│           ├── application.yml
│           └── sql/
├── frontend/
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── api/
│       ├── router/
│       └── views/`
</script>
