<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDifficulty, formatQuestionType } from '../utils/enumMap'
import {
  deleteCivilWrongQuestion,
  getCivilModuleAnalysis,
  getCivilArchiveCatalog,
  getCivilArchiveFilters,
  getCivilModules,
  getCivilOverview,
  getCivilPracticeQuestions,
  getCivilPracticeTrend,
  getCivilRandomQuestion,
  getCivilRecommendations,
  getCivilTestPaper,
  getCivilWrongQuestions,
  markCivilWrongMastered,
  startCivilDrill,
  submitCivilDrillAnswer,
  submitCivilPractice
} from '../api/civilServiceSkill'

const activeTab = ref('practice')
const modules = ref([])
const archiveFilters = ref({ years: [], scopes: [], provinces: [], paperTypes: [] })
const archiveCatalog = ref([])
const questions = ref([])
const wrongRows = ref([])
const moduleRows = ref([])
const trendRows = ref([])
const recommendations = ref([])
const overview = ref({})
const result = ref(null)
const loading = ref(false)
const submitting = ref(false)
const answers = ref({})
const chartRef = ref()
const trendChartRef = ref()
const wrongDetailVisible = ref(false)
const wrongDetail = ref(null)
let moduleChart
let trendChart
let testTimer

const query = reactive({ moduleCode: '', difficulty: '', count: 10 })
const drillQuery = reactive({ moduleCode: '', difficulty: '' })
const drillActive = ref(false)
const drillSessionId = ref(null)
const drillQuestion = ref(null)
const drillAnswer = ref('')
const drillMultiAnswer = ref([])
const drillFeedback = ref(null)
const drillStats = ref({ questionCount: 0, correctCount: 0, accuracy: 0 })
const drillSeenIds = ref([])
const drillLoading = ref(false)
const drillSubmitting = ref(false)
const drillStartedAt = ref(0)

const testCatalog = ref([])
const testSelected = ref(null)
const testPaper = ref(null)
const testActive = ref(false)
const testAnswers = ref({})
const testMultiAnswers = ref({})
const testCurrentIndex = ref(0)
const testDurationMinutes = ref(90)
const testRemainingSeconds = ref(0)
const testResult = ref(null)
const testSubmitting = ref(false)
const archiveQuery = reactive({
  examYear: null,
  examScope: '',
  province: '',
  paperType: '',
  moduleCode: '',
  count: 10
})
const selectedModule = computed(() => modules.value.find(m => m.moduleCode === query.moduleCode))
const currentTestQuestion = computed(() => testPaper.value?.questions?.[testCurrentIndex.value] || null)
const testCountdownText = computed(() => formatCountdown(testRemainingSeconds.value))

function formatCountdown(seconds) {
  const safe = Math.max(0, Number(seconds) || 0)
  const minutes = String(Math.floor(safe / 60)).padStart(2, '0')
  const secs = String(safe % 60).padStart(2, '0')
  return `${minutes}:${secs}`
}

function resetDrillAnswer() {
  drillAnswer.value = ''
  drillMultiAnswer.value = []
}

function currentDrillAnswerValue() {
  if (drillQuestion.value?.questionType === 'MULTIPLE_CHOICE') {
    return [...drillMultiAnswer.value].sort().join(',')
  }
  return drillAnswer.value
}

async function startDrillMode() {
  drillLoading.value = true
  try {
    const session = await startCivilDrill({
      moduleCode: drillQuery.moduleCode || undefined,
      difficulty: drillQuery.difficulty || undefined
    })
    drillSessionId.value = session.sessionId
    drillActive.value = true
    drillFeedback.value = null
    drillStats.value = { questionCount: 0, correctCount: 0, accuracy: 0 }
    drillSeenIds.value = []
    resetDrillAnswer()
    await loadDrillQuestion()
  } finally {
    drillLoading.value = false
  }
}

async function loadDrillQuestion() {
  drillLoading.value = true
  try {
    const question = await getCivilRandomQuestion({
      moduleCode: drillQuery.moduleCode || undefined,
      difficulty: drillQuery.difficulty || undefined,
      excludeIds: drillSeenIds.value.join(',') || undefined
    })
    if (!question?.id) {
      ElMessage.warning('当前条件下没有更多题目了')
      return endDrillMode()
    }
    drillQuestion.value = question
    drillSeenIds.value.push(question.id)
    drillFeedback.value = null
    resetDrillAnswer()
    drillStartedAt.value = Date.now()
  } finally {
    drillLoading.value = false
  }
}

