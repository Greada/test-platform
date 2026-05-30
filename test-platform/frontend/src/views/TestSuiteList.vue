<template>
  <div style="padding: 20px">
    <h2>测试套件</h2>
    <el-button type="primary" style="margin: 10px 0" @click="openDialog(null)">新建套件</el-button>
    <el-table :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column prop="name" label="名称" min-width="200"/>
      <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip/>
      <el-table-column prop="createTime" label="创建时间" width="170"/>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="router.push('/suites/' + row.id)">详情</el-button>
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑套件' : '新建套件'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="套件名称"/>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {suiteApi} from '../api'
import {ElMessage} from 'element-plus'
import {useRouter} from 'vue-router'

const router = useRouter()
const list = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const form = ref({name: '', description: ''})

onMounted(() => fetchList())

async function fetchList() {
  const res = await suiteApi.list()
  list.value = res.data.data || []
}

function openDialog(row) {
  if (row) {
    editId.value = row.id
    form.value = {name: row.name, description: row.description || ''}
  } else {
    editId.value = null
    form.value = {name: '', description: ''}
  }
  dialogVisible.value = true
}

async function save() {
  if (editId.value) {
    await suiteApi.update(editId.value, form.value)
    ElMessage.success('已更新')
  } else {
    await suiteApi.save(form.value)
    ElMessage.success('已创建')
  }
  dialogVisible.value = false
  await fetchList()
}

async function remove(id) {
  await suiteApi.delete(id)
  ElMessage.success('已删除')
  await fetchList()
}
</script>