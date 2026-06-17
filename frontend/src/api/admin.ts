import axios from 'axios'

const apiClient = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json'
    }
})

// 动态身份凭证拦截器
apiClient.interceptors.request.use(config => {
    const userId = localStorage.getItem('userId')
    if (userId) {
        config.headers['user-id'] = userId
    }
    return config
})

// 定义商品入参接口 (抛弃了 imageUrl)
export interface AdminProductRequest {
    name: string;
    description: string;
    price: number;
    stock: number;
}

export const adminApi = {
    // ====== 商品管理 ======
    getAllProducts() {
        return apiClient.get('/admin/products')
    },

    addProduct(data: AdminProductRequest) {
        return apiClient.post('/admin/products', data)
    },
    updateProduct(id: number, data: AdminProductRequest) {
        return apiClient.put(`/admin/products/${id}`, data)
    },
    deactivateProduct(id: number) {
        return apiClient.delete(`/admin/products/${id}`)
    },

    // ====== 订单管理 ======
    getAllOrders() {
        return apiClient.get('/admin/orders')
    },
    confirmOrder(id: number) {
        return apiClient.put(`/admin/orders/${id}/confirm`)
    },
    shipOrder(id: number) {
        return apiClient.put(`/admin/orders/${id}/ship`)
    }
}