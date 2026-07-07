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
import { generateAiQuestions, parseAiDocument, saveAiQuestions } from '../api/aiQuestion'
import { getCourses } from '../api/course'
import { useAuthStore } from '../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, MagicStick, Upload } from '@element-plus/icons-vue'
import {
  examScopeOptions,
  formatDifficulty,
  formatQuestionType,
  formatSourceCategory,
  formatSourceSummary,
  provinceOptions,
  sourceCategoryOptions
} from '../utils/enumMap'

const rows = ref([])
const authStore = useAuthStore()
const courses = ref([])
const total = ref(0)
const visible = ref(false)
const editing = ref(null)
const importVisible = ref(false)
const importFile = ref(null)
const importResult = ref(null)
const importing = ref(false)
const aiVisible = ref(false)
const parseVisible = ref(false)
const parseLoading = ref(false)
const parseFile = ref(null)
const parseForm = reactive({ courseId: null, knowledgePoint: '' })
const aiGenerating = ref(false)
const aiSaving = ref(false)
const aiPreviewRows = ref([])
const query = reactive({
  page: 1,
  size: 10,
  courseId: null,
  questionType: '',
  difficulty: '',
  sourceCategory: '',
  examYear: null,
  examScope: '',
  province: '',
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
  knowledgeTag: '',
  sourceCategory: 'PRACTICE',
  examYear: null,
  examScope: '',
  province: '',
  paperType: '',
  sourceRef: ''
})
const aiForm = reactive({
  courseId: null,
  questionType: 'SINGLE_CHOICE',
  difficulty: 'EASY',
  knowledgePoint: '',
  count: 5,
  score: 5,
  requirement: ''
})
const choice = computed(() => ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.questionType))
const canUseAi = computed(() => ['ADMIN', 'TEACHER'].includes(authStore.user?.role))
const isRealExam = computed(() => form.sourceCategory === 'REAL_EXAM')
const isMockExam = computed(() => form.sourceCategory === 'MOCK_EXAM')
const isSelfAuthored = computed(() => form.sourceCategory === 'SELF_AUTHORED')
const showArchiveFields = computed(() => isRealExam.value || isMockExam.value)
const yearOptions = computed(() => {
  const current = new Date().getFullYear()
  return Array.from({ length: 12 }, (_, index) => current - index)
})
const types = [
  ['SINGLE_CHOICE', '单选'],
  ['MULTIPLE_CHOICE', '多选'],
  ['TRUE_FALSE', '判断'],
  ['FILL_BLANK', '填空'],
  ['SHORT_ANSWER', '简答']
]

function difficultyTagType(value) {
  return { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }[value] || 'info'
}

function questionTypeTagType(value) {
  return {
    SINGLE_CHOICE: 'primary',
    MULTIPLE_CHOICE: 'success',
    TRUE_FALSE: 'info',
    FILL_BLANK: 'warning',
    SHORT_ANSWER: 'danger'
  }[value] || 'info'
}

function defaultForm() {
  return {
    courseId: courses.value[0]?.id,
    questionType: 'SINGLE_CHOICE',
    content: '',
    options: ['', '', '', ''],
    answer: '',
    analysis: '',
    difficulty: 'EASY',
    score: 5,
    knowledgeTag: '',
    sourceCategory: 'PRACTICE',
    examYear: null,
    examScope: '',
    province: '',
    paperType: '',
    sourceRef: ''
  }
}

function defaultAiForm() {
  return {
    courseId: query.courseId || courses.value[0]?.id,
    questionType: 'SINGLE_CHOICE',
    difficulty: 'EASY',
    knowledgePoint: '',
    count: 5,
    score: 5,
    requirement: ''
  }
}

async function load() {
  const data = await getQuestions({
    ...query,
    examYear: query.examYear || undefined,
    sourceCategory: query.sourceCategory || undefined,
    examScope: query.examScope || undefined,
    province: query.province || undefined
  })
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
    ? {
        ...row,
        sourceCategory: row.sourceCategory || 'PRACTICE',
        options: parseOptions(row.optionsJson)
      }
    : defaultForm())
  visible.value = true
}

