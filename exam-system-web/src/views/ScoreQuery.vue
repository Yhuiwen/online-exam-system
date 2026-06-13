<script setup>
import { onMounted, ref } from 'vue'
import { getAnswers, getStudentExams } from '../api/studentExam'
const rows=ref([]),answers=ref([]),visible=ref(false)
async function detail(id){answers.value=await getAnswers(id);visible.value=true}
onMounted(async()=>rows.value=await getStudentExams())
</script>
<template><div class="page"><div class="page-header"><h1 class="page-title">成绩查询</h1></div><div class="panel"><el-table :data="rows"><el-table-column prop="examId" label="考试 ID" width="100"/><el-table-column prop="startTime" label="开始时间"/><el-table-column prop="submitTime" label="提交时间"/><el-table-column prop="totalScore" label="得分" width="90"/><el-table-column label="状态" width="150"><template #default="{row}"><el-tag :type="row.status==='GRADED'?'success':'warning'">{{row.status}}</el-tag></template></el-table-column><el-table-column label="操作" width="100"><template #default="{row}"><el-button link @click="detail(row.id)">答题详情</el-button></template></el-table-column></el-table></div><el-dialog v-model="visible" title="答题详情" width="700"><el-table :data="answers"><el-table-column prop="questionId" label="题目 ID" width="100"/><el-table-column prop="answer" label="我的答案"/><el-table-column prop="score" label="得分" width="80"/><el-table-column label="结果" width="90"><template #default="{row}"><el-tag v-if="row.isCorrect===true" type="success">正确</el-tag><el-tag v-else-if="row.isCorrect===false" type="danger">错误</el-tag><el-tag v-else type="warning">待批改</el-tag></template></el-table-column></el-table></el-dialog></div></template>
