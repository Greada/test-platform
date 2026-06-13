<template>
  <div class="page-container">
    <el-skeleton :rows="8" animated v-if="loading" />
    <template v-else>
    <el-page-header @back="$router.push('/')" :title="'返回用例列表'">
      <template #content>
        <span class="page-header-title">{{ isEdit ? '编辑用例' : '新建用例' }}</span>
      </template>
    </el-page-header>
    <el-form ref="formRef" :model="form" label-width="120px" style="margin-top: 20px">
      <el-form-item label="编号" prop="testNo"
                    :rules="[{ required: true, message: '编号不能为空', trigger: 'blur' }]">
        <el-input v-model="form.testNo" placeholder="例如：TC-001"/>
      </el-form-item>
      <el-form-item label="名称">
        <el-input v-model="form.name"/>
      </el-form-item>
      <el-form-item label="请求地址">
        <el-input v-model="form.requestUrl" placeholder="https://..."/>
      </el-form-item>
      <el-form-item label="请求方法">
        <el-select v-model="form.requestMethod">
          <el-option label="GET" value="GET"/>
          <el-option label="POST" value="POST"/>
          <el-option label="PUT" value="PUT"/>
          <el-option label="PATCH" value="PATCH"/>
          <el-option label="DELETE" value="DELETE"/>
        </el-select>
      </el-form-item>
       <el-form-item label="请求头">
        <el-input v-model="form.requestHeaders" type="textarea" :rows="3"
                  placeholder='{"Content-Type":"application/json"}'/>
        <el-button size="small" @click="validateJson('requestHeaders')">验证 JSON</el-button>
      </el-form-item>
      <el-form-item label="请求参数">
        <el-input v-model="form.requestParams" type="textarea" :rows="3"/>
        <el-button size="small" @click="validateJson('requestParams')">验证 JSON</el-button>
      </el-form-item>
      <el-form-item label="预期结果" prop="expectedResult"
                    :rules="[{ required: true, message: '预期结果不能为空', trigger: 'blur' }]">
        <el-input v-model="form.expectedResult" type="textarea" :rows="3"/>
        <el-button size="small" @click="validateJson('expectedResult')">验证 JSON</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存</el-button>
        <el-button @click="$router.push('/')">返回</el-button>
      </el-form-item>
    </el-form>
    </template>
  </div>
</template>
<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import api from '../api'
import {ElMessage} from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const isEdit = computed(() => !!route.params.id)
const form = ref({
  testNo: '',
  name: '',
  requestUrl: '',
  requestMethod: 'GET',
  requestHeaders: '',
  requestParams: '',
  expectedResult: ''
})
onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await api.get('/testcases/' + route.params.id)
      if (res.data.data) {
        form.value = res.data.data
      }
    } catch (e) {
      ElMessage.error('加载用例失败')
    }
  }
})

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (isEdit.value) {
      await api.put('/testcases/' + route.params.id, form.value)
    } else {
      await api.post('/testcases', form.value)
    }
    ElMessage.success('保存成功')
    router.push('/')
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
    ElMessage.success('    JSON 格式正确')
  } catch (e) {
    ElMessage.error('JSON 格式错误: ' + e.message)
  }
}

const rules = {
  testNo: [{required: true, message: '编号不能为空', trigger: 'blur'}],
  expectedResult: [{required: true, message: '预期结果不能为空', trigger: 'blur'}]
}
</script>

<style scoped>
.page-container {
  max-width: 800px;
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