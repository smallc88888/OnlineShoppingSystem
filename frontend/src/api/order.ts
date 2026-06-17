import axios from 'axios'
import { cartApi } from './cart' // 引入购物车API，结算成功后需要清空前端徽章

const apiClient = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json'
    }
})

// 动态读取身份凭证
apiClient.interceptors.request.use(config => {
    const userId = localStorage.getItem('userId')
    if (userId) {
        config.headers['user-id'] = userId
    }
    return config
})

export interface CheckoutRequest {
    receiverName: string;
    receiverPhone: string;
    receiverAddress: string;
}

export const orderApi = {
    // 提交订单
    async checkout(data: CheckoutRequest) {
        const res = await apiClient.post('/orders/checkout', data)
        // 提交成功后，意味着购物车已被后端清空，前端同步清零徽章
        if (res.data.code === 200) {
            cartApi.getCart()
        }
        return res
    },

    // 获取历史订单列表
    getOrders() {
        return apiClient.get('/orders')
    },

    payOrder(id: number) {
        return apiClient.put(`/orders/${id}/pay`)
    },

    // 确认收货
    receiveOrder(id: number) {
        return apiClient.put(`/orders/${id}/receive`)
    }
}