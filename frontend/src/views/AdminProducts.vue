<template>
  <div class="admin-container">
    <div class="admin-header">
      <h2>📦 后台管理 - 商品库</h2>
      <div class="actions">
        <button class="nav-btn" @click="router.push('/admin/orders')">前往订单管理</button>
        <button class="add-btn" @click="openAddModal">＋ 新增商品</button>
        <button class="back-btn" @click="router.push('/products')">退出后台</button>
      </div>
    </div>

    <table class="admin-table">
      <thead>
      <tr>
        <th>ID</th>
        <th>商品名称</th>
        <th>价格</th>
        <th>库存</th>
        <th>状态</th>
        <th>操作</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="p in products" :key="p.id" :class="{ inactive: !p.active }">
        <td>{{ p.id }}</td>
        <td>{{ p.name }}</td>
        <td class="price">¥{{ p.price.toFixed(2) }}</td>
        <td>{{ p.stock }}</td>
        <td>
            <span :class="p.active ? 'tag-active' : 'tag-inactive'">
              {{ p.active ? '上架中' : '已下架' }}
            </span>
        </td>
        <td>
          <button class="edit-btn" @click="openEditModal(p)">编辑</button>
          <button class="del-btn" v-if="p.active" @click="handleDeactivate(p.id)">下架</button>
        </td>
      </tr>
      </tbody>
    </table>

    <div class="modal-overlay" v-if="showModal">
      <div class="modal-content">
        <h3>{{ isEditMode ? '编辑商品' : '新增商品' }}</h3>

        <div class="form-group">
          <label>商品名称 (2-50字符)</label>
          <input type="text" v-model="form.name" />
        </div>
        <div class="form-group">
          <label>商品描述 (10-500字符)</label>
          <textarea v-model="form.description" rows="3"></textarea>
        </div>
        <div class="form-group">
          <label>商品价格 (0.01 - 999999.99)</label>
          <input type="number" step="0.01" v-model.number="form.price" />
        </div>
        <div class="form-group">
          <label>库存数量 (0 - 99999)</label>
          <input type="number" v-model.number="form.stock" />
        </div>

        <div class="modal-actions">
          <button class="cancel-btn" @click="showModal = false">取消</button>
          <button class="save-btn" :disabled="submitting" @click="handleSubmit">
            {{ submitting ? '保存中...' : '确认保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi, AdminProductRequest } from '../api/admin'

const router = useRouter()
const products = ref<any[]>([])
const showModal = ref(false)
const isEditMode = ref(false)
const submitting = ref(false)
const currentEditId = ref<number | null>(null)

// 统一的表单绑定对象
const form = ref<AdminProductRequest>({
  name: '',
  description: '',
  price: 0,
  stock: 0
})

// 加载全部商品
const loadProducts = async () => {
  try {
    // 直接调用管理员专属的全量接口
    const res = await adminApi.getAllProducts()
    products.value = res.data.data // 这里直接拿 data 数组，不需要解析 pageData.items 了
  } catch (error: any) {
    if (error.response && error.response.status === 403) {
      alert('您没有管理员权限！')
      router.push('/products')
    }
  }
}

// 打开新增弹窗
const openAddModal = () => {
  isEditMode.value = false
  currentEditId.value = null
  form.value = { name: '', description: '', price: 0, stock: 0 }
  showModal.value = true
}

// 打开编辑弹窗（数据回显）
const openEditModal = (p: any) => {
  isEditMode.value = true
  currentEditId.value = p.id
  form.value = {
    name: p.name,
    description: p.description,
    price: p.price,
    stock: p.stock
  }
  showModal.value = true
}

// 下架商品
const handleDeactivate = async (id: number) => {
  if (!confirm('确认要下架该商品吗？下架后前台将无法搜索和购买。')) return
  try {
    await adminApi.deactivateProduct(id)
    alert('已成功下架')
    loadProducts()
  } catch (error) {
    alert('下架失败')
  }
}

// 前端表单预校验 (防呆)
const validateForm = () => {
  const { name, description, price, stock } = form.value
  if (!name || name.trim().length < 2 || name.length > 50) return '名称必须在2-50字符之间'
  if (!description || description.trim().length < 10 || description.length > 500) return '描述必须在10-500字符之间'
  if (price < 0.01 || price > 999999.99) return '价格超出允许范围'
  if (stock < 0 || stock > 99999 || !Number.isInteger(stock)) return '库存必须为非负整数'
  return null
}

// 提交保存
const handleSubmit = async () => {
  const errMsg = validateForm()
  if (errMsg) {
    alert(errMsg)
    return
  }

  submitting.value = true
  try {
    if (isEditMode.value && currentEditId.value) {
      await adminApi.updateProduct(currentEditId.value, form.value)
      alert('商品更新成功')
    } else {
      await adminApi.addProduct(form.value)
      alert('商品新增成功')
    }
    showModal.value = false
    loadProducts()
  } catch (error: any) {
    if (error.response && error.response.data) {
      alert('保存失败：' + error.response.data.message)
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.admin-container { max-width: 1100px; margin: 30px auto; font-family: sans-serif; }
.admin-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #333; padding-bottom: 15px; margin-bottom: 20px; }
.actions button { margin-left: 10px; padding: 8px 15px; border-radius: 4px; cursor: pointer; border: none; font-weight: bold; }
.nav-btn { background: #17a2b8; color: white; }
.add-btn { background: #28a745; color: white; }
.back-btn { background: #6c757d; color: white; }

.admin-table { width: 100%; border-collapse: collapse; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
.admin-table th, .admin-table td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #eee; }
.admin-table th { background: #f4f4f4; color: #333; }
.admin-table tr:hover { background: #f9f9f9; }
.inactive { opacity: 0.6; background: #fafafa; }
.price { color: #e4393c; font-weight: bold; }

.tag-active { background: #d4edda; color: #155724; padding: 4px 8px; border-radius: 4px; font-size: 12px; }
.tag-inactive { background: #f8d7da; color: #721c24; padding: 4px 8px; border-radius: 4px; font-size: 12px; }

.edit-btn, .del-btn { padding: 5px 10px; margin-right: 5px; border: none; border-radius: 4px; cursor: pointer; }
.edit-btn { background: #007bff; color: white; }
.del-btn { background: #dc3545; color: white; }

/* 弹窗样式 */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center; z-index: 100; }
.modal-content { background: white; padding: 30px; border-radius: 8px; width: 400px; box-shadow: 0 4px 12px rgba(0,0,0,0.2); }
.modal-content h3 { margin-top: 0; margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 10px; }
.form-group { margin-bottom: 15px; display: flex; flex-direction: column; }
.form-group label { margin-bottom: 5px; font-weight: bold; font-size: 14px; }
.form-group input, .form-group textarea { padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 25px; }
.cancel-btn { padding: 8px 15px; border: 1px solid #ccc; background: white; cursor: pointer; border-radius: 4px; }
.save-btn { padding: 8px 15px; background: #28a745; color: white; border: none; cursor: pointer; border-radius: 4px; }
.save-btn:disabled { background: #888; cursor: not-allowed; }
</style>