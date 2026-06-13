<template>
  <div class="page-container">
    <div class="page-header">
      <h2>执行报告</h2>
      <el-select
        v-model="selectedSuiteId"
        placeholder="选择套件筛选"
        clearable
        style="width: 200px"
        @change="fetchReports"
      >
        <el-option
          v-for="suite in suites"
          :key="suite.id"
          :label="suite.name"
          :value="suite.id"
        />
      </el-select>
    </div>

    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6">
        <el-statistic title="总计" :value="totalReports" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="平均通过率" :value="averagePassRate" :precision="1" suffix="%" />
      </el-col>
    </el-row>

    <el-skeleton :rows="5" animated v-if="loading" />
    <el-empty v-else-if="reports.length === 0" description="暂无执行报告" />
    <el-table v-else :data="reports" border stripe>
      <el-table-column prop="reportName" label="报告名称" min-width="250"/>
      <el-table-column prop="total" label="总计" width="60"/>
      <el-table-column prop="passed" label="通过" width="60">
        <template #default="{ row }"><span style="color: #67c23a">{{ row.passed }}</span></template>
      </el-table-column>
      <el-table-column prop="failed" label="失败" width="60">
        <template #default="{ row }"><span style="color: #f56c6c">{{ row.failed }}</span></template>
      </el-table-column>
      <el-table-column prop="errored" label="错误" width="60">
        <template #default="{ row }"><span style="color: #e6a23c">{{ row.errored }}</span></template>
      </el-table-column>
      <el-table-column prop="passRate" label="通过率" width="150">
        <template #default="{ row }">
          <el-progress :percentage="row.passRate || 0"
                       :color="row.passRate >= 80 ? '#67c23a' : '#f56c6c'"/>
        </template>
      </el-table-column>
      <el-table-column prop="executeTime" label="执行时间" width="180"/>
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button size="small" @click="router.push('/reports/' + row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import {onMounted, ref, computed} from 'vue'
import {reportApi, suiteApi} from '../api'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'

const router = useRouter()
const reports = ref([])
const suites = ref([])
const selectedSuiteId = ref(null)
const loading = ref(true)

const totalReports = computed(() => reports.value.length)

const averagePassRate = computed(() => {
  if (reports.value.length === 0) return 0
  const total = reports.value.reduce((sum, r) => sum + (r.passRate || 0), 0)
  return Math.round((total / reports.value.length) * 10) / 10
})

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([fetchSuites(), fetchReports()])
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
})

async function fetchSuites() {
  const res = await suiteApi.list()
  suites.value = res.data.data || []
}

async function fetchReports() {
  loading.value = true
  try {
    const params = selectedSuiteId.value ? {suiteId: selectedSuiteId.value} : undefined
    const res = await reportApi.list(params)
    reports.value = res.data.data || []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
  font-weight: 600;
}
</style>