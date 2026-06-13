<script setup>
import { onMounted, ref } from 'vue'
import { deleteWrongQuestion, getWrongDetail, getWrongQuestions } from '../api/wrongQuestion'
import { ElMessageBox } from 'element-plus'
const rows=ref([]),detail=ref(null),visible=ref(false)
async function load(){rows.value=await getWrongQuestions()}
async function show(id){detail.value=await getWrongDetail(id);visible.value=true}
async function remove(id){await ElMessageBox.confirm('从错题本中删除？','提示');await deleteWrongQuestion(id);load()}
onMounted(load)
</script>
<template><div class="page"><div class="page-header"><h1 class="page-title">错题本</h1></div><div class="panel"><el-table :data="rows"><el-table-column prop="questionId" label="题目 ID" width="100"/><el-table-column prop="examId" label="考试 ID" width="100"/><el-table-column prop="studentAnswer" label="我的答案"/><el-table-column prop="correctAnswer" label="正确答案"/><el-table-column prop="createTime" label="记录时间"/><el-table-column label="操作" width="150"><template #default="{row}"><el-button link @click="show(row.id)">查看解析</el-button><el-button link type="danger" @click="remove(row.id)">删除</el-button></template></el-table-column></el-table></div><el-dialog v-model="visible" title="错题解析" width="620" v-if="detail"><el-descriptions :column="1" border><el-descriptions-item label="题目">{{detail.question.content}}</el-descriptions-item><el-descriptions-item label="我的答案">{{detail.wrongQuestion.studentAnswer}}</el-descriptions-item><el-descriptions-item label="正确答案">{{detail.question.answer}}</el-descriptions-item><el-descriptions-item label="解析">{{detail.question.analysis}}</el-descriptions-item></el-descriptions></el-dialog></div></template>
