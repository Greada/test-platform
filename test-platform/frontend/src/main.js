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
  // 生产环境脱敏：err.message 可能含内部路径/堆栈，不展示给用户
  if (import.meta.env.PROD) {
    ElMessage.error('系统异常，请稍后重试')
  } else {
    ElMessage.error('页面异常: ' + err.message)
  }
}

app.use(ElementPlus)
app.use(router)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')