import axios from 'axios'
import { ref } from 'vue'

// 1. 轻量级全局状态：用于存储右上角购物车的商品总种类数
export const globalCartCount = ref(0)

const apiClient = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json'
    }
})

// 动态身份拦截器：每次发请求前，实时去 localStorage 抓取 userId
apiClient.interceptors.request.use(config => {
    const userId = localStorage.getItem('userId')

    if (userId) {
        // 如果已登录，带上真实的 ID
        config.headers['user-id'] = userId
    } else {
        // 如果没登录，可以在这里选择直接拦截，或者让请求发出去被后端返回 401
        // 为了防止未登录时无意义的报错，我们可以直接清空本地的购物车徽章
        globalCartCount.value = 0
    }
    return config
})

export interface CartItem {
    id: number;
    productId: number;
    productName: string;
    price: number;
    quantity: number;
    subtotal: number;
}

export const cartApi = {
    // 获取购物车列表，并顺便更新全局徽章数字
    async getCart() {
        const res = await apiClient.get('/cart')
        if (res.data.code === 200) {
            globalCartCount.value = res.data.data.length
        }
        return res
    },
    addToCart(productId: number, quantity: number) {
        return apiClient.post('/cart', { productId, quantity })
    },
    updateQuantity(cartItemId: number, quantity: number) {
        return apiClient.put(`/cart/${cartItemId}`, { quantity })
    },
    removeCartItem(cartItemId: number) {
        return apiClient.delete(`/cart/${cartItemId}`)
    }
}