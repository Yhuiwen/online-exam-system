<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { register } from '../api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const showRegister = ref(false)
const form = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', realName: '', email: '', phone: '' })
async function submit() {
  loading.value = true
  try { await auth.signIn(form); router.push('/') } finally { loading.value = false }
}
async function doRegister() {
  await register(registerForm)
  ElMessage.success('注册成功，请登录')
  form.username = registerForm.username
  form.password = registerForm.password
  showRegister.value = false
}
</script>

<template>
  <div class="login-page">
    <section class="intro">
      <div class="mark">EXAM</div>
      <h1>在线考试与智能题库管理系统</h1>
      <p>课程、题库、组卷、在线考试与成绩分析一体化管理</p>
    </section>
    <el-form class="login-form" label-position="top" @submit.prevent="submit">
      <h2>账号登录</h2>
      <el-form-item label="用户名"><el-input v-model="form.username" size="large" /></el-form-item>
      <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password size="large" @keyup.enter="submit" /></el-form-item>
      <el-button type="primary" size="large" :loading="loading" native-type="submit">登录</el-button>
      <el-button link type="primary" @click="showRegister = true">注册学生账号</el-button>
      <small>测试账号：admin / teacher / student，密码均为 123456</small>
    </el-form>
  </div>
  <el-dialog v-model="showRegister" title="学生注册" width="460">
    <el-form :model="registerForm" label-width="80">
      <el-form-item label="用户名"><el-input v-model="registerForm.username" /></el-form-item>
      <el-form-item label="密码"><el-input v-model="registerForm.password" type="password" /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="registerForm.realName" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="registerForm.email" /></el-form-item>
      <el-form-item label="手机"><el-input v-model="registerForm.phone" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="showRegister=false">取消</el-button><el-button type="primary" @click="doRegister">注册</el-button></template>
  </el-dialog>
</template>

<style scoped>
.login-page { min-height: 100vh; display: grid; grid-template-columns: 1.2fr 1fr; background: #fff; }
.intro { background: #12355b; color: #fff; padding: 12vh 9vw; display: flex; flex-direction: column; justify-content: center; }
.mark { width: 70px; border-top: 4px solid #f59e0b; padding-top: 12px; font-weight: 800; }
h1 { font-size: 44px; line-height: 1.25; max-width: 620px; margin: 28px 0 16px; }
.intro p { color: #cbd5e1; font-size: 18px; }
.login-form { width: min(380px, 80%); margin: auto; }
.login-form h2 { font-size: 28px; margin-bottom: 28px; }
.login-form > .el-button { width: 100%; margin: 8px 0; }
small { display: block; color: #6b7280; margin-top: 18px; line-height: 1.7; }
@media (max-width: 760px) { .login-page { grid-template-columns: 1fr; } .intro { padding: 35px 24px; } h1 { font-size: 28px; } .login-form { padding: 44px 0; } }
</style>
