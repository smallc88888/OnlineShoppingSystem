<template>
  <div class="admin-container">
    <div class="admin-header">
      <h2>📋 后台管理 - 订单大盘</h2>
      <div class="actions">
        <button class="nav-btn" @click="router.push('/admin/products')">前往商品管理</button>
        <button class="back-btn" @click="router.push('/products')">退出后台</button>
      </div>
    </div>

    <div v-if="loading" class="loading">加载订单数据中...</div>

    <div class="order-list" v-else>
      <div class="order-card" v-for="order in orders" :key="order.orderNo">
        <div class="order-header">
          <div class="info-group">
            <span class="label">订单编号：</span>
            <span class="value">{{ order.orderNo }}</span>
          </div>
          <div class="info-group">
            <span class="label">收货人：</span>
            <span class="value">{{ order.receiverName }}</span>
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
            <button class="action-btn confirm-btn" v-if="order.status === 0" @click="handleConfirm(order.orderNo)">
              核对无误并确认
            </button>
            <button class="action-btn ship-btn" v-if="order.status === 2" @click="handleShip(order.orderNo)">
              录入单号并发货
            </button>
            <button class="toggle-btn" @click="toggleDetail(order.orderNo)">
              {{ expandedOrder === order.orderNo ? '收起详情 ▲' : '展开详情 ▼' }}
            </button>
          </div>
        </div>

        <div class="order-detail" v-show="expandedOrder === order.orderNo">
          <div class="receiver-info">
            <h4>📍 收货详细信息</h4>
            <p><strong>联系电话：</strong>{{ order.receiverPhone }}</p>
            <p><strong>详细地址：</strong>{{ order.receiverAddress }}</p>
            <p><strong>下单时间：</strong>{{ formatTime(order.createdAt) }}</p>
          </div>

          <div class="items-info">
            <h4>📦 购买商品清单</h4>
            <table class="item-table">
              <thead>
              <tr>
                <th>商品名称</th>
                <th>单价</th>
                <th>数量</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="item in order.items" :key="item.productId">
                <td>{{ item.productName }}</td>
                <td>¥{{ item.buyPrice.toFixed(2) }}</td>
                <td>x{{ item.quantity }}</td>
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
import { adminApi } from '../api/admin'

const router = useRouter()
const orders = ref<any[]>([])
const loading = ref(true)
const expandedOrder = ref<string | null>(null)

// 拉取全站订单
const loadOrders = async () => {
  try {
    const res = await adminApi.getAllOrders()
    orders.value = res.data.data
  } catch (error: any) {
    if (error.response && error.response.status === 403) {
      alert('您没有管理员权限！')
      router.push('/products')
    }
  } finally {
    loading.value = false
  }
}

// 展开/收起详情
const toggleDetail = (orderNo: string) => {
  expandedOrder.value = expandedOrder.value === orderNo ? null : orderNo
}

// 管理员执行：确认订单 (状态 0 -> 1)
const handleConfirm = async (orderNo: string) => {
  // 从列表中找到对应的订单 ID（注意：前端展示用的是 orderNo，但接口可能需要内部 id。
  // 我们在之前写 OrderResponseDTO 时好像漏了把内部 id 传给前端，但没关系，我们稍微修改一下找 ID 的逻辑，
  // 或者直接去 OrderController.java 里的 OrderResponseDTO 补上 public Long id;
  // 假定后端已经补上了 id 字段：)
  const targetOrder = orders.value.find(o => o.orderNo === orderNo)
  if (!targetOrder || !targetOrder.id) {
    alert('前端未获取到订单的内部ID，请检查后端 DTO 是否返回了 id 字段')
    return
  }

  if (!confirm('确认该订单库存及物流条件无误吗？')) return
  try {
    await adminApi.confirmOrder(targetOrder.id)
    alert('订单已确认，等待用户付款')
    loadOrders() // 刷新列表
  } catch (error: any) {
    alert('操作失败：' + (error.response?.data?.message || '未知错误'))
  }
}

