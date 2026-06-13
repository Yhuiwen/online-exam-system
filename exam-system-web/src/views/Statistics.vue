<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getExamStatistics, getStudentStatistics } from '../api/statistics'
import { getExams } from '../api/exam'
import { useAuthStore } from '../store/auth'
const auth=useAuthStore(),exams=ref([]),examId=ref(null),data=ref({})
const chart1=ref(),chart2=ref(),chart3=ref()
let distributionChart,rateChart,trendChart
function renderTeacher(){
  distributionChart ||= echarts.init(chart1.value)
  rateChart ||= echarts.init(chart2.value)
  distributionChart.setOption({title:{text:'成绩分布'},tooltip:{},xAxis:{type:'category',data:Object.keys(data.value.distribution||{})},yAxis:{type:'value'},series:[{type:'bar',data:Object.values(data.value.distribution||{}),itemStyle:{color:'#2563eb'}}]},true)
  rateChart.setOption({title:{text:'题目正确率'},tooltip:{},xAxis:{type:'category',data:(data.value.questionRates||[]).map(x=>`题${x.questionId}`)},yAxis:{type:'value',max:100},series:[{type:'bar',data:(data.value.questionRates||[]).map(x=>x.correctRate),itemStyle:{color:'#0f766e'}}]},true)
}
function renderStudent(){const trend=data.value.scoreTrend||[];trendChart ||= echarts.init(chart3.value);trendChart.setOption({title:{text:'个人成绩趋势'},tooltip:{trigger:'axis'},xAxis:{type:'category',data:trend.map(x=>x.examName)},yAxis:{type:'value'},series:[{type:'line',smooth:true,data:trend.map(x=>x.score),itemStyle:{color:'#d97706'}}]},true)}
async function load(){if(auth.user.role==='STUDENT'){data.value=await getStudentStatistics();await nextTick();renderStudent()}else if(examId.value){data.value=await getExamStatistics(examId.value);await nextTick();renderTeacher()}}
function resizeCharts(){distributionChart?.resize();rateChart?.resize();trendChart?.resize()}
onMounted(async()=>{window.addEventListener('resize',resizeCharts);if(auth.user.role!=='STUDENT'){exams.value=await getExams();examId.value=exams.value[0]?.id}await load()})
onBeforeUnmount(()=>{window.removeEventListener('resize',resizeCharts);distributionChart?.dispose();rateChart?.dispose();trendChart?.dispose()})
</script>
<template><div class="page"><div class="page-header"><h1 class="page-title">统计分析</h1><el-select v-if="auth.user?.role!=='STUDENT'" v-model="examId" placeholder="选择考试" style="width:240px" @change="load"><el-option v-for="e in exams" :key="e.id" :label="e.examName" :value="e.id"/></el-select></div>
<div class="metric-grid"><div class="metric"><span>{{auth.user?.role==='STUDENT'?'考试次数':'参加人数'}}</span><strong>{{data.examCount??data.participantCount??0}}</strong></div><div class="metric"><span>平均分</span><strong>{{data.averageScore??0}}</strong></div><div class="metric"><span>{{auth.user?.role==='STUDENT'?'错题数量':'最高分'}}</span><strong>{{data.wrongCount??data.highestScore??0}}</strong></div><div class="metric"><span>{{auth.user?.role==='STUDENT'?'学习状态':'最低分'}}</span><strong>{{auth.user?.role==='STUDENT'?'进行中':data.lowestScore??0}}</strong></div></div>
<div v-if="auth.user?.role!=='STUDENT'" class="chart-grid"><div ref="chart1" class="chart"></div><div ref="chart2" class="chart"></div></div><div v-else ref="chart3" class="chart" style="margin-top:18px"></div></div></template>
