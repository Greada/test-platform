<template>
  <div class="edit-panel">
    <div class="edit-header">
      <el-button text @click="$emit('cancel')">
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>
      <span class="edit-title">{{ isEdit ? '编辑用例' : '新建用例' }}</span>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="edit-form">
      <el-form-item label="编号" prop="testNo">
        <el-input v-model="form.testNo" placeholder="例如：TC-001" />
      </el-form-item>
      <el-form-item label="名称">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="所属分类">
        <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width: 100%">
          <el-option label="无分类" :value="null" />
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="请求地址">
        <el-input v-model="form.requestUrl" placeholder="https://..." />
      </el-form-item>
      <el-form-item label="请求方法">
        <el-select v-model="form.requestMethod">
          <el-option label="GET" value="GET" />
          <el-option label="POST" value="POST" />
          <el-option label="PUT" value="PUT" />
          <el-option label="PATCH" value="PATCH" />
          <el-option label="DELETE" value="DELETE" />
        </el-select>
      </el-form-item>
      <el-form-item label="请求头">
        <el-input v-model="form.requestHeaders" type="textarea" :rows="3"
          placeholder='{"Content-Type":"application/json"}' />
        <el-button size="small" @click="validateJson('requestHeaders')">验证 JSON</el-button>
      </el-form-item>
      <el-form-item label="请求参数">
        <el-input v-model="form.requestParams" type="textarea" :rows="3" />
        <el-button size="small" @click="validateJson('requestParams')">验证 JSON</el-button>
      </el-form-item>
      <el-form-item label="预期结果" prop="expectedResult">
        <div class="expected-row">
          <el-input v-model="form.expectedResult" type="textarea" :rows="3" />
          <div class="expected-actions">
            <el-button size="small" @click="validateJson('expectedResult')">验证 JSON</el-button>
            <el-button size="small" type="warning" :loading="aiLoading" @click="generateExpected">
              <el-icon style="margin-right:4px"><MagicStick /></el-icon>
              AI 生成
            </el-button>
          </div>
        </div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存</el-button>
        <el-button @click="$emit('cancel')">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api'
import { aiApi } from '../api'
import { ElMessage } from 'element-plus'
import { ArrowLeft, MagicStick } from '@element-plus/icons-vue'

const props = defineProps({
  editId: { type: [Number, String], default: null },
  categories: { type: Array, default: () => [] },
  selectedCategoryId: { type: [Number, String], default: null },
  initData: { type: Object, default: null }
})

const emit = defineEmits(['save', 'cancel'])
const formRef = ref(null)
const loading = ref(false)
const aiLoading = ref(false)

const isEdit = computed(() => !!props.editId)

const form = ref({
  testNo: '',
  name: '',
  categoryId: null,
  requestUrl: '',
  requestMethod: 'GET',
  requestHeaders: '',
  requestParams: '',
  expectedResult: ''
})

const rules = {
  testNo: [{ required: true, message: '编号不能为空', trigger: 'blur' }],
  name: [
    { required: true, message: '名称不能为空', trigger: 'blur' },
    { min: 1, max: 100, message: '名称长度在 1-100 个字符', trigger: 'blur' }
  ],
  expectedResult: [{ required: true, message: '预期结果不能为空', trigger: 'blur' }]
}

onMounted(async () => {
  if (!isEdit.value) return
  if (props.initData) {
    form.value = { ...props.initData }
    return
  }
  loading.value = true
  try {
    const res = await api.get('/testcases/' + props.editId)
    if (res.data.data) {
      form.value = res.data.data
    }
  } catch (e) {
    ElMessage.error('加载用例失败')
  } finally {
    loading.value = false
  }
})

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (isEdit.value) {
      await api.put('/testcases/' + props.editId, form.value)
    } else {
      await api.post('/testcases', form.value)
    }
    ElMessage.success('保存成功')
    emit('save')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

function validateJson(field) {
  const value = form.value[field]
  if (!value) {
    ElMessage.warning('请先输入内容')
    return
  }
  try {
    JSON.parse(value)
    ElMessage.success('JSON 格式正确')
  } catch (e) {
    ElMessage.error('JSON 格式错误: ' + e.message)
  }
}

async function generateExpected() {
  if (!form.value.requestUrl) {
    ElMessage.warning('请先填写请求地址')
    return
  }
  aiLoading.value = true
  try {
    const res = await aiApi.expected({
      requestUrl: form.value.requestUrl,
      requestMethod: form.value.requestMethod,
      requestHeaders: form.value.requestHeaders,
      requestParams: form.value.requestParams
    })
    if (res.data.code === 200 && res.data.data) {
      let result = res.data.data
      try {
        result = JSON.stringify(JSON.parse(result), null, 2)
      } catch {}
      form.value.expectedResult = result
      ElMessage.success('AI 生成成功')
    } else {
      ElMessage.error(res.data.message || '生成失败')
    }
  } catch (e) {
    ElMessage.error('AI 生成失败: ' + (e.response?.data?.message || e.message))
  } finally {
    aiLoading.value = false
  }
}
</script>

<style scoped>
.edit-panel {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 20px;
}

.edit-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.edit-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.edit-form {
  max-width: 700px;
}

.expected-row {
  width: 100%;
}

.expected-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}
</style>
