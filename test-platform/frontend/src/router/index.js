import {createRouter, createWebHistory} from 'vue-router'
import TestCaseList from '../views/TestCaseList.vue'
import ExecutionList from '../views/ExecutionList.vue'
import DocView from '../views/DocView.vue'
import TestSuiteList from '../views/TestSuiteList.vue'
import TestSuiteDetail from '../views/TestSuiteDetail.vue'
import ExecutionReportList from '../views/ExecutionReportList.vue'
import ExecutionReportDetail from '../views/ExecutionReportDetail.vue'
import Login from '../views/Login.vue'

const routes = [
    {path: '/login', name: 'Login', component: Login},
    {path: '/', name: 'TestCaseList', component: TestCaseList},
    {path: '/executions', name: 'ExecutionList', component: ExecutionList},
    {path: '/docs', name: 'DocView', component: DocView},
    {path: '/suites', name: 'TestSuiteList', component: TestSuiteList},
    {path: '/suites/:id', name: 'TestSuiteDetail', component: TestSuiteDetail},
    {path: '/reports', name: 'ExecutionReportList', component: ExecutionReportList},
    {path: '/reports/:id', name: 'ExecutionReportDetail', component: ExecutionReportDetail},
    {path: '/:pathMatch(.*)*', name: 'NotFound', redirect: '/'},
]
const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (to.path !== '/login' && !token) {
        next('/login')
    } else if (to.path === '/login' && token) {
        next('/')
    } else {
        next()
    }
})

export default router
