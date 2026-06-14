<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getExam, getExamQuestions } from '../api/exam'
import { submitExam, getExamSession } from '../api/studentExam'
import { getMyViolationSummary, reportViolation } from '../api/examViolation'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const exam = ref({})
const questions = ref([])
const answers = reactive({})
const seconds = ref(0)
const submitting = ref(false)
const violationCount = ref(0)
const monitoring = ref(false)
const warnedAtThree = ref(false)
const reportTimes = new Map()

let timer
let syncTimer
let devtoolsTimer

const studentExamId = computed(() => Number(route.query.studentExamId))
const timeText = computed(() =>
  `${String(Math.floor(seconds.value / 60)).padStart(2, '0')}:${String(seconds.value % 60).padStart(2, '0')}`
)
const highRisk = computed(() => violationCount.value > 5)

function options(question) {
  try {
    return JSON.parse(question.optionsJson || '[]')
  } catch {
    return []
  }
}

async function report(type, description) {
  if (!monitoring.value || submitting.value) return
  const now = Date.now()
  if (now - (reportTimes.get(type) || 0) < 5000) return
  reportTimes.set(type, now)
  try {
    await reportViolation({
      studentExamId: studentExamId.value,
      examId: Number(route.params.id),
      violationType: type,
      description
    })
    const previousCount = violationCount.value
    const summary = await getMyViolationSummary(studentExamId.value)
    violationCount.value = summary?.violationCount || previousCount
    if (previousCount < 3 && violationCount.value >= 3 && !warnedAtThree.value) {
      warnedAtThree.value = true
      ElMessageBox.alert(
        '系统已记录 3 次异常行为，请保持考试页面处于前台并遵守考试规则。',
        '异常行为警告',
        { type: 'warning', confirmButtonText: '我知道了' }
      ).catch(() => {})
    }
  } catch {
    // Monitoring failures must never interrupt answering or submission.
  }
}

function onVisibilityChange() {
  if (document.hidden) {
    report('PAGE_HIDDEN', '页面被隐藏，可能切换标签页或最小化窗口')
  }
}

function onWindowBlur() {
  report('WINDOW_BLUR', '考试窗口失去焦点')
}

function onFullscreenChange() {
  if (!document.fullscreenElement) {
    report('FULLSCREEN_EXIT', '学生退出全屏模式')
  }
}

function onCopy(event) {
  event.preventDefault()
  report('COPY', '考试过程中尝试复制内容')
}

function onPaste(event) {
  event.preventDefault()
  report('PASTE', '考试过程中尝试粘贴内容')
}

function onContextMenu(event) {
  event.preventDefault()
  report('RIGHT_CLICK', '考试过程中尝试使用右键菜单')
}

function checkDevtools() {
  const widthDifference = window.outerWidth - window.innerWidth
  const heightDifference = window.outerHeight - window.innerHeight
  if (widthDifference > 160 || heightDifference > 160) {
    report('DEVTOOLS_SUSPECTED', '窗口尺寸异常，疑似打开开发者工具')
  }
}

function startMonitoring() {
  if (monitoring.value) return
  monitoring.value = true
  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('blur', onWindowBlur)
  document.addEventListener('fullscreenchange', onFullscreenChange)
  document.addEventListener('copy', onCopy)
  document.addEventListener('paste', onPaste)
  document.addEventListener('contextmenu', onContextMenu)
  devtoolsTimer = window.setInterval(checkDevtools, 2000)
}

function stopMonitoring() {
  if (!monitoring.value) return
  monitoring.value = false
  document.removeEventListener('visibilitychange', onVisibilityChange)
  window.removeEventListener('blur', onWindowBlur)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  document.removeEventListener('copy', onCopy)
  document.removeEventListener('paste', onPaste)
  document.removeEventListener('contextmenu', onContextMenu)
  window.clearInterval(devtoolsTimer)
  devtoolsTimer = undefined
}

async function promptFullscreen() {
  try {
    await ElMessageBox.confirm(
      '建议进入全屏模式完成考试。退出全屏、切换页面等行为将被记录，但不会自动交卷。',
      '考试环境提示',
      {
        confirmButtonText: '进入全屏',
        cancelButtonText: '暂不进入',
        type: 'info'
      }
    )
    if (!document.fullscreenElement) {
      await document.documentElement.requestFullscreen()
    }
  } catch {
    // Fullscreen is recommended rather than mandatory.
  }
}

async function syncRemainingTime() {
  const session = await getExamSession(studentExamId.value)
  seconds.value = session.remainingSeconds
  if (session.timedOut && !submitting.value) {
    await submit(true)
  }
}

function startCountdown() {
  timer = window.setInterval(() => {
    seconds.value = Math.max(0, seconds.value - 1)
    if (seconds.value <= 0) {
      window.clearInterval(timer)
      submit(true)
    }
  }, 1000)
  syncTimer = window.setInterval(() => {
    syncRemainingTime().catch(() => {})
  }, 30000)
}

