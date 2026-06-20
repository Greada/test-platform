import {createApp} from "vue";
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { ElMessage } from 'element-plus'


const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.mount('#app')

app.config.errorHandler = (err, instance, info) => {
  console.error('Vue error:', err)
  ElMessage.error('页面异常: ' + err.message)
}