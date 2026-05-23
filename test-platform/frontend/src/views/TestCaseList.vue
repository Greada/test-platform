<template>
  <div style="padding: 20px">
    <h2>测试用例</h2>
    <el-button type="primary" style="margin: 10px 0" @click="router.push('/edit')">
      新建用例
    </el-button>
    <el-table :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column prop="name" label="名称"/>
      <el-table-column prop="requestMethod" label="方法" width="80"/>
      <el-table-column prop="requestHeaders" label="请求头" min-width="150" show-overflow-tooltip/>
      <el-table-column prop="requestParams" label="请求参数" min-width="150" show-overflow-tooltip/>
      <el-table-column prop="expectedResult" label="预期结果" min-width="150" show-overflow-tooltip/>
      <el-table-column prop="createTime" label="创建时间" width="170"/>
      <el-table-column prop="updateTime" label="更新时间" width="170"/>
      <el-table-column prop="requestUrl" label="地址" min-width="250"/>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="router.push('/edit/' + row.id)">编辑</el-button>
          <el-button size="small" type="success" @click="execute(row.id)" :loading="loadingId === row.id">
            执行
          </el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import {onMounted, ref} from 'vue'
import api from '../api'
import {ElMessage} from "element-plus";
import {useRouter} from 'vue-router'

const list = ref([])
const loadingId = ref(null)
const router = useRouter()
onMounted(() => fetchList())

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
    } else {
      ElMessage.error(res.data.message)
    }
  } catch (e) {
    ElMessage.error('执行失败')
  }
  loadingId.value = null
}

async function remove(id) {
  await api.delete('/testcases/' + id)
  ElMessage.success('已删除')
  await fetchList()
}
</script>