async function submit(auto = false) {
  if (submitting.value) return
  if (!auto) {
    await ElMessageBox.confirm('提交后不能再次作答，确认提交吗？', '提交确认', { type: 'warning' })
  }
  submitting.value = true
  try {
    const items = questions.value.map(question => ({
      questionId: question.id,
      answer: Array.isArray(answers[question.id])
        ? answers[question.id].join(',')
        : answers[question.id] || ''
    }))
    await submitExam({ studentExamId: studentExamId.value, answers: items })
    stopMonitoring()
    ElMessage.success('试卷已提交')
    router.replace('/scores')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (!studentExamId.value) {
    ElMessage.error('缺少考试记录，请重新开始考试')
    return router.replace('/student-exams')
  }
  exam.value = await getExam(route.params.id)
  questions.value = await getExamQuestions(route.params.id)
  try {
    const summary = await getMyViolationSummary(studentExamId.value)
    violationCount.value = summary?.violationCount || 0
    warnedAtThree.value = violationCount.value >= 3
  } catch {
    violationCount.value = 0
  }

  const session = await getExamSession(studentExamId.value)
  seconds.value = session.remainingSeconds
  if (session.timedOut || seconds.value === 0) return submit(true)

  startMonitoring()
  promptFullscreen()
  startCountdown()
})

onBeforeUnmount(() => {
  window.clearInterval(timer)
  window.clearInterval(syncTimer)
  stopMonitoring()
})
</script>

<template>
  <div class="exam-page">
    <header>
      <div>
        <h1>{{ exam.examName }}</h1>
        <span>共 {{ questions.length }} 题 · 总分 {{ exam.totalScore }}</span>
      </div>
      <div class="monitor-status">
        <span>异常次数：<strong>{{ violationCount }}</strong></span>
        <el-tag v-if="highRisk" type="danger">高风险，请遵守考试规则</el-tag>
        <el-tag v-else-if="violationCount >= 3" type="warning">异常行为已警告</el-tag>
      </div>
      <div class="timer">{{ timeText }}</div>
      <el-button type="danger" :loading="submitting" @click="submit(false)">提交试卷</el-button>
    </header>

    <main>
      <section v-for="(question, index) in questions" :key="question.id" class="question">
        <h3>{{ index + 1 }}. {{ question.content }} <small>{{ question.score }} 分</small></h3>
        <el-radio-group
          v-if="question.questionType === 'SINGLE_CHOICE'"
          v-model="answers[question.id]"
          class="options"
        >
          <el-radio
            v-for="(option, optionIndex) in options(question)"
            :key="optionIndex"
            :value="String.fromCharCode(65 + optionIndex)"
          >
            {{ String.fromCharCode(65 + optionIndex) }}. {{ option }}
          </el-radio>
        </el-radio-group>
        <el-checkbox-group
          v-else-if="question.questionType === 'MULTIPLE_CHOICE'"
          v-model="answers[question.id]"
          class="options"
        >
          <el-checkbox
            v-for="(option, optionIndex) in options(question)"
            :key="optionIndex"
            :value="String.fromCharCode(65 + optionIndex)"
          >
            {{ String.fromCharCode(65 + optionIndex) }}. {{ option }}
          </el-checkbox>
        </el-checkbox-group>
        <el-radio-group v-else-if="question.questionType === 'TRUE_FALSE'" v-model="answers[question.id]">
          <el-radio value="TRUE">正确</el-radio>
          <el-radio value="FALSE">错误</el-radio>
        </el-radio-group>
        <el-input
          v-else-if="question.questionType === 'FILL_BLANK'"
          v-model="answers[question.id]"
          placeholder="请输入答案"
        />
        <el-input
          v-else
          v-model="answers[question.id]"
          type="textarea"
          :rows="5"
          placeholder="请输入简答内容"
        />
      </section>
    </main>
  </div>
</template>

<style scoped>
.exam-page { min-height: 100vh; background: #eef2f6; }
.exam-page header { position: sticky; top: 0; z-index: 5; background: #fff; border-bottom: 1px solid #dbe1e8; padding: 16px 5vw; display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.exam-page h1 { font-size: 20px; margin: 0 0 6px; }
.exam-page header span { color: #6b7280; }
.monitor-status { display: flex; align-items: center; gap: 10px; }
.monitor-status strong { color: #b91c1c; }
.timer { font: 700 28px monospace; color: #b91c1c; }
main { max-width: 900px; margin: 22px auto; padding: 0 14px 50px; }
.question { background: #fff; border: 1px solid #dfe4ea; padding: 24px; margin-bottom: 14px; }
.question h3 { font-size: 16px; line-height: 1.7; margin-top: 0; }
.question small { color: #64748b; font-weight: 400; }
.options { display: flex; flex-direction: column; align-items: flex-start; gap: 12px; }
@media (max-width: 760px) {
  .exam-page header { flex-wrap: wrap; }
  .monitor-status { order: 3; width: 100%; }
  .timer { font-size: 22px; }
}
</style>
