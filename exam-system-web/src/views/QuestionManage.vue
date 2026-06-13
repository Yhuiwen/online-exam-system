<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createQuestion,
  deleteQuestion,
  downloadQuestionTemplate,
  exportQuestions,
  getQuestions,
  importQuestions,
  updateQuestion
} from '../api/question'
import { getCourses } from '../api/course'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Upload } from '@element-plus/icons-vue'

const rows = ref([])
const courses = ref([])
const total = ref(0)
const visible = ref(false)
const editing = ref(null)
const importVisible = ref(false)
const importFile = ref(null)
const importResult = ref(null)
const importing = ref(false)
const query = reactive({
  page: 1,
  size: 10,
  courseId: null,
  questionType: '',
  difficulty: '',
  keyword: ''
})
const form = reactive({
  courseId: null,
  questionType: 'SINGLE_CHOICE',
  content: '',
  options: ['', '', '', ''],
  answer: '',
  analysis: '',
  difficulty: 'EASY',
  score: 5,
  knowledgeTag: ''
})
const choice = computed(() => ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.questionType))
const types = [
  ['SINGLE_CHOICE', '单选'],
  ['MULTIPLE_CHOICE', '多选'],
  ['TRUE_FALSE', '判断'],
  ['FILL_BLANK', '填空'],
  ['SHORT_ANSWER', '简答']
]

async function load() {
  const data = await getQuestions(query)
  rows.value = data.records
  total.value = data.total
}

function parseOptions(json) {
  try {
    const values = json ? JSON.parse(json) : []
    return [...values, '', '', '', ''].slice(0, 4)
  } catch {
    return ['', '', '', '']
  }
}

function open(row) {
  editing.value = row?.id || null
  Object.assign(form, row
    ? { ...row, options: parseOptions(row.optionsJson) }
    : {
        courseId: courses.value[0]?.id,
        questionType: 'SINGLE_CHOICE',
        content: '',
        options: ['', '', '', ''],
        answer: '',
        analysis: '',
        difficulty: 'EASY',
        score: 5,
        knowledgeTag: ''
      })
  visible.value = true
}

async function save() {
  const data = {
    ...form,
    optionsJson: choice.value ? JSON.stringify(form.options) : null
  }
  delete data.options
  editing.value ? await updateQuestion(editing.value, data) : await createQuestion(data)
  ElMessage.success('保存成功')
  visible.value = false
  load()
}

async function remove(id) {
  await ElMessageBox.confirm('确认删除题目？', '提示')
  await deleteQuestion(id)
  load()
}

function saveBlob(response, fallbackName) {
  const disposition = response.headers['content-disposition'] || ''
  const match = disposition.match(/filename="?([^";]+)"?/)
  const filename = match?.[1] || fallbackName
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

async function downloadTemplate() {
  saveBlob(await downloadQuestionTemplate(), 'question-import-template.xlsx')
}

async function exportBank() {
  const params = {
    courseId: query.courseId || undefined,
    questionType: query.questionType || undefined,
    difficulty: query.difficulty || undefined,
    keyword: query.keyword || undefined
  }
  saveBlob(await exportQuestions(params), 'question-bank.xlsx')
}

function chooseFile(uploadFile) {
  importFile.value = uploadFile.raw
  importResult.value = null
}

async function submitImport() {
  if (!importFile.value) return ElMessage.warning('请选择 .xlsx 文件')
  if (!importFile.value.name.toLowerCase().endsWith('.xlsx')) {
    return ElMessage.warning('只支持 .xlsx 文件')
  }
  importing.value = true
  try {
    importResult.value = await importQuestions(importFile.value)
    if (importResult.value.successCount > 0) {
      ElMessage.success(`成功导入 ${importResult.value.successCount} 道题`)
      await load()
    } else {
      ElMessage.warning('没有有效题目被导入，请检查错误明细')
    }
  } finally {
    importing.value = false
  }
}

