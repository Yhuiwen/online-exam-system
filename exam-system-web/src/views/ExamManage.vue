<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  assignExamProctors,
  createExam,
  deleteExam,
  getExamQuestions,
  getExams,
  updateExamStatus
} from '../api/exam'
import { generateAiPaper } from '../api/aiQuestion'
import { getCourses } from '../api/course'
import { getUsers } from '../api/user'
import { useAuthStore } from '../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatExamStatus, formatQuestionType } from '../utils/enumMap'

const router = useRouter()
const auth = useAuthStore()
const isAdmin = computed(() => auth.user?.role === 'ADMIN')

const rows = ref([])
const courses = ref([])
const teachers = ref([])
const visible = ref(false)
const questionVisible = ref(false)
const proctorVisible = ref(false)
const questions = ref([])
const loading = ref(false)
const saving = ref(false)
const statusLoadingId = ref(null)
const proctorLoading = ref(false)
const currentExam = ref(null)
const selectedProctors = ref([])
const aiPaperVisible = ref(false)
const aiPaperLoading = ref(false)
const aiPaperExam = ref(null)
const aiPaperSections = ref([])

const defaultSection = () => ({
  questionType: 'SINGLE_CHOICE',
  difficulty: 'EASY',
  knowledgePoint: '',
  count: 5,
  score: 5,
  requirement: ''
})

const form = reactive({
  examName: '',
  courseId: null,
  startTime: null,
  endTime: null,
  durationMinutes: 60
})

function statusTagType(status) {
  return { DRAFT: 'info', PUBLISHED: 'success', CLOSED: 'warning' }[status] || 'info'
}

async function load() {
  loading.value = true
  try {
    rows.value = await getExams()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    examName: '',
    courseId: courses.value[0]?.id || null,
    startTime: null,
    endTime: null,
    durationMinutes: 60
  })
  visible.value = true
}

async function save() {
  saving.value = true
  try {
    await createExam(form)
    visible.value = false
    ElMessage.success('考试创建成功')
    await load()
  } finally {
    saving.value = false
  }
}

async function changeStatus(id, value) {
  statusLoadingId.value = id
  try {
    await updateExamStatus(id, value)
    await load()
  } finally {
    statusLoadingId.value = null
  }
}

async function removeExam(row) {
  await ElMessageBox.confirm(`确认删除考试“${row.examName}”吗？`, '删除确认', { type: 'warning' })
  await deleteExam(row.id)
  ElMessage.success('考试已删除')
  await load()
}

async function openProctorDialog(row) {
  currentExam.value = row
  selectedProctors.value = (row.proctors || []).map(item => item.teacherId)
  if (!teachers.value.length) {
    const page = await getUsers({ role: 'TEACHER', pageNum: 1, pageSize: 200, status: 1 })
    teachers.value = page.records || []
  }
  proctorVisible.value = true
}

async function saveProctors() {
  if (!selectedProctors.value.length) {
    ElMessage.warning('请至少选择一名监考教师')
    return
  }
  proctorLoading.value = true
  try {
    await assignExamProctors(currentExam.value.id, selectedProctors.value)
    ElMessage.success('监考教师分配成功')
    proctorVisible.value = false
    await load()
  } finally {
    proctorLoading.value = false
  }
}

async function showQuestions(id) {
  questions.value = await getExamQuestions(id)
  questionVisible.value = true
}

function proctorText(row) {
  return (row.proctors || []).map(item => item.teacherName).join('、') || '未分配'
}

function openAiPaper(row) {
  aiPaperExam.value = row
  aiPaperSections.value = [defaultSection()]
  aiPaperVisible.value = true
}

function addAiPaperSection() {
  aiPaperSections.value.push(defaultSection())
}

function removeAiPaperSection(index) {
  if (aiPaperSections.value.length <= 1) return
  aiPaperSections.value.splice(index, 1)
}

async function submitAiPaper() {
  aiPaperLoading.value = true
  try {
    const preview = await generateAiPaper({
      examId: aiPaperExam.value.id,
      sections: aiPaperSections.value
    })
    aiPaperVisible.value = false
    ElMessage.success(`AI 组卷成功，共 ${preview.questionCount} 题，总分 ${preview.totalScore}`)
    await load()
  } catch {
    ElMessage.error('AI 组卷失败，请检查规则后重试')
  } finally {
    aiPaperLoading.value = false
  }
}

onMounted(async () => {
  courses.value = await getCourses()
  await load()
})
</script>

