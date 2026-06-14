<template>
  <el-dialog v-model="visible" :title="editingId ? '编辑分类' : '新建分类'" width="500px"
    :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="分类名称" />
      </el-form-item>
      <el-form-item label="父级" prop="parentId">
        <el-select v-model="form.parentId" placeholder="选择父级（选全部表示顶级）" style="width: 100%">
          <el-option label="全部（顶级分类）" :value="0" />
          <el-option v-for="c in allTreeNodes" :key="c.id"
            :label="getIndent(c.level) + c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="层级">
        <span>{{ form._level || 1 }}</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
      <el-button v-if="editingId" type="danger" @click="remove">删除</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { categoryApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const visible = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const allTreeNodes = ref([])

const form = ref({
  id: null,
  name: '',
  parentId: 0,
  _level: 1
})

const rules = {
  name: [{ required: true, message: '名称不能为空', trigger: 'blur' }]
}

onMounted(async () => {
  await fetchData()
})

async function fetchData() {
  try {
    const res = await categoryApi.tree()
    const roots = res.data.data || []
    allTreeNodes.value = flattenTree(roots)
  } catch {
    ElMessage.error('加载分类列表失败')
  }
}

function flattenTree(nodes) {
  const result = []
  for (const node of nodes) {
    result.push(node)
    if (node.children && node.children.length > 0) {
      result.push(...flattenTree(node.children))
    }
  }
  return result
}

function getIndent(level) {
  if (!level || level <= 1) return ''
  return '  '.repeat(level - 1) + '└ '
}

watch(() => form.value.parentId, (val) => {
  if (val == null || val === 0) {
    form.value._level = 1
  } else {
    const parent = allTreeNodes.value.find(c => c.id === val)
    if (parent) {
      form.value._level = parent.level + 1
    }
  }
})

function openNew() {
  editingId.value = null
  form.value = { id: null, name: '', parentId: 0, _level: 1 }
  visible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.value = {
    id: row.id,
    name: row.name,
    parentId: row.parentId,
    _level: row.level
  }
  visible.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const payload = {
      name: form.value.name,
      parentId: form.value.parentId,
      level: form.value._level
    }
    if (editingId.value) {
      payload.id = editingId.value
      await categoryApi.update(payload)
      ElMessage.success('已更新')
    } else {
      await categoryApi.save(payload)
      ElMessage.success('已创建')
    }
    visible.value = false
    emit('saved')
    await fetchData()
  } catch (e) {
    if (e.response && e.response.data && e.response.data.message) {
      ElMessage.error(e.response.data.message)
    } else {
      ElMessage.error('保存失败')
    }
  }
}

async function remove() {
  if (!editingId.value) return
  try {
    await ElMessageBox.confirm('确定要删除这个分类吗？有子分类则无法删除', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await categoryApi.delete(editingId.value)
    ElMessage.success('已删除')
    visible.value = false
    emit('saved')
    await fetchData()
  } catch {
  }
}

const emit = defineEmits(['saved'])

defineExpose({ openNew, openEdit })
</script>
