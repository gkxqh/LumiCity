<!--
  系统管理页 - 用户管理
  - 搜索栏：用户名 / 状态 / 查询 / 重置 / 新增
  - 表格：用户名、昵称、手机号、邮箱、状态、操作
  - 新增/编辑弹窗：用户名、密码、昵称、手机号、邮箱、状态、角色(多选)
  - 调 pageUser / addUser / updateUser / deleteUser / listRole / getUserRoles / assignUserRoles
  - 分页
-->
<template>
  <div class="system-page">
    <!-- ============ 搜索栏 ============ -->
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="用户名">
          <el-input
            v-model="query.username"
            placeholder="请输入用户名"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="success" :icon="Plus" v-hasPerm="'system:user:add'" @click="openAdd">新增</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ============ 表格 ============ -->
    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" v-hasPerm="'system:user:edit'" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" v-hasPerm="'system:user:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- ============ 分页 ============ -->
      <el-pagination
        class="pagination"
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- ============ 新增/编辑弹窗 ============ -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :disabled="!!form.id"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="form.id ? '留空则不修改密码' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select
            v-model="form.roleIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="请为该用户分配角色"
            style="width: 100%"
          >
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="item.roleName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { pageUser, addUser, updateUser, deleteUser } from '@/api/other'
import { listRole, getUserRoles, assignUserRoles } from '@/api/system'

/* ---------------- 字典数据 ---------------- */

// 用户状态选项：value 对齐后端 Integer status（0禁用 1启用）
const statusOptions = [
  { label: '禁用', value: 0 },
  { label: '启用', value: 1 }
]
const statusMap = Object.fromEntries(statusOptions.map(i => [i.value, i.label]))

// 状态 Tag 颜色：启用-绿、禁用-红
function statusTagType(status) {
  return { 0: 'danger', 1: 'success' }[status] || 'info'
}

// 自定义校验：手机号格式
function validatePhone(rule, value, callback) {
  if (!value) return callback()
  if (!/^1[3-9]\d{9}$/.test(value)) {
    return callback(new Error('手机号格式不正确'))
  }
  callback()
}

// 自定义校验：邮箱格式
function validateEmail(rule, value, callback) {
  if (!value) return callback()
  if (!/^[\w.-]+@[\w.-]+\.[A-Za-z]{2,}$/.test(value)) {
    return callback(new Error('邮箱格式不正确'))
  }
  callback()
}

/* ---------------- 查询 & 表格 ---------------- */

// 分页参数用 current/size，对齐后端 PageQuery
const query = reactive({
  current: 1,
  size: 10,
  username: '',
  status: null
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)

// 角色下拉选项：供用户编辑弹窗分配角色（onMounted 时拉取启用角色）
const roleOptions = ref([])

async function loadData() {
  loading.value = true
  try {
    const res = await pageUser(query)
    const page = res.data || {}
    tableData.value = page.records || page.list || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.current = 1
  loadData()
}

function handleReset() {
  query.username = ''
  query.status = null
  query.current = 1
  loadData()
}

/* ---------------- 新增/编辑弹窗 ---------------- */

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const submitting = ref(false)
const formRef = ref()

// 表单数据：id 为空表示新增
const form = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  status: 1,  // 对齐后端 Integer status（0禁用 1启用）
  roleIds: []  // 用户绑定的角色 ID 列表（提交时单独走 assignUserRoles，不随 SysUser 入库）
})

// 表单校验规则（密码在编辑时可为空）
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    {
      // 编辑时密码可留空，新增时必填
      required: false,
      validator(rule, value, callback) {
        if (!form.id && !value) {
          return callback(new Error('请输入密码'))
        }
        if (value && value.length < 6) {
          return callback(new Error('密码至少 6 位'))
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  email: [{ validator: validateEmail, trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 重置表单（弹窗关闭时触发）
function resetForm() {
  form.id = null
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.phone = ''
  form.email = ''
  form.status = 1
  form.roleIds = []
  formRef.value?.clearValidate()
}

// 打开新增弹窗
function openAdd() {
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true
}

// 打开编辑弹窗：把当前行数据回填到表单
function openEdit(row) {
  dialogTitle.value = '编辑用户'
  resetForm()
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '', // 编辑时密码留空，表示不修改
    nickname: row.nickname,
    phone: row.phone || '',
    email: row.email || '',
    status: row.status
  })
  // 回填该用户已绑定的角色 ID 列表
  getUserRoles(row.id)
    .then(res => { form.roleIds = res.data || [] })
    .catch(() => { form.roleIds = [] })
  dialogVisible.value = true
}

// 提交新增/编辑
async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    // 提交给后端的用户对象不带 roleIds（SysUser 无此字段，角色单独走绑定接口）
    const { roleIds, ...userPayload } = form
    if (form.id) {
      // 编辑：密码留空则不传 password
      if (!userPayload.password) delete userPayload.password
      await updateUser(userPayload)
      // 重新分配角色（先删后插，传空数组即清空）
      await assignUserRoles(form.id, roleIds || [])
      ElMessage.success('修改成功')
    } else {
      await addUser(userPayload)
      // 新增后需拿到新用户 id 才能分配角色：按用户名精确查一条
      const res = await pageUser({ current: 1, size: 1, username: form.username })
      const newId = res.data?.records?.[0]?.id
      if (newId) {
        await assignUserRoles(newId, roleIds || [])
      } else {
        ElMessage.warning('用户已创建，可在编辑中补充角色')
      }
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

/* ---------------- 删除 ---------------- */

async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除用户「${row.username}」吗？`,
    '提示',
    { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
  )
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  loadData()
}

/* ---------------- 初始化 ---------------- */

onMounted(() => {
  loadData()
  // 加载启用状态的角色，供用户编辑弹窗下拉选择
  listRole({ status: 1 })
    .then(res => { roleOptions.value = res.data || [] })
    .catch(() => {})
})
</script>

<style scoped>
.system-page {
  padding: 0;
}
.search-card {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
