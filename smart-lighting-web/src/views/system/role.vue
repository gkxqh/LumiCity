<!--
  角色管理页
  - 搜索栏：角色名称 / 状态
  - 表格：角色名称、角色编码、状态、创建时间、操作（分配菜单 / 编辑 / 删除）
  - 新增/编辑弹窗：角色名称、角色编码、状态
  - 分配菜单弹窗：菜单树（el-tree 多选），保存调用 assignRoleMenus
  - 按钮受 v-hasPerm 控制（与后端 @RequiresPerms 对齐）
-->
<template>
  <div class="system-page">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" placeholder="请输入角色名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="success" :icon="Plus" v-hasPerm="'system:role:add'" @click="openAdd">新增角色</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleCode" label="角色编码" min-width="140" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="warning" link :icon="Menu" v-hasPerm="'system:role:edit'" @click="openAssign(row)">分配菜单</el-button>
            <el-button type="primary" link :icon="Edit" v-hasPerm="'system:role:edit'" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" v-hasPerm="'system:role:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="如 admin / operator" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单弹窗 -->
    <el-dialog v-model="assignVisible" title="分配菜单权限" width="520px" @closed="menuTree = []">
      <el-tree
        ref="treeRef"
        :data="menuTree"
        :props="{ label: 'menuName', children: 'children' }"
        node-key="id"
        show-checkbox
        default-expand-all
        :default-checked-keys="checkedMenuIds"
        style="max-height: 50vh; overflow: auto"
      />
      <template #footer>
        <el-button @click="assignVisible = false">取 消</el-button>
        <el-button type="primary" :loading="assigning" @click="handleAssign">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Menu } from '@element-plus/icons-vue'
import { pageRole, addRole, updateRole, deleteRole, getRoleMenus, assignRoleMenus, treeMenu } from '@/api/system'

const statusOptions = [
  { label: '禁用', value: 0 },
  { label: '启用', value: 1 }
]

const query = reactive({ current: 1, size: 10, roleName: '', status: null })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await pageRole(query)
    const page = res.data || {}
    tableData.value = page.records || page.list || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}
function handleSearch() { query.current = 1; loadData() }
function handleReset() { query.roleName = ''; query.status = null; query.current = 1; loadData() }

/* ---------------- 新增/编辑 ---------------- */
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const submitting = ref(false)
const formRef = ref()
const form = reactive({ id: null, roleName: '', roleCode: '', status: 1 })
const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}
function resetForm() {
  form.id = null; form.roleName = ''; form.roleCode = ''; form.status = 1
  formRef.value?.clearValidate()
}
function openAdd() { dialogTitle.value = '新增角色'; resetForm(); dialogVisible.value = true }
function openEdit(row) {
  dialogTitle.value = '编辑角色'; resetForm()
  Object.assign(form, { id: row.id, roleName: row.roleName, roleCode: row.roleCode, status: row.status })
  dialogVisible.value = true
}
async function handleSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    if (form.id) await updateRole({ ...form })
    else await addRole({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  loadData()
}

/* ---------------- 分配菜单 ---------------- */
const assignVisible = ref(false)
const assigning = ref(false)
const menuTree = ref([])
const checkedMenuIds = ref([])
const currentRoleId = ref(null)
const treeRef = ref()

async function openAssign(row) {
  currentRoleId.value = row.id
  // 并行加载菜单树与该角色已绑定的菜单 ID
  const [treeRes, menuRes] = await Promise.all([treeMenu(), getRoleMenus(row.id)])
  menuTree.value = treeRes.data || []
  checkedMenuIds.value = menuRes.data || []
  assignVisible.value = true
}
async function handleAssign() {
  const keys = treeRef.value?.getCheckedKeys() || []
  assigning.value = true
  try {
    await assignRoleMenus(currentRoleId.value, keys)
    ElMessage.success('分配成功')
    assignVisible.value = false
  } finally { assigning.value = false }
}

onMounted(() => loadData())
</script>

<style scoped>
.system-page { padding: 0; }
.search-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
