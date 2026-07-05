<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="login-title">测试平台</h2>
      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginForm" :model="loginData" :rules="loginRules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="loginData.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginData.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">登 录</el-button>
            </el-form-item>
          </el-form>
          <div v-if="loginError" class="login-error">{{ loginError }}</div>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form ref="registerForm" :model="registerData" :rules="registerRules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="registerData.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerData.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="displayName">
              <el-input v-model="registerData.displayName" placeholder="显示名称（选填）" prefix-icon="Edit" />
            </el-form-item>
            <el-form-item>
              <el-button type="success" :loading="loading" style="width: 100%" @click="handleRegister">注 册</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api/auth'

const router = useRouter()
const tab = ref('login')
const loading = ref(false)
const loginError = ref('')
const loginForm = ref(null)
const registerForm = ref(null)

const loginData = reactive({ username: '', password: '' })
const registerData = reactive({ username: '', password: '', displayName: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await loginForm.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  loginError.value = ''
  try {
    const res = await authApi.login(loginData)
    if (res.data.code === 200) {
      localStorage.setItem('token', res.data.data.token)
      localStorage.setItem('user', JSON.stringify(res.data.data.user))
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      loginError.value = res.data.message || '登录失败'
    }
  } catch (e) {
    loginError.value = e.response?.data?.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  const valid = await registerForm.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await authApi.register(registerData)
    if (res.data.code === 200) {
      ElMessage.success('注册成功，请登录')
      tab.value = 'login'
      loginData.username = registerData.username
    } else {
      ElMessage.error(res.data.message || '注册失败')
    }
  } catch (e) {
    ElMessage.error('注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}
.login-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 30px 0;
  letter-spacing: 4px;
}
.login-error {
  color: #f56c6c;
  font-size: 13px;
  text-align: center;
  margin-top: -10px;
}
</style>
