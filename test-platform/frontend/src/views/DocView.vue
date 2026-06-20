<template>
  <div style="padding: 20px; max-width: 900px; margin: 0 auto; line-height: 1.8">
    <h1>全功能测试平台 V3.2 — 文档</h1>
    <el-alert title="docs/ 目录下还有更多文档可供阅读" type="info" :closable="false" show-icon style="margin-bottom: 20px"/>

    <el-divider/>

    <h2>项目定位</h2>
    <p>一站式测试管理平台 V3.1，聚焦测试用例管理、执行、报告、日志展示、测试套件、执行报告统计、JSON Diff 分析、错误模式聚合、分类管理与 AI 智能生成预期结果。</p>

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

    <h2>文档导航</h2>
    <el-table :data="docNav" border stripe size="small" style="width: 100%">
      <el-table-column prop="file" label="文件" min-width="200"/>
      <el-table-column prop="desc" label="说明"/>
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

    <el-divider/>

    <h2>测试套件 (V2.1)</h2>
    <p>将多个测试用例组合为套件，支持批量添加/移除用例，一键批量执行。</p>
    <p>批量执行时自动创建执行报告，汇总通过/失败/错误数及通过率。</p>

    <h2>执行报告 (V2.1)</h2>
    <p>套件执行后自动生成执行报告，包含统计卡片（总计/通过/失败/错误/通过率）和明细表格。</p>
    <p>执行记录快照用例编号（test_no）和用例名称（case_name），历史报告不受用例修改影响。</p>

    <h2>JSON Diff 分析 (V2.2)</h2>
    <p>执行失败后可查看 JSON Diff 可视化对比：左侧预期结果、右侧实际结果，差异字段逐行展示。</p>
    <p>支持自动修复建议：分析差异后生成格式化的 actualResult，一键应用（存入 localStorage）。</p>
    <p>支持嵌套 JSON 递归展开、Map/List 深层对比、数值安全比较。</p>

    <h2>错误模式聚合 (V2.2)</h2>
    <p>执行报告自动聚合错误模式：按 URL+Method 分组统计通过率，自动标出通过率最低的端点。</p>

    <h2>分类管理 (V3)</h2>
    <p>树状三层分类管理，支持新建/编辑/删除分类。编辑面板可设置用例所属分类，列表页可按分类筛选。</p>

    <h2>AI 预期结果生成 (V3.1)</h2>
    <p>接入 Agnes AI（agnes-2.0-flash），在编辑面板填写 URL/方法/参数后，点击"AI 生成"按钮自动填充预期结果。</p>

    <h2>OpenAPI 批量导入 (V3.1)</h2>
    <p>粘贴 Swagger/OpenAPI JSON，后端解析所有端点并调用 AI 填充预期结果，预览后批量入库。</p>

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
      <el-descriptions-item label="数据库 V1">执行 <code>init_v1.sql</code> + <code>insert_test_case_v1.sql</code>，默认连接 <code>root:1234@localhost:3306</code></el-descriptions-item>
      <el-descriptions-item label="数据库 V2.1 升级">在 V1 基础上执行 <code>init_v2.sql</code>（新增 3 表 + 4 字段）</el-descriptions-item>
      <el-descriptions-item label="数据库 V3 升级">在 V2.1 基础上执行 <code>init_v3.sql</code>（新增 test_category 表）</el-descriptions-item>
      <el-descriptions-item label="数据库 V3.2 升级">执行 <code>sql/init_v3.sql</code> 中的 user 表部分（新增 user 表 + 默认管理员）</el-descriptions-item>
      <el-descriptions-item label="AI 集成">启动前设置环境变量 <code>AGNES_API_KEY</code></el-descriptions-item>
    </el-descriptions>

    <div style="margin-top: 40px; text-align: center; color: #999; font-size: 13px">
      测试平台 V3.2 &copy; 2026
    </div>
  </div>
