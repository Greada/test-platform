import { ElMessageBox, ElMessage } from 'element-plus'

export function useConfirmDelete(deleteFn, refreshFn) {
  return async function(id) {
    try {
      await ElMessageBox.confirm('确定要删除吗？', '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await deleteFn(id)
      ElMessage.success('已删除')
      if (refreshFn) await refreshFn()
    } catch (e) {
      if (e !== 'cancel' && e?.message !== 'cancel') {
        ElMessage.error('删除失败: ' + (e.response?.data?.message || e.message))
      }
    }
  }
}
