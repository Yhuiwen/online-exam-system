<script setup>
import { computed, onMounted, ref } from 'vue'
import { getExams } from '../api/exam'
import { getPendingReviews, getReviewDetail, reviewAnswer } from '../api/review'
import { ElMessage } from 'element-plus'
import { formatQuestionType, formatReviewStatus } from '../utils/enumMap'

const exams = ref([])
const examId = ref(null)
const pendingRows = ref([])
const detail = ref(null)
const visible = ref(false)
const savingId = ref(null)

const shortAnswers = computed(() =>
  (detail.value?.questions || []).filter(item => item.questionType === 'SHORT_ANSWER')
)
const reviewComplete = computed(() =>
  detail.value?.studentExam?.status === 'SUBMITTED'
)

async function loadPending() {
  pendingRows.value = examId.value ? await getPendingReviews(examId.value) : []
}

async function openReview(row) {
  detail.value = await getReviewDetail(row.studentExamId)
  visible.value = true
}

async function save(item) {
  savingId.value = item.answerId
  try {
    const result = await reviewAnswer(item.answerId, {
      score: item.score,
      reviewComment: item.reviewComment
    })
    ElMessage.success(result.status === 'SUBMITTED' ? '批改完成，最终成绩已更新' : '本题批改已保存')
    detail.value = await getReviewDetail(result.studentExamId)
    await loadPending()
  } finally {
    savingId.value = null
  }
}

onMounted(async () => {
  exams.value = await getExams()
  examId.value = exams.value[0]?.id || null
  await loadPending()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">主观题批改</h1>
      <el-select v-model="examId" placeholder="选择考试" style="width: 260px" @change="loadPending">
        <el-option v-for="exam in exams" :key="exam.id" :label="exam.examName" :value="exam.id" />
      </el-select>
    </div>

    <div class="panel">
      <el-table :data="pendingRows" empty-text="当前考试没有待批改答卷">
        <el-table-column prop="studentName" label="学生" />
        <el-table-column prop="submitTime" label="提交时间" />
        <el-table-column prop="currentScore" label="当前客观题得分" width="150" />
        <el-table-column prop="pendingCount" label="待批改题数" width="120" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link @click="openReview(row)">批改</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="visible" title="答卷批改" width="880px">
      <template v-if="detail">
        <el-descriptions :column="3" border class="summary">
          <el-descriptions-item label="考试">{{ detail.exam?.examName }}</el-descriptions-item>
          <el-descriptions-item label="学生">{{ detail.student?.realName }}</el-descriptions-item>
          <el-descriptions-item label="当前得分">{{ detail.studentExam?.totalScore }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="reviewComplete"
          title="批改完成，最终成绩已更新"
          type="success"
          :closable="false"
          show-icon
        />

        <section v-for="(item, index) in detail.questions" :key="item.questionId" class="answer-block">
          <div class="question-title">
            <strong>{{ index + 1 }}. {{ item.content }}</strong>
            <el-tag>{{ formatQuestionType(item.questionType) }}</el-tag>
            <span>{{ item.score ?? 0 }} / {{ item.maxScore }} 分</span>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="学生答案">{{ item.studentAnswer || '未作答' }}</el-descriptions-item>
            <el-descriptions-item label="参考答案">{{ item.correctAnswer || '无固定答案' }}</el-descriptions-item>
            <el-descriptions-item label="标准解析">{{ item.analysis || '暂无解析' }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="item.questionType === 'SHORT_ANSWER'" class="review-form">
            <el-form inline>
              <el-form-item label="得分">
                <el-input-number v-model="item.score" :min="0" :max="Number(item.maxScore)" :precision="2" />
              </el-form-item>
              <el-form-item label="评语" class="comment-item">
                <el-input v-model="item.reviewComment" maxlength="500" show-word-limit />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  :loading="savingId === item.answerId"
                  @click="save(item)"
                >
                  保存批改
                </el-button>
              </el-form-item>
            </el-form>
            <el-tag :type="item.reviewStatus === 'REVIEWED' ? 'success' : 'warning'">
              {{ formatReviewStatus(item.reviewStatus) }}
            </el-tag>
          </div>
        </section>

        <el-empty v-if="shortAnswers.length === 0" description="该答卷没有简答题" />
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.summary { margin-bottom: 16px; }
.answer-block { margin-top: 18px; padding-top: 18px; border-top: 1px solid #e5e7eb; }
.question-title { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.question-title strong { flex: 1; line-height: 1.6; }
.question-title span { color: #64748b; }
.review-form { margin-top: 14px; padding: 14px; background: #f8fafc; border-left: 3px solid #2563eb; }
.comment-item { width: 390px; }
.comment-item :deep(.el-form-item__content) { width: 320px; }
</style>
