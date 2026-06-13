import {createApp} from "vue";
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'


const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.mount('#app')

app.config.errorHandler = (err, instance, info) => {
  console.error('Vue error:', err)
}