<template>
  <div class="page exam-manage-page">
    <div class="page-header">
      <h1 class="page-title">考试管理</h1>
      <el-button type="primary" @click="openCreate">创建考试</el-button>
    </div>

    <section class="flow-card">
      <div>
        <h2>考试流程</h2>
        <p>从考试创建到成绩统计，覆盖教师组卷、学生答题与后续分析的完整闭环。</p>
      </div>
      <div class="flow-steps">
        <span>创建考试</span>
        <span>组卷</span>
        <span>预览</span>
        <span>发布</span>
        <span>学生答题</span>
        <span>统计</span>
      </div>
    </section>

    <div class="panel">
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="examName" label="考试名称" min-width="180" />
        <el-table-column prop="teacherName" label="创建教师" width="120" />
        <el-table-column v-if="isAdmin" label="监考教师" min-width="180">
          <template #default="{ row }">{{ proctorText(row) }}</template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">{{ formatExamStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="520" fixed="right">
          <template #default="{ row }">
            <el-button link @click="showQuestions(row.id)">查看题目</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="warning" @click="openAiPaper(row)">AI 组卷</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="router.push(`/auto-paper/${row.id}`)">自动组卷</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="router.push(`/teacher/exam/${row.id}/manual-paper`)">手动组卷</el-button>
            <el-button link @click="router.push(`/teacher/exam/${row.id}/preview`)">试卷预览</el-button>
            <el-button v-if="isAdmin" link type="primary" @click="openProctorDialog(row)">分配监考</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="success" :loading="statusLoadingId === row.id" @click="changeStatus(row.id, 'PUBLISHED')">发布</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" link type="danger" :loading="statusLoadingId === row.id" @click="changeStatus(row.id, 'CLOSED')">关闭</el-button>
            <el-button v-if="row.status !== 'PUBLISHED'" link type="danger" @click="removeExam(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!rows.length" description="暂无考试，请先创建考试并完成组卷" />
    </div>

    <el-dialog v-model="visible" title="创建考试" width="560px">
      <el-form label-width="100px">
        <el-form-item label="考试名称"><el-input v-model="form.examName" /></el-form-item>
        <el-form-item label="课程">
          <el-select v-model="form.courseId">
            <el-option v-for="course in courses" :key="course.id" :label="course.courseName" :value="course.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="考试时长">
          <el-input-number v-model="form.durationMinutes" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="proctorVisible" title="分配监考教师" width="520px">
      <p class="dialog-tip">为考试“{{ currentExam?.examName }}”选择监考教师，被选中的教师可查看该场考试监控数据。</p>
      <el-select v-model="selectedProctors" multiple filterable placeholder="请选择监考教师" style="width: 100%">
        <el-option v-for="teacher in teachers" :key="teacher.id" :label="`${teacher.realName} (${teacher.username})`" :value="teacher.id" />
      </el-select>
      <template #footer>
        <el-button @click="proctorVisible = false">取消</el-button>
        <el-button type="primary" :loading="proctorLoading" @click="saveProctors">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiPaperVisible" title="AI 一键组卷" width="760px">
      <p class="dialog-tip">为考试“{{ aiPaperExam?.examName }}”按规则生成题目并写入试卷，生成后可在试卷预览中检查。</p>
      <div v-for="(section, index) in aiPaperSections" :key="index" class="ai-section">
        <div class="ai-section-header">
          <strong>规则 {{ index + 1 }}</strong>
          <el-button v-if="aiPaperSections.length > 1" link type="danger" @click="removeAiPaperSection(index)">删除</el-button>
        </div>
        <el-form label-width="90px">
          <el-form-item label="题型">
            <el-select v-model="section.questionType">
              <el-option label="单选题" value="SINGLE_CHOICE" />
              <el-option label="多选题" value="MULTIPLE_CHOICE" />
              <el-option label="判断题" value="TRUE_FALSE" />
              <el-option label="填空题" value="FILL_BLANK" />
              <el-option label="简答题" value="SHORT_ANSWER" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="section.difficulty">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </el-form-item>
          <el-form-item label="知识点"><el-input v-model="section.knowledgePoint" placeholder="可选" /></el-form-item>
          <el-form-item label="数量"><el-input-number v-model="section.count" :min="1" :max="20" /></el-form-item>
          <el-form-item label="分值"><el-input-number v-model="section.score" :min="1" :max="100" /></el-form-item>
          <el-form-item label="额外要求"><el-input v-model="section.requirement" type="textarea" :rows="2" /></el-form-item>
        </el-form>
      </div>
      <el-button link type="primary" @click="addAiPaperSection">+ 添加规则</el-button>
      <template #footer>
        <el-button @click="aiPaperVisible = false">取消</el-button>
        <el-button type="primary" :loading="aiPaperLoading" @click="submitAiPaper">开始 AI 组卷</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="questionVisible" title="试卷题目" width="720px">
      <el-table :data="questions">
        <el-table-column type="index" width="55" />
        <el-table-column prop="content" label="题目" />
        <el-table-column label="题型" width="150">
          <template #default="{ row }">{{ formatQuestionType(row.questionType) }}</template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="70" />
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.exam-manage-page { display: grid; gap: 16px; }
.flow-card {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 20px;
  align-items: center;
  padding: 20px 22px;
  border-radius: 10px;
  border: 1px solid #dbeafe;
  background: #fff;
}
.flow-card h2 { margin: 0 0 8px; font-size: 18px; }
.flow-card p { margin: 0; color: #64748b; line-height: 1.7; }
.flow-steps {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
}
.flow-steps span {
  position: relative;
  padding: 12px 10px;
  border-radius: 8px;
  background: #eff6ff;
  color: #1d4ed8;
  text-align: center;
  font-weight: 650;
}
.dialog-tip { color: #64748b; margin: 0 0 16px; line-height: 1.6; }
.ai-section {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 16px 4px;
  margin-bottom: 12px;
}
.ai-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
@media (max-width: 980px) {
  .flow-card { grid-template-columns: 1fr; }
  .flow-steps { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
</style>
