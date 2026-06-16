import { createRouter, createWebHistory } from 'vue-router'
// 懒加载的页面
const Login = () => import('../views/Login.vue')
const Register = () => import('../views/Register.vue')
const ProductLsit = () => import('../views/ProductList.vue')

const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/', redirect: '/login' }, // 默认跳转登录
        { path: '/login', component: Login },
        { path: '/register', component: Register },
        { path: '/products', component: ProductLsit}
    ]
})

export default router