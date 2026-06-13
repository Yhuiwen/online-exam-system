<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getExams } from '../api/exam'
import { getStudentExams, startExam } from '../api/studentExam'
const rows=ref([]),records=ref(new Map()),router=useRouter()
async function start(row){
  const record=await startExam(row.id)
  router.push({path:`/online-exam/${row.id}`,query:{studentExamId:record.id,startedAt:record.startTime}})
}
function recordOf(examId){return records.value.get(examId)}
onMounted(async()=>{
  const [exams,attempts]=await Promise.all([getExams(),getStudentExams()])
  rows.value=exams
  records.value=new Map(attempts.map(item=>[item.examId,item]))
})
</script>
<template><div class="page"><div class="page-header"><h1 class="page-title">可参加考试</h1></div><div class="panel"><el-table :data="rows"><el-table-column prop="examName" label="考试名称"/><el-table-column prop="startTime" label="开始时间"/><el-table-column prop="endTime" label="结束时间"/><el-table-column prop="durationMinutes" label="时长(分钟)" width="110"/><el-table-column prop="totalScore" label="总分" width="80"/><el-table-column label="操作" width="110"><template #default="{row}"><el-button v-if="!recordOf(row.id)" type="primary" link @click="start(row)">开始考试</el-button><el-button v-else-if="recordOf(row.id).status==='IN_PROGRESS'" type="warning" link @click="start(row)">继续答题</el-button><el-tag v-else type="success">已提交</el-tag></template></el-table-column></el-table></div></div></template>
