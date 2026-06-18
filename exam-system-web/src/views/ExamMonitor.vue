<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getMonitorableExams } from '../api/exam'
import {
  getExamViolationSummary,
  getStudentExamViolations
} from '../api/examViolation'
import { useAuthStore } from '../store/auth'
import { formatRiskLevel } from '../utils/enumMap'
import { connectExamMonitor } from '../utils/monitorSocket'

const POLL_INTERVAL_MS = 30000
const auth = useAuthStore()

const exams = ref([])
const examId = ref(null)
const rows = ref([])
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const selectedStudent = ref(null)
const violations = ref([])
const riskFilter = ref('')
const autoRefresh = ref(true)

let pollTimer = null
let disconnectWs = null

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

const filteredRows = computed(() => {
  if (!riskFilter.value) return rows.value
  return rows.value.filter(row => row.riskLevel === riskFilter.value)
})

function formatTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}

async function loadSummary(silent = false) {
  if (!examId.value) {
    rows.value = []
    return
  }
  if (!silent) loading.value = true
  try {
    rows.value = await getExamViolationSummary(examId.value)
  } catch {
    if (!silent) {
      rows.value = []
      ElMessage.error('考试异常汇总加载失败')
    }
  } finally {
    if (!silent) loading.value = false
  }
}

function startPolling() {
  stopPolling()
  if (!autoRefresh.value) return
  pollTimer = setInterval(() => loadSummary(true), POLL_INTERVAL_MS)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
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

function onAutoRefreshChange(value) {
  if (value) startPolling()
  else stopPolling()
}

function applySummaryUpdate(summary) {
  if (!summary?.studentExamId) return
  const index = rows.value.findIndex(row => row.studentExamId === summary.studentExamId)
  if (index >= 0) rows.value[index] = summary
  else rows.value.push(summary)
  rows.value.sort((a, b) => b.riskScore - a.riskScore || b.violationCount - a.violationCount)
}

function connectWs() {
  disconnectWs?.()
  if (!examId.value || !auth.token) return
  disconnectWs = connectExamMonitor(auth.token, examId.value, applySummaryUpdate)
}

watch(examId, () => {
  connectWs()
})

onMounted(async () => {
  try {
    exams.value = await getMonitorableExams()
    examId.value = exams.value[0]?.id || null
    await loadSummary()
    startPolling()
    connectWs()
  } catch {
    ElMessage.error('考试列表加载失败')
  }
})

onBeforeUnmount(() => {
  stopPolling()
  disconnectWs?.()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">考试监控</h1>
      <div class="toolbar">
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
        <el-select v-model="riskFilter" clearable placeholder="风险等级" style="width: 140px">
          <el-option label="正常" value="NORMAL" />
          <el-option label="低风险" value="LOW" />
          <el-option label="中风险" value="MEDIUM" />
          <el-option label="高风险" value="HIGH" />
        </el-select>
        <el-switch
          v-model="autoRefresh"
          active-text="自动刷新"
          @change="onAutoRefreshChange"
        />
        <el-button @click="loadSummary()">立即刷新</el-button>
      </div>
    </div>

    <div class="panel">
      <el-table
        v-loading="loading"
        :data="filteredRows"
        empty-text="该考试暂无异常记录"
        default-sort="{ prop: 'riskScore', order: 'descending' }"
      >
        <el-table-column prop="studentName" label="学生姓名" min-width="110" />
        <el-table-column prop="studentExamId" label="答卷 ID" width="90" />
        <el-table-column prop="riskScore" label="风险评分" width="95" sortable />
        <el-table-column prop="violationCount" label="异常总次数" width="105" sortable />
        <el-table-column prop="pageHiddenCount" label="切屏次数" width="90" />
        <el-table-column prop="windowBlurCount" label="失焦次数" width="90" />
        <el-table-column prop="fullscreenExitCount" label="退出全屏" width="95" />
        <el-table-column prop="copyCount" label="复制次数" width="90" />
        <el-table-column prop="pasteCount" label="粘贴次数" width="90" />
        <el-table-column prop="rightClickCount" label="右键次数" width="90" />
        <el-table-column prop="devtoolsCount" label="DevTools" width="95" />
        <el-table-column label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="riskTagTypes[row.riskLevel] || 'info'">
              {{ formatRiskLevel(row.riskLevel) }}
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

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
</style>
