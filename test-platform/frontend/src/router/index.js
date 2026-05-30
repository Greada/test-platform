import {createRouter, createWebHistory} from 'vue-router'
import TestCaseList from '../views/TestCaseList.vue'
import TestCaseEdit from '../views/TestCaseEdit.vue'
import ExecutionList from '../views/ExecutionList.vue'
import DocView from '../views/DocView.vue'
import TestSuiteList from '../views/TestSuiteList.vue'
import TestSuiteDetail from '../views/TestSuiteDetail.vue'
import ExecutionReportList from '../views/ExecutionReportList.vue'
import ExecutionReportDetail from '../views/ExecutionReportDetail.vue'

const routes = [
    {path: '/', name: 'TestCaseList', component: TestCaseList},
    {path: '/edit/:id?', name: 'TestCaseEdit', component: TestCaseEdit},
    {path: '/executions', name: 'ExecutionList', component: ExecutionList},
    {path: '/docs', name: 'DocView', component: DocView},
    {path: '/suites', name: 'TestSuiteList', component: TestSuiteList},
    {path: '/suites/:id', name: 'TestSuiteDetail', component: TestSuiteDetail},
    {path: '/reports', name: 'ExecutionReportList', component: ExecutionReportList},
    {path: '/reports/:id', name: 'ExecutionReportDetail', component: ExecutionReportDetail},
]
const router = createRouter({
    history: createWebHistory(),
    routes
})
export default router