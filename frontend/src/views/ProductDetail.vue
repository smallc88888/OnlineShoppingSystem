<template>
  <div class="container">
    <button class="back-btn" @click="router.back()">← 返回商品列表</button>

    <div v-if="loading" class="loading">正在加载商品信息...</div>
    <div v-else-if="errorMessage" class="error-msg">{{ errorMessage }}</div>

    <div v-else-if="product" class="detail-card">
      <h2>{{ product.name }}</h2>
      <div class="meta-info">
        <span class="price">¥{{ product.price.toFixed(2) }}</span>
        <span class="stock">库存: {{ product.stock }} 件</span>
      </div>

      <div class="description">
        <h3>商品详情</h3>
        <p>{{ product.description }}</p>
      </div>

      <div class="actions">
        <button class="add-to-cart" :disabled="product.stock <= 0" @click="addToCart">
          {{ product.stock > 0 ? '加入购物车' : '暂时缺货' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi, Product } from '../api/product'

const route = useRoute()
const router = useRouter()

const product = ref<Product | null>(null)
const loading = ref(true)
const errorMessage = ref('')

const loadDetail = async () => {
  // 从动态路由 /product/:id 中提取 id
  const id = route.params.id as string

  try {
    const res = await productApi.getProductById(id)
    if (res.data.code === 200) {
      product.value = res.data.data
    }
  } catch (error: any) {
    if (error.response && error.response.status === 404) {
      errorMessage.value = '抱歉，该商品不存在或已下架。'
    } else {
      errorMessage.value = '网络或服务器错误，请稍后再试。'
    }
  } finally {
    loading.value = false
  }
}

const addToCart = () => {
  alert('加入购物车功能即将开放！当前商品 ID: ' + product.value?.id)
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.container { max-width: 800px; margin: 30px auto; font-family: sans-serif; }
.back-btn { margin-bottom: 20px; padding: 8px 12px; cursor: pointer; background: #f0f0f0; border: 1px solid #ccc; border-radius: 4px; }
.loading, .error-msg { text-align: center; margin-top: 50px; font-size: 18px; }
.error-msg { color: #d9534f; }

.detail-card { border: 1px solid #ddd; padding: 30px; border-radius: 8px; background: #fff; }
.detail-card h2 { margin-top: 0; color: #333; }
.meta-info { display: flex; align-items: center; gap: 20px; margin: 20px 0; padding-bottom: 20px; border-bottom: 1px solid #eee; }
.price { color: #e4393c; font-size: 28px; font-weight: bold; }
.stock { color: #666; }

.description { margin: 20px 0; line-height: 1.6; color: #444; }
.description p { white-space: pre-wrap; /* 保留换行符 */ }

.actions { margin-top: 30px; }
.add-to-cart { background: #e4393c; color: white; border: none; padding: 12px 30px; font-size: 16px; border-radius: 4px; cursor: pointer; }
.add-to-cart:disabled { background: #ccc; cursor: not-allowed; }
</style>