<template>
  <div>
    <el-menu mode="horizontal" :ellipsis="false" router>
      <el-menu-item index="/">测试用例</el-menu-item>
      <el-menu-item index="/executions">执行记录</el-menu-item>
      <el-menu-item index="/docs">文档</el-menu-item>
    </el-menu>
    <router-view/>
  </div>
</template>
