import axios from 'axios'

// 注意：如果 Tomcat 配置了项目上下文路径（比如 /ShoppingSystem），请加在 8080 后面。
// 如果是根目录运行，这里就是 http://localhost:8080/api
const apiClient = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json'
    }
})

// 定义与后端一致的接口格式
export interface ApiResponse<T> {
    code: number;
    message: string;
    data: T;
}

// 封装登录和注册请求
export const authApi = {
    login(data: any) {
        return apiClient.post<ApiResponse<any>>('/users/login', data)
    },
    register(data: any) {
        return apiClient.post<ApiResponse<any>>('/users/register', data)
    }
}