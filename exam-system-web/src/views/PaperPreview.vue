<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPaperPreview } from '../api/paper'

const route = useRoute()
const router = useRouter()
const examId = computed(() => Number(route.params.examId))
const preview = ref(null)
const loading = ref(false)

const typeLabels = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题'
}

const difficultyLabels = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
}

function parseOptions(question) {
  const source = question.optionsJson ?? question.options_json
  if (!source) return []
  try {
    const parsed = typeof source === 'string' ? JSON.parse(source) : source
    if (Array.isArray(parsed)) return parsed
    return ['A', 'B', 'C', 'D']
      .filter(key => parsed?.[key])
      .map(key => parsed[key])
  } catch {
    return []
  }
}

async function loadPreview() {
  loading.value = true
  try {
    preview.value = await getPaperPreview(examId.value)
  } finally {
    loading.value = false
  }
}

onMounted(loadPreview)
</script>

<template>
  <div v-loading="loading" class="page preview-page">
    <div class="page-header">
      <h1 class="page-title">试卷预览</h1>
      <div class="header-actions">
        <el-button @click="router.push('/exams')">返回考试管理</el-button>
        <el-button type="primary" @click="router.push(`/teacher/exam/${examId}/manual-paper`)">
          返回手动组卷
        </el-button>
      </div>
    </div>

    <template v-if="preview">
      <section class="paper-heading">
        <h2>{{ preview.examName }}</h2>
        <div class="paper-meta">
          <span>课程：{{ preview.courseName || `课程 #${preview.courseId}` }}</span>
          <span>总分：{{ preview.totalScore }}</span>
          <span>题目数量：{{ preview.questionCount }}</span>
          <span>考试时长：{{ preview.durationMinutes }} 分钟</span>
        </div>
      </section>

      <el-empty
        v-if="preview.questions.length === 0"
        description="当前试卷暂无题目"
        class="panel"
      />

      <main v-else class="question-list">
        <article
          v-for="(question, index) in preview.questions"
          :key="question.questionId"
          class="question-block"
        >
          <header class="question-header">
            <strong>{{ index + 1 }}. {{ question.content }}</strong>
            <div class="question-tags">
              <el-tag>{{ typeLabels[question.questionType] || question.questionType }}</el-tag>
              <el-tag type="info">{{ difficultyLabels[question.difficulty] || question.difficulty }}</el-tag>
              <span>{{ question.score }} 分</span>
            </div>
          </header>

          <div v-if="parseOptions(question).length" class="option-list">
            <div v-for="(option, optionIndex) in parseOptions(question)" :key="optionIndex">
              <b>{{ String.fromCharCode(65 + optionIndex) }}.</b>
              <span>{{ option }}</span>
            </div>
          </div>

          <el-descriptions :column="1" border class="answer-section">
            <el-descriptions-item label="正确答案">
              {{ question.answer || '无固定答案' }}
            </el-descriptions-item>
            <el-descriptions-item label="题目解析">
              {{ question.analysis || '暂无解析' }}
            </el-descriptions-item>
          </el-descriptions>
        </article>
      </main>
    </template>
  </div>
</template>

<style scoped>
.preview-page {
  min-height: 100%;
}
.header-actions {
  display: flex;
  gap: 10px;
}
.paper-heading {
  padding: 22px;
  margin-bottom: 18px;
  background: #fff;
  border-bottom: 3px solid #2563eb;
}
.paper-heading h2 {
  margin: 0 0 14px;
  text-align: center;
  font-size: 24px;
}
.paper-meta {
  display: flex;
  justify-content: center;
  gap: 28px;
  flex-wrap: wrap;
  color: #475569;
}
.question-list {
  max-width: 960px;
  margin: 0 auto;
}
.question-block {
  padding: 22px;
  margin-bottom: 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
}
.question-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}
.question-header strong {
  line-height: 1.7;
}
.question-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.question-tags span {
  color: #b91c1c;
  font-weight: 700;
}
.option-list {
  display: grid;
  gap: 10px;
  padding: 16px 18px;
  margin: 16px 0;
  background: #f8fafc;
}
.option-list div {
  display: flex;
  gap: 10px;
}
.answer-section {
  margin-top: 16px;
}
@media (max-width: 760px) {
  .page-header,
  .question-header,
  .header-actions {
    flex-wrap: wrap;
  }
  .question-tags {
    width: 100%;
  }
}
</style>
