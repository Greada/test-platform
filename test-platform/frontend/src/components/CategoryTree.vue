<template>
  <div class="category-tree-wrapper">
    <div class="tree-header">
      <span>分类</span>
      <el-button size="small" text @click="emitEvent('manage')">
        <el-icon><Setting /></el-icon>
      </el-button>
    </div>
    <div v-if="treeLoading" class="tree-loading">
      <el-icon class="is-loading" :size="18"><Loading /></el-icon>
    </div>
    <el-tree
      v-else
      :data="treeData"
      :props="{ children: 'children', label: 'name' }"
      node-key="id"
      highlight-current
      :expand-on-click-node="false"
      :current-node-key="selectedCategoryId"
      @node-click="handleNodeClick"
    >
      <template #default="{ node, data }">
        <span class="tree-node-label">{{ node.label }}</span>
      </template>
    </el-tree>
    <div v-if="!treeLoading && treeData.length === 0" class="tree-empty">
      暂无分类
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, defineEmits } from 'vue'
import { categoryApi } from '../api'
import { ElMessage } from 'element-plus'
import { Setting, Loading } from '@element-plus/icons-vue'

const emitEvent = defineEmits(['categoryChange'])
const treeData = ref([])
const treeLoading = ref(true)
const selectedCategoryId = ref(null)

const selectedCategory = ref(null)

onMounted(async () => {
  await fetchTree()
})

async function fetchTree() {
  treeLoading.value = true
  try {
    const res = await categoryApi.tree()
    treeData.value = res.data.data || []
    selectedCategoryId.value = null
  } catch {
    ElMessage.error('加载分类树失败')
  } finally {
    treeLoading.value = false
  }
}

function handleNodeClick(data) {
  selectedCategory.value = data
  selectedCategoryId.value = data.id
  emitEvent('categoryChange', data)
}

defineExpose({ fetchTree })
</script>

<style scoped>
.category-tree-wrapper {
  padding: 10px 0;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 10px;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.tree-loading,
.tree-empty {
  text-align: center;
  padding: 30px 0;
  color: #909399;
  font-size: 13px;
}

:deep(.el-tree-node__content) {
  height: 36px;
  padding: 0 5px;
}

.tree-node-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  flex: 1;
}

.tree-node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node-count {
  font-size: 11px;
  color: #909399;
  background-color: #f0f2f5;
  padding: 0 5px;
  border-radius: 8px;
  margin-left: 4px;
}
</style>
