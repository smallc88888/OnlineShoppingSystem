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

        // 以下是受保护的路由，加上 meta 标记
        {
            path: '/cart',
            component: Cart,
            meta: { requiresAuth: true } // 需要登录
        },
        {
            path: '/checkout',
            component: Checkout,
            meta: { requiresAuth: true }
        },
        {
            path: '/order-success',
            component: OrderSuccess,
            meta: { requiresAuth: true }
        },
        {
            path: '/orders',
            component: OrderList,
            meta: { requiresAuth: true }
        },
        {
            path: '/admin/products',
            component: AdminProducts,
            meta: { requiresAuth: true, requiresAdmin: true } // 需要登录 且 必须是管理员
        },
        {
            path: '/admin/orders',
            component: AdminOrders,
            meta: { requiresAuth: true, requiresAdmin: true }
        }
    ]
})

// 全局前置路由守卫
router.beforeEach((to, from, next) => {
    const isLoggedIn = !!localStorage.getItem('userId');
    const role = localStorage.getItem('userRole'); // 假设 '1' 代表管理员，'0' 代表普通用户

    // 1. 检查是否需要登录
    if (to.meta.requiresAuth && !isLoggedIn) {
        alert('系统拦截：请先登录后再访问该页面！');
        next('/login');
    }
    // 2. 检查是否需要管理员权限 (防御垂直越权)
    else if (to.meta.requiresAdmin && role !== '1') {
        alert('警告：您不是管理员，无权访问该页面。');
        next('/');
    }
    // 3. 正常放行
    else {
        next();
    }
});

export default router