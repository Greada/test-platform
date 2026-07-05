<template>
  <div class="diff-viewer">
    <div v-if="!result" style="text-align:center;padding:30px;color:#999">
      <el-icon class="is-loading" :size="24"><i class="el-icon-loading"></i></el-icon>
      <p style="margin-top:10px">正在分析差异...</p>
    </div>

    <template v-else>
      <div v-if="result.match && result.differences.length === 0" style="text-align:center;padding:30px">
        <el-icon :size="40" color="#67c23a"><i class="el-icon-success"></i></el-icon>
        <p style="color:#67c23a;margin-top:10px;font-size:16px">完全匹配</p>
      </div>

      <div v-else class="diff-container">
        <div class="diff-section">
          <div class="diff-header">预期结果</div>
          <pre class="diff-content">{{ formatJson(expectedJson) }}</pre>
        </div>
        <div class="diff-section">
          <div class="diff-header">实际结果</div>
          <pre class="diff-content">{{ formatJson(actualJson) }}</pre>
        </div>
      </div>

      <div v-if="result.differences.length > 0" style="margin-top:16px">
        <div style="font-weight:bold;margin-bottom:8px;color:#f56c6c">
          差异明细 ({{ result.differences.length }} 处)
        </div>
        <el-table :data="result.differences" border size="small" max-height="300">
          <el-table-column prop="fieldPath" label="字段路径" width="200"/>
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 'MISMATCH' ? 'danger' : row.type === 'MISSING' ? 'warning' : 'info'"
                      size="small">
                {{ row.type === 'MISMATCH' ? '值不匹配' : row.type === 'MISSING' ? '缺少字段' : '多余字段' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="expectedValue" label="预期值" min-width="150"/>
          <el-table-column prop="actualValue" label="实际值" min-width="150"/>
        </el-table>
      </div>

      <div v-if="result.suggestedExpected" style="margin-top:16px;padding:12px;background:#f0f9eb;border-radius:6px">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div>
            <el-icon color="#67c23a"><i class="el-icon-lightbulb"></i></el-icon>
            <span style="margin-left:6px;font-weight:bold;color:#67c23a">修复建议</span>
            <el-button size="small" type="success" style="margin-left:12px" @click="applyFix">一键应用</el-button>
          </div>
        </div>
        <pre
            style="margin-top:8px;background:#fff;padding:8px;border-radius:4px;font-size:13px;overflow:auto;white-space:pre-wrap;word-break:break-all">{{
            formatJson(result.suggestedExpected)
          }}</pre>
      </div>
    </template>
  </div>
</template>

<script setup>
import { formatJson } from '../utils/format'

const props = defineProps({
  result: {type: Object, default: null},
  expectedJson: {type: String, default: ''},
  actualJson: {type: String, default: ''}
})

const emit = defineEmits(['applyFix'])

function deepUnescape(obj) {
  if (typeof obj === 'string') {
    try {
      const inner = JSON.parse(obj)
      if (typeof inner === 'object' && inner !== null) {
        return deepUnescape(inner)
      }
    } catch {
    }
    return obj
  }
  if (Array.isArray(obj)) {
    return obj.map(deepUnescape)
  }
  if (obj && typeof obj === 'object') {
    const result = {}
    for (const key of Object.keys(obj)) {
      result[key] = deepUnescape(obj[key])
    }
    return result
  }
  return obj
}

function applyFix() {
  emit('applyFix', props.result?.suggestedExpected)
}
</script>

<style scoped>
.diff-container {
  display: flex;
  gap: 12px;
}

.diff-section {
  flex: 1;
  min-height: 0;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
}

.diff-header {
  background: #f5f7fa;
  padding: 8px 12px;
  font-weight: bold;
  font-size: 13px;
  border-bottom: 1px solid #dcdfe6;
}

.diff-content {
  margin: 0;
  padding: 12px;
  font-size: 13px;
  line-height: 1.6;
  max-height: 600px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  background: #fafafa;
}
</style>