function openAiDialog() {
  Object.assign(aiForm, defaultAiForm())
  aiPreviewRows.value = []
  aiVisible.value = true
}

function resetAiDialog() {
  aiGenerating.value = false
  aiSaving.value = false
  aiPreviewRows.value = []
  Object.assign(aiForm, defaultAiForm())
}

function isAiChoice(row) {
  return ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(row.questionType)
}

function validateAiForm() {
  if (!aiForm.courseId) return 'Please select a course'
  if (!aiForm.questionType) return 'Please select a question type'
  if (!aiForm.difficulty) return 'Please select a difficulty'
  if (!aiForm.count || aiForm.count < 1 || aiForm.count > 20) return 'Count must be between 1 and 20'
  if (!aiForm.score || aiForm.score <= 0) return 'Score must be greater than 0'
  return ''
}

async function generateAi() {
  const message = validateAiForm()
  if (message) return ElMessage.warning(message)
  aiGenerating.value = true
  try {
    aiPreviewRows.value = await generateAiQuestions({ ...aiForm })
    if (!aiPreviewRows.value.length) ElMessage.warning('AI returned no questions')
  } catch (error) {
    ElMessage.error(error?.message || 'Failed to generate AI questions')
  } finally {
    aiGenerating.value = false
  }
}

function removeAiRow(index) {
  aiPreviewRows.value.splice(index, 1)
}

async function saveAiPreview() {
  if (!aiForm.courseId) return ElMessage.warning('Please select a course')
  if (!aiPreviewRows.value.length) return ElMessage.warning('Please generate questions first')
  aiSaving.value = true
  try {
    await saveAiQuestions({
      courseId: aiForm.courseId,
      questions: aiPreviewRows.value
    })
    ElMessage.success('AI questions saved')
    aiVisible.value = false
    parseVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error?.message || 'Failed to save AI questions')
  } finally {
    aiSaving.value = false
  }
}

function openParseDialog() {
  parseForm.courseId = query.courseId || courses.value[0]?.id || null
  parseForm.knowledgePoint = ''
  parseFile.value = null
  aiPreviewRows.value = []
  parseVisible.value = true
}

async function parseDocument() {
  if (!parseForm.courseId) return ElMessage.warning('请选择课程')
  if (!parseFile.value) return ElMessage.warning('请选择 PDF 或 Word 文档')
  parseLoading.value = true
  try {
    aiForm.courseId = parseForm.courseId
    aiPreviewRows.value = await parseAiDocument(
      parseForm.courseId,
      parseFile.value,
      parseForm.knowledgePoint
    )
    if (!aiPreviewRows.value.length) ElMessage.warning('未能从文档解析出题目')
    else ElMessage.success(`已解析 ${aiPreviewRows.value.length} 道题目，请确认后保存`)
  } catch (error) {
    ElMessage.error(error?.message || '文档解析失败')
  } finally {
    parseLoading.value = false
  }
}

function onParseFileChange(file) {
  parseFile.value = file.raw
}

function resetSourceFields() {
  form.examYear = null
  form.examScope = ''
  form.province = ''
  form.paperType = ''
  form.sourceRef = ''
}