</template>

<script setup>
const docNav = [
  { file: 'docs/PROJECT_INTRO.md', desc: '项目介绍（文档入口）' },
  { file: 'docs/API.md', desc: 'API 接口文档' },
  { file: 'docs/sql.md', desc: '数据库 ER 图与表结构' },
  { file: 'docs/进度报告.md', desc: '项目进度总览、里程碑、功能统计' },
  { file: 'docs/开发进度.md', desc: '分阶段详细任务跟踪与修复记录' },
  { file: 'docs/阶段总结报告.md', desc: 'V3.1 阶段总结、技术决策、经验教训' },
  { file: 'docs/resume.html', desc: '开发恢复指南' },
]

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
  { phase: 'V2.1', content: '测试套件 + 执行报告 + HttpExecutor 升级', status: '已完成' },
  { phase: 'V2.2', content: 'JSON Diff + 错误模式聚合 + 批执行修复', status: '已完成' },
  { phase: 'V3', content: '分类管理（树状 3 层）', status: '已完成' },
  { phase: 'V3.1', content: 'AI 智能生成预期结果 + OpenAPI 批量导入', status: '已完成' },
  { phase: 'V3.2', content: 'JWT 权限管理（登录/注册/路由守卫）', status: '已完成' },
]

const apiList = [
  { method: 'GET', path: '/api/testcases', desc: '查询全部用例' },
  { method: 'GET', path: '/api/testcases/{id}', desc: '查询单个用例' },
  { method: 'POST', path: '/api/testcases', desc: '新建用例' },
  { method: 'PUT', path: '/api/testcases/{id}', desc: '修改用例' },
  { method: 'DELETE', path: '/api/testcases/{id}', desc: '删除用例' },
  { method: 'POST', path: '/api/execution-records/{testCaseId}/execute', desc: '执行用例' },
  { method: 'GET', path: '/api/execution-records?testCaseId={id}', desc: '查询执行记录' },
  { method: 'GET', path: '/api/test-suites', desc: '查询全部套件' },
  { method: 'GET', path: '/api/test-suites/{id}', desc: '查询单个套件' },
  { method: 'POST', path: '/api/test-suites', desc: '新建套件' },
  { method: 'PUT', path: '/api/test-suites/{id}', desc: '修改套件' },
  { method: 'DELETE', path: '/api/test-suites/{id}', desc: '删除套件' },
  { method: 'GET', path: '/api/test-suites/{id}/cases', desc: '查询套件内用例' },
  { method: 'POST', path: '/api/test-suites/{id}/cases', desc: '添加单个用例' },
  { method: 'POST', path: '/api/test-suites/{id}/cases/batch', desc: '批量添加用例' },
  { method: 'DELETE', path: '/api/test-suites/{id}/cases/{caseId}', desc: '从套件移除用例' },
  { method: 'POST', path: '/api/test-suites/{id}/execute', desc: '批量执行套件' },
  { method: 'GET', path: '/api/execution-reports', desc: '查询报告列表' },
  { method: 'GET', path: '/api/execution-reports/{id}', desc: '查询报告详情' },
  { method: 'GET', path: '/api/execution-reports/{id}/details', desc: '查询报告明细' },
  { method: 'GET', path: '/api/execution-records/{id}/diff', desc: 'JSON Diff 分析 (V2.2)' },
  { method: 'GET', path: '/api/execution-reports/{id}/error-patterns', desc: '错误模式聚合 (V2.2)' },
  { method: 'GET', path: '/api/categories/tree', desc: '查询分类树 (V3)' },
  { method: 'GET', path: '/api/categories', desc: '查询分类列表 (V3)' },
  { method: 'POST', path: '/api/categories', desc: '新建分类 (V3)' },
  { method: 'PUT', path: '/api/categories', desc: '修改分类 (V3)' },
  { method: 'DELETE', path: '/api/categories/{id}', desc: '删除分类 (V3)' },
  { method: 'POST', path: '/api/ai/expected', desc: 'AI 生成预期结果 (V3.1)' },
  { method: 'POST', path: '/api/testcases/import-openapi', desc: '导入 OpenAPI 解析+AI 填充 (V3.1)' },
  { method: 'POST', path: '/api/testcases/batch-save', desc: '批量保存用例 (V3.1)' },
  { method: 'POST', path: '/api/auth/login', desc: '用户登录 (V3.2)' },
  { method: 'POST', path: '/api/auth/register', desc: '用户注册 (V3.2)' },
  { method: 'GET', path: '/api/auth/me', desc: '获取当前用户 (V3.2)' },
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
│   ├── API.md
│   ├── sql.md
│   ├── 进度报告.md
│   ├── 开发进度.md
│   ├── 阶段总结报告.md
│   └── resume.html
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/testplatform/
│       │   ├── TestPlatformApplication.java
│       │   ├── common/
│       │   │   ├── Result.java
│       │   │   ├── HttpResult.java
│       │   │   ├── JsonDiffResult.java      # V2.2
│       │   │   ├── ErrorPatternItem.java    # V2.2
│       │   │   ├── ErrorPatternResult.java  # V2.2
│       │   │   ├── EndpointDef.java         # V3.1
│       │   │   └── exception/
│       │   ├── config/
│       │   │   ├── CorsConfig.java
│       │   │   ├── SecurityConfig.java
│       │   │   ├── RestTemplateConfig.java
│       │   │   ├── JwtUtil.java              # V3.2
│       │   │   ├── JwtAuthFilter.java        # V3.2
│       │   │   ├── PasswordEncoderConfig.java# V3.2
│       │   │   └── AiConfig.java            # V3.1
│       │   ├── controller/
│       │   │   ├── AuthController.java       # V3.2
│       │   │   ├── AiController.java        # V3.1
│       │   │   ├── CategoryController.java  # V3
│       │   │   ├── TestCaseController.java
│       │   │   ├── ExecutionController.java
│       │   │   ├── TestSuiteController.java
│       │   │   └── ExecutionReportController.java
│       │   ├── entity/
│       │   │   ├── User.java                # V3.2
│       │   │   ├── TestCase.java
│       │   │   ├── ExecutionRecord.java
│       │   │   ├── TestSuite.java
│       │   │   ├── TestSuiteCase.java
│       │   │   └── ExecutionReport.java
│       │   ├── mapper/
│       │   │   ├── UserMapper.java          # V3.2
│       │   ├── service/
│       │   │   ├── UserService.java         # V3.2
│       │   │   ├── AiService.java           # V3.1
│       │   │   ├── CategoryService.java     # V3
│       │   │   ├── ...
│       │   │   ├── JsonDiffService.java     # V2.2
│       │   │   └── ErrorPatternService.java # V2.2
│       │   └── util/
│       │       └── OpenApiParser.java       # V3.1
│       └── resources/
│           ├── application.yml
│           └── sql/
│               ├── init_v1.sql
│               ├── init_v2.sql
│               ├── init_v3.sql
│               └── insert_test_case_v1.sql
├── frontend/
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── api/
│       │   ├── index.js
│       │   └── auth.js                      # V3.2
│       ├── router/
│       ├── components/
│       │   ├── CategoryTree.vue             # V3
│       │   ├── CategoryDialog.vue            # V3
│       │   ├── JsonDiffViewer.vue           # V2.2
│       │   └── ErrorPatternCard.vue         # V2.2
│       └── views/
│           ├── Login.vue                    # V3.2
│           ├── TestCaseList.vue
│           ├── TestCaseEdit.vue
│           ├── DocView.vue
│           ├── TestSuiteList.vue
│           ├── TestSuiteDetail.vue
│           ├── ExecutionReportList.vue
│           └── ExecutionReportDetail.vue`
</script>
