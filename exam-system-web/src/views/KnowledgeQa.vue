<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Delete, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCourses } from '../api/course'
import {
  askKnowledgeQuestion,
  deleteKnowledgeDocument,
  listKnowledgeDocuments,
  uploadKnowledgeDocument
} from '../api/aiKnowledge'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const courses = ref([])
const documents = ref([])
const uploadRef = ref(null)
const loadingDocs = ref(false)
const uploading = ref(false)
const asking = ref(false)
const deletingId = ref(null)
const selectedCourseId = ref(null)
const answer = ref('')
const references = ref([])
const uploadFile = ref(null)

const canManage = computed(() => ['ADMIN', 'TEACHER'].includes(auth.user?.role))
const uploadForm = reactive({ title: '' })
const askForm = reactive({
  question: '',
  topK: 5
})

async function loadDocuments() {
  if (!selectedCourseId.value) {
    documents.value = []
    return
  }
  loadingDocs.value = true
  try {
    documents.value = await listKnowledgeDocuments(selectedCourseId.value)
  } catch (error) {
    ElMessage.error(error?.message || '加载资料列表失败')
  } finally {
    loadingDocs.value = false
  }
}

function chooseFile(uploadFileItem) {
  uploadFile.value = uploadFileItem.raw
  if (!uploadForm.title && uploadFile.value?.name) {
    uploadForm.title = uploadFile.value.name.replace(/\.[^.]+$/, '')
  }
}

function removeChosenFile() {
  uploadFile.value = null
}

function validateUploadFile(file) {
  if (!file) return '请选择文件'
  const name = file.name.toLowerCase()
  if (!/\.(pdf|docx|txt|md)$/.test(name)) return '仅支持 PDF、DOCX、TXT、MD 文件'
  if (file.size > 10 * 1024 * 1024) return '文件大小不能超过 10MB'
  return ''
}

async function submitUpload() {
  if (!selectedCourseId.value) return ElMessage.warning('请先选择课程')
  if (!uploadForm.title.trim()) return ElMessage.warning('请输入资料标题')
  const message = validateUploadFile(uploadFile.value)
  if (message) return ElMessage.warning(message)
  const formData = new FormData()
  formData.append('courseId', selectedCourseId.value)
  formData.append('title', uploadForm.title.trim())
  formData.append('file', uploadFile.value)
  uploading.value = true
  try {
    await uploadKnowledgeDocument(formData)
    ElMessage.success('上传成功')
    uploadForm.title = ''
    uploadFile.value = null
    uploadRef.value?.clearFiles()
    await loadDocuments()
  } catch (error) {
    ElMessage.error(error?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function removeDocument(id) {
  try {
    await ElMessageBox.confirm('确认删除该课程资料？删除后对应片段也会同步删除。', '提示')
    deletingId.value = id
    await deleteKnowledgeDocument(id)
    ElMessage.success('删除成功')
    await loadDocuments()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '删除失败')
    }
  } finally {
    deletingId.value = null
  }
}

async function ask() {
  if (!selectedCourseId.value) return ElMessage.warning('请先选择课程')
  if (!askForm.question.trim()) return ElMessage.warning('请输入问题')
  asking.value = true
  answer.value = ''
  references.value = []
  try {
    const result = await askKnowledgeQuestion({
      courseId: selectedCourseId.value,
      question: askForm.question.trim(),
      topK: askForm.topK
    })
    answer.value = result.answer
    references.value = result.references || []
  } catch (error) {
    ElMessage.error(error?.message || '提问失败')
  } finally {
    asking.value = false
  }
}

function formatSize(size) {
  if (!size) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

watch(selectedCourseId, () => {
  answer.value = ''
  references.value = []
  loadDocuments()
})

onMounted(async () => {
  courses.value = await getCourses()
  selectedCourseId.value = courses.value[0]?.id || null
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">课程知识库答疑</h1>
    </div>

    <div class="toolbar">
      <el-select v-model="selectedCourseId" placeholder="选择课程" filterable style="width: 260px">
        <el-option v-for="course in courses" :key="course.id" :label="course.courseName" :value="course.id" />
      </el-select>
    </div>

    <div v-if="canManage" class="panel upload-panel">
      <div class="panel-title">课程资料管理</div>
      <div class="upload-row">
        <el-input v-model="uploadForm.title" placeholder="资料标题" />
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :limit="1"
          accept=".pdf,.docx,.txt,.md"
          :on-change="chooseFile"
          :on-remove="removeChosenFile"
        >
          <el-button :icon="UploadFilled">选择文件</el-button>
        </el-upload>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传并解析</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">已上传资料</div>
      <el-table v-loading="loadingDocs" :data="documents">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="originalFilename" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="fileType" label="类型" width="90" />
        <el-table-column label="大小" width="110">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="片段数" width="90" />
        <el-table-column prop="createTime" label="上传时间" width="180" />
        <el-table-column v-if="canManage" label="操作" width="90">
          <template #default="{ row }">
            <el-button
              link
              type="danger"
              :icon="Delete"
              :loading="deletingId === row.id"
              @click="removeDocument(row.id)"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel qa-panel">
      <div class="panel-title">课程答疑</div>
      <el-input
        v-model="askForm.question"
        type="textarea"
        :rows="4"
        placeholder="请输入你想基于课程资料提问的问题"
      />
      <div class="ask-actions">
        <el-input-number v-model="askForm.topK" :min="1" :max="8" />
        <el-button type="primary" :loading="asking" @click="ask">提问</el-button>
      </div>

      <div v-if="answer" class="answer-block">
        <h3>AI 回答</h3>
        <p>{{ answer }}</p>
      </div>

      <div v-if="references.length" class="references">
        <h3>引用来源</h3>
        <el-table :data="references" border>
          <el-table-column prop="documentTitle" label="文档" min-width="160" show-overflow-tooltip />
          <el-table-column prop="chunkIndex" label="片段" width="80" />
          <el-table-column prop="score" label="相关度" width="90" />
          <el-table-column prop="contentPreview" label="片段预览" min-width="300" show-overflow-tooltip />
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar { margin-bottom: 16px; }
.panel-title { font-weight: 700; margin-bottom: 12px; }
.upload-panel { margin-bottom: 16px; }
.upload-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto auto;
  gap: 10px;
  align-items: center;
}
.qa-panel { margin-top: 16px; }
.ask-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  align-items: center;
}
.answer-block {
  margin-top: 18px;
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  line-height: 1.7;
}
.answer-block h3,
.references h3 {
  margin: 0 0 10px;
  font-size: 16px;
}
.references { margin-top: 18px; }
@media (max-width: 760px) {
  .upload-row { grid-template-columns: 1fr; }
}
</style>
