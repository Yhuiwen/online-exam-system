<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../store/auth'
import {
  createUser,
  getUsers,
  resetUserPassword,
  updateUser,
  updateUserStatus
} from '../api/user'
import { formatUserRole, formatUserStatus } from '../utils/enumMap'

const auth = useAuthStore()
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const operatingId = ref(null)
const userVisible = ref(false)
const passwordVisible = ref(false)
const editingId = ref(null)
const passwordUser = ref(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  role: '',
  status: null
})

const userForm = reactive({
  username: '',
  password: '',
  realName: '',
  role: 'STUDENT',
  email: '',
  phone: '',
  status: 1
})

const passwordForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const roles = [
  ['ADMIN', '管理员'],
  ['TEACHER', '教师'],
  ['STUDENT', '学生']
]

async function load() {
  loading.value = true
  try {
    const data = await getUsers({
      ...query,
      role: query.role || undefined,
      status: query.status ?? undefined,
      keyword: query.keyword || undefined
    })
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function resetQuery() {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    role: '',
    status: null
  })
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(userForm, {
    username: '',
    password: '',
    realName: '',
    role: 'STUDENT',
    email: '',
    phone: '',
    status: 1
  })
  userVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(userForm, {
    username: row.username,
    password: '',
    realName: row.realName,
    role: row.role,
    email: row.email || '',
    phone: row.phone || '',
    status: row.status
  })
  userVisible.value = true
}

function validateUser() {
  if (!editingId.value && !userForm.username.trim()) {
    ElMessage.warning('请输入用户名')
    return false
  }
  if (!editingId.value && userForm.password.length < 6) {
    ElMessage.warning('初始密码长度至少为 6 位')
    return false
  }
  if (!userForm.realName.trim()) {
    ElMessage.warning('请输入姓名')
    return false
  }
  return true
}

async function saveUser() {
  if (!validateUser()) return
  if (editingId.value === auth.user?.id && userForm.status === 0) {
    return ElMessage.warning('不能禁用当前登录管理员')
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateUser(editingId.value, {
        realName: userForm.realName,
        role: userForm.role,
        email: userForm.email,
        phone: userForm.phone,
        status: userForm.status
      })
      if (editingId.value === auth.user?.id) await auth.refreshUser()
      ElMessage.success('用户信息已更新')
    } else {
      await createUser({
        username: userForm.username,
        password: userForm.password,
        realName: userForm.realName,
        role: userForm.role,
        email: userForm.email,
        phone: userForm.phone
      })
      ElMessage.success('用户创建成功')
    }
    userVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function openPassword(row) {
  passwordUser.value = row
  Object.assign(passwordForm, { newPassword: '', confirmPassword: '' })
  passwordVisible.value = true
}

async function submitPassword() {
  if (passwordForm.newPassword.length < 6) {
    return ElMessage.warning('新密码长度至少为 6 位')
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    return ElMessage.warning('两次输入的密码不一致')
  }
  await ElMessageBox.confirm(
    `确认重置用户“${passwordUser.value.username}”的密码吗？`,
    '重置密码',
    { type: 'warning', confirmButtonText: '确认重置', cancelButtonText: '取消' }
  )
  saving.value = true
  try {
    await resetUserPassword(passwordUser.value.id, {
      newPassword: passwordForm.newPassword
    })
    passwordVisible.value = false
    ElMessage.success('密码已重置')
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  if (row.id === auth.user?.id && row.status === 1) {
    return ElMessage.warning('不能禁用当前登录管理员')
  }
  const nextStatus = row.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(
    `确认${action}用户“${row.username}”吗？`,
    `${action}用户`,
    { type: nextStatus === 1 ? 'info' : 'warning' }
  )
  operatingId.value = row.id
  try {
    await updateUserStatus(row.id, nextStatus)
    ElMessage.success(`用户已${action}`)
    await load()
  } finally {
    operatingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="用户名 / 姓名"
        clearable
        style="width: 220px"
        @keyup.enter="search"
      />
      <el-select v-model="query.role" clearable placeholder="全部角色" style="width: 140px">
        <el-option v-for="role in roles" :key="role[0]" :label="role[1]" :value="role[0]" />
      </el-select>
      <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 130px">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <div class="panel">
      <el-table v-loading="loading" :data="rows">
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="110" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">{{ formatUserRole(row.role) }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ formatUserStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="175" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openPassword(row)">重置密码</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              :loading="operatingId === row.id"
              :disabled="row.id === auth.user?.id && row.status === 1"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="query.pageNum = 1; load()"
      />
    </div>

    <el-dialog
      v-model="userVisible"
      :title="editingId ? '编辑用户' : '新增用户'"
      width="560px"
    >
      <el-form label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="userForm.username" :disabled="Boolean(editingId)" maxlength="50" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="初始密码">
          <el-input v-model="userForm.password" type="password" show-password maxlength="100" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="userForm.realName" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.role">
            <el-option v-for="role in roles" :key="role[0]" :label="role[1]" :value="role[0]" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" maxlength="100" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" maxlength="20" />
        </el-form-item>
        <el-form-item v-if="editingId" label="状态">
          <el-radio-group v-model="userForm.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0" :disabled="editingId === auth.user?.id">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="重置密码" width="460px">
      <el-alert
        :title="`正在重置用户：${passwordUser?.username || ''}`"
        type="warning"
        :closable="false"
        class="password-alert"
      />
      <el-form label-width="90px">
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.el-pagination {
  margin-top: 18px;
  justify-content: flex-end;
}
.password-alert {
  margin-bottom: 18px;
}
</style>
