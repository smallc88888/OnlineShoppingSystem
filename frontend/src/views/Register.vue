<template>
  <div class="container">
    <h2>新用户注册</h2>
    <form @submit.prevent="handleRegister" class="form-box">

      <div class="form-group">
        <label>用户名：</label>
        <input type="text" v-model="regForm.username" required />
      </div>

      <div class="form-group">
        <label>密  码：</label>
        <input type="password" v-model="regForm.password" required />
      </div>

      <div class="form-group">
        <label>确认密码：</label>
        <input type="password" v-model="regForm.confirmPassword" required />
      </div>

      <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>

      <button type="submit">提交注册</button>

      <p>已有账号？ <router-link to="/login">去登录</router-link></p>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api/auth'

const router = useRouter()
const regForm = ref({ username: '', password: '', confirmPassword: '' })
const errorMessage = ref('')

const handleRegister = async () => {
  errorMessage.value = ''

  // 前端的基础格式拦截（也可以依赖后端抛错）
  if (regForm.value.password !== regForm.value.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致！'
    return
  }

  try {
    const res = await authApi.register(regForm.value)
    if (res.data.code === 200) {
      alert('注册成功，请登录！')
      router.push('/login') // 注册成功后自动跳转到登录页
    }
  } catch (error: any) {
    if (error.response && error.response.data) {
      errorMessage.value = error.response.data.message
    } else {
      errorMessage.value = '服务器连接失败'
    }
  }
}
</script>

<style scoped>
/* 复用上面的极简样式 */
.container { display: flex; flex-direction: column; align-items: center; margin-top: 50px; }
.form-box { display: flex; flex-direction: column; width: 300px; gap: 15px; }
.form-group { display: flex; justify-content: space-between; }
.error-msg { color: red; font-size: 14px; text-align: center; }
button { padding: 8px; cursor: pointer; }
</style>