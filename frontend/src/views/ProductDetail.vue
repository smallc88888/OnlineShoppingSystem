<template>
  <div class="container">
    <button class="back-btn" @click="router.push('/products')">← 返回商品列表</button>

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

      <div class="actions" v-if="product.stock > 0">
        <div class="quantity-control">
          <label>购买数量：</label>
          <input
              type="number"
              v-model.number="buyQuantity"
              min="1"
              :max="Math.min(99, product.stock)"
          />
        </div>
        <button class="add-to-cart" @click="handleAddToCart">加入购物车</button>
      </div>
      <div class="actions" v-else>
        <button class="add-to-cart" disabled>暂时缺货</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi, Product } from '../api/product'
import { cartApi, globalCartCount } from '../api/cart'

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

// 新增响应式变量
const buyQuantity = ref(1)

// 处理加入购物车逻辑
const handleAddToCart = async () => {
  if (!product.value) return

  // 1. 前端物理防线：需求规定必须为 1-99 且不超过库存
  if (buyQuantity.value < 1 || buyQuantity.value > 99) {
    alert('购买数量必须在 1 到 99 之间')
    return
  }
  if (buyQuantity.value > product.value.stock) {
    alert(`库存不足，当前最多只能购买 ${product.value.stock} 件`)
    return
  }

  try {
    // 2. 发起请求
    const res = await cartApi.addToCart(product.value.id, buyQuantity.value)
    if (res.data.code === 200) {
      alert('已成功加入购物车！')
      // 3. 静默刷新全局购物车数量徽章
      cartApi.getCart()
    }
  } catch (error: any) {
    if (error.response && error.response.data) {
      alert(error.response.data.message) // 抛出后端的超限拦截提示
    }
  }
}

// 在 onMounted 的最后，初始化徽章数量
onMounted(() => {
  loadDetail()
  cartApi.getCart()
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

.quantity-control { margin-bottom: 15px; }
.quantity-control input { width: 60px; padding: 5px; font-size: 16px; text-align: center; }
</style>