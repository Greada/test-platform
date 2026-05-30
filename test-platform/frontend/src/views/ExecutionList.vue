<template>
  <div style="padding: 20px">
    <h2>执行记录</h2>
    <el-form inline>
      <el-form-item label="用例 ID">
        <el-input-number v-model="testCaseId" :min="1"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchRecords">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="records" border stripe>
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column prop="testCaseId" label="用例ID" width="80"/>
      <el-table-column prop="testNo" label="用例编号" width="100"/>
      <el-table-column prop="caseName" label="用例名称" min-width="150" show-overflow-tooltip/>
      <el-table-column prop="status" label="结果" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PASS' ? 'success' : row.status === 'FAIL' ? 'danger' : 'warning'">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="executeDuration" label="耗时(ms)" width="100"/>
      <el-table-column prop="actualResult" label="实际结果" min-width="200" show-overflow-tooltip/>
      <el-table-column prop="executeTime" label="执行时间" width="180"/>
      <el-table-column label="详情" width="80">
        <template #default="{ row }">
          <el-button size="small" @click="showDetail(row)">日志</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" title="执行日志" width="700px">
      <pre style="background:#1e1e1e;color:#d4d4d4;padding:16px;border-radius:6px;overflow:auto;max-height:500px">
{{ dialogData }}
      </pre>
    </el-dialog>
  </div>
</template>
<script setup>
import {onMounted, ref} from 'vue'
import api from '../api'
import {useRoute} from "vue-router";

const testCaseId = ref(1)
const records = ref([])
const dialogVisible = ref(false)
const dialogData = ref('')
const route = useRoute()

onMounted(() => {
  if (route.query.testCaseId) {
    testCaseId.value = Number(route.query.testCaseId)
    fetchRecords()
  }
})

async function fetchRecords() {
  const res = await api.get('/execution-records', {params: {testCaseId: testCaseId.value}})
  records.value = res.data.data || []
}

function showDetail(row) {
  dialogData.value = JSON.stringify({
    status: row.status,
    requestDetail: row.requestDetail,
    responseDetail: row.responseDetail,
    actualResult: row.actualResult
  }, null, 2)
  dialogVisible.value = true
}
</script>