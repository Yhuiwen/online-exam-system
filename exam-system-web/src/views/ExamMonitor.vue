<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getExams } from '../api/exam'
import {
  getExamViolationSummary,
  getStudentExamViolations
} from '../api/examViolation'

const exams = ref([])
const examId = ref(null)
const rows = ref([])
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const selectedStudent = ref(null)
const violations = ref([])

const countFields = {
  PAGE_HIDDEN: 'pageHiddenCount',
  WINDOW_BLUR: 'windowBlurCount',
  FULLSCREEN_EXIT: 'fullscreenExitCount',
  COPY: 'copyCount',
  PASTE: 'pasteCount',
  RIGHT_CLICK: 'rightClickCount'
}

const riskLabels = {
  NORMAL: '正常',
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险'
}

const riskTagTypes = {
  NORMAL: 'success',
  LOW: 'info',
  MEDIUM: 'warning',
  HIGH: 'danger'
}

const violationLabels = {
  PAGE_HIDDEN: '页面隐藏/切屏',
  WINDOW_BLUR: '窗口失焦',
  FULLSCREEN_EXIT: '退出全屏',
  COPY: '复制',
  PASTE: '粘贴',
  RIGHT_CLICK: '右键',
  DEVTOOLS_SUSPECTED: '疑似开发者工具',
  OTHER: '其他'
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}

function countByType(details) {
  const counts = {
    pageHiddenCount: 0,
    windowBlurCount: 0,
    fullscreenExitCount: 0,
    copyCount: 0,
    pasteCount: 0,
    rightClickCount: 0
  }
  details.forEach(item => {
    const field = countFields[item.violationType]
    if (field) counts[field]++
  })
  return counts
}

async function loadSummary() {
  if (!examId.value) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    const summaries = await getExamViolationSummary(examId.value)
    rows.value = await Promise.all(
      summaries.map(async summary => {
        const details = await getStudentExamViolations(summary.studentExamId)
        return { ...summary, ...countByType(details) }
      })
    )
  } catch {
    rows.value = []
    ElMessage.error('考试异常汇总加载失败')
  } finally {
    loading.value = false
  }
}

async function openDetails(row) {
  selectedStudent.value = row
  violations.value = []
  detailVisible.value = true
  detailLoading.value = true
  try {
    violations.value = await getStudentExamViolations(row.studentExamId)
  } catch {
    ElMessage.error('异常明细加载失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  try {
    exams.value = await getExams()
    examId.value = exams.value[0]?.id || null
    await loadSummary()
  } catch {
    ElMessage.error('考试列表加载失败')
  }
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">考试监控</h1>
      <el-select
        v-model="examId"
        placeholder="选择考试"
        filterable
        style="width: 280px"
        @change="loadSummary"
      >
        <el-option
          v-for="exam in exams"
          :key="exam.id"
          :label="exam.examName"
          :value="exam.id"
        />
      </el-select>
    </div>

    <div class="panel">
      <el-table
        v-loading="loading"
        :data="rows"
        empty-text="该考试暂无异常记录"
      >
        <el-table-column prop="studentName" label="学生姓名" min-width="110" />
        <el-table-column prop="studentExamId" label="答卷 ID" width="90" />
        <el-table-column prop="violationCount" label="异常总次数" width="105" />
        <el-table-column prop="pageHiddenCount" label="切屏次数" width="90" />
        <el-table-column prop="windowBlurCount" label="失焦次数" width="90" />
        <el-table-column prop="fullscreenExitCount" label="退出全屏" width="95" />
        <el-table-column prop="copyCount" label="复制次数" width="90" />
        <el-table-column prop="pasteCount" label="粘贴次数" width="90" />
        <el-table-column prop="rightClickCount" label="右键次数" width="90" />
        <el-table-column label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="riskTagTypes[row.riskLevel] || 'info'">
              {{ riskLabels[row.riskLevel] || row.riskLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetails(row)">查看明细</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="detailVisible"
      :title="`${selectedStudent?.studentName || ''} - 异常明细`"
      width="760px"
    >
      <el-table
        v-loading="detailLoading"
        :data="violations"
        empty-text="暂无异常明细"
        max-height="520"
      >
        <el-table-column label="异常类型" width="150">
          <template #default="{ row }">
            {{ violationLabels[row.violationType] || row.violationType }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="异常描述" min-width="280" />
        <el-table-column label="发生时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.panel {
  overflow: hidden;
}
</style>
