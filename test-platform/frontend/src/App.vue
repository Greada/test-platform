<template>
  <el-container class="app-container">
    <el-aside width="220px" class="app-aside">
      <div class="logo">
        <h1>测试平台</h1>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        class="app-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
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
        <el-menu-item index="/executions">
          <el-icon><Clock /></el-icon>
          <span>执行记录</span>
        </el-menu-item>
        <el-menu-item index="/docs">
          <el-icon><Reading /></el-icon>
          <span>文档</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import {
  Document,
  List,
  TrendCharts,
  Clock,
  Reading,
} from '@element-plus/icons-vue'
</script>

<style scoped>
.app-container {
  min-height: 100vh;
}

.app-aside {
  background-color: #304156;
  overflow-x: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background-color: #2b3a4d;
  border-bottom: 1px solid #3a4a5e;
}

.logo h1 {
  font-size: 18px;
  margin: 0;
  font-weight: 600;
  letter-spacing: 2px;
}

.app-menu {
  border-right: none;
}

.app-main {
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
  padding: 20px;
}
</style>