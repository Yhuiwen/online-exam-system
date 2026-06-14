<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDifficulty, formatQuestionType } from '../utils/enumMap'
import {
  deleteCivilWrongQuestion,
  getCivilModuleAnalysis,
  getCivilModules,
  getCivilOverview,
  getCivilPracticeQuestions,
  getCivilRecommendations,
  getCivilWrongQuestions,
  markCivilWrongMastered,
  submitCivilPractice
} from '../api/civilServiceSkill'

const activeTab = ref('practice')
const modules = ref([])
const questions = ref([])
const wrongRows = ref([])
const moduleRows = ref([])
const recommendations = ref([])
const overview = ref({})
const result = ref(null)
const loading = ref(false)
const submitting = ref(false)
const answers = ref({})
const chartRef = ref()
let moduleChart

const query = reactive({ moduleCode: '', difficulty: '', count: 10 })
const selectedModule = computed(() => modules.value.find(m => m.moduleCode === query.moduleCode))

function parseOptions(json) {
  try { return json ? JSON.parse(json) : [] } catch { return [] }
}

function optionLetter(index) {
  return String.fromCharCode(65 + index)
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
      moduleCode: query.moduleCode || '',
      moduleName: selectedModule.value?.moduleName || '综合练习',
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
  overview.value = await getCivilOverview()
  moduleRows.value = await getCivilModuleAnalysis()
  recommendations.value = await getCivilRecommendations()
  await nextTick()
  renderChart()
}

function renderChart() {
  if (!chartRef.value || activeTab.value !== 'analysis') return
  moduleChart ||= echarts.init(chartRef.value)
  moduleChart.setOption({
    title: { text: '行测模块正确率' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: moduleRows.value.map(x => x.moduleName) },
    yAxis: { type: 'value', max: 100 },
    series: [{ type: 'bar', data: moduleRows.value.map(x => x.accuracy) }]
  }, true)
}

function resizeChart() { moduleChart?.resize() }

async function tabChange() {
  if (activeTab.value === 'wrong') await loadWrongQuestions()
  if (activeTab.value === 'analysis') await loadAnalysis()
}

onMounted(async () => {
  window.addEventListener('resize', resizeChart)
  modules.value = await getCivilModules()
  await Promise.all([loadQuestions(), loadWrongQuestions(), loadAnalysis()])
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  moduleChart?.dispose()
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

      <el-tab-pane label="公考错题本" name="wrong">
        <div class="panel">
          <el-table :data="wrongRows">
            <el-table-column prop="moduleName" label="模块" width="120" />
            <el-table-column prop="content" label="题目" min-width="300" show-overflow-tooltip />
            <el-table-column prop="userAnswer" label="我的答案" width="110" />
            <el-table-column prop="correctAnswer" label="正确答案" width="110" />
            <el-table-column prop="wrongCount" label="错误次数" width="100" />
            <el-table-column prop="lastWrongTime" label="最近错误时间" width="180" />
            <el-table-column label="操作" width="170">
              <template #default="{ row }">
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
.analysis-grid { display: grid; grid-template-columns: 1.2fr .8fr; gap: 18px; margin-top: 18px; }
.tip + .tip { margin-top: 10px; }
@media (max-width: 900px) { .analysis-grid { grid-template-columns: 1fr; } .question-head { align-items: flex-start; flex-direction: column; } }
</style>
