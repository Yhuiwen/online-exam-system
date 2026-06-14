<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { DataAnalysis, Document, EditPen, House, List, Monitor, Reading, Tickets, User, Notebook } from '@element-plus/icons-vue'

const auth = useAuthStore()
const router = useRouter()
const menus = computed(() => {
  const all = [
    { path: '/dashboard', label: '首页', icon: House, roles: ['ADMIN','TEACHER','STUDENT'] },
    { path: '/users', label: '用户管理', icon: User, roles: ['ADMIN'] },
    { path: '/operation-logs', label: '操作日志', icon: Notebook, roles: ['ADMIN'] },
    { path: '/courses', label: '课程管理', icon: Reading, roles: ['ADMIN','TEACHER'] },
    { path: '/questions', label: '题库管理', icon: EditPen, roles: ['TEACHER'] },
    { path: '/exams', label: '考试管理', icon: Document, roles: ['ADMIN','TEACHER'] },
    { path: '/teacher/review', label: '主观题批改', icon: EditPen, roles: ['TEACHER'] },
    { path: '/teacher/monitor', label: '考试监控', icon: Monitor, roles: ['ADMIN','TEACHER'] },
    { path: '/student-exams', label: '参加考试', icon: Tickets, roles: ['STUDENT'] },
    { path: '/civil-service-skill', label: '公考刷题', icon: Reading, roles: ['STUDENT'] },
    { path: '/scores', label: '成绩查询', icon: List, roles: ['STUDENT'] },
    { path: '/wrong-questions', label: '错题本', icon: EditPen, roles: ['STUDENT'] },
    { path: '/statistics', label: '统计分析', icon: DataAnalysis, roles: ['ADMIN','TEACHER','STUDENT'] }
  ]
  return all.filter(x => x.roles.includes(auth.user?.role))
})
function logout() { auth.logout(); router.push('/login') }
</script>

<template>
  <el-container class="shell">
    <el-aside width="220px">
      <div class="brand">Exam System</div>
      <el-menu router :default-active="$route.path">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <span>在线考试与智能题库管理系统</span>
        <div><span>{{ auth.user?.realName }} · {{ auth.user?.role }}</span><el-button link type="primary" @click="logout">退出</el-button></div>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.shell { min-height: 100vh; }
.el-aside { background: #fff; border-right: 1px solid #e5e7eb; }
.brand { height: 64px; display: flex; align-items: center; padding: 0 20px; font-size: 19px; font-weight: 700; color: #1d4ed8; }
.el-menu { border-right: 0; }
.el-header { background: #fff; border-bottom: 1px solid #e5e7eb; display: flex; align-items: center; justify-content: space-between; }
.el-header div { display: flex; align-items: center; gap: 12px; }
.el-main { padding: 0; }
@media (max-width: 700px) { .el-aside { width: 72px !important; } .brand { font-size: 0; } .brand::after { content: "ES"; font-size: 18px; } .el-menu-item span { display: none; } }
</style>
