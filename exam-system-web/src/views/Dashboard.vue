<script setup>
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const roleText = { ADMIN: '系统管理员', TEACHER: '教师', STUDENT: '学生' }
const entries = [
  { title: '题库管理', desc: '维护题型、难度、知识点与题目来源，支持批量导入导出。', to: '/questions', roles: ['ADMIN', 'TEACHER'] },
  { title: '考试管理', desc: '创建考试、组卷、预览、发布并跟踪考试进度。', to: '/exams', roles: ['ADMIN', 'TEACHER'] },
  { title: '主观题批改', desc: '集中处理简答题等主观题，提升阅卷效率。', to: '/teacher/review', roles: ['ADMIN', 'TEACHER'] },
  { title: '成绩统计', desc: '查看参考人数、分数分布、题目正确率和成绩趋势。', to: '/statistics', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { title: '参加考试', desc: '查看可参加考试并进入在线答题流程。', to: '/student-exams', roles: ['STUDENT'] },
  { title: '错题复习', desc: '沉淀错题记录，辅助学生针对性复习。', to: '/wrong-questions', roles: ['STUDENT'] }
]
</script>

<template>
  <div class="page dashboard-page">
    <section class="welcome">
      <div>
        <span>{{ roleText[auth.user?.role] || '用户' }}工作台</span>
        <h1>你好，{{ auth.user?.realName || auth.user?.username }}</h1>
        <p>覆盖题库维护、智能组卷、在线考试、自动判分、主观题批改与成绩统计的完整考试业务闭环。</p>
      </div>
      <div class="welcome-metrics">
        <strong>6</strong>
        <span>核心能力模块</span>
      </div>
    </section>

    <section class="showcase-panel">
      <div class="section-heading">
        <h2>系统能力</h2>
        <p>围绕教师出卷阅卷、学生在线答题、管理员运营监控构建前后端分离实践。</p>
      </div>
      <div class="ability-grid">
        <router-link
          v-for="entry in entries.filter(item => item.roles.includes(auth.user?.role))"
          :key="entry.title"
          class="ability-card"
          :to="entry.to"
        >
          <strong>{{ entry.title }}</strong>
          <span>{{ entry.desc }}</span>
        </router-link>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 18px;
}
.welcome {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 34px;
  border-radius: 12px;
  background: linear-gradient(135deg, #12355b, #2563eb);
  color: #fff;
  overflow: hidden;
}
.welcome span {
  color: #bfdbfe;
  font-weight: 700;
}
.welcome h1 {
  margin: 12px 0;
  font-size: 34px;
}
.welcome p {
  max-width: 760px;
  margin: 0;
  color: #dbeafe;
  line-height: 1.8;
}
.welcome-metrics {
  min-width: 150px;
  align-self: center;
  padding: 20px;
  border-radius: 10px;
  background: rgba(255, 255, 255, .12);
  text-align: center;
}
.welcome-metrics strong {
  display: block;
  font-size: 38px;
}
.showcase-panel {
  padding: 24px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
}
.section-heading {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: end;
  margin-bottom: 18px;
}
.section-heading h2 {
  margin: 0;
}
.section-heading p {
  max-width: 620px;
  margin: 0;
  color: #64748b;
}
.ability-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}
.ability-card {
  min-height: 148px;
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  color: #0f172a;
  text-decoration: none;
  background: #f8fafc;
  transition: .18s ease;
}
.ability-card:hover {
  border-color: #93c5fd;
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(15, 23, 42, .08);
}
.ability-card strong {
  display: block;
  margin-bottom: 12px;
  color: #1d4ed8;
  font-size: 18px;
}
.ability-card span {
  color: #64748b;
  line-height: 1.7;
}
@media (max-width: 980px) {
  .ability-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .welcome, .section-heading { flex-direction: column; align-items: flex-start; }
}
</style>
