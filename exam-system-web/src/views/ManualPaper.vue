<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getExam } from '../api/exam'
import { getManualQuestions, getPaperPreview, saveManualPaper } from '../api/paper'

const route = useRoute()
const router = useRouter()
const examId = computed(() => Number(route.params.examId))
const preview = ref(null)
const questionRows = ref([])
const selectedQuestions = ref([])
const loading = ref(false)
const saving = ref(false)
const removingId = ref(null)

const filters = reactive({
  questionType: '',
  difficulty: '',
  knowledgeTag: '',
  keyword: ''
})

const typeOptions = [
  ['SINGLE_CHOICE', '单选题'],
  ['MULTIPLE_CHOICE', '多选题'],
  ['TRUE_FALSE', '判断题'],
  ['FILL_BLANK', '填空题'],
  ['SHORT_ANSWER', '简答题']
]

const typeLabels = Object.fromEntries(typeOptions)
const difficultyLabels = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
}

const selectedIds = computed(() => new Set(selectedQuestions.value.map(item => item.questionId)))
const localTotalScore = computed(() =>
  selectedQuestions.value.reduce((sum, item) => sum + Number(item.score || 0), 0)
)

function formatTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}

function normalizeSelected(question, index) {
  return {
    questionId: question.questionId,
    content: question.content,
    questionType: question.questionType,
    score: Number(question.score ?? question.selectedScore ?? question.defaultScore ?? 0),
    sortNo: index + 1
  }
}

function syncSelectedFromPreview(data) {
  selectedQuestions.value = (data?.questions || []).map(normalizeSelected)
}

async function loadPreview() {
  preview.value = await getPaperPreview(examId.value)
  syncSelectedFromPreview(preview.value)
}

async function loadQuestions() {
  loading.value = true
  try {
    questionRows.value = await getManualQuestions(examId.value, {
      questionType: filters.questionType || undefined,
      difficulty: filters.difficulty || undefined,
      knowledgeTag: filters.knowledgeTag || undefined,
      keyword: filters.keyword || undefined
    })
  } finally {
    loading.value = false
  }
}

async function loadPage() {
  const exam = await getExam(examId.value)
  if (exam.status !== 'DRAFT') {
    ElMessage.warning('考试已发布，试卷只能预览，不能继续修改')
    return router.replace(`/teacher/exam/${examId.value}/preview`)
  }
  await loadPreview()
  await loadQuestions()
}

function resetFilters() {
  Object.assign(filters, {
    questionType: '',
    difficulty: '',
    knowledgeTag: '',
    keyword: ''
  })
  loadQuestions()
}

function addQuestion(question) {
  if (selectedIds.value.has(question.questionId)) {
    return ElMessage.warning('该题已加入试卷')
  }
  selectedQuestions.value.push(normalizeSelected(question, selectedQuestions.value.length))
}

function renumber() {
  selectedQuestions.value.forEach((item, index) => {
    item.sortNo = index + 1
  })
}

function move(index, offset) {
  const target = index + offset
  if (target < 0 || target >= selectedQuestions.value.length) return
  const list = selectedQuestions.value
  ;[list[index], list[target]] = [list[target], list[index]]
  renumber()
}

async function removeQuestion(item) {
  await ElMessageBox.confirm(
    `确认从试卷中移除“${item.content}”吗？保存试卷后生效。`,
    '移除题目',
    { type: 'warning', confirmButtonText: '确认移除', cancelButtonText: '取消' }
  )
  removingId.value = item.questionId
  try {
    selectedQuestions.value = selectedQuestions.value.filter(
      question => question.questionId !== item.questionId
    )
    renumber()
  } finally {
    removingId.value = null
  }
}

function validatePaper() {
  if (!selectedQuestions.value.length) {
    ElMessage.warning('请至少选择一道题')
    return false
  }
  if (selectedQuestions.value.some(item => !Number.isFinite(Number(item.score)) || Number(item.score) <= 0)) {
    ElMessage.warning('所有题目分值必须大于 0')
    return false
  }
  const sortNumbers = selectedQuestions.value.map(item => Number(item.sortNo))
  if (new Set(sortNumbers).size !== sortNumbers.length) {
    ElMessage.warning('题目排序不能重复')
    return false
  }
  return true
}

async function savePaper() {
  if (!validatePaper()) return
  saving.value = true
  try {
    preview.value = await saveManualPaper(
      examId.value,
      selectedQuestions.value.map(item => ({
        questionId: item.questionId,
        score: Number(item.score),
        sortNo: Number(item.sortNo)
      }))
    )
    syncSelectedFromPreview(preview.value)
    await loadQuestions()
    ElMessage.success('试卷保存成功')
  } finally {
    saving.value = false
  }
}

