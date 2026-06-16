<template>
  <div class="container success-page">
    <div class="success-icon">✅</div>
    <h2>订单提交成功！</h2>
    <p class="order-no">您的订单号为：<strong>{{ orderNo }}</strong></p>

    <p class="tips">
      (模拟环境：管理员审核后方可付款，请在3天内完成操作)
    </p>

    <div class="actions">
      <button class="primary-btn" @click="router.push('/orders')">查看我的订单</button>
      <button class="secondary-btn" @click="router.push('/products')">继续购物</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
// 从 URL 的 Query 参数中提取订单号
const orderNo = route.query.orderNo as string || '未知订单号'
</script>

<style scoped>
.container { max-width: 600px; margin: 80px auto; text-align: center; font-family: sans-serif; }
.success-icon { font-size: 72px; margin-bottom: 20px; }
h2 { color: #333; margin-bottom: 15px; }
.order-no { font-size: 18px; color: #555; margin-bottom: 10px; }
.tips { color: #888; font-size: 14px; margin-bottom: 40px; }

.actions { display: flex; justify-content: center; gap: 20px; }
button { padding: 12px 24px; font-size: 16px; border-radius: 4px; cursor: pointer; border: none; }
.primary-btn { background-color: #e4393c; color: white; }
.secondary-btn { background-color: #f0f0f0; color: #333; border: 1px solid #ccc; }
</style>