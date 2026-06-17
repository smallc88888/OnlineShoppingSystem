<template>
  <div class="container">
    <div class="top-nav">
      <h2>商品浏览中心</h2>
      <div class="nav-links">

        <template v-if="!isLoggedIn">
          <router-link to="/login">登录</router-link>
          <router-link to="/register">注册账号</router-link>
        </template>

        <template v-else>
          <router-link to="/cart" class="cart-badge">
            购物车 ({{ globalCartCount }})
          </router-link>
          <router-link to="/orders">我的订单</router-link>
          <a href="#" @click.prevent="handleLogout">退出登录</a>
        </template>

      </div>
    </div>

    <div class="search-box">
      <input
          type="text"
          v-model="searchKeyword"
          placeholder="输入商品名称或描述..."
          @keyup.enter="handleSearch"
      />
      <button @click="handleSearch">搜索</button>
      <button @click="resetSearch" class="btn-reset">重置</button>
    </div>

    <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>

    <div class="product-grid" v-if="pageData.items.length > 0">
      <div class="product-card" v-for="item in pageData.items" :key="item.id" @click="goToDetail(item.id)">
        <h3>{{ item.name }}</h3>
        <p class="desc">{{ item.description }}</p>
        <div class="price-stock">
          <span class="price">¥{{ item.price.toFixed(2) }}</span>
          <span class="stock">库存: {{ item.stock }}</span>
        </div>
      </div>
    </div>

    <div v-else-if="!errorMessage" class="empty-msg">
      未找到相关商品
    </div>

    <div class="pagination" v-if="pageData.totalPages > 1">
      <button :disabled="currentPage === 1" @click="changePage(currentPage - 1)">上一页</button>
      <span class="page-info">第 {{ currentPage }} / {{ pageData.totalPages }} 页 (共 {{ pageData.total }} 件)</span>
      <button :disabled="currentPage === pageData.totalPages" @click="changePage(currentPage + 1)">下一页</button>
    </div>

    <button class="floating-cart" @click="router.push('/cart')">
      🛒 购物车
      <span v-if="globalCartCount > 0" class="badge">{{ globalCartCount }}</span>
    </button>
    <button v-if="isAdmin" class="admin-entrance-btn" @click="router.push('/admin/products')">
      ⚙️ 进入后台
    </button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ref, onMounted } from 'vue'
import { productApi, PageResult, Product } from '../api/product'
import { cartApi, globalCartCount } from '../api/cart'
const router = useRouter()

// 定义响应式的登录状态变量，初始值设为 false
const isLoggedIn = ref(false)

const goToDetail = (id: number) => {
  router.push(`/product/${id}`)
}

// 响应式状态管理
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const errorMessage = ref('')

// 分页数据容器
const pageData = ref<PageResult<Product>>({
  total: 0,
  totalPages: 0,
  page: 1,
  pageSize: 10,
  items: []
})

// 加载商品核心逻辑
const loadProducts = async () => {
  errorMessage.value = ''
  try {
    const res = await productApi.getProducts(searchKeyword.value, currentPage.value, pageSize.value)
    if (res.data.code === 200) {
      pageData.value = res.data.data
    }
  } catch (error: any) {
    if (error.response && error.response.data) {
      errorMessage.value = error.response.data.message // 显示后端抛出的 400 校验错误
    } else {
      errorMessage.value = '网络请求失败，请检查后端状态'
    }
    // 报错时清空列表
    pageData.value.items = []
  }
}

// 触发搜索（永远从第一页开始查）
const handleSearch = () => {
  currentPage.value = 1
  loadProducts()
}

// 重置搜索
const resetSearch = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  loadProducts()
}

// 翻页操作
const changePage = (newPage: number) => {
  currentPage.value = newPage
  loadProducts()
}

const isAdmin = ref(false)

// 页面一加载，自动请求第一页数据
onMounted(() => {
  // 页面每次挂载时，实时去 localStorage 查验一次真实状态
  isLoggedIn.value = !!localStorage.getItem('userId')
  isAdmin.value = localStorage.getItem('userRole') === '1'
  loadProducts()
  if (isLoggedIn.value) {
    cartApi.getCart()
  }
})

const handleLogout = () => {
  // 清除本地存储的身份凭证
  localStorage.removeItem('userId')
  // 清理权限标识
  localStorage.removeItem('userRole')
  // 清空全局购物车徽章
  globalCartCount.value = 0
  // 退出时同步重置状态
  isLoggedIn.value = false
  // 退出时同步重置管理员状态
  isAdmin.value = false
  // 提示并跳回登录页
  alert('已安全退出')
  router.push('/')
}
</script>

<style scoped>
.top-nav { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #eee; margin-bottom: 20px; padding-bottom: 10px; }
.top-nav h2 { margin: 0; }
.nav-links a { margin-left: 15px; text-decoration: none; color: #007bff; }
.nav-links a:hover { text-decoration: underline; }

.container { max-width: 800px; margin: 30px auto; font-family: sans-serif; }
.search-box { display: flex; gap: 10px; margin-bottom: 20px; }
.search-box input { flex: 1; padding: 8px; }
.search-box button { padding: 8px 15px; cursor: pointer; }
.btn-reset { background-color: #f0f0f0; border: 1px solid #ccc; }

.error-msg { color: red; margin-bottom: 10px; }
.empty-msg { text-align: center; color: #888; margin-top: 50px; }

/* 原生极简网格布局 */
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 15px; }
.product-card { border: 1px solid #ddd; border-radius: 4px; padding: 15px; background: #fafafa; cursor: pointer; }
.product-card h3 { margin: 0 0 10px 0; font-size: 18px; color: #333; }
.desc { color: #666; font-size: 14px; height: 40px; overflow: hidden; }
.price-stock { display: flex; justify-content: space-between; align-items: center; margin-top: 15px; }
.price { color: #e4393c; font-weight: bold; font-size: 18px; }
.stock { color: #999; font-size: 12px; }

.pagination { display: flex; justify-content: center; align-items: center; margin-top: 30px; gap: 15px; }
.pagination button { padding: 5px 15px; cursor: pointer; }
.pagination button:disabled { cursor: not-allowed; opacity: 0.5; }

.floating-cart {
  position: fixed;
  bottom: 40px;
  right: 40px;
  background-color: #e4393c;
  color: white;
  border: none;
  border-radius: 30px;
  padding: 15px 25px;
  font-size: 16px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(228, 57, 60, 0.4);
  transition: transform 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
}
.floating-cart:hover { transform: scale(1.05); }
.badge {
  background-color: white;
  color: #e4393c;
  border-radius: 50%;
  padding: 2px 6px;
  font-size: 12px;
  font-weight: bold;
}

/* 追加管理员入口悬浮按钮样式 */
.admin-entrance-btn {
  position: fixed;
  bottom: 40px;
  left: 40px;
  background-color: #343a40;
  color: white;
  border: none;
  border-radius: 30px;
  padding: 15px 25px;
  font-size: 16px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(52, 58, 64, 0.4);
  transition: transform 0.2s;
  z-index: 1000;
}
.admin-entrance-btn:hover {
  transform: scale(1.05);
  background-color: #23272b;
}
</style>