// 管理员执行：发货 (状态 2 -> 3)
const handleShip = async (orderNo: string) => {
  const targetOrder = orders.value.find(o => o.orderNo === orderNo)
  if (!targetOrder || !targetOrder.id) return

  if (!confirm('确认将该订单标记为已发货吗？')) return
  try {
    await adminApi.shipOrder(targetOrder.id)
    alert('发货成功！')
    loadOrders()
  } catch (error: any) {
    alert('操作失败：' + (error.response?.data?.message || '未知错误'))
  }
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待确认', 1: '待付款', 2: '已付款待发货',
    3: '已发货待收货', 4: '已完成', 5: '已取消'
  }
  return map[status] || '未知'
}

const formatTime = (timeData: string | number[]) => {
  if (!timeData) return ''

  // 如果后端传过来的是标准字符串 (例如: "2026-06-17T10:00:00")
  if (typeof timeData === 'string') {
    return timeData.replace('T', ' ').substring(0, 19)
  }

  // 如果后端传过来的是数组 (例如: [2026, 6, 17, 10, 0, 0])
  if (Array.isArray(timeData)) {
    const pad = (n: number) => n.toString().padStart(2, '0')
    const year = timeData[0]
    const month = pad(timeData[1])
    const day = pad(timeData[2])
    const hour = pad(timeData[3] || 0)
    const minute = pad(timeData[4] || 0)
    const second = pad(timeData[5] || 0)
    return `${year}-${month}-${day} ${hour}:${minute}:${second}`
  }

  return '格式异常'
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.admin-container { max-width: 1100px; margin: 30px auto; font-family: sans-serif; }
.admin-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #333; padding-bottom: 15px; margin-bottom: 20px; }
.actions button { margin-left: 10px; padding: 8px 15px; border-radius: 4px; cursor: pointer; border: none; font-weight: bold; }
.nav-btn { background: #17a2b8; color: white; }
.back-btn { background: #6c757d; color: white; }

.loading { text-align: center; padding: 50px; color: #888; }
.order-list { display: flex; flex-direction: column; gap: 15px; }
.order-card { border: 1px solid #ddd; border-radius: 8px; background: #fff; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
.order-header { display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; background: #fdfdfd; border-bottom: 1px solid #eee; }

.info-group { display: flex; flex-direction: column; gap: 5px; font-size: 14px; }
.info-group .label { color: #888; }
.info-group .price { color: #e4393c; font-weight: bold; font-size: 16px; }

.status-tag { padding: 4px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; }
.status-0 { background: #fff3cd; color: #856404; }
.status-1 { background: #cce5ff; color: #004085; }
.status-2 { background: #d4edda; color: #155724; }
.status-3 { background: #d1ecf1; color: #0c5460; }
.status-4 { background: #e2e3e5; color: #383d41; }
.status-5 { background: #f8d7da; color: #721c24; }

.action-group { display: flex; gap: 10px; align-items: center; }
.action-btn { padding: 6px 12px; border: none; border-radius: 4px; cursor: pointer; color: white; font-weight: bold; font-size: 13px; }
.confirm-btn { background: #f0ad4e; }
.ship-btn { background: #28a745; }
.toggle-btn { background: none; border: 1px solid #ccc; padding: 6px 12px; border-radius: 4px; cursor: pointer; color: #555; font-size: 13px; }

.order-detail { padding: 20px; background: #fff; display: flex; gap: 40px; }
.receiver-info { flex: 1; border-right: 1px dashed #eee; padding-right: 20px; }
.receiver-info p { margin: 8px 0; color: #555; font-size: 14px; }
.items-info { flex: 2; }
.item-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.item-table th, .item-table td { padding: 8px; text-align: left; border-bottom: 1px solid #f0f0f0; }
</style>