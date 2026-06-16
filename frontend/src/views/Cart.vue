<template>
  <div class="container">
    <div class="header">
      <h2>我的购物车</h2>
      <button class="back-btn" @click="router.push('/products')">🏠 返回商品页</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="cartItems.length === 0" class="empty-msg">
      购物车内暂无商品
    </div>

    <div v-else>
      <table class="cart-table">
        <thead>
        <tr>
          <th>商品名称</th>
          <th>单价</th>
          <th>数量</th>
          <th>小计</th>
          <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="item in cartItems" :key="item.id">
          <td>{{ item.productName }}</td>
          <td class="price">¥{{ item.price.toFixed(2) }}</td>
          <td>
            <input
                type="number"
                v-model.number="item.quantity"
                @change="handleQuantityChange(item)"
                min="0"
                max="99"
                class="qty-input"
            />
          </td>
          <td class="subtotal">¥{{ item.subtotal.toFixed(2) }}</td>
          <td>
            <button class="del-btn" @click="confirmRemove(item.id)">删除</button>
          </td>
        </tr>
        </tbody>
      </table>

      <div class="cart-footer">
        <div class="total-price">
          总金额：<span>¥{{ totalPrice.toFixed(2) }}</span>
        </div>
        <button class="checkout-btn" @click="handleCheckout" :disabled="cartItems.length === 0">
          去结算
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { cartApi, CartItem } from '../api/cart'

const router = useRouter()
const cartItems = ref<CartItem[]>([])
const loading = ref(true)

// 计算总价
const totalPrice = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.subtotal, 0)
})

const loadCart = async () => {
  loading.value = true
  try {
    const res = await cartApi.getCart()
    cartItems.value = res.data.data
  } catch (error) {
    alert('获取购物车失败')
  } finally {
    loading.value = false
  }
}

// 处理数量修改（包含输入 0 的情况）
const handleQuantityChange = async (item: CartItem) => {
  if (item.quantity === 0) {
    if (confirm('确认移除该商品？')) {
      await removeItem(item.id)
    } else {
      // 用户取消删除，恢复为至少 1 件
      item.quantity = 1
      await updateItemQuantity(item.id, 1)
    }
    return
  }

  // 边界拦截：防止手填超过 99 或负数
  if (item.quantity < 1) item.quantity = 1
  if (item.quantity > 99) item.quantity = 99

  await updateItemQuantity(item.id, item.quantity)
}

const updateItemQuantity = async (id: number, qty: number) => {
  try {
    await cartApi.updateQuantity(id, qty)
    await loadCart() // 刷新列表，重新获取后端计算的小计
  } catch (error: any) {
    if (error.response && error.response.data) {
      alert(error.response.data.message) // 比如后端报：库存不足
    }
    await loadCart() // 修改失败，强行重置为后端真实的数量
  }
}

// 主动删除
const confirmRemove = async (id: number) => {
  if (confirm('确认移除该商品？')) {
    await removeItem(id)
  }
}

const removeItem = async (id: number) => {
  try {
    await cartApi.removeCartItem(id)
    await loadCart()
  } catch (error) {
    alert('删除失败')
  }
}

onMounted(() => {
  loadCart()
})

const handleCheckout = () => {
  if (cartItems.value.length === 0) {
    alert('购物车中没有商品，无法结算')
    return
  }
  router.push('/checkout')
}
</script>

<style scoped>
.container { max-width: 900px; margin: 30px auto; font-family: sans-serif; }
.header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #eee; padding-bottom: 10px; margin-bottom: 20px; }
.back-btn { padding: 8px 15px; cursor: pointer; }
.empty-msg, .loading { text-align: center; padding: 50px; color: #888; }

.cart-table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }
.cart-table th, .cart-table td { padding: 15px; text-align: left; border-bottom: 1px solid #eee; }
.qty-input { width: 60px; padding: 5px; text-align: center; }
.price { color: #666; }
.subtotal { color: #e4393c; font-weight: bold; }
.del-btn { background: #ff4d4f; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer; }

.cart-footer { display: flex; justify-content: flex-end; align-items: center; gap: 30px; background: #f9f9f9; padding: 20px; border-radius: 4px; }
.total-price span { color: #e4393c; font-size: 24px; font-weight: bold; }
.checkout-btn { background: #e4393c; color: white; border: none; padding: 12px 30px; font-size: 18px; border-radius: 4px; cursor: pointer; }
</style>