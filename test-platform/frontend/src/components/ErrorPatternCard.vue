<template>
  <el-card>
    <template #header>
      <span>错误模式分析</span>
    </template>
    <div v-if="!items || items.length === 0" style="text-align:center;color:#999;padding:16px">
      暂无数据
    </div>
    <template v-else>
      <div v-if="worstEndpoint" style="margin-bottom:12px;padding:8px 12px;background:#fef0f0;border-radius:4px;font-size:13px;color:#f56c6c">
        <el-icon style="vertical-align:-2px"><i class="el-icon-warning-outline"></i></el-icon>
        通过率最低端点：{{ worstEndpoint }}
      </div>
      <el-table :data="items" border size="small" max-height="400">
        <el-table-column label="URL" prop="requestUrl" min-width="300" show-overflow-tooltip/>
        <el-table-column label="方法" prop="requestMethod" width="80"/>
        <el-table-column label="总数" prop="total" width="60"/>
        <el-table-column label="通过" prop="pass" width="60"/>
        <el-table-column label="失败" prop="fail" width="60"/>
        <el-table-column label="错误" prop="error" width="60"/>
        <el-table-column label="通过率" width="100">
          <template #default="{ row }">
            <el-tag
              :type="parseFloat(row.passRate) >= 80 ? 'success' : parseFloat(row.passRate) >= 50 ? 'warning' : 'danger'"
              size="small">
              {{ row.passRate }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </el-card>
</template>

<script setup>
defineProps({
  items: {type: Array, default: () => []},
  worstEndpoint: {type: String, default: ''}
})
</script>