onMounted(loadPage)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">手动组卷</h1>
      <div class="header-actions">
        <el-button @click="router.push('/exams')">返回考试管理</el-button>
        <el-button type="primary" plain @click="router.push(`/teacher/exam/${examId}/preview`)">
          试卷预览
        </el-button>
        <el-button type="primary" :loading="saving" @click="savePaper">保存试卷</el-button>
      </div>
    </div>

    <el-descriptions v-if="preview" :column="3" border class="exam-summary">
      <el-descriptions-item label="考试名称">{{ preview.examName }}</el-descriptions-item>
      <el-descriptions-item label="课程">{{ preview.courseName || `课程 #${preview.courseId}` }}</el-descriptions-item>
      <el-descriptions-item label="考试时长">{{ preview.durationMinutes }} 分钟</el-descriptions-item>
      <el-descriptions-item label="开始时间">{{ formatTime(preview.startTime) }}</el-descriptions-item>
      <el-descriptions-item label="结束时间">{{ formatTime(preview.endTime) }}</el-descriptions-item>
      <el-descriptions-item label="当前试卷">
        {{ selectedQuestions.length }} 题 / {{ localTotalScore }} 分
      </el-descriptions-item>
    </el-descriptions>

    <section class="panel question-bank">
      <div class="section-header">
        <h2>课程题库</h2>
        <span>筛选结果 {{ questionRows.length }} 道</span>
      </div>
      <div class="toolbar">
        <el-select v-model="filters.questionType" clearable placeholder="题型" style="width: 150px">
          <el-option
            v-for="item in typeOptions"
            :key="item[0]"
            :label="item[1]"
            :value="item[0]"
          />
        </el-select>
        <el-select v-model="filters.difficulty" clearable placeholder="难度" style="width: 130px">
          <el-option label="简单" value="EASY" />
          <el-option label="中等" value="MEDIUM" />
          <el-option label="困难" value="HARD" />
        </el-select>
        <el-input
          v-model="filters.knowledgeTag"
          clearable
          placeholder="知识点标签"
          style="width: 170px"
        />
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="题目关键词"
          style="width: 210px"
          @keyup.enter="loadQuestions"
        />
        <el-button type="primary" @click="loadQuestions">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="questionRows" empty-text="没有符合条件的题目">
        <el-table-column prop="content" label="题目内容" min-width="300" show-overflow-tooltip />
        <el-table-column label="题型" width="105">
          <template #default="{ row }">{{ typeLabels[row.questionType] || row.questionType }}</template>
        </el-table-column>
        <el-table-column label="难度" width="80">
          <template #default="{ row }">{{ difficultyLabels[row.difficulty] || row.difficulty }}</template>
        </el-table-column>
        <el-table-column prop="defaultScore" label="默认分值" width="90" />
        <el-table-column prop="knowledgeTag" label="知识点标签" width="130">
          <template #default="{ row }">{{ row.knowledgeTag || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="selectedIds.has(row.questionId)" type="success">已选</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              :disabled="saving || selectedIds.has(row.questionId)"
              @click="addQuestion(row)"
            >
              加入试卷
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="panel selected-paper">
      <div class="section-header">
        <h2>已选题目</h2>
        <strong>{{ selectedQuestions.length }} 题 / {{ localTotalScore }} 分</strong>
      </div>
      <el-table :data="selectedQuestions" row-key="questionId" empty-text="请从题库加入题目">
        <el-table-column label="顺序" width="100">
          <template #default="{ row }">
            <el-input-number
              v-model="row.sortNo"
              :min="1"
              :max="selectedQuestions.length"
              controls-position="right"
              :disabled="saving"
              class="sort-input"
            />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题目内容" min-width="300" show-overflow-tooltip />
        <el-table-column label="题型" width="110">
          <template #default="{ row }">{{ typeLabels[row.questionType] || row.questionType }}</template>
        </el-table-column>
        <el-table-column label="分值" width="130">
          <template #default="{ row }">
            <el-input-number
              v-model="row.score"
              :min="0.01"
              :precision="2"
              :step="1"
              controls-position="right"
              :disabled="saving"
              class="score-input"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row, $index }">
            <el-button
              link
              :disabled="saving || $index === 0"
              @click="move($index, -1)"
            >
              上移
            </el-button>
            <el-button
              link
              :disabled="saving || $index === selectedQuestions.length - 1"
              @click="move($index, 1)"
            >
              下移
            </el-button>
            <el-button
              link
              type="danger"
              :loading="removingId === row.questionId"
              :disabled="saving"
              @click="removeQuestion(row)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.header-actions,
.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.exam-summary {
  margin-bottom: 18px;
  background: #fff;
}
.question-bank,
.selected-paper {
  margin-bottom: 18px;
}
.section-header {
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-header h2 {
  margin: 0;
  font-size: 17px;
}
.section-header span {
  color: #64748b;
}
.section-header strong {
  color: #1d4ed8;
}
.sort-input {
  width: 76px;
}
.score-input {
  width: 105px;
}
@media (max-width: 760px) {
  .page-header,
  .header-actions {
    align-items: flex-start;
    flex-wrap: wrap;
  }
}
</style>
