<template>
  <div style="padding: 20px">
    <el-button style="margin-bottom: 10px" @click="router.push('/suites')">← 返回套件列表</el-button>

    <el-card style="margin-bottom: 20px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>{{ suite.name }}</span>
          <div>
            <el-button type="success" @click="executeSuite">批量执行</el-button>
          </div>
        </div>
      </template>
      <p v-if="suite.description">{{ suite.description }}</p>
      <p v-else style="color: #999">暂无描述</p>
    </el-card>

    <div style="margin-bottom: 10px">
      <el-button type="primary" @click="openAddDialog">添加用例</el-button>
    </div>

    <el-table :data="cases" border stripe>
      <el-table-column prop="testNo" label="编号" width="100"/>
      <el-table-column prop="name" label="名称" min-width="200"/>
      <el-table-column prop="requestMethod" label="方法" width="80"/>
      <el-table-column prop="requestUrl" label="地址" min-width="250" show-overflow-tooltip/>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="removeCase(row.id)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAddDialog" title="添加用例" width="800px">
      <el-table :data="allCases" border stripe @selection-change="selectedCases = $event">
        <el-table-column type="selection" width="50"/>
        <el-table-column prop="testNo" label="编号" width="100"/>
        <el-table-column prop="name" label="名称" min-width="200"/>
        <el-table-column prop="requestMethod" label="方法" width="80"/>
        <el-table-column prop="requestUrl" label="地址" min-width="250" show-overflow-tooltip/>
      </el-table>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addSelectedCases">添加选中 ({{ selectedCases.length }})</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showExecuting" title="批量执行" width="350px"
               :close-on-click-modal="false" :close-on-press-escape="false">
      <div style="text-align: center; padding: 30px">
        <el-icon class="is-loading" :size="32" style="margin-bottom: 15px">
          <i class="el-icon-loading"></i>
        </el-icon>
        <p style="font-size: 16px; margin-top: 10px">正在执行套件，请稍候...</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import api, {suiteApi} from '../api'
import {ElMessage} from 'element-plus'

const route = useRoute()
const router = useRouter()
const suite = ref({})
const cases = ref([])
const allCases = ref([])
const selectedCases = ref([])
const showAddDialog = ref(false)
const executing = ref(false)
const showExecuting = ref(false)

onMounted(async () => {
  const res = await suiteApi.get(route.params.id)
  suite.value = res.data.data || {}
  await fetchCases()
})

async function fetchCases() {
  const res = await suiteApi.listCases(route.params.id)
  cases.value = res.data.data || []
}

async function executeSuite() {
  showExecuting.value = true
  try {
    const res = await suiteApi.execute(route.params.id)
    showExecuting.value = false
    if (res.data.code === 200 && res.data.data) {
      ElMessage.success('执行完成')
      await router.push('/reports/' + res.data.data.id)
    } else {
      ElMessage.error(res.data.message || '执行失败')
    }
  } catch (e) {
    showExecuting.value = false
    ElMessage.error('执行异常')
  }
}

async function removeCase(caseId) {
  await suiteApi.removeCase(route.params.id, caseId)
  ElMessage.success('已移除')
  await fetchCases()
}

async function openAddDialog() {
  const res = await api.get('/testcases')
  const all = res.data.data || []
  const existingIds = new Set(cases.value.map(c => c.id))
  allCases.value = all.filter(c => !existingIds.has(c.id))
  selectedCases.value = []
  showAddDialog.value = true
}

async function addSelectedCases() {
  const ids = selectedCases.value.map(tc => tc.id)
  await suiteApi.batchAddCases(route.params.id, ids)
  ElMessage.success('已添加 ' + ids.length + ' 个用例')
  showAddDialog.value = false
  await fetchCases()
}
</script>