function openImport() {
  importFile.value = null
  importResult.value = null
  importVisible.value = true
}

onMounted(async () => {
  courses.value = await getCourses()
  load()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">题库管理</h1>
      <div class="header-actions">
        <el-button :icon="Download" @click="downloadTemplate">下载导入模板</el-button>
        <el-button :icon="Upload" @click="openImport">导入题库</el-button>
        <el-button :icon="Download" @click="exportBank">导出题库</el-button>
        <el-button type="primary" @click="open()">新增题目</el-button>
      </div>
    </div>

    <div class="toolbar">
      <el-select v-model="query.courseId" clearable placeholder="课程" style="width: 180px">
        <el-option v-for="course in courses" :key="course.id" :label="course.courseName" :value="course.id" />
      </el-select>
      <el-select v-model="query.questionType" clearable placeholder="题型" style="width: 150px">
        <el-option v-for="type in types" :key="type[0]" :label="type[1]" :value="type[0]" />
      </el-select>
      <el-select v-model="query.difficulty" clearable placeholder="难度" style="width: 130px">
        <el-option label="简单" value="EASY" />
        <el-option label="中等" value="MEDIUM" />
        <el-option label="困难" value="HARD" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="题目内容或知识点" clearable style="width: 210px" />
      <el-button type="primary" @click="query.page = 1; load()">筛选</el-button>
    </div>

    <div class="panel">
      <el-table :data="rows">
        <el-table-column prop="content" label="题目" min-width="300" show-overflow-tooltip />
        <el-table-column prop="questionType" label="题型" width="150" />
        <el-table-column prop="difficulty" label="难度" width="100" />
        <el-table-column prop="knowledgeTag" label="知识点" width="140" />
        <el-table-column prop="score" label="分值" width="80" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link @click="open(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="total,prev,pager,next"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="importVisible" title="导入题库" width="620px">
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx"
        :on-change="chooseFile"
      >
        <el-icon class="el-icon--upload"><Upload /></el-icon>
        <div class="el-upload__text">将 .xlsx 文件拖到此处，或点击选择</div>
      </el-upload>
      <el-alert
        v-if="importResult"
        :title="`成功 ${importResult.successCount} 条，失败 ${importResult.failCount} 条`"
        :type="importResult.failCount ? 'warning' : 'success'"
        :closable="false"
        show-icon
        class="import-summary"
      />
      <el-table v-if="importResult?.errors?.length" :data="importResult.errors" max-height="260">
        <el-table-column prop="rowNum" label="行号" width="80" />
        <el-table-column prop="message" label="失败原因" />
      </el-table>
      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="visible" :title="editing ? '编辑题目' : '新增题目'" width="680">
      <el-form label-width="90">
        <el-form-item label="课程">
          <el-select v-model="form.courseId">
            <el-option v-for="course in courses" :key="course.id" :label="course.courseName" :value="course.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="form.questionType">
            <el-option v-for="type in types" :key="type[0]" :label="type[1]" :value="type[0]" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item v-if="choice" label="选项">
          <div class="option-list">
            <el-input v-for="(_, index) in form.options" :key="index" v-model="form.options[index]">
              <template #prepend>{{ String.fromCharCode(65 + index) }}</template>
            </el-input>
          </div>
        </el-form-item>
        <el-form-item label="答案">
          <el-input v-model="form.answer" :placeholder="form.questionType === 'MULTIPLE_CHOICE' ? '如 A,B,C' : '请输入标准答案'" />
        </el-form-item>
        <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" /></el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="form.difficulty">
            <el-radio-button value="EASY">简单</el-radio-button>
            <el-radio-button value="MEDIUM">中等</el-radio-button>
            <el-radio-button value="HARD">困难</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分值"><el-input-number v-model="form.score" :min="1" /></el-form-item>
        <el-form-item label="知识点"><el-input v-model="form.knowledgeTag" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.option-list { display: grid; gap: 8px; width: 100%; }
.import-summary { margin: 16px 0; }
</style>
