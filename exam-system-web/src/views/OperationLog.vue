<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getOperationLogs } from '../api/operationLog'
import { ElMessage } from 'element-plus'

const rows = ref([])
const loading = ref(false)
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  module: ''
})

const moduleOptions = [
  { label: '全部模块', value: '' },
  { label: '认证', value: '认证' },
  { label: '用户管理', value: '用户管理' },
  { label: '课程管理', value: '课程管理' },
  { label: '题库管理', value: '题库管理' },
  { label: '考试管理', value: '考试管理' },
  { label: '学生考试', value: '学生考试' },
  { label: '主观题批改', value: '主观题批改' },
  { label: '考试监控', value: '考试监控' }
]

async function load() {
  loading.value = true
  try {
    const page = await getOperationLogs(query)
    rows.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">操作日志</h1>
    </div>

    <div class="panel toolbar">
      <el-input v-model="query.keyword" placeholder="搜索用户/动作/详情" clearable style="width: 260px" @keyup.enter="search" />
      <el-select v-model="query.module" style="width: 160px" @change="search">
        <el-option v-for="item in moduleOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>

    <div class="panel">
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="110" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column prop="module" label="模块" width="110" />
        <el-table-column prop="action" label="动作" width="110" />
        <el-table-column prop="method" label="方法" width="70" />
        <el-table-column prop="path" label="接口路径" min-width="180" />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="load"
          @size-change="load"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 14px; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
