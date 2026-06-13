<template>
  <div class="page-container">
    <div class="page-header">
      <h2>测试用例</h2>
      <div class="header-actions">
        <el-input
          v-model="searchQuery"
          placeholder="搜索用例编号或名称"
          clearable
          style="width: 300px; margin-right: 10px"
          prefix-icon="Search"
        />
        <el-button type="primary" @click="router.push('/edit')">
          新建用例
        </el-button>
      </div>
    </div>
    <el-skeleton :rows="5" animated v-if="loading" />
    <el-empty v-else-if="filteredAndSearchedList.length === 0" description="暂无测试用例，点击新建用例开始" />
    <el-table v-else :data="filteredAndSearchedList" border stripe>
      <el-table-column prop="testNo" label="编号" width="100"/>
      <el-table-column prop="name" label="名称"/>
      <el-table-column prop="requestMethod" label="方法" width="80"/>
      <el-table-column prop="requestHeaders" label="请求头" min-width="150" show-overflow-tooltip/>
      <el-table-column prop="requestParams" label="请求参数" min-width="150" show-overflow-tooltip/>
      <el-table-column prop="expectedResult" label="预期结果" min-width="150" show-overflow-tooltip/>
      <el-table-column prop="createTime" label="创建时间" width="170"/>
      <el-table-column prop="updateTime" label="更新时间" width="170"/>
      <el-table-column prop="requestUrl" label="地址" min-width="250"/>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="router.push('/edit/' + row.id)">
            编辑
          </el-button>
          <el-button size="small" type="success" link @click="execute(row.id)" :loading="loadingId === row.id">
            执行
          </el-button>
          <el-button size="small" type="danger" link @click="remove(row.id)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import {onMounted, ref, computed} from 'vue'
import api from '../api'
import {ElMessage, ElMessageBox} from "element-plus";
import {useRouter} from 'vue-router'
import {Search} from '@element-plus/icons-vue'

const list = ref([])
const loading = ref(true)
const loadingId = ref(null)
const searchQuery = ref('')
const router = useRouter()

const filteredAndSearchedList = computed(() => {
  const query = searchQuery.value.toLowerCase().trim()
  if (!query) return list.value
  return list.value.filter(item =>
    (item.testNo && item.testNo.toLowerCase().includes(query)) ||
    (item.name && item.name.toLowerCase().includes(query))
  )
})
onMounted(async () => {
  loading.value = true
  try {
    await fetchList()
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
})

async function fetchList() {
  const res = await api.get('/testcases')
  list.value = res.data.data || []
}

async function execute(id) {
  loadingId.value = id
  try {
    const res = await api.post(`/execution-records/${id}/execute`)
    if (res.data.code === 200) {
      ElMessage.success('执行完成')
      await router.push('/executions?testCaseId=' + id)
    } else {
      ElMessage.error(res.data.message)
    }
  } catch (e) {
    ElMessage.error('执行失败')
  }
  loadingId.value = null
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('确定要删除这个用例吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.delete('/testcases/' + id)
    ElMessage.success('已删除')
    await fetchList()
  } catch {
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

.header-actions {
  display: flex;
  align-items: center;
}
</style>