import {createRouter, createWebHistory} from 'vue-router'
import TestCaseList from '../views/TestCaseList.vue'
import TestCaseEdit from '../views/TestCaseEdit.vue'
import ExecutionList from '../views/ExecutionList.vue'
import DocView from '../views/DocView.vue'

const routes = [
    {path: '/', name: 'TestCaseList', component: TestCaseList},
    {path: '/edit/:id?', name: 'TestCaseEdit', component: TestCaseEdit},
    {path: '/executions', name: 'ExecutionList', component: ExecutionList},
    {path: '/docs', name: 'DocView', component: DocView}
]
const router = createRouter({
    history: createWebHistory(),
    routes
})
export default router