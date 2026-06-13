<template>
  <div class="page-container">
    <el-skeleton :rows="5" animated v-if="loading" />
    <template v-else>
    <el-page-header @back="router.push('/reports')" :title="'返回报告列表'">
      <template #content>
        <span class="page-header-title">{{ report.reportName || '报告详情' }}</span>
      </template>
    </el-page-header>

    <el-row :gutter="20" style="margin: 20px 0">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="总计" :value="report.total" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-success">
          <el-statistic title="通过" :value="report.passed" value-style="color: #67c23a" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-danger">
          <el-statistic title="失败" :value="report.failed" value-style="color: #f56c6c" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-warning">
          <el-statistic title="错误" :value="report.errored" value-style="color: #e6a23c" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat-info">
          <el-statistic title="通过率" :value="report.passRate != null ? report.passRate : 0" :precision="1" suffix="%" />
        </el-card>
      </el-col>
    </el-row>

    <ErrorPatternCard
        v-if="errorPatterns && errorPatterns.items && errorPatterns.items.length > 0"
        style="margin-bottom:20px"
        :items="errorPatterns.items"
        :worst-endpoint="errorPatterns.worstEndpoint"/>

    <h3>执行明细</h3>
    <el-table :data="details" border stripe>
      <el-table-column prop="testNo" label="用例编号" width="100"/>
      <el-table-column prop="caseName" label="用例名称" min-width="200"/>
      <el-table-column prop="status" label="结果" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PASS' ? 'success' : row.status === 'FAIL' ? 'danger' : 'warning'">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="executeDuration" label="耗时(ms)" width="100"/>
      <el-table-column prop="executeTime" label="执行时间" width="180"/>
      <el-table-column label="日志" width="80">
        <template #default="{ row }">
          <el-button size="small" @click="showLog(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="logVisible" title="执行日志" width="900px">
      <JsonDiffViewer
          :result="diffResult"
          :expected-json="diffExpected"
          :actual-json="diffActual"
          @apply-fix="handleApplyFix"/>
    </el-dialog>
    </template>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {diffApi, reportApi} from '../api'
import JsonDiffViewer from "../components/JsonDiffViewer.vue";
import ErrorPatternCard from "../components/ErrorPatternCard.vue";
import {ElMessage} from "element-plus";

const diffResult = ref(null)
const diffExpected = ref('')
const diffActual = ref('')
const currentRecordId = ref(null)

const route = useRoute()
const router = useRouter()
const report = ref({})
const details = ref([])
const logVisible = ref(false)
const errorPatterns = ref(null)
const loading = ref(true)

onMounted(async () => {
  loading.value = true
  try {
    const res = await reportApi.get(route.params.id)
    report.value = res.data.data || {}
    const res2 = await reportApi.getDetails(route.params.id)
    details.value = res2.data.data || []
    const res3 = await reportApi.errorPatterns(route.params.id)
    errorPatterns.value = res3.data.data || null
  } catch (e) {
    ElMessage.error('加载报告详情失败')
  } finally {
    loading.value = false
  }
})

async function showLog(row) {
  currentRecordId.value = row.id
  diffResult.value = null
  diffExpected.value = ''
  diffActual.value = ''
  logVisible.value = true

  let res
  try {
    res = await diffApi.get(row.id)
    if (res.data.code === 200 && res.data.data) {
      diffResult.value = res.data.data
    }
  } catch {
    // ignore
  }

  diffExpected.value = res?.data?.data?.expectedResult || ''
  diffActual.value = res?.data?.data?.actualResult || ''
}

function handleApplyFix(suggested) {
  if (!suggested) return
  // 把建议的 expected 存入 localStorage，编辑页读取
  localStorage.setItem('fix_expected', suggested)
  ElMessage.success('已填充修复建议，请确认后保存')
  logVisible.value = false
}
</script>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 20px;
}

.page-header-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.stat-card {
  text-align: center;
}

.stat-success {
  border-top: 3px solid #67c23a;
}

.stat-danger {
  border-top: 3px solid #f56c6c;
}

.stat-warning {
  border-top: 3px solid #e6a23c;
}

.stat-info {
  border-top: 3px solid #409eff;
}
</style>