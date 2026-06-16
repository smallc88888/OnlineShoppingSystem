<template>
  <div class="container">
    <h2>商品浏览中心</h2>

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
      <div class="product-card" v-for="item in pageData.items" :key="item.id">
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { productApi, PageResult, Product } from '../api/product'

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

// 页面一加载，自动请求第一页数据
onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.container { max-width: 800px; margin: 30px auto; font-family: sans-serif; }
.search-box { display: flex; gap: 10px; margin-bottom: 20px; }
.search-box input { flex: 1; padding: 8px; }
.search-box button { padding: 8px 15px; cursor: pointer; }
.btn-reset { background-color: #f0f0f0; border: 1px solid #ccc; }

.error-msg { color: red; margin-bottom: 10px; }
.empty-msg { text-align: center; color: #888; margin-top: 50px; }

/* 原生极简网格布局 */
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 15px; }
.product-card { border: 1px solid #ddd; border-radius: 4px; padding: 15px; background: #fafafa; }
.product-card h3 { margin: 0 0 10px 0; font-size: 18px; color: #333; }
.desc { color: #666; font-size: 14px; height: 40px; overflow: hidden; }
.price-stock { display: flex; justify-content: space-between; align-items: center; margin-top: 15px; }
.price { color: #e4393c; font-weight: bold; font-size: 18px; }
.stock { color: #999; font-size: 12px; }

.pagination { display: flex; justify-content: center; align-items: center; margin-top: 30px; gap: 15px; }
.pagination button { padding: 5px 15px; cursor: pointer; }
.pagination button:disabled { cursor: not-allowed; opacity: 0.5; }
</style>