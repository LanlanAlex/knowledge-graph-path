import { createApp } from 'vue'
import './assets/style.css' // 或者 './style.css'，确保路径对
import App from './App.vue'
import router from './router' // 引入路由
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

// 1. 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

// 2. 必须挂载路由，否则页面不认识 <router-view />
app.use(router)
app.use(ElementPlus)

// 3. 只能 mount 一次！
app.mount('#app')