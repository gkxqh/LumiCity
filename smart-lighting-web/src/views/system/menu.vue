<!--
  菜单管理页
  - 树形表格：菜单名称、类型、路由、权限标识、图标、排序、操作
  - 新增/编辑弹窗：父级菜单（下拉）、名称、类型(M/C/F)、路由、组件、权限标识、图标、排序
  - 删除：存在子项时后端拒绝
  - 按钮受 v-hasPerm 控制
-->
<template>
  <div class="system-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="success" :icon="Plus" v-hasPerm="'system:menu:add'" @click="openAdd()">新增菜单</el-button>
      </div>

      <el-table
        :data="treeData"
        v-loading="loading"
        row-key="id"
        border
        stripe
        default-expand-all
        :tree-props="{ children: 'children' }"
        style="width: 100%"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column prop="menuType" label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.menuType)" effect="plain">{{ typeText(row.menuType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="160" show-overflow-tooltip />
        <el-table-column prop="component" label="组件路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="perms" label="权限标识" min-width="160" show-overflow-tooltip />
        <el-table-column prop="icon" label="图标" width="100" align="center" />
        <el-table-column prop="orderNum" label="排序" width="80" align="center" />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" v-hasPerm="'system:menu:edit'" @click="openAdd(row)">新增子项</el-button>
            <el-button type="primary" link :icon="Edit" v-hasPerm="'system:menu:edit'" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" v-hasPerm="'system:menu:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="父级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="treeData"
            :props="{ label: 'menuName', children: 'children', value: 'id' }"
            value-key="id"
            check-strictly
            :render-after-expand="false"
            clearable
            style="width: 100%"
            placeholder="顶级菜单（留空为一级）"
          />
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="DIRECTORY">目录</el-radio>
            <el-radio value="MENU">菜单</el-radio>
            <el-radio value="BUTTON">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="form.path" placeholder="如 /system/role" />
        </el-form-item>
        <el-form-item label="组件路径">
          <el-input v-model="form.component" placeholder="如 system/role/index" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="form.perms" placeholder="如 system:role:list" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="如 Setting" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.orderNum" :min="0" />
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
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { treeMenu, addMenu, updateMenu, deleteMenu } from '@/api/system'

const treeData = ref([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await treeMenu()
    treeData.value = res.data || []
  } finally { loading.value = false }
}

function typeText(t) { return { DIRECTORY: '目录', MENU: '菜单', BUTTON: '按钮' }[t] || t }
function typeTag(t) { return { DIRECTORY: 'warning', MENU: 'success', BUTTON: 'info' }[t] || 'info' }

/* ---------------- 新增/编辑 ---------------- */
const dialogVisible = ref(false)
const dialogTitle = ref('新增菜单')
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  id: null, parentId: null, menuName: '', menuType: 'MENU',
  path: '', component: '', perms: '', icon: '', orderNum: 1
})
const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}
function resetForm() {
  Object.assign(form, {
    id: null, parentId: null, menuName: '', menuType: 'MENU',
    path: '', component: '', perms: '', icon: '', orderNum: 1
  })
  formRef.value?.clearValidate()
}
function openAdd(parent) {
  dialogTitle.value = '新增菜单'
  resetForm()
  if (parent) form.parentId = parent.id  // 新增子项时默认父级
  dialogVisible.value = true
}
function openEdit(row) {
  dialogTitle.value = '编辑菜单'
  resetForm()
  Object.assign(form, {
    id: row.id, parentId: row.parentId, menuName: row.menuName, menuType: row.menuType,
    path: row.path || '', component: row.component || '', perms: row.perms || '',
    icon: row.icon || '', orderNum: row.orderNum ?? 1
  })
  dialogVisible.value = true
}
async function handleSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    const payload = { ...form }
    if (!payload.parentId) payload.parentId = 0
    if (!payload.id) delete payload.id
    if (payload.id) await updateMenu(payload)
    else await addMenu(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除菜单「${row.menuName}」吗？`, '提示', { type: 'warning' })
  try {
    await deleteMenu(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // 含子项时后端返回 PARAM_ERROR 提示，拦截器已弹错
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.system-page { padding: 0; }
.toolbar { margin-bottom: 16px; }
</style>
