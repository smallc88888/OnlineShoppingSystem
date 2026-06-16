import axios from 'axios'
// 复用 auth.ts 里的 ApiResponse 接口
// 为了方便，这里重新定义一下基础架构
const apiClient = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json'
    }
})

export interface ApiResponse<T> {
    code: number;
    message: string;
    data: T;
}

// 对应后端的 Product 实体
export interface Product {
    id: number;
    name: string;
    description: string;
    price: number;
    stock: number;
    active: boolean; // 注意：Java 的 isActive() 默认会被 Jackson 序列化为 active
    createdAt: string;
}

// 对应后端的 PageResultDTO
export interface PageResult<T> {
    total: number;
    totalPages: number;
    page: number;
    pageSize: number;
    items: T[];
}

export const productApi = {
    // GET 请求参数通过 params 传递
    getProducts(keyword: string = '', page: number = 1, pageSize: number = 10) {
        return apiClient.get<ApiResponse<PageResult<Product>>>('/products', {
            params: {
                keyword: keyword.trim() === '' ? undefined : keyword, // 如果是空字符串，就不传 keyword 参数
                page,
                pageSize
            }
        })
    },

    // 获取商品详情
    getProductById(id: number | string) {
        return apiClient.get<ApiResponse<Product>>(`/products/${id}`)
    }
}