async function submitDrillAnswer() {
  if (!drillQuestion.value) return
  const answer = currentDrillAnswerValue()
  if (!answer) return ElMessage.warning('请先选择或填写答案')
  drillSubmitting.value = true
  try {
    const durationSeconds = Math.max(1, Math.round((Date.now() - drillStartedAt.value) / 1000))
    drillFeedback.value = await submitCivilDrillAnswer({
      sessionId: drillSessionId.value,
      questionId: drillQuestion.value.id,
      answer,
      durationSeconds
    })
    drillStats.value = {
      questionCount: drillFeedback.value.questionCount,
      correctCount: drillFeedback.value.correctCount,
      accuracy: drillFeedback.value.accuracy
    }
    if (drillFeedback.value.correct) {
      ElMessage.success('回答正确')
    } else {
      ElMessage.error('回答错误，已加入错题本')
      await loadWrongQuestions()
    }
  } finally {
    drillSubmitting.value = false
  }
}

async function nextDrillQuestion() {
  await loadDrillQuestion()
}

function endDrillMode() {
  drillActive.value = false
  drillSessionId.value = null
  drillQuestion.value = null
  drillFeedback.value = null
  resetDrillAnswer()
}

async function loadTestCatalog() {
  testCatalog.value = await getCivilArchiveCatalog()
}

async function prepareTestPaper(item) {
  testSelected.value = item
  testResult.value = null
  testActive.value = false
  clearTestTimer()
  loading.value = true
  try {
    testPaper.value = await getCivilTestPaper({
      examYear: item.examYear,
      examScope: item.examScope,
      province: item.province,
      paperType: item.paperType
    })
    testDurationMinutes.value = testPaper.value.durationMinutes
    testAnswers.value = {}
    testMultiAnswers.value = {}
    testPaper.value.questions.forEach(q => {
      if (q.questionType === 'MULTIPLE_CHOICE') testMultiAnswers.value[q.id] = []
    })
    testCurrentIndex.value = 0
  } finally {
    loading.value = false
  }
}

function clearTestTimer() {
  if (testTimer) {
    clearInterval(testTimer)
    testTimer = null
  }
}

function startTestMode() {
  if (!testPaper.value?.questions?.length) return ElMessage.warning('请先选择一套真题试卷')
  testActive.value = true
  testResult.value = null
  testCurrentIndex.value = 0
  testRemainingSeconds.value = testDurationMinutes.value * 60
  clearTestTimer()
  testTimer = setInterval(() => {
    if (testRemainingSeconds.value <= 1) {
      testRemainingSeconds.value = 0
      clearTestTimer()
      submitTestMode(true)
      return
    }
    testRemainingSeconds.value -= 1
  }, 1000)
}

function currentTestAnswerValue(question) {
  if (!question) return ''
  if (question.questionType === 'MULTIPLE_CHOICE') {
    return [...(testMultiAnswers.value[question.id] || [])].sort().join(',')
  }
  return testAnswers.value[question.id] || ''
}

async function submitTestMode(auto = false) {
  if (!testPaper.value || testSubmitting.value) return
  if (!auto) {
    await ElMessageBox.confirm('确认交卷？交卷后将统计成绩并记录错题。', '交卷确认')
  }
  clearTestTimer()
  testSubmitting.value = true
  try {
    const durationSeconds = testDurationMinutes.value * 60 - testRemainingSeconds.value
    testResult.value = await submitCivilPractice({
      moduleCode: 'ARCHIVE_TEST',
      moduleName: testPaper.value.paperTitle,
      durationSeconds: Math.max(durationSeconds, 0),
      answers: testPaper.value.questions.map(q => ({
        questionId: q.id,
        answer: currentTestAnswerValue(q),
        durationSeconds: 0
      }))
    })
    testActive.value = false
    ElMessage.success(auto ? '时间到，已自动交卷' : '交卷成功')
    await Promise.all([loadWrongQuestions(), loadAnalysis()])
  } finally {
    testSubmitting.value = false
  }
}

function parseOptions(json) {
  try { return json ? JSON.parse(json) : [] } catch { return [] }
}

function optionLetter(index) {
  return String.fromCharCode(65 + index)
}

function showWrongDetail(row) {
  wrongDetail.value = row
  wrongDetailVisible.value = true
}

