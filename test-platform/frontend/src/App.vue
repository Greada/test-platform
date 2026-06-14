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
        <el-menu-item index="/docs">
          <el-icon><Reading /></el-icon>
          <span>文档</span>
        </el-menu-item>
      </el-menu>
    </div>
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
  Reading,
} from '@element-plus/icons-vue'
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

.app-main {
  background-color: #f0f2f5;
  padding: 0;
  overflow: hidden;
}
</style>
