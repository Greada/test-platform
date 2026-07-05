<template>
    <el-container class="app-container">
    <div class="top-nav">
      <div class="nav-logo">
        <h1>测试平台</h1>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        mode="horizontal"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        style="border-bottom: none; flex: 1"
      >
        <el-menu-item index="/">
          <el-icon><Document /></el-icon>
          <span>测试用例</span>
        </el-menu-item>
        <el-menu-item index="/suites">
          <el-icon><List /></el-icon>
          <span>测试套件</span>
        </el-menu-item>
        <el-menu-item index="/reports">
          <el-icon><TrendCharts /></el-icon>
          <span>执行报告</span>
        </el-menu-item>
        <el-menu-item index="/ci">
          <el-icon><Monitor /></el-icon>
          <span>CI 状态</span>
        </el-menu-item>
        <el-menu-item index="/docs">
          <el-icon><Reading /></el-icon>
          <span>文档</span>
        </el-menu-item>
      </el-menu>
      <div class="nav-user" v-if="user">
        <el-dropdown trigger="click">
          <span class="nav-user-name">
            <el-icon><UserFilled /></el-icon>
            {{ user.displayName || user.username }}
          </span>
          <template #dropdown>
            <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
          </template>
        </el-dropdown>
      </div>
    </div>
    <el-container>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Document,
  List,
  TrendCharts,
  Reading,
  Monitor,
  UserFilled,
} from '@element-plus/icons-vue'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  user.value = null
  router.push('/login')
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  flex-direction: column;
}

.top-nav {
  display: flex;
  align-items: center;
  background-color: #304156;
}

.nav-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background-color: #2b3a4d;
  min-width: 180px;
  height: 60px;
  border-bottom: 1px solid #3a4a5e;
}

.nav-logo h1 {
  font-size: 18px;
  margin: 0;
  font-weight: 600;
  letter-spacing: 2px;
}

.nav-user {
  display: flex;
  align-items: center;
  padding: 0 20px;
  cursor: pointer;
}

.nav-user-name {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #bfcbd9;
  font-size: 14px;
}

.nav-user-name:hover {
  color: #fff;
}

.app-main {
  background-color: #f0f2f5;
  padding: 0;
  overflow: hidden;
}
</style>
