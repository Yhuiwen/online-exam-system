<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getExams } from '../api/exam'
import { getStudentExams, startExam } from '../api/studentExam'

const rows = ref([])
const records = ref(new Map())
const router = useRouter()

async function start(row) {
  const record = await startExam(row.id)
  router.push({ path: `/online-exam/${row.id}`, query: { studentExamId: record.id } })
}

function recordOf(examId) {
  return records.value.get(examId)
}

onMounted(async () => {
  const [exams, attempts] = await Promise.all([getExams(), getStudentExams()])
  rows.value = exams
  records.value = new Map(attempts.map(item => [item.examId, item]))
})
</script>

<template>
  <div class="page student-exam-page">
    <div class="page-header">
      <h1 class="page-title">可参加考试</h1>
    </div>

    <section class="exam-student-hero">
      <div>
        <h2>学生在线考试入口</h2>
        <p>展示已发布考试、考试时间、时长与总分，支持开始考试、继续答题和提交状态跟踪。</p>
      </div>
      <el-tag type="primary" effect="dark">Online Exam</el-tag>
    </section>

    <div class="panel">
      <el-table :data="rows">
        <el-table-column prop="examName" label="考试名称" />
        <el-table-column prop="startTime" label="开始时间" />
        <el-table-column prop="endTime" label="结束时间" />
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="!recordOf(row.id)" type="primary" link @click="start(row)">开始考试</el-button>
            <el-button v-else-if="recordOf(row.id).status === 'IN_PROGRESS'" type="warning" link @click="start(row)">继续答题</el-button>
            <el-tag v-else type="success">已提交</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!rows.length" description="暂无可参加考试，考试发布后会显示在这里" />
    </div>
  </div>
</template>

<style scoped>
.student-exam-page { display: grid; gap: 16px; }
.exam-student-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 22px;
  border-radius: 10px;
  background: linear-gradient(135deg, #eff6ff, #ffffff);
  border: 1px solid #dbeafe;
}
.exam-student-hero h2 { margin: 0 0 8px; font-size: 18px; }
.exam-student-hero p { margin: 0; color: #64748b; line-height: 1.7; }
</style>
