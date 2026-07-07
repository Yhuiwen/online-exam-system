<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getExamStatistics, getStudentStatistics } from '../api/statistics'
import { getExams } from '../api/exam'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const exams = ref([])
const examId = ref(null)
const data = ref({})
const chart1 = ref()
const chart2 = ref()
const chart3 = ref()
let distributionChart
let rateChart
let trendChart

const isStudent = computed(() => auth.user?.role === 'STUDENT')
const passRate = computed(() => {
  if (data.value.passRate !== undefined) return `${data.value.passRate}%`
  if (!data.value.participantCount) return '0%'
  const passed = data.value.passedCount ?? data.value.passCount ?? 0
  return `${Math.round((passed / data.value.participantCount) * 100)}%`
})
const hasTeacherData = computed(() =>
  Object.keys(data.value.distribution || {}).length > 0 ||
  (data.value.questionRates || []).length > 0
)
const hasStudentData = computed(() => (data.value.scoreTrend || []).length > 0)

function renderTeacher() {
  distributionChart ||= echarts.init(chart1.value)
  rateChart ||= echarts.init(chart2.value)
  distributionChart.setOption({
    title: { text: '成绩分布', left: 8, top: 6, textStyle: { fontSize: 15 } },
    grid: { left: 42, right: 20, top: 58, bottom: 36 },
    tooltip: {},
    xAxis: { type: 'category', data: Object.keys(data.value.distribution || {}) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: Object.values(data.value.distribution || {}), itemStyle: { color: '#2563eb' }, barMaxWidth: 42 }]
  }, true)
  rateChart.setOption({
    title: { text: '题目正确率', left: 8, top: 6, textStyle: { fontSize: 15 } },
    grid: { left: 42, right: 20, top: 58, bottom: 36 },
    tooltip: {},
    xAxis: { type: 'category', data: (data.value.questionRates || []).map(x => `题${x.questionId}`) },
    yAxis: { type: 'value', max: 100 },
    series: [{ type: 'bar', data: (data.value.questionRates || []).map(x => x.correctRate), itemStyle: { color: '#0f766e' }, barMaxWidth: 42 }]
  }, true)
}

function renderStudent() {
  const trend = data.value.scoreTrend || []
  trendChart ||= echarts.init(chart3.value)
  trendChart.setOption({
    title: { text: '个人成绩趋势', left: 8, top: 6, textStyle: { fontSize: 15 } },
    grid: { left: 42, right: 20, top: 58, bottom: 36 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.map(x => x.examName) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: trend.map(x => x.score), itemStyle: { color: '#d97706' }, areaStyle: { color: 'rgba(217, 119, 6, .12)' } }]
  }, true)
}

async function load() {
  if (isStudent.value) {
    data.value = await getStudentStatistics()
    await nextTick()
    renderStudent()
  } else if (examId.value) {
    data.value = await getExamStatistics(examId.value)
    await nextTick()
    renderTeacher()
  }
}

function resizeCharts() {
  distributionChart?.resize()
  rateChart?.resize()
  trendChart?.resize()
}

onMounted(async () => {
  window.addEventListener('resize', resizeCharts)
  if (!isStudent.value) {
    exams.value = await getExams()
    examId.value = exams.value[0]?.id
  }
  await load()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  distributionChart?.dispose()
  rateChart?.dispose()
  trendChart?.dispose()
})
</script>

<template>
  <div class="page statistics-page">
    <div class="page-header">
      <h1 class="page-title">统计分析</h1>
      <el-select v-if="!isStudent" v-model="examId" placeholder="选择考试" style="width:240px" @change="load">
        <el-option v-for="e in exams" :key="e.id" :label="e.examName" :value="e.id" />
      </el-select>
    </div>

    <section class="analysis-card">
      <div>
        <h2>{{ isStudent ? '个人学习数据' : '考试成绩概览' }}</h2>
        <p>{{ isStudent ? '展示学生考试次数、错题数量与个人成绩趋势。' : '汇总参考人数、平均分、最高分、及格率，并通过图表展示分数分布和题目正确率。' }}</p>
      </div>
      <el-tag type="success" effect="dark">ECharts</el-tag>
    </section>

    <div class="metric-grid">
      <div class="metric"><span>{{ isStudent ? '考试次数' : '参考人数' }}</span><strong>{{ data.examCount ?? data.participantCount ?? 0 }}</strong></div>
      <div class="metric"><span>平均分</span><strong>{{ data.averageScore ?? 0 }}</strong></div>
      <div class="metric"><span>{{ isStudent ? '错题数量' : '最高分' }}</span><strong>{{ data.wrongCount ?? data.highestScore ?? 0 }}</strong></div>
      <div class="metric"><span>{{ isStudent ? '学习状态' : '及格率' }}</span><strong>{{ isStudent ? '进行中' : passRate }}</strong></div>
    </div>

    <template v-if="!isStudent">
      <div v-show="hasTeacherData" class="chart-grid">
        <div ref="chart1" class="chart"></div>
        <div ref="chart2" class="chart"></div>
      </div>
      <el-empty v-if="!hasTeacherData" class="panel" description="当前考试暂无统计数据，学生提交后将生成图表" />
    </template>
    <template v-else>
      <div v-show="hasStudentData" ref="chart3" class="chart student-chart"></div>
      <el-empty v-if="!hasStudentData" class="panel" description="暂无考试成绩，完成考试后将显示趋势图" />
    </template>
  </div>
</template>

<style scoped>
.statistics-page { display: grid; gap: 16px; }
.analysis-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 20px 22px;
  border: 1px solid #dcfce7;
  border-radius: 10px;
  background: linear-gradient(135deg, #f0fdf4, #ffffff);
}
.analysis-card h2 { margin: 0 0 8px; font-size: 18px; }
.analysis-card p { margin: 0; color: #64748b; line-height: 1.7; }
.student-chart { margin-top: 2px; }
</style>
