<template>
  <div class="container">
    <h2>确认订单信息</h2>

    <button class="back-btn" @click="router.back()">← 返回购物车</button>

    <div class="checkout-content" v-if="!loading">
      <!-- 左侧：收货表单 -->
      <div class="form-section">
        <h3>收货信息</h3>
        <div class="form-group">
          <label>收货人姓名</label>
          <input type="text" v-model="form.receiverName" placeholder="2-20个中英文字符" />
          <span class="error" v-if="errors.receiverName">{{ errors.receiverName }}</span>
        </div>

        <div class="form-group">
          <label>联系电话</label>
          <input type="text" v-model="form.receiverPhone" placeholder="11位手机号" />
          <span class="error" v-if="errors.receiverPhone">{{ errors.receiverPhone }}</span>
        </div>

        <div class="form-group">
          <label>详细地址</label>
          <textarea v-model="form.receiverAddress" placeholder="10-100个字符，需包含省市区及门牌号" rows="3"></textarea>
          <span class="error" v-if="errors.receiverAddress">{{ errors.receiverAddress }}</span>
        </div>
      </div>

      <!-- 右侧：商品清单与总价 -->
      <div class="summary-section">
        <h3>商品清单</h3>
        <div class="item-list">
          <div class="item" v-for="item in cartItems" :key="item.id">
            <span class="name">{{ item.productName }}</span>
            <span class="qty">x{{ item.quantity }}</span>
            <span class="subtotal">¥{{ item.subtotal.toFixed(2) }}</span>
          </div>
        </div>
        <div class="total-row">
          实付总额：<span class="total-price">¥{{ totalPrice.toFixed(2) }}</span>
        </div>

        <button class="submit-btn" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? '提交中...' : '提交订单' }}
        </button>
      </div>
    </div>
    <div v-else class="loading">正在加载结算信息...</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { cartApi, CartItem } from '../api/cart'
import { orderApi } from '../api/order'

const router = useRouter()
const cartItems = ref<CartItem[]>([])
const loading = ref(true)
const submitting = ref(false)

// 表单数据
const form = ref({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: ''
})

// 错误提示
const errors = ref({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: ''
})

// 计算总价
const totalPrice = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.subtotal, 0)
})

// 初始化结算页（复用拉取购物车接口）
const initCheckout = async () => {
  try {
    const res = await cartApi.getCart()
    cartItems.value = res.data.data

    // 如果发现购物车是空的，直接强制踢回商品页，防止恶意绕过
    if (cartItems.value.length === 0) {
      alert('购物车中没有商品，无法结算')
      router.push('/products')
    }
  } catch (error) {
    alert('加载结算信息失败')
  } finally {
    loading.value = false
  }
}

// 前端正则防线
const validateForm = () => {
  let isValid = true
  errors.value = { receiverName: '', receiverPhone: '', receiverAddress: '' }

  const nameRegex = /^[\u4e00-\u9fa5a-zA-Z]{2,20}$/
  if (!nameRegex.test(form.value.receiverName)) {
    errors.value.receiverName = '姓名必须为2-20个中英文字符'
    isValid = false
  }

  const phoneRegex = /^1[3-9]\d{9}$/
  if (!phoneRegex.test(form.value.receiverPhone)) {
    errors.value.receiverPhone = '请输入合法的11位手机号'
    isValid = false
  }

  const addrLen = form.value.receiverAddress.length
  if (addrLen < 10 || addrLen > 100) {
    errors.value.receiverAddress = '详细地址长度必须在10到100个字符之间'
    isValid = false
  }

  return isValid
}

// 提交订单
const handleSubmit = async () => {
  if (!validateForm()) return

  submitting.value = true
  try {
    const res = await orderApi.checkout(form.value)
    if (res.data.code === 200) {
      // 携带生成的订单号跳转到成功页
      const orderNo = res.data.data.orderNo
      router.push(`/order-success?orderNo=${orderNo}`)
    }
  } catch (error: any) {
    // 捕获后端的库存不足、已下架或正则拦截提示
    if (error.response && error.response.data) {
      alert(error.response.data.message)
    } else {
      alert('订单提交失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  initCheckout()
})
</script>

<style scoped>
.container { max-width: 1000px; margin: 30px auto; font-family: sans-serif; }
.back-btn { margin-bottom: 20px; padding: 8px 15px; cursor: pointer; }
.checkout-content { display: flex; gap: 40px; align-items: flex-start; }

.form-section { flex: 2; background: #f9f9f9; padding: 30px; border-radius: 8px; }
.form-group { margin-bottom: 20px; display: flex; flex-direction: column; }
.form-group label { margin-bottom: 8px; font-weight: bold; }
.form-group input, .form-group textarea { padding: 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; }
.error { color: #d9534f; font-size: 12px; margin-top: 5px; }

.summary-section { flex: 1; background: #fff; padding: 30px; border: 1px solid #eee; border-radius: 8px; }
.item-list { border-bottom: 1px solid #eee; margin-bottom: 20px; padding-bottom: 20px; }
.item { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px; }
.item .name { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-right: 10px; }
.item .qty { color: #888; width: 40px; text-align: center; }
.item .subtotal { color: #333; font-weight: bold; width: 80px; text-align: right; }

.total-row { font-size: 18px; text-align: right; margin-bottom: 20px; }
.total-price { color: #e4393c; font-size: 24px; font-weight: bold; }
.submit-btn { width: 100%; background: #e4393c; color: white; border: none; padding: 15px; font-size: 18px; border-radius: 4px; cursor: pointer; }
.submit-btn:disabled { background: #ccc; cursor: not-allowed; }
</style>