import {createRouter, createWebHistory} from 'vue-router'

const TestCaseList = () => import('../views/TestCaseList.vue')
const ExecutionList = () => import('../views/ExecutionList.vue')
const DocView = () => import('../views/DocView.vue')
const TestSuiteList = () => import('../views/TestSuiteList.vue')
const TestSuiteDetail = () => import('../views/TestSuiteDetail.vue')
const ExecutionReportList = () => import('../views/ExecutionReportList.vue')
const ExecutionReportDetail = () => import('../views/ExecutionReportDetail.vue')
const CiStatus = () => import('../views/CiStatus.vue')
const Login = () => import('../views/Login.vue')

const routes = [
    {path: '/login', name: 'Login', component: Login},
    {path: '/', name: 'TestCaseList', component: TestCaseList},
    {path: '/executions', name: 'ExecutionList', component: ExecutionList},
    {path: '/docs', name: 'DocView', component: DocView},
    {path: '/suites', name: 'TestSuiteList', component: TestSuiteList},
    {path: '/suites/:id', name: 'TestSuiteDetail', component: TestSuiteDetail},
    {path: '/reports', name: 'ExecutionReportList', component: ExecutionReportList},
    {path: '/reports/:id', name: 'ExecutionReportDetail', component: ExecutionReportDetail},
    {path: '/ci', name: 'CiStatus', component: CiStatus},
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
