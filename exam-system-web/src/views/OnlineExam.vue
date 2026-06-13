<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getExam, getExamQuestions } from '../api/exam'
import { submitExam } from '../api/studentExam'
import { ElMessage, ElMessageBox } from 'element-plus'
const route=useRoute(),router=useRouter(),exam=ref({}),questions=ref([]),answers=reactive({}),seconds=ref(0),submitting=ref(false)
let timer
const timeText=computed(()=>`${String(Math.floor(seconds.value/60)).padStart(2,'0')}:${String(seconds.value%60).padStart(2,'0')}`)
function options(q){try{return JSON.parse(q.optionsJson||'[]')}catch{return[]}}
async function submit(auto=false){if(submitting.value)return;if(!auto)await ElMessageBox.confirm('提交后不能再次作答，确认提交吗？','提交确认',{type:'warning'});submitting.value=true;try{const items=questions.value.map(q=>({questionId:q.id,answer:Array.isArray(answers[q.id])?answers[q.id].join(','):answers[q.id]||''}));await submitExam({studentExamId:Number(route.query.studentExamId),answers:items});ElMessage.success('试卷已提交');router.replace('/scores')}finally{submitting.value=false}}
onMounted(async()=>{
  if(!route.query.studentExamId){ElMessage.error('缺少考试记录，请重新开始考试');return router.replace('/student-exams')}
  exam.value=await getExam(route.params.id)
  questions.value=await getExamQuestions(route.params.id)
  const startedAt=route.query.startedAt?new Date(route.query.startedAt).getTime():Date.now()
  const durationEnd=startedAt+(exam.value.durationMinutes||60)*60*1000
  const examEnd=exam.value.endTime?new Date(exam.value.endTime).getTime():durationEnd
  seconds.value=Math.max(0,Math.floor((Math.min(durationEnd,examEnd)-Date.now())/1000))
  if(seconds.value===0)return submit(true)
  timer=setInterval(()=>{seconds.value--;if(seconds.value<=0){clearInterval(timer);submit(true)}},1000)
})
onBeforeUnmount(()=>clearInterval(timer))
</script>
<template><div class="exam-page"><header><div><h1>{{exam.examName}}</h1><span>共 {{questions.length}} 题 · 总分 {{exam.totalScore}}</span></div><div class="timer">{{timeText}}</div><el-button type="danger" :loading="submitting" @click="submit(false)">提交试卷</el-button></header>
<main><section v-for="(q,index) in questions" :key="q.id" class="question"><h3>{{index+1}}. {{q.content}} <small>{{q.score}} 分</small></h3>
<el-radio-group v-if="q.questionType==='SINGLE_CHOICE'" v-model="answers[q.id]" class="options"><el-radio v-for="(o,i) in options(q)" :key="i" :value="String.fromCharCode(65+i)">{{String.fromCharCode(65+i)}}. {{o}}</el-radio></el-radio-group>
<el-checkbox-group v-else-if="q.questionType==='MULTIPLE_CHOICE'" v-model="answers[q.id]" class="options"><el-checkbox v-for="(o,i) in options(q)" :key="i" :value="String.fromCharCode(65+i)">{{String.fromCharCode(65+i)}}. {{o}}</el-checkbox></el-checkbox-group>
<el-radio-group v-else-if="q.questionType==='TRUE_FALSE'" v-model="answers[q.id]"><el-radio value="TRUE">正确</el-radio><el-radio value="FALSE">错误</el-radio></el-radio-group>
<el-input v-else-if="q.questionType==='FILL_BLANK'" v-model="answers[q.id]" placeholder="请输入答案"/>
<el-input v-else v-model="answers[q.id]" type="textarea" :rows="5" placeholder="请输入简答内容"/>
</section></main></div></template>
<style scoped>.exam-page{min-height:100vh;background:#eef2f6}.exam-page header{position:sticky;top:0;z-index:5;background:#fff;border-bottom:1px solid #dbe1e8;padding:16px 5vw;display:flex;align-items:center;justify-content:space-between;gap:20px}.exam-page h1{font-size:20px;margin:0 0 6px}.exam-page header span{color:#6b7280}.timer{font:700 28px monospace;color:#b91c1c}main{max-width:900px;margin:22px auto;padding:0 14px 50px}.question{background:#fff;border:1px solid #dfe4ea;padding:24px;margin-bottom:14px}.question h3{font-size:16px;line-height:1.7;margin-top:0}.question small{color:#64748b;font-weight:400}.options{display:flex;flex-direction:column;align-items:flex-start;gap:12px}@media(max-width:650px){.exam-page header{flex-wrap:wrap}.timer{font-size:22px}}</style>
