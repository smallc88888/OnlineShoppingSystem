<template>
  <div class="container">
    <div class="header">
      <h2>我的订单</h2>
      <button class="back-btn" @click="router.push('/products')">🏠 返回首页</button>
    </div>

    <div v-if="loading" class="loading">加载订单数据中...</div>
    <div v-else-if="orders.length === 0" class="empty-msg">
      未查询到历史订单
    </div>

    <div class="order-list" v-else>
      <div class="order-card" v-for="order in orders" :key="order.orderNo">
        <div class="order-header">
          <div class="info-group">
            <span class="label">订单编号：</span>
            <span class="value">{{ order.orderNo }}</span>
          </div>
          <div class="info-group">
            <span class="label">下单时间：</span>
            <span class="value">{{ formatTime(order.createdAt) }}</span>
          </div>
          <div class="info-group">
            <span class="label">总金额：</span>
            <span class="value price">¥{{ order.totalAmount.toFixed(2) }}</span>
          </div>
          <div class="info-group status">
            <span :class="'status-tag status-' + order.status">
              {{ getStatusText(order.status) }}
            </span>
          </div>
          <div class="action-group">
            <button class="action-btn pay-btn" v-if="order.status === 1" @click="handlePay(order.id)">
              💳 付款
            </button>
            <button class="action-btn receive-btn" v-if="order.status === 3" @click="handleReceive(order.id)">
              📦 确认收货
            </button>
            <button class="toggle-btn" @click="toggleDetail(order.orderNo)">
              {{ expandedOrder === order.orderNo ? '收起详情 ▲' : '查看详情 ▼' }}
            </button>
          </div>
        </div>

        <div class="order-detail" v-show="expandedOrder === order.orderNo">
          <div class="receiver-info">
            <h4>📍 收货信息</h4>
            <p><strong>收货人：</strong>{{ order.receiverName }} ({{ order.receiverPhone }})</p>
            <p><strong>地址：</strong>{{ order.receiverAddress }}</p>
          </div>

          <div class="items-info">
            <h4>📦 商品明细快照</h4>
            <table class="item-table">
              <thead>
              <tr>
                <th>商品名称</th>
                <th>购买单价</th>
                <th>数量</th>
                <th>小计</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="item in order.items" :key="item.productId">
                <td>{{ item.productName }}</td>
                <td>¥{{ item.buyPrice.toFixed(2) }}</td>
                <td>x{{ item.quantity }}</td>
                <td class="subtotal">¥{{ (item.buyPrice * item.quantity).toFixed(2) }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi } from '../api/order'

const router = useRouter()
const orders = ref<any[]>([])
const loading = ref(true)
const expandedOrder = ref<string | null>(null) // 记录当前展开了哪个订单的详情

const loadOrders = async () => {
  try {
    const res = await orderApi.getOrders()
    if (res.data.code === 200) {
      orders.value = res.data.data
    }
  } catch (error) {
    alert('拉取订单列表失败')
  } finally {
    loading.value = false
  }
}

// 展开/收起详情逻辑
const toggleDetail = (orderNo: string) => {
  if (expandedOrder.value === orderNo) {
    expandedOrder.value = null
  } else {
    expandedOrder.value = orderNo
  }
}

// 状态机字典转换
const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待确认',
    1: '待付款',
    2: '已付款待发货',
    3: '已发货待收货',
    4: '已完成',
    5: '已取消'
  }
  return map[status] || '未知状态'
}

// 简单的时间格式化处理
const formatTime = (timeArray: number[] | string) => {
  // 兼容后端传来的 LocalDateTime 数组 [year, month, day, hour, minute, second]
  if (Array.isArray(timeArray)) {
    // 解构赋值，注意 JS 的 month 必须减 1
    const [year, month, day, hour = 0, minute = 0, second = 0] = timeArray;
    const date = new Date(year, month - 1, day, hour, minute, second);

    // 返回字符串格式
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`;
  }

  // 如果后端发来的是字符串 (因为我们配了 JSR310 模块，应该是 ISO 字符串)
  if (typeof timeArray === 'string') {
    return timeArray.replace('T', ' ').substring(0, 19)
  }

  return '时间格式解析异常'
}

// 模拟付款
const handlePay = async (id: number) => {
  if (!confirm('付款：将扣除您的余额，确认支付吗？')) return
  try {
    await orderApi.payOrder(id)
    alert('支付成功！等待管理员发货。')
    loadOrders() // 刷新列表
  } catch (error: any) {
    alert(error.response?.data?.message || '支付失败')
  }
}

// 确认收货
const handleReceive = async (id: number) => {
  if (!confirm('确认您已收到商品且完好无损吗？')) return
  try {
    await orderApi.receiveOrder(id)
    alert('交易完成！感谢您的购买。')
    loadOrders() // 刷新列表
  } catch (error: any) {
    alert(error.response?.data?.message || '确认失败')
  }
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.container { max-width: 900px; margin: 30px auto; font-family: sans-serif; }
.header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #eee; padding-bottom: 10px; margin-bottom: 20px; }
.back-btn { padding: 8px 15px; cursor: pointer; }
.loading, .empty-msg { text-align: center; padding: 50px; color: #888; }

.order-card { border: 1px solid #ddd; border-radius: 8px; margin-bottom: 20px; background: #fff; overflow: hidden; }
.order-header { display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; background: #f9f9f9; border-bottom: 1px solid #eee; }
.info-group { display: flex; flex-direction: column; gap: 5px; font-size: 14px; }
.info-group .label { color: #888; }
.info-group .price { color: #e4393c; font-weight: bold; font-size: 16px; }

.status-tag { padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; }
.status-0 { background: #fff3cd; color: #856404; }
.status-1 { background: #cce5ff; color: #004085; }
.status-2, .status-3 { background: #d4edda; color: #155724; }
.status-4 { background: #e2e3e5; color: #383d41; }
.status-5 { background: #f8d7da; color: #721c24; }

.toggle-btn { background: none; border: 1px solid #ccc; padding: 6px 12px; border-radius: 4px; cursor: pointer; color: #555; }
.toggle-btn:hover { background: #eee; }

.order-detail { padding: 20px; background: #fff; display: flex; gap: 40px; }
.receiver-info { flex: 1; border-right: 1px dashed #eee; padding-right: 20px; }
.receiver-info p { margin: 8px 0; color: #555; font-size: 14px; }
.items-info { flex: 2; }

h4 { margin-top: 0; color: #333; margin-bottom: 15px; }
.item-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.item-table th, .item-table td { padding: 10px; text-align: left; border-bottom: 1px solid #f0f0f0; }
.item-table .subtotal { font-weight: bold; color: #333; }

.action-group { display: flex; gap: 10px; align-items: center; }
.action-btn { padding: 5px 10px; border: none; border-radius: 4px; cursor: pointer; color: white; font-weight: bold; }
.pay-btn { background-color: #28a745; }
.receive-btn { background-color: #17a2b8; }
</style>