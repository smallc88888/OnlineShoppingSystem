import { createRouter, createWebHistory } from 'vue-router'
// 懒加载的两个页面
const Login = () => import('../views/Login.vue')
const Register = () => import('../views/Register.vue')

const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/', redirect: '/login' }, // 默认跳转登录
        { path: '/login', component: Login },
        { path: '/register', component: Register }
    ]
})

export default router