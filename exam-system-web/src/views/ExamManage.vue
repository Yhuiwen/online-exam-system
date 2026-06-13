<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createExam, getExamQuestions, getExams, updateExamStatus } from '../api/exam'
import { getCourses } from '../api/course'
import { ElMessage } from 'element-plus'
const rows=ref([]),courses=ref([]),visible=ref(false),questionVisible=ref(false),questions=ref([])
const form=reactive({examName:'',courseId:null,startTime:null,endTime:null,durationMinutes:60})
async function load(){rows.value=await getExams()}
async function save(){await createExam(form);visible.value=false;ElMessage.success('考试创建成功');load()}
async function status(id,value){await updateExamStatus(id,value);load()}
async function showQuestions(id){questions.value=await getExamQuestions(id);questionVisible.value=true}
onMounted(async()=>{courses.value=await getCourses();load()})
</script>
<template><div class="page"><div class="page-header"><h1 class="page-title">考试管理</h1><el-button type="primary" @click="visible=true">创建考试</el-button></div><div class="panel"><el-table :data="rows"><el-table-column prop="examName" label="考试名称"/><el-table-column prop="durationMinutes" label="时长(分钟)" width="110"/><el-table-column prop="totalScore" label="总分" width="80"/><el-table-column prop="status" label="状态" width="110"/><el-table-column label="操作" width="310"><template #default="{row}"><el-button link @click="showQuestions(row.id)">查看题目</el-button><el-button link type="primary" @click="$router.push(`/auto-paper/${row.id}`)">自动组卷</el-button><el-button v-if="row.status==='DRAFT'" link type="success" @click="status(row.id,'PUBLISHED')">发布</el-button><el-button v-if="row.status==='PUBLISHED'" link type="danger" @click="status(row.id,'CLOSED')">关闭</el-button></template></el-table-column></el-table></div>
<el-dialog v-model="visible" title="创建考试" width="560"><el-form label-width="100"><el-form-item label="考试名称"><el-input v-model="form.examName"/></el-form-item><el-form-item label="课程"><el-select v-model="form.courseId"><el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id"/></el-select></el-form-item><el-form-item label="考试时间"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"/></el-form-item><el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"/></el-form-item><el-form-item label="时长"><el-input-number v-model="form.durationMinutes" :min="1"/></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">创建</el-button></template></el-dialog>
<el-dialog v-model="questionVisible" title="试卷题目" width="720"><el-table :data="questions"><el-table-column type="index" width="55"/><el-table-column prop="content" label="题目"/><el-table-column prop="questionType" label="题型" width="150"/><el-table-column prop="score" label="分值" width="70"/></el-table></el-dialog>
</div></template>
