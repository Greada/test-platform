<template>
  <div style="padding: 20px">
    <el-button style="margin-bottom: 10px" @click="router.push('/reports')">← 返回报告列表</el-button>

    <div style="display: flex; gap: 20px; margin-bottom: 20px">
      <el-card style="flex: 1; text-align: center">
        <div style="font-size: 32px; font-weight: bold">{{ report.total }}</div>
        <div style="color: #999; margin-top: 5px">总计</div>
      </el-card>
      <el-card style="flex: 1; text-align: center; border-top: 3px solid #67c23a">
        <div style="font-size: 32px; font-weight: bold; color: #67c23a">{{ report.passed }}</div>
        <div style="color: #999; margin-top: 5px">通过</div>
      </el-card>
      <el-card style="flex: 1; text-align: center; border-top: 3px solid #f56c6c">
        <div style="font-size: 32px; font-weight: bold; color: #f56c6c">{{ report.failed }}</div>
        <div style="color: #999; margin-top: 5px">失败</div>
      </el-card>
      <el-card style="flex: 1; text-align: center; border-top: 3px solid #e6a23c">
        <div style="font-size: 32px; font-weight: bold; color: #e6a23c">{{ report.errored }}</div>
        <div style="color: #999; margin-top: 5px">错误</div>
      </el-card>
      <el-card style="flex: 1; text-align: center">
        <div style="font-size: 32px; font-weight: bold; color: #409eff">
          {{ report.passRate != null ? report.passRate + '%' : '-' }}
        </div>
        <div style="color: #999; margin-top: 5px">通过率</div>
      </el-card>
    </div>

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

onMounted(async () => {
  const res = await reportApi.get(route.params.id)
  report.value = res.data.data || {}
  const res2 = await reportApi.getDetails(route.params.id)
  details.value = res2.data.data || []
  const res3 = await reportApi.errorPatterns(route.params.id)
  errorPatterns.value = res3.data.data || null
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