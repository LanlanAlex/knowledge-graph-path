import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        name: 'Graph',
        component: () => import('../views/GraphView.vue') // 我们稍后创建这个页面
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router