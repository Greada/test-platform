<template>
  <div class="page-container">
    <div class="main-layout">
      <!-- 左侧分类树 -->
      <div class="side-panel">
        <CategoryTree ref="categoryTreeRef" @category-change="handleCategoryChange" @manage="openCategoryManage" />
      </div>

      <!-- 右侧主内容 -->
      <div class="content-panel">
        <!-- 列表/详情模式 -->
        <template v-if="!editingCase && !creating">
          <div class="page-header">
            <div class="header-left">
              <h2>测试用例</h2>
              <span v-if="selectedCategory" class="category-tag">
                当前分类：{{ selectedCategory.name }}
              </span>
            </div>
            <div class="header-actions">
              <el-input v-model="searchQuery" placeholder="搜索用例编号或名称" clearable
                style="width: 200px; margin-right: 10px" prefix-icon="Search" />
              <el-button @click="openCategoryManage">分类管理</el-button>
              <el-button @click="openImportDialog">导入 OpenAPI</el-button>
              <el-button type="primary" @click="handleCreate">
                新建用例
              </el-button>
            </div>
          </div>

          <el-skeleton :rows="5" animated v-if="loading" />
          <el-empty v-else-if="filteredAndSearchedList.length === 0"
            :description="selectedCategory ? '该分类暂无用例' : '暂无测试用例，点击新建用例开始'" />
          <el-table v-else :data="filteredAndSearchedList" border stripe>
            <el-table-column prop="testNo" label="编号" width="100" />
            <el-table-column prop="name" label="名称" />
            <el-table-column label="分类" width="120">
              <template #default="{ row }">
                <span>{{ getCategoryName(row.categoryId) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="requestMethod" label="方法" width="80" />
            <el-table-column prop="requestUrl" label="地址" min-width="250" show-overflow-tooltip />
            <el-table-column prop="expectedResult" label="预期结果" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="230">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="handleEdit(row)">
                  编辑
                </el-button>
                <el-button size="small" type="success" link @click="execute(row.id)"
                  :loading="loadingId === row.id">
                  执行
                </el-button>
                <el-button size="small" link @click="showHistory(row)">
                  历史
                </el-button>
                <el-button size="small" type="danger" link @click="remove(row.id)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <!-- 编辑/新建面板 -->
        <TestCaseEditPanel v-else :edit-id="editingCase ? editingCase.id : null"
          :categories="allCategories" :selected-category-id="editingCase?.categoryId"
          :init-data="editingCase" @save="handleEditSaved" @cancel="cancelEdit" />
      </div>
    </div>

    <!-- 分类管理弹窗 -->
    <CategoryDialog ref="categoryDialogRef" @saved="handleCategorySaved" />

    <!-- 历史执行记录弹窗 -->
    <el-dialog v-model="historyVisible" title="执行历史记录" width="800px">
      <el-table :data="historyRecords" border stripe v-loading="historyLoading">
        <el-table-column prop="status" label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PASS' ? 'success' : row.status === 'FAIL' ? 'danger' : 'warning'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="executeDuration" label="耗时(ms)" width="100"/>
        <el-table-column prop="actualResult" label="实际结果" min-width="200" show-overflow-tooltip/>
        <el-table-column prop="executeTime" label="执行时间" width="180" :formatter="formatDate"/>
        <el-table-column label="日志" width="60">
          <template #default="{ row }">
            <el-button size="small" @click="showHistoryLog(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!historyLoading && historyRecords.length === 0" description="暂未执行过" />
    </el-dialog>

    <el-dialog v-model="historyLogVisible" title="执行日志" width="800px">
      <template v-if="historyLogRow">
        <div class="log-section">
          <div class="log-section-title">HTTP 请求</div>
          <pre class="log-pre"><code>{{ historyLogRow.requestDetail || '-' }}</code></pre>
        </div>
        <div class="log-section">
          <div class="log-section-title">HTTP 响应</div>
          <pre class="log-pre"><code>{{ historyLogRow.responseDetail || '-' }}</code></pre>
        </div>
        <div class="log-section">
          <div class="log-section-title">实际结果</div>
          <pre class="log-pre"><code>{{ formatJson(historyLogRow.actualResult) }}</code></pre>
        </div>
      </template>
    </el-dialog>

    <!-- 导入 OpenAPI 对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入 OpenAPI" width="800px">
      <div style="margin-bottom:12px">
        <p style="margin:0 0 8px;color:#606266;font-size:13px">粘贴 OpenAPI JSON 内容（支持 Swagger 2.0 / OpenAPI 3.0）</p>
        <el-input v-model="importJson" type="textarea" :rows="6" placeholder="粘贴 OpenAPI JSON..." />
      </div>
      <div style="text-align:right;margin-bottom:12px">
        <el-button :loading="importLoading" @click="parseOpenApi">解析预览</el-button>
      </div>
      <el-table v-if="importPreview.length > 0" :data="importPreview" border stripe max-height="400">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="requestMethod" label="方法" width="80" />
        <el-table-column prop="requestUrl" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="name" label="名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="expectedResult" label="预期结果" min-width="150" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="importPreview.length === 0" @click="confirmImport">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi, testCaseApi } from '../api'
import CategoryTree from '../components/CategoryTree.vue'
import CategoryDialog from '../components/CategoryDialog.vue'
import TestCaseEditPanel from './TestCaseEdit.vue'
import { formatDate } from '../utils/format'

const list = ref([])
const loading = ref(true)
const loadingId = ref(null)
const searchQuery = ref('')
const selectedCategory = ref(null)
const allCategories = ref([])
const categoryTreeRef = ref(null)
const categoryDialogRef = ref(null)
const editingCase = ref(null)
const creating = ref(false)
const route = useRoute()
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyRecords = ref([])
const historyLogVisible = ref(false)
const historyLogRow = ref(null)

const importDialogVisible = ref(false)
const importJson = ref('')
const importLoading = ref(false)
const importPreview = ref([])

const filteredAndSearchedList = computed(() => {
  let result = list.value
  if (selectedCategory.value) {
    result = result.filter(item => item.categoryId === selectedCategory.value.id)
  }
  const query = searchQuery.value.toLowerCase().trim()
  if (!query) return result
  return result.filter(item =>
    (item.testNo && item.testNo.toLowerCase().includes(query)) ||
    (item.name && item.name.toLowerCase().includes(query))
  )
})

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([fetchList(), fetchCategories()])

    if (route.query.editId && route.query.fix) {
      const suggested = localStorage.getItem('fix_expected')
      localStorage.removeItem('fix_expected')
      const res = await api.get('/testcases/' + route.query.editId)
      if (res.data.data) {
        if (suggested) res.data.data.expectedResult = suggested
        editingCase.value = res.data.data
        creating.value = false
      }
    }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
})