async function save() {
  const data = {
    ...form,
    sourceCategory: form.sourceCategory === 'PRACTICE' ? null : form.sourceCategory,
    examYear: ['REAL_EXAM', 'MOCK_EXAM'].includes(form.sourceCategory) ? form.examYear : null,
    examScope: ['REAL_EXAM', 'MOCK_EXAM'].includes(form.sourceCategory) ? form.examScope || null : null,
    province: ['REAL_EXAM', 'MOCK_EXAM'].includes(form.sourceCategory) ? form.province || null : null,
    paperType: ['REAL_EXAM', 'MOCK_EXAM'].includes(form.sourceCategory) ? form.paperType || null : null,
    sourceRef: form.sourceCategory === 'PRACTICE' ? null : form.sourceRef || null,
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
    keyword: query.keyword || undefined,
    sourceCategory: query.sourceCategory || undefined,
    examYear: query.examYear || undefined,
    examScope: query.examScope || undefined,
    province: query.province || undefined
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
        <el-button v-if="canUseAi" type="success" :icon="MagicStick" @click="openAiDialog">AI 出题</el-button>
        <el-button v-if="canUseAi" @click="openParseDialog">解析文档</el-button>
        <el-button type="primary" @click="open()">新增题目</el-button>
      </div>
    </div>

    <section class="info-card">
      <div>
        <h2>题库筛选与批量维护</h2>
        <p>支持题型、难度、知识点筛选，支持 Excel 批量导入导出。教师可按课程、来源和考试范围维护题目，便于后续自动组卷与试卷预览。</p>
      </div>
      <el-tag type="primary" effect="dark">Question Bank</el-tag>
    </section>

    <div class="toolbar">
      <el-select v-model="query.courseId" clearable placeholder="课程" style="width: 180px">
        <el-option v-for="course in courses" :key="course.id" :label="course.courseName" :value="course.id" />
      </el-select>
      <el-select v-model="query.sourceCategory" clearable placeholder="题目分类" style="width: 130px">
        <el-option v-for="item in sourceCategoryOptions" :key="item[0]" :label="item[1]" :value="item[0]" />
      </el-select>
      <el-select v-model="query.questionType" clearable placeholder="题型" style="width: 150px">
        <el-option v-for="type in types" :key="type[0]" :label="type[1]" :value="type[0]" />
      </el-select>
      <el-select v-model="query.difficulty" clearable placeholder="难度" style="width: 130px">
        <el-option label="简单" value="EASY" />
        <el-option label="中等" value="MEDIUM" />
        <el-option label="困难" value="HARD" />
      </el-select>
      <el-select v-model="query.examYear" clearable placeholder="年份" style="width: 110px">
        <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
      </el-select>
      <el-select v-model="query.examScope" clearable placeholder="考试类型" style="width: 120px">
        <el-option v-for="item in examScopeOptions" :key="item[0]" :label="item[1]" :value="item[0]" />
      </el-select>
      <el-select v-model="query.province" clearable filterable placeholder="省份" style="width: 130px">
        <el-option v-for="province in provinceOptions" :key="province" :label="province" :value="province" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="题目内容或知识点" clearable style="width: 210px" />
      <el-button type="primary" @click="query.page = 1; load()">筛选</el-button>
    </div>

    <div class="panel">
      <el-table :data="rows">
        <el-table-column prop="content" label="题目" min-width="260" show-overflow-tooltip />
        <el-table-column label="分类" width="90">
          <template #default="{ row }">
            <el-tag :type="row.sourceCategory === 'REAL_EXAM' ? 'success' : row.sourceCategory ? 'warning' : 'info'" effect="plain">
              {{ formatSourceCategory(row.sourceCategory) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ formatSourceSummary(row) }}</template>
        </el-table-column>
        <el-table-column label="题型" width="110">
          <template #default="{ row }">
            <el-tag :type="questionTypeTagType(row.questionType)" effect="plain">
              {{ formatQuestionType(row.questionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="90">
          <template #default="{ row }">
            <el-tag :type="difficultyTagType(row.difficulty)" effect="plain">
              {{ formatDifficulty(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="knowledgeTag" label="知识点" width="120" show-overflow-tooltip />
        <el-table-column prop="score" label="分值" width="70" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link @click="open(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!rows.length" description="暂无题目，请调整筛选条件或导入题库" />
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
      <p class="import-tip">
        模板支持题目分类：练习题、真题、模拟题、自命题。真题需标注年份、国考/省考、省份与来源；模拟题和自命题需填写来源说明。
      </p>
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

    <el-dialog v-model="aiVisible" title="AI 出题" width="95%" top="4vh" class="ai-question-dialog" @closed="resetAiDialog">
      <el-form :model="aiForm" label-width="96">
        <div class="ai-form-grid">
          <el-form-item label="课程" required>
            <el-select v-model="aiForm.courseId" placeholder="选择课程" style="width: 100%">
              <el-option v-for="course in courses" :key="course.id" :label="course.courseName" :value="course.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="题型" required>
            <el-select v-model="aiForm.questionType" style="width: 100%">
              <el-option v-for="type in types" :key="type[0]" :label="type[1]" :value="type[0]" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度" required>
            <el-select v-model="aiForm.difficulty" style="width: 100%">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </el-form-item>
          <el-form-item label="知识点">
            <el-input v-model="aiForm.knowledgePoint" placeholder="如 Spring MVC" />
          </el-form-item>
          <el-form-item label="生成数量" required>
            <el-input-number v-model="aiForm.count" :min="1" :max="20" />
          </el-form-item>
          <el-form-item label="每题分值" required>
            <el-input-number v-model="aiForm.score" :min="1" />
          </el-form-item>
        </div>
        <el-form-item label="额外要求">
          <el-input
            v-model="aiForm.requirement"
            type="textarea"
            :rows="2"
            placeholder="可补充考查范围、题目风格、避免内容等"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="MagicStick" :loading="aiGenerating" @click="generateAi">生成题目</el-button>
        </el-form-item>
      </el-form>

      <el-table v-if="aiPreviewRows.length" :data="aiPreviewRows" border class="ai-preview-table" max-height="520">
        <el-table-column label="题型" width="150">
          <template #default="{ row }">
            <el-select v-model="row.questionType">
              <el-option v-for="type in types" :key="type[0]" :label="type[1]" :value="type[0]" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="题干" min-width="260">
          <template #default="{ row }"><el-input v-model="row.content" type="textarea" :rows="2" /></template>
        </el-table-column>
        <el-table-column label="选项A" min-width="180">
          <template #default="{ row }"><el-input v-if="isAiChoice(row)" v-model="row.optionA" /></template>
        </el-table-column>
        <el-table-column label="选项B" min-width="180">
          <template #default="{ row }"><el-input v-if="isAiChoice(row)" v-model="row.optionB" /></template>
        </el-table-column>
        <el-table-column label="选项C" min-width="180">
          <template #default="{ row }"><el-input v-if="isAiChoice(row)" v-model="row.optionC" /></template>
        </el-table-column>
        <el-table-column label="选项D" min-width="180">
          <template #default="{ row }"><el-input v-if="isAiChoice(row)" v-model="row.optionD" /></template>
        </el-table-column>
        <el-table-column label="答案" width="150">
          <template #default="{ row }"><el-input v-model="row.correctAnswer" /></template>
        </el-table-column>
        <el-table-column label="解析" min-width="260">
          <template #default="{ row }"><el-input v-model="row.analysis" type="textarea" :rows="2" /></template>
        </el-table-column>
        <el-table-column label="难度" width="130">
          <template #default="{ row }">
            <el-select v-model="row.difficulty">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="分值" width="120">
          <template #default="{ row }"><el-input-number v-model="row.score" :min="1" controls-position="right" /></template>
        </el-table-column>
        <el-table-column label="知识点" min-width="160">
          <template #default="{ row }"><el-input v-model="row.knowledgePoint" /></template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" :icon="Delete" @click="removeAiRow($index)" />
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="aiVisible = false">取消</el-button>
        <el-button type="primary" :loading="aiSaving" :disabled="!aiPreviewRows.length" @click="saveAiPreview">
          保存入题库
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parseVisible" title="解析试卷文档" width="95%" top="4vh">
      <el-form inline>
        <el-form-item label="课程">
          <el-select v-model="parseForm.courseId" style="width: 200px">
            <el-option v-for="course in courses" :key="course.id" :label="course.courseName" :value="course.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="parseForm.knowledgePoint" placeholder="可选" style="width: 200px" />
        </el-form-item>
        <el-form-item label="文档">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.docx,.txt,.md"
            :on-change="onParseFileChange"
          >
            <el-button type="primary">选择 PDF / Word</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="success" :loading="parseLoading" @click="parseDocument">开始解析</el-button>
        </el-form-item>
      </el-form>
      <el-table v-if="aiPreviewRows.length" :data="aiPreviewRows" max-height="420" style="margin-top: 12px">
        <el-table-column prop="content" label="题干" min-width="220" />
        <el-table-column label="题型" width="120">
          <template #default="{ row }">{{ formatQuestionType(row.questionType) }}</template>
        </el-table-column>
        <el-table-column prop="correctAnswer" label="答案" width="100" />
      </el-table>
      <template #footer>
        <el-button @click="parseVisible = false">取消</el-button>
        <el-button type="primary" :loading="aiSaving" :disabled="!aiPreviewRows.length" @click="saveAiPreview">
          保存入题库
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="visible" :title="editing ? '编辑题目' : '新增题目'" width="760">
      <el-form label-width="96">
        <el-form-item label="课程">
          <el-select v-model="form.courseId">
            <el-option v-for="course in courses" :key="course.id" :label="course.courseName" :value="course.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目分类">
          <el-radio-group v-model="form.sourceCategory" @change="resetSourceFields">
            <el-radio-button v-for="item in sourceCategoryOptions" :key="item[0]" :value="item[0]">
              {{ item[1] }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.sourceCategory !== 'PRACTICE'">
          <el-form-item v-if="isRealExam || isMockExam" label="年份">
            <el-select v-model="form.examYear" clearable placeholder="选择年份" style="width: 180px">
              <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
            </el-select>
            <span v-if="isRealExam" class="field-tip">真题必填</span>
          </el-form-item>
          <el-form-item v-if="isRealExam || isMockExam" label="考试类型">
            <el-select v-model="form.examScope" clearable placeholder="国考/省考" style="width: 180px">
              <el-option v-for="item in examScopeOptions" :key="item[0]" :label="item[1]" :value="item[0]" />
            </el-select>
            <span v-if="isRealExam" class="field-tip">真题必填</span>
          </el-form-item>
          <el-form-item v-if="isRealExam || (isMockExam && form.examScope === 'PROVINCIAL')" label="省份">
            <el-select v-model="form.province" clearable filterable allow-create placeholder="选择或输入省份" style="width: 220px">
              <el-option v-for="province in provinceOptions" :key="province" :label="province" :value="province" />
            </el-select>
            <span v-if="isRealExam && form.examScope === 'PROVINCIAL'" class="field-tip">省考必填</span>
          </el-form-item>
          <el-form-item v-if="showArchiveFields" label="卷别">
            <el-input v-model="form.paperType" placeholder="如 地市级、副省级、县级、通用" />
          </el-form-item>
          <el-form-item label="来源说明">
            <el-input
              v-model="form.sourceRef"
              :placeholder="isSelfAuthored ? '如 本校2024期末自编、教研室命题' : isMockExam ? '如 华图2024冲刺卷、粉笔模考' : '如 公开资料整理、机构回忆版'"
            />
          </el-form-item>
        </template>
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
.info-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
  padding: 20px 22px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: linear-gradient(135deg, #eff6ff, #ffffff);
}
.info-card h2 { margin: 0 0 8px; font-size: 18px; }
.info-card p { margin: 0; color: #64748b; line-height: 1.7; }
.toolbar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; padding: 14px; border-radius: 10px; background: #fff; border: 1px solid #e5e7eb; }
.option-list { display: grid; gap: 8px; width: 100%; }
.import-summary { margin: 16px 0; }
.import-tip { margin: 12px 0 0; color: #64748b; font-size: 13px; line-height: 1.6; }
.field-tip { margin-left: 10px; color: #94a3b8; font-size: 12px; }
.ai-form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  column-gap: 16px;
}
.ai-preview-table { margin-top: 12px; }
.ai-preview-table :deep(.el-input-number) { width: 100%; }
.ai-question-dialog :deep(.el-dialog__body) { padding-top: 10px; }
</style>
