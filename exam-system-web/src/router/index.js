import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import Login from '../views/Login.vue'

const routes = [
  { path: '/login', component: Login, meta: { public: true } },
  {
    path: '/', component: MainLayout, redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'users', component: () => import('../views/UserManage.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'operation-logs', component: () => import('../views/OperationLog.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'courses', component: () => import('../views/CourseManage.vue'), meta: { roles: ['ADMIN', 'TEACHER'] } },
      { path: 'questions', component: () => import('../views/QuestionManage.vue'), meta: { roles: ['ADMIN', 'TEACHER'] } },
      { path: 'exams', component: () => import('../views/ExamManage.vue'), meta: { roles: ['ADMIN', 'TEACHER'] } },
      { path: 'auto-paper/:id', component: () => import('../views/AutoPaper.vue'), meta: { roles: ['ADMIN', 'TEACHER'] } },
      { path: 'teacher/exam/:examId/manual-paper', component: () => import('../views/ManualPaper.vue'), meta: { roles: ['ADMIN', 'TEACHER'] } },
      { path: 'teacher/exam/:examId/preview', component: () => import('../views/PaperPreview.vue'), meta: { roles: ['ADMIN', 'TEACHER'] } },
      { path: 'teacher/review', component: () => import('../views/TeacherReview.vue'), meta: { roles: ['ADMIN', 'TEACHER'] } },
      { path: 'teacher/monitor', component: () => import('../views/ExamMonitor.vue'), meta: { roles: ['ADMIN', 'TEACHER'] } },
      { path: 'student-exams', component: () => import('../views/StudentExamList.vue'), meta: { roles: ['STUDENT'] } },
      { path: 'online-exam/:id', component: () => import('../views/OnlineExam.vue'), meta: { roles: ['STUDENT'] } },
      { path: 'civil-service-skill', component: () => import('../views/CivilServiceSkill.vue'), meta: { roles: ['STUDENT'] } },
      { path: 'scores', component: () => import('../views/ScoreQuery.vue'), meta: { roles: ['STUDENT'] } },
      { path: 'wrong-questions', component: () => import('../views/WrongQuestion.vue'), meta: { roles: ['STUDENT'] } },
      { path: 'statistics', component: () => import('../views/Statistics.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach(to => {
  let token = localStorage.getItem('exam-token')
  let user = null
  try {
    user = JSON.parse(localStorage.getItem('exam-user') || 'null')
  } catch {
    localStorage.removeItem('exam-token')
    localStorage.removeItem('exam-user')
    token = ''
  }
  if (!to.meta.public && !token) return '/login'
  if (!to.meta.public && !user) return '/login'
  if (to.path === '/login' && token) return '/'
  if (to.meta.roles && !to.meta.roles.includes(user?.role)) return '/dashboard'
})
export default router
