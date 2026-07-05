<template>
  <div class="page-container">
    <el-skeleton :rows="5" animated v-if="loading" />
    <template v-else>
    <el-page-header @back="router.push('/suites')" :title="'返回套件列表'" style="margin-bottom: 20px">
      <template #content>
        <span class="page-header-title">{{ suite.name }}</span>
      </template>
      <template #extra>
        <el-button type="success" @click="executeSuite">批量执行</el-button>
      </template>
    </el-page-header>

    <el-card style="margin-bottom: 20px">
      <template #header>
        <span>套件描述</span>
      </template>
      <p v-if="suite.description">{{ suite.description }}</p>
      <p v-else style="color: #999">暂无描述</p>
    </el-card>

    <div style="margin-bottom: 10px">
      <el-button type="primary" @click="openAddDialog">添加用例</el-button>
    </div>

    <el-empty v-if="cases.length === 0" description="该套件暂无测试用例" />
    <el-table v-else :data="cases" border stripe>
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
    </template>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import api, {suiteApi} from '../api'
import {ElMessage, ElMessageBox} from 'element-plus'

const route = useRoute()
const router = useRouter()
const suite = ref({})
const cases = ref([])
const allCases = ref([])
const selectedCases = ref([])
const showAddDialog = ref(false)
const executing = ref(false)
const showExecuting = ref(false)
const loading = ref(true)

onMounted(async () => {
  loading.value = true
  try {
    const res = await suiteApi.get(route.params.id)
    suite.value = res.data.data || {}
    await fetchCases()
  } catch (e) {
    ElMessage.error('加载套件详情失败')
  } finally {
    loading.value = false
  }
})

async function fetchCases() {
  try {
    const res = await suiteApi.listCases(route.params.id)
    cases.value = res.data.data || []
  } catch (e) {
    ElMessage.error('加载用例列表失败')
  }
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
  try {
    await ElMessageBox.confirm('确定要从套件中移除此用例吗？', '确认移除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await suiteApi.removeCase(route.params.id, caseId)
    ElMessage.success('已移除')
    await fetchCases()
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      ElMessage.error('移除失败: ' + (e.response?.data?.message || e.message))
    }
  }
}

async function openAddDialog() {
  try {
    const res = await api.get('/testcases')
    const all = res.data.data || []
    const existingIds = new Set(cases.value.map(c => c.id))
    allCases.value = all.filter(c => !existingIds.has(c.id))
    selectedCases.value = []
    showAddDialog.value = true
  } catch (e) {
    ElMessage.error('加载用例列表失败')
  }
}

async function addSelectedCases() {
  try {
    const ids = selectedCases.value.map(tc => tc.id)
    await suiteApi.batchAddCases(route.params.id, ids)
    ElMessage.success('已添加 ' + ids.length + ' 个用例')
    showAddDialog.value = false
    await fetchCases()
  } catch (e) {
    ElMessage.error('添加用例失败')
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

.page-header-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
</style>