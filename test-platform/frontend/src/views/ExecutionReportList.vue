<template>
  <div style="padding: 20px">
    <h2>执行报告</h2>
    <el-table :data="reports" border stripe>
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
import {onMounted, ref} from 'vue'
import {reportApi} from '../api'
import {useRouter} from 'vue-router'

const router = useRouter()
const reports = ref([])

onMounted(() => fetchReports())

async function fetchReports() {
  const res = await reportApi.list()
  reports.value = res.data.data || []
}
</script>