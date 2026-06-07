import axios from 'axios'

const api = axios.create({
    baseURL: '/api'
})
export default api

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