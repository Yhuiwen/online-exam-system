<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createCourse, deleteCourse, getCourses, updateCourse } from '../api/course'
import { ElMessage, ElMessageBox } from 'element-plus'

const rows = ref([])
const visible = ref(false)
const editing = ref(null)
const form = reactive({ courseName: '', description: '' })

async function load() {
  rows.value = await getCourses()
}

function open(row) {
  editing.value = row?.id || null
  Object.assign(form, row || { courseName: '', description: '' })
  visible.value = true
}

async function save() {
  editing.value ? await updateCourse(editing.value, form) : await createCourse(form)
  ElMessage.success('保存成功')
  visible.value = false
  load()
}

async function remove(id) {
  await ElMessageBox.confirm('确认删除该课程？', '提示')
  await deleteCourse(id)
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">课程管理</h1>
      <el-button type="primary" @click="open()">新增课程</el-button>
    </div>
    <div class="panel">
      <el-table :data="rows">
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="teacherName" label="授课教师" width="140" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link @click="open(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="visible" :title="editing ? '编辑课程' : '新增课程'" width="520">
      <el-form label-width="90">
        <el-form-item label="课程名称"><el-input v-model="form.courseName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
