<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { autoPaper, getExam } from '../api/exam'
import { ElMessage } from 'element-plus'
const route=useRoute(),result=ref([]),loading=ref(false)
const form=reactive({courseId:1,singleChoiceCount:2,multipleChoiceCount:1,trueFalseCount:1,fillBlankCount:1,shortAnswerCount:0,easyRatio:0.4,mediumRatio:0.4,hardRatio:0.2})
async function generate(){loading.value=true;try{result.value=await autoPaper(route.params.id,form);ElMessage.success(`组卷成功，共 ${result.value.length} 题`)}finally{loading.value=false}}
onMounted(async()=>{const exam=await getExam(route.params.id);form.courseId=exam.courseId})
</script>
<template><div class="page"><div class="page-header"><h1 class="page-title">自动组卷</h1><el-button @click="$router.back()">返回</el-button></div><div class="panel"><el-form :model="form" label-width="130"><el-form-item label="课程 ID"><el-input-number v-model="form.courseId" :min="1"/></el-form-item><el-form-item label="各题型数量"><div class="counts"><label>单选<el-input-number v-model="form.singleChoiceCount" :min="0"/></label><label>多选<el-input-number v-model="form.multipleChoiceCount" :min="0"/></label><label>判断<el-input-number v-model="form.trueFalseCount" :min="0"/></label><label>填空<el-input-number v-model="form.fillBlankCount" :min="0"/></label><label>简答<el-input-number v-model="form.shortAnswerCount" :min="0"/></label></div></el-form-item><el-form-item label="难度比例"><div class="counts"><label>简单<el-input-number v-model="form.easyRatio" :step="0.1" :min="0" :max="1"/></label><label>中等<el-input-number v-model="form.mediumRatio" :step="0.1" :min="0" :max="1"/></label><label>困难<el-input-number v-model="form.hardRatio" :step="0.1" :min="0" :max="1"/></label></div></el-form-item><el-form-item><el-button type="primary" :loading="loading" @click="generate">生成试卷</el-button></el-form-item></el-form></div>
<div v-if="result.length" class="panel result"><h3>组卷结果</h3><el-table :data="result"><el-table-column type="index" width="55"/><el-table-column prop="content" label="题目"/><el-table-column prop="questionType" label="题型" width="150"/><el-table-column prop="difficulty" label="难度" width="100"/><el-table-column prop="score" label="分值" width="80"/></el-table></div></div></template>
<style scoped>.counts{display:flex;gap:16px;flex-wrap:wrap}.counts label{display:grid;gap:6px;color:#6b7280}.result{margin-top:18px}</style>
