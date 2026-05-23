import {createRouter, createWebHistory} from 'vue-router'
import TestCaseList from '../views/TestCaseList.vue'
import TestCaseEdit from '../views/TestCaseEdit.vue'
import ExecutionList from '../views/ExecutionList.vue'

const routes = [
    {path: '/', name: 'TestCaseList', component: TestCaseList},
    {path: '/edit/:id?', name: 'TestCaseEdit', component: TestCaseEdit},
    {path: '/executions', name: 'ExecutionList', component: ExecutionList}
]
const router = createRouter({
    history: createWebHistory(),
    routes
})
export default router