<template>
  <div class="container">
    <h2>系统登录</h2>
    <form @submit.prevent="handleLogin" class="form-box">

      <div class="form-group">
        <label>用户名：</label>
        <input type="text" v-model="loginForm.username" required />
      </div>

      <div class="form-group">
        <label>密  码：</label>
        <input type="password" v-model="loginForm.password" required />
      </div>

      <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>

      <button type="submit">登 录</button>

      <p>没有账号？ <router-link to="/register">去注册</router-link></p>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api/auth'

const router = useRouter()
const loginForm = ref({ username: '', password: '' })
const errorMessage = ref('')

const handleLogin = async () => {
  errorMessage.value = '' // 清空历史错误
  try {
    const res = await authApi.login(loginForm.value)
    if (res.data.code === 200) {
      alert('登录成功！欢迎: ' + res.data.data.username)
      // 将真实的用户 ID 存入浏览器本地（请根据你后端实际返回的字段名调整 data.data.id）
      localStorage.setItem('userId', res.data.data.id.toString())
      router.push('/products')
    }
  } catch (error: any) {
    // 捕获后端的 400 或 403 错误（账号锁定等）
    if (error.response && error.response.data) {
      errorMessage.value = error.response.data.message
    } else {
      errorMessage.value = '网络连接失败，请检查后端是否启动'
    }
  }
}
</script>

<style scoped>
/* 极简原生态样式 */
.container { display: flex; flex-direction: column; align-items: center; margin-top: 50px; }
.form-box { display: flex; flex-direction: column; width: 300px; gap: 15px; }
.form-group { display: flex; justify-content: space-between; }
.error-msg { color: red; font-size: 14px; text-align: center; }
button { padding: 8px; cursor: pointer; }
</style>