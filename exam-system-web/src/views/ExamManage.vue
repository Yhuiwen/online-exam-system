<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createExam, getExamQuestions, getExams, updateExamStatus } from '../api/exam'
import { getCourses } from '../api/course'
import { ElMessage } from 'element-plus'

const router = useRouter()
const rows = ref([])
const courses = ref([])
const visible = ref(false)
const questionVisible = ref(false)
const questions = ref([])
const loading = ref(false)
const saving = ref(false)
const statusLoadingId = ref(null)

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

async function showQuestions(id) {
  questions.value = await getExamQuestions(id)
  questionVisible.value = true
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
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column label="操作" min-width="430" fixed="right">
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
            <el-button
              link
              @click="router.push(`/teacher/exam/${row.id}/preview`)"
            >
              试卷预览
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
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
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

    <el-dialog v-model="questionVisible" title="试卷题目" width="720px">
      <el-table :data="questions">
        <el-table-column type="index" width="55" />
        <el-table-column prop="content" label="题目" />
        <el-table-column prop="questionType" label="题型" width="150" />
        <el-table-column prop="score" label="分值" width="70" />
      </el-table>
    </el-dialog>
  </div>
</template>
