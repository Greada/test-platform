<template>
  <div style="padding: 20px; max-width: 700px">
    <h2>{{ isEdit ? '编辑用例' : '新建用例' }}</h2>
    <el-form :model="form" label-width="100px">
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
          <el-option label="DELETE" value="DELETE"/>
        </el-select>
      </el-form-item>
      <el-form-item label="请求头">
        <el-input v-model="form.requestHeaders" type="textarea" :rows="3"
                  placeholder='{"Content-Type":"application/json"}'/>
      </el-form-item>
      <el-form-item label="请求参数">
        <el-input v-model="form.requestParams" type="textarea" :rows="3"/>
      </el-form-item>
      <el-form-item label="预期结果">
        <el-input v-model="form.expectedResult" type="textarea" :rows="3"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存</el-button>
        <el-button @click="$router.push('/')">返回</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import api from '../api'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const form = ref({
  name: '',
  requestUrl: '',
  requestMethod: 'GET',
  requestHeaders: '',
  requestParams: '',
  expectedResult: ''
})
onMounted(async () => {
  if (isEdit.value) {
    const res = await api.get('/testcases/' + route.params.id)
    if (res.data.data) {
      form.value = res.data.data
    }
  }
})

async function save() {
  if (isEdit.value) {
    await api.put('/testcases/' + route.params.id, form.value)
  } else {
    await api.post('/testcases', form.value)
  }
  ElMessage.success('保存成功')
  router.push('/')
}
</script>