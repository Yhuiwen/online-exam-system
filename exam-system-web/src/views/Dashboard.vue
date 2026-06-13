<script setup>
import { useAuthStore } from '../store/auth'
const auth = useAuthStore()
const roleText = { ADMIN: '系统管理员', TEACHER: '教师', STUDENT: '学生' }
</script>
<template>
  <div class="page">
    <div class="page-header"><h1 class="page-title">工作台</h1></div>
    <div class="welcome">
      <span>{{ roleText[auth.user?.role] }}</span>
      <h2>你好，{{ auth.user?.realName }}</h2>
      <p>今天也来高效地完成教学与考试任务。</p>
    </div>
    <div class="quick-grid">
      <router-link v-if="auth.user?.role==='TEACHER'" to="/questions">维护题库</router-link>
      <router-link v-if="auth.user?.role==='TEACHER'" to="/exams">管理考试</router-link>
      <router-link v-if="auth.user?.role==='STUDENT'" to="/student-exams">参加考试</router-link>
      <router-link v-if="auth.user?.role==='STUDENT'" to="/wrong-questions">复习错题</router-link>
      <router-link to="/statistics">查看统计</router-link>
    </div>
  </div>
</template>
<style scoped>
.welcome { padding: 38px; background: #12355b; color: #fff; }
.welcome span { color: #fbbf24; }.welcome h2 { font-size: 32px; margin: 12px 0; }.welcome p { color: #cbd5e1; }
.quick-grid { display: grid; grid-template-columns: repeat(auto-fit,minmax(180px,1fr)); gap: 14px; margin-top: 18px; }
.quick-grid a { background: #fff; padding: 24px; color: #1d4ed8; text-decoration: none; border: 1px solid #e5e7eb; border-radius: 6px; font-weight: 600; }
</style>
