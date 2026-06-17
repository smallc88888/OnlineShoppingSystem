import { createRouter, createWebHistory } from 'vue-router'
// 懒加载的页面
const Login = () => import('../views/Login.vue')
const Register = () => import('../views/Register.vue')
const ProductList = () => import('../views/ProductList.vue')
const ProductDetail = () => import('../views/ProductDetail.vue')
const Cart = () => import('../views/Cart.vue')
const Checkout = () => import('../views/Checkout.vue')
const OrderSuccess = () => import('../views/OrderSuccess.vue')
const OrderList = () => import('../views/OrderList.vue')
const AdminProducts = () => import('../views/AdminProducts.vue')
const AdminOrders = () => import('../views/AdminOrders.vue')

const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/', redirect: '/products' },
        { path: '/login', component: Login },
        { path: '/register', component: Register },
        { path: '/products', component: ProductList},
        { path: '/product/:id', component: ProductDetail},
        { path: '/cart', component: Cart },
        { path: '/checkout', component: Checkout },
        { path: '/order-success', component: OrderSuccess },
        { path: '/orders', component: OrderList },
        { path: '/admin/products', component: AdminProducts },
        { path: '/admin/orders', component: AdminOrders }
    ]
})

export default router