import {createApp} from "vue";
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { ElMessage } from 'element-plus'


const app = createApp(App)

app.config.errorHandler = (err, instance, info) => {
  console.error('Vue error:', err)
  ElMessage.error('页面异常: ' + err.message)
}

app.use(ElementPlus)
app.use(router)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')