async function loadArchiveData() {
  const [filters, catalog] = await Promise.all([
    getCivilArchiveFilters(),
    getCivilArchiveCatalog()
  ])
  archiveFilters.value = filters
  archiveCatalog.value = catalog
}

async function loadArchiveQuestions() {
  loading.value = true
  try {
    questions.value = await getCivilPracticeQuestions({
      archiveOnly: true,
      moduleCode: archiveQuery.moduleCode || undefined,
      examYear: archiveQuery.examYear || undefined,
      examScope: archiveQuery.examScope || undefined,
      province: archiveQuery.province || undefined,
      paperType: archiveQuery.paperType || undefined,
      count: archiveQuery.count
    })
    answers.value = {}
    result.value = null
    if (!questions.value.length) ElMessage.warning('当前筛选条件下暂无历年真题')
  } finally {
    loading.value = false
  }
}

function applyCatalog(item) {
  archiveQuery.examYear = item.examYear
  archiveQuery.examScope = item.examScope
  archiveQuery.province = item.province
  archiveQuery.paperType = item.paperType
  activeTab.value = 'archive'
  loadArchiveQuestions()
}

function catalogTitle(item) {
  return `${item.examYear}年 ${item.examScopeLabel} ${item.province} ${item.paperType}`
}

async function loadQuestions() {
  loading.value = true
  try {
    questions.value = await getCivilPracticeQuestions({
      moduleCode: query.moduleCode || undefined,
      difficulty: query.difficulty || undefined,
      count: query.count
    })
    answers.value = {}
    result.value = null
    if (!questions.value.length) ElMessage.warning('暂无公考题，请先执行数据库迁移脚本')
  } finally {
    loading.value = false
  }
}

async function submitPractice() {
  if (!questions.value.length) return ElMessage.warning('请先加载题目')
  submitting.value = true
  try {
    result.value = await submitCivilPractice({
      moduleCode: (activeTab.value === 'archive' ? archiveQuery.moduleCode : query.moduleCode) || '',
      moduleName: selectedModule.value?.moduleName || (activeTab.value === 'archive' ? '历年真题' : '综合练习'),
      durationSeconds: 0,
      answers: questions.value.map(q => ({ questionId: q.id, answer: answers.value[q.id] || '', durationSeconds: 0 }))
    })
    ElMessage.success(`本次正确率 ${result.value.accuracy}%`)
    await Promise.all([loadWrongQuestions(), loadAnalysis()])
  } finally {
    submitting.value = false
  }
}

async function loadWrongQuestions() {
  wrongRows.value = await getCivilWrongQuestions({ includeMastered: false })
}

async function markMastered(row) {
  await markCivilWrongMastered(row.id)
  ElMessage.success('已标记掌握')
  await Promise.all([loadWrongQuestions(), loadAnalysis()])
}

async function removeWrong(row) {
  await ElMessageBox.confirm('确认删除这道错题？', '提示')
  await deleteCivilWrongQuestion(row.id)
  await Promise.all([loadWrongQuestions(), loadAnalysis()])
}

async function loadAnalysis() {
  const [overviewData, moduleData, trendData, recommendationData] = await Promise.all([
    getCivilOverview(),
    getCivilModuleAnalysis(),
    getCivilPracticeTrend(),
    getCivilRecommendations()
  ])
  overview.value = overviewData
  moduleRows.value = moduleData
  trendRows.value = trendData
  recommendations.value = recommendationData
  await nextTick()
  renderChart()
}

function formatTrendTime(value) {
  if (!value) return ''
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value.slice(5, 10)
  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function renderChart() {
  if (activeTab.value !== 'analysis') return
  if (chartRef.value) {
    moduleChart ||= echarts.init(chartRef.value)
    moduleChart.setOption({
      title: { text: '行测模块正确率' },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: moduleRows.value.map(x => x.moduleName) },
      yAxis: { type: 'value', max: 100 },
      series: [{ type: 'bar', data: moduleRows.value.map(x => x.accuracy) }]
    }, true)
  }
  if (!trendChartRef.value) return
  trendChart ||= echarts.init(trendChartRef.value)
  trendChart.setOption({
    title: { text: '最近 7 次练习正确率趋势' },
    tooltip: {
      trigger: 'axis',
      formatter(params) {
        const index = params[0]?.dataIndex
        const row = trendRows.value[index]
        if (!row) return ''
        return [
          row.moduleName || '综合练习',
          `正确率：${row.accuracy}%`,
          `答题数：${row.questionCount}`,
          `正确数：${row.correctCount}`,
          `练习时间：${row.createTime || '-'}`
        ].join('<br>')
      }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendRows.value.map(row => `${row.moduleName || '综合练习'} ${formatTrendTime(row.createTime)}`)
    },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{
      name: '正确率',
      type: 'line',
      smooth: true,
      symbolSize: 8,
      data: trendRows.value.map(row => Number(row.accuracy || 0))
    }]
  }, true)
}