async function fetchList() {
  const res = await api.get('/testcases')
  list.value = res.data.data || []
}

function getCategoryName(id) {
  if (!id) return '-'
  const cat = allCategories.value.find(c => c.id === id)
  return cat ? cat.name : '-'
}

async function fetchCategories() {
  try {
    const res = await categoryApi.list()
    allCategories.value = res.data.data || []
  } catch {
    // ignore
  }
}

async function handleCategoryChange(category) {
  selectedCategory.value = category
}

function openCategoryManage() {
  if (!categoryDialogRef.value) return
  categoryDialogRef.value.openNew()
}

function handleCreate() {
  editingCase.value = null
  creating.value = true
}

function handleEdit(row) {
  editingCase.value = row
  creating.value = false
}

async function handleEditSaved() {
  editingCase.value = null
  creating.value = false
  selectedCategory.value = null
  await fetchList()
}

function cancelEdit() {
  editingCase.value = null
  creating.value = false
}

async function handleCategorySaved() {
  await fetchCategories()
  if (categoryTreeRef.value) {
    await categoryTreeRef.value.fetchTree()
  }
}

async function execute(id) {
  loadingId.value = id
  try {
    const res = await api.post(`/execution-records/${id}/execute`)
    if (res.data.code === 200) {
      ElMessage.success('执行完成')
      await fetchList()
    } else {
      ElMessage.error(res.data.message)
    }
  } catch (e) {
    ElMessage.error('执行失败')
  }
  loadingId.value = null
}

async function showHistory(row) {
  historyRecords.value = []
  historyVisible.value = true
  historyLoading.value = true
  try {
    const res = await api.get('/execution-records', { params: { testCaseId: row.id } })
    historyRecords.value = res.data.data || []
  } catch {
    ElMessage.error('加载执行记录失败')
  } finally {
    historyLoading.value = false
  }
}

function showHistoryLog(row) {
  historyLogRow.value = row
  historyLogVisible.value = true
}

function formatJson(text) {
  if (!text) return '-'
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return text
  }
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('确定要删除这个用例吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.delete('/testcases/' + id)
    ElMessage.success('已删除')
    await fetchList()
  } catch {
  }
}

function openImportDialog() {
  importJson.value = ''
  importPreview.value = []
  importDialogVisible.value = true
}

async function parseOpenApi() {
  if (!importJson.value.trim()) {
    ElMessage.warning('请先粘贴 OpenAPI JSON')
    return
  }
  importLoading.value = true
  try {
    const res = await testCaseApi.importOpenapi({ openapi: importJson.value })
    if (res.data.code === 200) {
      importPreview.value = res.data.data || []
      if (importPreview.value.length === 0) {
        ElMessage.info('未解析到任何 API 端点')
      } else {
        ElMessage.success(`解析到 ${importPreview.value.length} 个端点`)
      }
    } else {
      ElMessage.error(res.data.message || '解析失败')
    }
  } catch (e) {
    ElMessage.error('解析失败: ' + (e.response?.data?.message || e.message))
  } finally {
    importLoading.value = false
  }
}

async function confirmImport() {
  importLoading.value = true
  try {
    const res = await testCaseApi.batchSave(importPreview.value)
    if (res.data.code === 200) {
      ElMessage.success(`成功导入 ${importPreview.value.length} 个用例`)
      importDialogVisible.value = false
      importPreview.value = []
      importJson.value = ''
      await fetchList()
    } else {
      ElMessage.error(res.data.message || '导入失败')
    }
  } catch (e) {
    ElMessage.error('导入失败: ' + (e.response?.data?.message || e.message))
  } finally {
    importLoading.value = false
  }
}
</script>

<style scoped>
.page-container {
  height: calc(100vh - 60px);
  padding: 0;
  background: transparent;
  box-shadow: none;
  border-radius: 0;
  overflow: hidden;
}

.main-layout {
  display: flex;
  height: 100%;
}

.side-panel {
  width: 220px;
  min-width: 220px;
  border-right: 1px solid #ebeef5;
  background-color: #fff;
  overflow-y: auto;
}

.content-panel {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f0f2f5;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
  font-weight: 600;
}

.category-tag {
  font-size: 13px;
  color: #409eff;
  background-color: #ecf5ff;
  padding: 2px 10px;
  border-radius: 4px;
}

.header-actions {
  display: flex;
  align-items: center;
}

.log-section {
  margin-bottom: 16px;
}

.log-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
}

.log-pre {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 6px;
  overflow: auto;
  max-height: 300px;
  font-size: 13px;
  line-height: 1.5;
  margin: 0;
}
</style>
