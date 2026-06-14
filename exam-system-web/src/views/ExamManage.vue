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

const form = reactive({
  examName: '',
  courseId: null,
  startTime: null,
  endTime: null,
  durationMinutes: 60
})

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
  await ElMessageBox.confirm(`确认删除考试「${row.examName}」吗？`, '删除确认', { type: 'warning' })
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

onMounted(async () => {
  courses.value = await getCourses()
  await load()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">考试管理</h1>
      <el-button type="primary" @click="openCreate">创建考试</el-button>
    </div>

    <div class="panel">
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="examName" label="考试名称" min-width="180" />
        <el-table-column prop="teacherName" label="创建教师" width="120" />
        <el-table-column v-if="isAdmin" label="监考教师" min-width="180">
          <template #default="{ row }">{{ proctorText(row) }}</template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">{{ formatExamStatus(row.status) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="520" fixed="right">
          <template #default="{ row }">
            <el-button link @click="showQuestions(row.id)">查看题目</el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              link
              type="primary"
              @click="router.push(`/auto-paper/${row.id}`)"
            >
              自动组卷
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              link
              type="primary"
              @click="router.push(`/teacher/exam/${row.id}/manual-paper`)"
            >
              手动组卷
            </el-button>
            <el-button link @click="router.push(`/teacher/exam/${row.id}/preview`)">试卷预览</el-button>
            <el-button
              v-if="isAdmin"
              link
              type="primary"
              @click="openProctorDialog(row)"
            >
              分配监考
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              link
              type="success"
              :loading="statusLoadingId === row.id"
              @click="changeStatus(row.id, 'PUBLISHED')"
            >
              发布
            </el-button>
            <el-button
              v-if="row.status === 'PUBLISHED'"
              link
              type="danger"
              :loading="statusLoadingId === row.id"
              @click="changeStatus(row.id, 'CLOSED')"
            >
              关闭
            </el-button>
            <el-button
              v-if="row.status !== 'PUBLISHED'"
              link
              type="danger"
              @click="removeExam(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="visible" title="创建考试" width="560px">
      <el-form label-width="100px">
        <el-form-item label="考试名称"><el-input v-model="form.examName" /></el-form-item>
        <el-form-item label="课程">
          <el-select v-model="form.courseId">
            <el-option
              v-for="course in courses"
              :key="course.id"
              :label="course.courseName"
              :value="course.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="考试时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="时长">
          <el-input-number v-model="form.durationMinutes" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="proctorVisible" title="分配监考教师" width="520px">
      <p class="proctor-tip">为考试「{{ currentExam?.examName }}」选择监考教师，被选中的教师可查看该场考试监控数据。</p>
      <el-select v-model="selectedProctors" multiple filterable placeholder="请选择监考教师" style="width: 100%">
        <el-option
          v-for="teacher in teachers"
          :key="teacher.id"
          :label="`${teacher.realName} (${teacher.username})`"
          :value="teacher.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="proctorVisible = false">取消</el-button>
        <el-button type="primary" :loading="proctorLoading" @click="saveProctors">保存</el-button>
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
.proctor-tip { color: #64748b; margin: 0 0 16px; line-height: 1.6; }
</style>