function resizeChart() {
  moduleChart?.resize()
  trendChart?.resize()
}

async function tabChange() {
  if (activeTab.value === 'wrong') await loadWrongQuestions()
  if (activeTab.value === 'analysis') await loadAnalysis()
  if (activeTab.value === 'archive') await loadArchiveData()
  if (activeTab.value === 'test') await loadTestCatalog()
}

onMounted(async () => {
  window.addEventListener('resize', resizeChart)
  modules.value = await getCivilModules()
  await Promise.all([loadQuestions(), loadWrongQuestions(), loadAnalysis(), loadArchiveData()])
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  clearTestTimer()
  moduleChart?.dispose()
  trendChart?.dispose()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">公考刷题与错题分析 Skill</h1>
        <p class="sub-title">按行测模块刷题，自动沉淀错题并生成复习建议。</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="tabChange">
      <el-tab-pane label="模块刷题" name="practice">
        <div class="toolbar">
          <el-select v-model="query.moduleCode" clearable placeholder="行测模块" style="width: 180px">
            <el-option v-for="m in modules" :key="m.moduleCode" :label="m.moduleName" :value="m.moduleCode" />
          </el-select>
          <el-select v-model="query.difficulty" clearable placeholder="难度" style="width: 140px">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
          <el-input-number v-model="query.count" :min="1" :max="50" />
          <el-button type="primary" :loading="loading" @click="loadQuestions">加载题目</el-button>
          <el-button type="success" :loading="submitting" @click="submitPractice">提交练习</el-button>
        </div>

        <div class="question-list">
          <div v-for="(q, index) in questions" :key="q.id" class="question-card">
            <div class="question-head">
              <strong>第 {{ index + 1 }} 题</strong>
              <div>
                <el-tag>{{ q.moduleName }}</el-tag>
                <el-tag type="info">{{ formatQuestionType(q.questionType) }}</el-tag>
                <el-tag type="warning">{{ formatDifficulty(q.difficulty) }}</el-tag>
              </div>
            </div>
            <p class="question-content">{{ q.content }}</p>
            <el-radio-group v-model="answers[q.id]" class="option-group">
              <el-radio v-for="(option, optionIndex) in parseOptions(q.optionsJson)" :key="optionIndex" :value="optionLetter(optionIndex)">
                {{ optionLetter(optionIndex) }}. {{ option }}
              </el-radio>
            </el-radio-group>
          </div>
        </div>

        <el-card v-if="result" class="result-card">
          <template #header>本次练习结果</template>
          <div class="metric-grid">
            <div class="metric"><span>题目数</span><strong>{{ result.questionCount }}</strong></div>
            <div class="metric"><span>正确数</span><strong>{{ result.correctCount }}</strong></div>
            <div class="metric"><span>错题数</span><strong>{{ result.wrongCount }}</strong></div>
            <div class="metric"><span>正确率</span><strong>{{ result.accuracy }}%</strong></div>
          </div>
          <el-table :data="result.details" class="detail-table">
            <el-table-column prop="content" label="题目" min-width="260" show-overflow-tooltip />
            <el-table-column label="结果" width="90">
              <template #default="{ row }"><el-tag :type="row.correct ? 'success' : 'danger'">{{ row.correct ? '正确' : '错误' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="userAnswer" label="我的答案" width="100" />
            <el-table-column prop="correctAnswer" label="正确答案" width="100" />
            <el-table-column prop="analysis" label="解析" min-width="260" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="刷题模式" name="drill">
        <div class="mode-intro">
          <p>随机弹出单题，答对直接进入下一题；答错展示解析与答题技巧，并自动收录到错题本。</p>
        </div>
        <div v-if="!drillActive" class="toolbar">
          <el-select v-model="drillQuery.moduleCode" clearable placeholder="行测模块" style="width: 180px">
            <el-option v-for="m in modules" :key="m.moduleCode" :label="m.moduleName" :value="m.moduleCode" />
          </el-select>
          <el-select v-model="drillQuery.difficulty" clearable placeholder="难度" style="width: 140px">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
          <el-button type="primary" :loading="drillLoading" @click="startDrillMode">开始刷题</el-button>
        </div>

        <div v-else class="drill-panel">
          <div class="drill-stats">
            <span>已答 {{ drillStats.questionCount }} 题</span>
            <span>正确 {{ drillStats.correctCount }} 题</span>
            <span>正确率 {{ drillStats.accuracy }}%</span>
            <el-button link type="danger" @click="endDrillMode">结束刷题</el-button>
          </div>

          <div v-if="drillQuestion" v-loading="drillLoading" class="question-card drill-card">
            <div class="question-head">
              <strong>随机题目</strong>
              <div>
                <el-tag>{{ drillQuestion.moduleName }}</el-tag>
                <el-tag type="info">{{ formatQuestionType(drillQuestion.questionType) }}</el-tag>
                <el-tag type="warning">{{ formatDifficulty(drillQuestion.difficulty) }}</el-tag>
              </div>
            </div>
            <p class="question-content">{{ drillQuestion.content }}</p>

            <el-radio-group
              v-if="drillQuestion.questionType === 'SINGLE_CHOICE'"
              v-model="drillAnswer"
              class="option-group"
              :disabled="!!drillFeedback"
            >
              <el-radio v-for="(option, optionIndex) in parseOptions(drillQuestion.optionsJson)" :key="optionIndex" :value="optionLetter(optionIndex)">
                {{ optionLetter(optionIndex) }}. {{ option }}
              </el-radio>
            </el-radio-group>
            <el-checkbox-group
              v-else-if="drillQuestion.questionType === 'MULTIPLE_CHOICE'"
              v-model="drillMultiAnswer"
              class="option-group"
              :disabled="!!drillFeedback"
            >
              <el-checkbox v-for="(option, optionIndex) in parseOptions(drillQuestion.optionsJson)" :key="optionIndex" :value="optionLetter(optionIndex)">
                {{ optionLetter(optionIndex) }}. {{ option }}
              </el-checkbox>
            </el-checkbox-group>
            <el-radio-group
              v-else-if="drillQuestion.questionType === 'TRUE_FALSE'"
              v-model="drillAnswer"
              class="option-group"
              :disabled="!!drillFeedback"
            >
              <el-radio value="TRUE">正确</el-radio>
              <el-radio value="FALSE">错误</el-radio>
            </el-radio-group>
            <el-input
              v-else
              v-model="drillAnswer"
              placeholder="请输入答案"
              :disabled="!!drillFeedback"
            />

            <div v-if="!drillFeedback" class="drill-actions">
              <el-button type="primary" :loading="drillSubmitting" @click="submitDrillAnswer">确认作答</el-button>
            </div>

            <el-alert
              v-if="drillFeedback"
              :title="drillFeedback.correct ? '回答正确，继续保持！' : '回答错误，请查看解析'"
              :type="drillFeedback.correct ? 'success' : 'error'"
              :closable="false"
              show-icon
              class="drill-feedback"
            />
            <div v-if="drillFeedback && !drillFeedback.correct" class="feedback-box">
              <p><strong>正确答案：</strong>{{ drillFeedback.correctAnswer }}</p>
              <p><strong>解析：</strong>{{ drillFeedback.analysis || '暂无解析' }}</p>
              <p><strong>答题技巧：</strong>{{ drillFeedback.tip }}</p>
              <p v-if="drillFeedback.addedToWrongBook" class="wrong-tip">本题已加入错题本，可在「公考错题本」中复盘。</p>
            </div>
            <div v-if="drillFeedback" class="drill-actions">
              <el-button type="primary" :loading="drillLoading" @click="nextDrillQuestion">下一题</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="测试模式" name="test">
        <div class="mode-intro">
          <p>选择一套完整真题试卷，限时作答。交卷后统计成绩，错题自动进入错题本。</p>
        </div>

        <div v-if="!testActive" class="test-layout">
          <div class="panel catalog-panel">
            <h3>选择真题试卷</h3>
            <el-table :data="testCatalog" max-height="360" highlight-current-row @row-click="prepareTestPaper">
              <el-table-column label="试卷" min-width="220">
                <template #default="{ row }">{{ catalogTitle(row) }}</template>
              </el-table-column>
              <el-table-column prop="questionCount" label="题量" width="70" />
            </el-table>
          </div>

          <div class="test-setup panel">
            <template v-if="testPaper">
              <h3>{{ testPaper.paperTitle }}</h3>
              <p class="catalog-tip">题量 {{ testPaper.questionCount }} 题 · 建议用时 {{ testPaper.durationMinutes }} 分钟</p>
              <p v-if="testPaper.sourceRef" class="catalog-tip">来源：{{ testPaper.sourceRef }}</p>
              <el-form label-width="90px" class="test-form">
                <el-form-item label="考试时长">
                  <el-input-number v-model="testDurationMinutes" :min="10" :max="240" />
                  <span class="field-tip">分钟</span>
                </el-form-item>
              </el-form>
              <el-button type="primary" @click="startTestMode">开始测试</el-button>
            </template>
            <el-empty v-else description="请先从左侧选择一套真题试卷" />
          </div>
        </div>

        <div v-else class="test-exam panel">
          <div class="test-header">
            <div>
              <strong>{{ testPaper.paperTitle }}</strong>
              <span>第 {{ testCurrentIndex + 1 }} / {{ testPaper.questionCount }} 题</span>
            </div>
            <div class="test-timer" :class="{ danger: testRemainingSeconds <= 300 }">
              剩余时间 {{ testCountdownText }}
            </div>
            <el-button type="danger" :loading="testSubmitting" @click="submitTestMode(false)">交卷</el-button>
          </div>

          <div class="test-nav">
            <el-button
              v-for="(q, index) in testPaper.questions"
              :key="q.id"
              size="small"
              :type="testCurrentIndex === index ? 'primary' : (currentTestAnswerValue(q) ? 'success' : 'default')"
              @click="testCurrentIndex = index"
            >
              {{ index + 1 }}
            </el-button>
          </div>

          <div v-if="currentTestQuestion" class="question-card">
            <p class="question-content">{{ currentTestQuestion.content }}</p>
            <el-radio-group
              v-if="currentTestQuestion.questionType === 'SINGLE_CHOICE'"
              v-model="testAnswers[currentTestQuestion.id]"
              class="option-group"
            >
              <el-radio v-for="(option, optionIndex) in parseOptions(currentTestQuestion.optionsJson)" :key="optionIndex" :value="optionLetter(optionIndex)">
                {{ optionLetter(optionIndex) }}. {{ option }}
              </el-radio>
            </el-radio-group>
            <el-checkbox-group
              v-else-if="currentTestQuestion.questionType === 'MULTIPLE_CHOICE'"
              v-model="testMultiAnswers[currentTestQuestion.id]"
              class="option-group"
            >
              <el-checkbox v-for="(option, optionIndex) in parseOptions(currentTestQuestion.optionsJson)" :key="optionIndex" :value="optionLetter(optionIndex)">
                {{ optionLetter(optionIndex) }}. {{ option }}
              </el-checkbox>
            </el-checkbox-group>
            <el-radio-group
              v-else-if="currentTestQuestion.questionType === 'TRUE_FALSE'"
              v-model="testAnswers[currentTestQuestion.id]"
              class="option-group"
            >
              <el-radio value="TRUE">正确</el-radio>
              <el-radio value="FALSE">错误</el-radio>
            </el-radio-group>
            <el-input v-else v-model="testAnswers[currentTestQuestion.id]" placeholder="请输入答案" />
          </div>

          <div class="test-actions">
            <el-button :disabled="testCurrentIndex === 0" @click="testCurrentIndex -= 1">上一题</el-button>
            <el-button :disabled="testCurrentIndex >= testPaper.questionCount - 1" @click="testCurrentIndex += 1">下一题</el-button>
          </div>
        </div>

        <el-card v-if="testResult" class="result-card">
          <template #header>测试成绩</template>
          <div class="metric-grid">
            <div class="metric"><span>题目数</span><strong>{{ testResult.questionCount }}</strong></div>
            <div class="metric"><span>正确数</span><strong>{{ testResult.correctCount }}</strong></div>
            <div class="metric"><span>错题数</span><strong>{{ testResult.wrongCount }}</strong></div>
            <div class="metric"><span>正确率</span><strong>{{ testResult.accuracy }}%</strong></div>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="历年真题" name="archive">
        <div class="archive-layout">
          <div class="panel catalog-panel">
            <h3>真题试卷目录</h3>
            <p class="catalog-tip">题目按年份、考试类型（国考/省考）、省份和卷别分类。来源标注为系统整理，可参考公开真题考点自行扩充。</p>
            <el-table :data="archiveCatalog" max-height="360" @row-click="applyCatalog">
              <el-table-column label="试卷" min-width="220">
                <template #default="{ row }">{{ catalogTitle(row) }}</template>
              </el-table-column>
              <el-table-column prop="questionCount" label="题量" width="70" />
              <el-table-column prop="sourceRef" label="来源" min-width="160" show-overflow-tooltip />
            </el-table>
          </div>

          <div class="archive-practice">
            <div class="toolbar">
              <el-select v-model="archiveQuery.examYear" clearable placeholder="年份" style="width: 110px">
                <el-option v-for="year in archiveFilters.years" :key="year" :label="`${year}年`" :value="year" />
              </el-select>
              <el-select v-model="archiveQuery.examScope" clearable placeholder="考试类型" style="width: 120px">
                <el-option v-for="scope in archiveFilters.scopes" :key="scope.value" :label="scope.label" :value="scope.value" />
              </el-select>
              <el-select v-model="archiveQuery.province" clearable placeholder="省份" style="width: 120px">
                <el-option v-for="province in archiveFilters.provinces" :key="province" :label="province" :value="province" />
              </el-select>
              <el-select v-model="archiveQuery.paperType" clearable placeholder="卷别" style="width: 120px">
                <el-option v-for="paper in archiveFilters.paperTypes" :key="paper" :label="paper" :value="paper" />
              </el-select>
              <el-select v-model="archiveQuery.moduleCode" clearable placeholder="模块" style="width: 140px">
                <el-option v-for="m in modules" :key="m.moduleCode" :label="m.moduleName" :value="m.moduleCode" />
              </el-select>
              <el-input-number v-model="archiveQuery.count" :min="1" :max="50" />
              <el-button type="primary" :loading="loading" @click="loadArchiveQuestions">开始练习</el-button>
              <el-button type="success" :loading="submitting" @click="submitPractice">提交练习</el-button>
            </div>

            <div class="question-list">
              <div v-for="(q, index) in questions" :key="q.id" class="question-card">
                <div class="question-head">
                  <strong>第 {{ index + 1 }} 题</strong>
                  <div>
                    <el-tag type="success">{{ q.examYear }}年 {{ q.examScopeLabel }}</el-tag>
                    <el-tag>{{ q.province }} · {{ q.paperType }}</el-tag>
                    <el-tag>{{ q.moduleName }}</el-tag>
                    <el-tag type="warning">{{ formatDifficulty(q.difficulty) }}</el-tag>
                  </div>
                </div>
                <p class="question-content">{{ q.content }}</p>
                <el-radio-group v-model="answers[q.id]" class="option-group">
                  <el-radio v-for="(option, optionIndex) in parseOptions(q.optionsJson)" :key="optionIndex" :value="optionLetter(optionIndex)">
                    {{ optionLetter(optionIndex) }}. {{ option }}
                  </el-radio>
                </el-radio-group>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="公考错题本" name="wrong">
        <div class="panel">
          <el-table :data="wrongRows">
            <el-table-column prop="moduleName" label="模块" width="120" />
            <el-table-column prop="content" label="题目" min-width="300" show-overflow-tooltip />
            <el-table-column prop="userAnswer" label="我的答案" width="110" />
            <el-table-column prop="correctAnswer" label="正确答案" width="110" />
            <el-table-column prop="wrongCount" label="错误次数" width="100" />
            <el-table-column prop="lastWrongTime" label="最近错误时间" width="180" />
            <el-table-column label="操作" width="240">
              <template #default="{ row }">
                <el-button link type="primary" @click="showWrongDetail(row)">查看解析</el-button>
                <el-button link type="primary" @click="markMastered(row)">已掌握</el-button>
                <el-button link type="danger" @click="removeWrong(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="分析建议" name="analysis">
        <div class="metric-grid">
          <div class="metric"><span>练习次数</span><strong>{{ overview.sessionCount || 0 }}</strong></div>
          <div class="metric"><span>累计答题</span><strong>{{ overview.answeredCount || 0 }}</strong></div>
          <div class="metric"><span>总体正确率</span><strong>{{ overview.accuracy || 0 }}%</strong></div>
          <div class="metric"><span>未掌握错题</span><strong>{{ overview.wrongCount || 0 }}</strong></div>
        </div>
        <div ref="chartRef" class="chart analysis-chart"></div>
        <div ref="trendChartRef" class="chart analysis-chart trend-chart"></div>
        <div class="analysis-grid">
          <div class="panel">
            <h3>模块表现</h3>
            <el-table :data="moduleRows">
              <el-table-column prop="moduleName" label="模块" />
              <el-table-column prop="answeredCount" label="答题数" />
              <el-table-column prop="correctCount" label="正确数" />
              <el-table-column prop="wrongCount" label="未掌握错题" />
              <el-table-column label="正确率">
                <template #default="{ row }">{{ row.accuracy }}%</template>
              </el-table-column>
            </el-table>
          </div>
          <div class="panel">
            <h3>复习建议</h3>
            <el-alert v-for="tip in recommendations" :key="tip" :title="tip" type="info" :closable="false" show-icon class="tip" />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="wrongDetailVisible" title="错题解析" width="680px">
      <template v-if="wrongDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="题型">{{ formatQuestionType(wrongDetail.questionType) }}</el-descriptions-item>
          <el-descriptions-item label="难度">{{ formatDifficulty(wrongDetail.difficulty) }}</el-descriptions-item>
          <el-descriptions-item label="题干" :span="2">
            <div class="detail-content">{{ wrongDetail.content }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="parseOptions(wrongDetail.optionsJson).length" label="选项" :span="2">
            <div class="detail-options">
              <div v-for="(option, index) in parseOptions(wrongDetail.optionsJson)" :key="index">
                {{ optionLetter(index) }}. {{ option }}
              </div>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="我的答案">{{ wrongDetail.userAnswer || '未作答' }}</el-descriptions-item>
          <el-descriptions-item label="正确答案">{{ wrongDetail.correctAnswer || '-' }}</el-descriptions-item>
          <el-descriptions-item label="错误次数">{{ wrongDetail.wrongCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="最近错误时间">{{ wrongDetail.lastWrongTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="解析" :span="2">
            <div class="detail-content">{{ wrongDetail.analysis || '暂无解析' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="wrongDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.sub-title { margin: 8px 0 0; color: #64748b; }
.question-list { display: grid; gap: 14px; }
.question-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; padding: 18px; }
.question-head { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.question-head div { display: flex; gap: 8px; flex-wrap: wrap; }
.question-content { line-height: 1.7; }
.option-group { display: grid; gap: 8px; align-items: flex-start; }
.result-card { margin-top: 18px; }
.detail-table { margin-top: 18px; }
.analysis-chart { margin-top: 18px; }
.trend-chart { margin-top: 24px; }
.analysis-grid { display: grid; grid-template-columns: 1.2fr .8fr; gap: 18px; margin-top: 18px; }
.tip + .tip { margin-top: 10px; }
.detail-content { line-height: 1.7; white-space: pre-wrap; }
.detail-options { display: grid; gap: 6px; line-height: 1.6; }
.archive-layout { display: grid; grid-template-columns: 360px 1fr; gap: 18px; }
.test-layout { display: grid; grid-template-columns: 360px 1fr; gap: 18px; }
.mode-intro { margin-bottom: 14px; color: #64748b; line-height: 1.7; }
.drill-panel { display: grid; gap: 14px; }
.drill-stats { display: flex; gap: 18px; align-items: center; flex-wrap: wrap; color: #334155; }
.drill-card { max-width: 900px; }
.drill-actions { margin-top: 16px; display: flex; gap: 10px; }
.drill-feedback { margin-top: 16px; }
.feedback-box { margin-top: 12px; padding: 14px; background: #fff7ed; border: 1px solid #fed7aa; border-radius: 6px; line-height: 1.7; }
.wrong-tip { color: #b45309; }
.test-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 14px; }
.test-header span { margin-left: 10px; color: #64748b; }
.test-timer { font-size: 20px; font-weight: 700; color: #1d4ed8; }
.test-timer.danger { color: #dc2626; }
.test-nav { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
.test-actions { margin-top: 16px; display: flex; gap: 10px; }
.test-form { max-width: 320px; margin: 16px 0; }
.field-tip { margin-left: 8px; color: #94a3b8; }
.catalog-panel h3 { margin: 0 0 8px; }
.catalog-tip { color: #64748b; font-size: 13px; line-height: 1.6; margin: 0 0 12px; }
@media (max-width: 900px) { .analysis-grid { grid-template-columns: 1fr; } .question-head { align-items: flex-start; flex-direction: column; } .archive-layout, .test-layout { grid-template-columns: 1fr; } }
</style>
