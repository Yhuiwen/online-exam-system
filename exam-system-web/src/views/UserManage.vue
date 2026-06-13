<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getUsers, updateUserStatus } from '../api/user'
import { ElMessage } from 'element-plus'
const rows=ref([]), total=ref(0)
const query=reactive({page:1,size:10,keyword:''})
async function load(){ const d=await getUsers(query); rows.value=d.records; total.value=d.total }
async function toggle(row){ await updateUserStatus(row.id,row.status===1?0:1); ElMessage.success('状态已更新'); load() }
onMounted(load)
</script>
<template><div class="page"><div class="page-header"><h1 class="page-title">用户管理</h1></div>
  <div class="toolbar"><el-input v-model="query.keyword" placeholder="用户名或姓名" clearable style="width:240px" @keyup.enter="load"/><el-button type="primary" @click="load">查询</el-button></div>
  <div class="panel"><el-table :data="rows"><el-table-column prop="username" label="用户名"/><el-table-column prop="realName" label="姓名"/><el-table-column prop="role" label="角色"/><el-table-column prop="email" label="邮箱"/><el-table-column label="状态"><template #default="{row}"><el-tag :type="row.status===1?'success':'danger'">{{row.status===1?'启用':'禁用'}}</el-tag></template></el-table-column><el-table-column label="操作"><template #default="{row}"><el-button link :type="row.status===1?'danger':'success'" @click="toggle(row)">{{row.status===1?'禁用':'启用'}}</el-button></template></el-table-column></el-table>
  <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load"/></div>
</div></template>
