<template>
  <div class="page-container">
    <div class="page-header">
      <h2>测试套件</h2>
      <el-button type="primary" @click="openDialog(null)">
        新建套件
      </el-button>
    </div>
    <el-skeleton :rows="5" animated v-if="loading" />
    <el-empty v-else-if="list.length === 0" description="暂无测试套件" />
    <el-table v-else :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column prop="name" label="名称" min-width="200"/>
      <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip/>
      <el-table-column prop="createTime" label="创建时间" width="170" :formatter="formatDate"/>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="router.push('/suites/' + row.id)">详情</el-button>
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑套件' : '新建套件'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
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
import {ElMessage, ElMessageBox} from 'element-plus'
import {useRouter} from 'vue-router'
import { formatDate } from '../utils/format'

const router = useRouter()
const list = ref([])
const loading = ref(true)
const dialogVisible = ref(false)
const editId = ref(null)
const form = ref({name: '', description: ''})
const formRef = ref(null)
const rules = {
  name: [{required: true, message: '名称不能为空', trigger: 'blur'}]
}

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
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (editId.value) {
    try {
      await suiteApi.update(editId.value, form.value)
      ElMessage.success('已更新')
    } catch (e) {
      ElMessage.error('更新失败')
    }
  } else {
    try {
      await suiteApi.save(form.value)
      ElMessage.success('已创建')
    } catch (e) {
      ElMessage.error('创建失败')
    }
  }
  dialogVisible.value = false
  await fetchList()
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('确定要删除这个套件吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await suiteApi.delete(id)
    ElMessage.success('已删除')
  } catch {
  }
  await fetchList()
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