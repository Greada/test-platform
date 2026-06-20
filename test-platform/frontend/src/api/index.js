import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
    baseURL: '/api',
    timeout: 30000
})

api.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

api.interceptors.response.use(
    response => response,
    error => {
        if (error.response?.status === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('user')
            window.location.href = '/login'
            return Promise.reject(error)
        }
        const msg = error.response?.data?.message || error.message || '请求失败'
        ElMessage.error(msg)
        return Promise.reject(error)
    }
)

export default api

// ===== test case =====
export const testCaseApi = {
    list: (params) => api.get('/testcases', {params}),
    get: (id) => api.get(`/testcases/${id}`),
    save: (data) => api.post('/testcases', data),
    update: (id, data) => api.put(`/testcases/${id}`, data),
    delete: (id) => api.delete(`/testcases/${id}`),
    importOpenapi: (data) => api.post('/testcases/import-openapi', data),
    batchSave: (cases) => api.post('/testcases/batch-save', cases)
}

// ===== suite =====
export const suiteApi = {
    list: () => api.get('/test-suites'),
    get: (id) => api.get(`/test-suites/${id}`),
    save: (data) => api.post('/test-suites', data),
    update: (id, data) => api.put(`/test-suites/${id}`, data),
    delete: (id) => api.delete(`/test-suites/${id}`),
    listCases: (id) => api.get(`/test-suites/${id}/cases`),
    addCase: (suiteId, caseId) => api.post(`/test-suites/${suiteId}/cases`, {caseId}),
    removeCase: (suiteId, caseId) => api.delete(`/test-suites/${suiteId}/cases/${caseId}`),
    execute: (suiteId) => api.post(`/test-suites/${suiteId}/execute`),
    batchAddCases: (suiteId, caseIds) => api.post(`/test-suites/${suiteId}/cases/batch`, {caseIds})
}

// ===== reports =====
export const reportApi = {
    list: (suiteId) => api.get('/execution-reports', {params: {suiteId}}),
    get: (id) => api.get(`/execution-reports/${id}`),
    getDetails: (id) => api.get(`/execution-reports/${id}/details`),
    errorPatterns: (id) => api.get(`/execution-reports/${id}/error-patterns`)
}

export const diffApi = {
    get: (recordId) => api.get(`/execution-records/${recordId}/diff`)
}

// ===== category =====
export const categoryApi = {
    tree: () => api.get('/categories/tree'),
    list: () => api.get('/categories'),
    save: (data) => api.post('/categories', data),
    update: (data) => api.put('/categories', data),
    delete: (id) => api.delete(`/categories/${id}`)
}

// ===== ai =====
export const aiApi = {
    expected: (data) => api.post('/ai/expected', data)
}
