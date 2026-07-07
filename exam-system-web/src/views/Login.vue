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

const demoAccounts = [
  ['管理员', 'admin', '123456'],
  ['教师', 'teacher', '123456'],
  ['学生', 'student', '123456']
]

async function submit() {
  loading.value = true
  try {
    await auth.signIn(form)
    router.push('/')
  } finally {
    loading.value = false
  }
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
      <p>支持题库维护、智能组卷、在线考试、自动判分、主观题批改、成绩统计。</p>
      <div class="capability-grid">
        <span>题库维护</span>
        <span>智能组卷</span>
        <span>在线考试</span>
        <span>自动判分</span>
        <span>主观题批改</span>
        <span>成绩统计</span>
      </div>
    </section>

    <section class="form-shell">
      <el-form class="login-form" label-position="top" @submit.prevent="submit">
        <div class="form-heading">
          <span>欢迎回来</span>
          <h2>账号登录</h2>
        </div>
        <el-form-item label="用户名">
          <el-input v-model="form.username" size="large" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            size="large"
            placeholder="请输入密码"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" native-type="submit">登录系统</el-button>
        <el-button link type="primary" @click="showRegister = true">注册学生账号</el-button>

        <div class="demo-card">
          <strong>演示账号</strong>
          <div v-for="account in demoAccounts" :key="account[0]" class="demo-row">
            <span>{{ account[0] }}</span>
            <code>{{ account[1] }} / {{ account[2] }}</code>
          </div>
        </div>
      </el-form>
    </section>
  </div>

  <el-dialog v-model="showRegister" title="学生注册" width="460">
    <el-form :model="registerForm" label-width="80">
      <el-form-item label="用户名"><el-input v-model="registerForm.username" /></el-form-item>
      <el-form-item label="密码"><el-input v-model="registerForm.password" type="password" /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="registerForm.realName" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="registerForm.email" /></el-form-item>
      <el-form-item label="手机"><el-input v-model="registerForm.phone" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showRegister = false">取消</el-button>
      <el-button type="primary" @click="doRegister">注册</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(420px, .85fr);
  background:
    radial-gradient(circle at 12% 18%, rgba(37, 99, 235, .16), transparent 28%),
    linear-gradient(135deg, #eef6ff 0%, #f8fbff 45%, #ffffff 100%);
}
.intro {
  padding: 12vh 8vw;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #0f172a;
}
.mark {
  width: 82px;
  border-top: 4px solid #2563eb;
  padding-top: 12px;
  color: #2563eb;
  font-weight: 800;
  letter-spacing: .08em;
}
h1 {
  max-width: 680px;
  margin: 28px 0 16px;
  font-size: 46px;
  line-height: 1.18;
}
.intro p {
  max-width: 660px;
  color: #475569;
  font-size: 18px;
  line-height: 1.8;
}
.capability-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  max-width: 660px;
  margin-top: 28px;
}
.capability-grid span {
  padding: 13px 16px;
  border: 1px solid rgba(37, 99, 235, .16);
  border-radius: 8px;
  background: rgba(255, 255, 255, .72);
  color: #1e3a8a;
  font-weight: 650;
}
.form-shell {
  display: grid;
  place-items: center;
  padding: 40px;
}
.login-form {
  width: min(420px, 100%);
  padding: 34px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: rgba(255, 255, 255, .92);
  box-shadow: 0 24px 70px rgba(15, 23, 42, .10);
}
.form-heading span {
  color: #2563eb;
  font-weight: 700;
}
.form-heading h2 {
  margin: 8px 0 26px;
  font-size: 28px;
}
.login-form > .el-button {
  width: 100%;
  margin: 8px 0;
}
.demo-card {
  margin-top: 22px;
  padding: 16px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
}
.demo-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 10px;
  color: #475569;
}
code {
  color: #0f172a;
  font-weight: 700;
}
@media (max-width: 860px) {
  .login-page { grid-template-columns: 1fr; }
  .intro { padding: 40px 24px; }
  h1 { font-size: 32px; }
  .capability-grid { grid-template-columns: repeat(2, 1fr); }
  .form-shell { padding: 24px; }
}
</style>
