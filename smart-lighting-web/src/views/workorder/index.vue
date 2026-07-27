<!--
  工单运维页
  - 搜索栏：工单类型 / 状态 / 设备编号 / 查询
  - 表格：工单编号、类型、标题、设备、优先级(Tag)、状态(步骤标签)、创建时间、操作
  - 操作按钮（仅处理中显示"处理"，其余无操作）：
      待处理(手动) → 派单
      处理中       → 填写处理备注 → 直接到已完成
      已完成       → 终点，不显示按钮
  - 新增工单弹窗、派单弹窗（选择处理人）、处理弹窗（填写处理备注）
  - 分页
-->
<template>
  <div class="workorder-page">
    <!-- ============ 搜索栏 ============ -->
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="工单类型">
          <el-select v-model="query.orderType" placeholder="全部" clearable style="width: 150px">
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
        <el-form-item label="设备编号">
          <el-input v-model="query.deviceId" placeholder="输入设备编号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button v-if="pageType === 'manual'" type="success" :icon="Plus" @click="openAdd">新增工单</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ============ 表格 ============ -->
    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="orderNo" label="工单编号" width="150" />
        <el-table-column prop="orderType" label="类型" width="120" align="center">
          <template #default="{ row }">
            {{ typeMap[row.orderType] || row.orderType }}
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deviceId" label="关联设备" width="140" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" effect="light">
              {{ priorityMap[row.priority] || row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <!-- 已完成：终点，无操作 -->
            <span v-if="row.status === 2">—</span>
            <!-- 处理中：显示"处理"按钮（告警工单/手动工单都走这个分支） -->
            <el-button
              v-else-if="row.status === 1"
              type="warning"
              link
              :icon="Edit"
              @click="openHandle(row)"
            >处理</el-button>
            <!-- 待处理：仅手动工单有（status=0 且无 alarmId），告警工单初始就是处理中 -->
            <el-button
              v-else-if="row.status === 0"
              type="primary"
              link
              :icon="User"
              @click="openAssign(row)"
            >派单</el-button>
            <!-- 其他情况（理论上不会出现） -->
            <span v-else>—</span>
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

    <!-- ============ 新增工单弹窗 ============ -->
    <el-dialog
      v-model="addDialogVisible"
      title="新增工单"
      width="560px"
      @closed="resetAddForm"
    >
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <el-form-item label="工单标题" prop="title">
          <el-input v-model="addForm.title" placeholder="请输入工单标题" />
        </el-form-item>
        <el-form-item label="工单类型" prop="orderType">
          <el-select v-model="addForm.orderType" placeholder="请选择类型" style="width: 100%">
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="addForm.priority" placeholder="请选择优先级" style="width: 100%">
            <el-option
              v-for="item in priorityOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关联设备" prop="deviceId">
          <el-input v-model="addForm.deviceId" placeholder="请输入设备编号，如 L-2024-0001" />
        </el-form-item>
        <el-form-item label="问题描述" prop="description">
          <el-input
            v-model="addForm.description"
            type="textarea"
            :rows="4"
            placeholder="请描述工单问题"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="addSubmitting" @click="handleAddSubmit">确 定</el-button>
      </template>
    </el-dialog>

    <!-- ============ 派单弹窗 ============ -->
    <el-dialog
      v-model="assignDialogVisible"
      title="派单"
      width="480px"
      @closed="resetAssignForm"
    >
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="90px">
        <el-form-item label="工单标题">
          <el-input :model-value="assignForm.title" disabled />
        </el-form-item>
        <el-form-item label="处理人" prop="assigneeId">
          <el-select
            v-model="assignForm.assigneeId"
            placeholder="请选择处理人"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="item in assigneeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="assignForm.remark"
            type="textarea"
            :rows="3"
            placeholder="派单备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="assignSubmitting" @click="handleAssignSubmit">派 单</el-button>
      </template>
    </el-dialog>

    <!-- ============ 处理弹窗 ============ -->
    <el-dialog
      v-model="handleDialogVisible"
      title="处理工单"
      width="480px"
      @closed="resetHandleForm"
    >
      <el-form ref="handleFormRef" :model="handleForm" :rules="handleRules" label-width="90px">
        <el-form-item label="工单标题">
          <el-input :model-value="handleForm.title" disabled />
        </el-form-item>
        <el-form-item label="处理备注" prop="handleRemark">
          <el-input
            v-model="handleForm.handleRemark"
            type="textarea"
            :rows="4"
            placeholder="请填写处理过程 / 结果"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="handleSubmitting" @click="handleWorkOrderSubmit">
          提交处理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus, Edit, User } from '@element-plus/icons-vue'
import {
  pageWorkOrder,
  addWorkOrder,
  assignWorkOrder,
  handleWorkOrder,
  listUsersByRole
} from '@/api/other'

const route = useRoute()

// 当前页面类型：alarm=告警工单 / manual=运维创建工单
const pageType = computed(() => route.meta?.type || 'alarm')
const pageTitle = computed(() => pageType.value === 'alarm' ? '告警工单' : '运维创建工单')

/* ---------------- 字典数据 ---------------- */

// 工单类型选项：value 对齐后端 String orderType（INSPECT/REPAIR）
const typeOptions = [
  { label: '巡检工单', value: 'INSPECT' },
  { label: '维修工单', value: 'REPAIR' }
]
const typeMap = Object.fromEntries(typeOptions.map(i => [i.value, i.label]))

// 工单状态选项：value 对齐后端 Integer status（0待处理 1处理中 2已完成）
const statusOptions = [
  { label: '待处理', value: 0 },
  { label: '处理中', value: 1 },
  { label: '已完成', value: 2 }
]
const statusMap = { 0: '待处理', 1: '处理中', 2: '已完成' }

// 状态 Tag 颜色
function statusTagType(status) {
  return { 0: 'danger', 1: 'warning', 2: 'success' }[status] || 'info'
}

// 优先级选项：value 对齐后端 Integer priority（1高 2中 3低）
const priorityOptions = [
  { label: '高', value: 1 },
  { label: '中', value: 2 },
  { label: '低', value: 3 }
]
const priorityMap = Object.fromEntries(priorityOptions.map(i => [i.value, i.label]))

// 优先级 Tag 颜色：高-红、中-橙、低-灰
function priorityTagType(p) {
  return { 1: 'danger', 2: 'warning', 3: 'info' }[p] || 'info'
}

// 处理人下拉选项（从后端加载巡检人员）
const assigneeOptions = ref([])

async function loadAssignees() {
  try {
    const res = await listUsersByRole('INSPECTOR')
    // 后端返回 [{ id, username, nickname }]，前端显示 nickname，value 用 id
    assigneeOptions.value = (res.data || []).map(user => ({
      label: user.nickname || user.username,
      value: user.id
    }))
  } catch {
    // 加载失败不影响主流程
  }
}

/* ---------------- 查询 & 表格 ---------------- */

// 分页参数：对齐后端 WorkOrderQueryDTO（orderType / status / deviceId / alarmId）
const query = reactive({
  current: 1,
  size: 10,
  orderType: '',
  status: null,
  deviceId: '',
  alarmId: null
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)

async function loadData() {
  // 根据页面类型自动设置 alarmId 过滤条件
  query.alarmId = pageType.value === 'alarm' ? 1 : 0
  loading.value = true
  try {
    const res = await pageWorkOrder(query)
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
  query.orderType = ''
  query.status = null
  query.deviceId = ''
  query.current = 1
  loadData()
}

/* ---------------- 新增工单弹窗 ---------------- */

const addDialogVisible = ref(false)
const addSubmitting = ref(false)
const addFormRef = ref()

const addForm = reactive({
  title: '',
  orderType: '',
  priority: null,  // 对齐后端 Integer priority（1高/2中/3低）
  deviceId: '',
  description: ''
})

const addRules = {
  title: [{ required: true, message: '请输入工单标题', trigger: 'blur' }],
  orderType: [{ required: true, message: '请选择工单类型', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  description: [{ required: true, message: '请描述工单问题', trigger: 'blur' }]
}

function resetAddForm() {
  addForm.title = ''
  addForm.orderType = ''
  addForm.priority = null
  addForm.deviceId = ''
  addForm.description = ''
  addFormRef.value?.clearValidate()
}

function openAdd() {
  resetAddForm()
  addDialogVisible.value = true
}

async function handleAddSubmit() {
  try {
    await addFormRef.value.validate()
  } catch {
    return
  }
  addSubmitting.value = true
  try {
    await addWorkOrder({
      ...addForm,
      orderNo: 'TEMP-' + Date.now(),
      status: 0
    })
    ElMessage.success('工单创建成功')
    addDialogVisible.value = false
    loadData()
  } finally {
    addSubmitting.value = false
  }
}

/* ---------------- 派单弹窗 ---------------- */

const assignDialogVisible = ref(false)
const assignSubmitting = ref(false)
const assignFormRef = ref()

const assignForm = reactive({
  id: null,
  title: '',
  assigneeId: null,
  remark: ''
})

const assignRules = {
  assigneeId: [{ required: true, message: '请选择处理人', trigger: 'change' }]
}

function resetAssignForm() {
  assignForm.id = null
  assignForm.title = ''
  assignForm.assigneeId = null
  assignForm.remark = ''
  assignFormRef.value?.clearValidate()
}

function openAssign(row) {
  resetAssignForm()
  assignForm.id = row.id
  assignForm.title = row.title
  assignDialogVisible.value = true
}

async function handleAssignSubmit() {
  try {
    await assignFormRef.value.validate()
  } catch {
    return
  }
  assignSubmitting.value = true
  try {
    await assignWorkOrder(assignForm.id, {
      assigneeId: assignForm.assigneeId
    })
    ElMessage.success('派单成功')
    assignDialogVisible.value = false
    loadData()
  } finally {
    assignSubmitting.value = false
  }
}

/* ---------------- 处理弹窗 ---------------- */

const handleDialogVisible = ref(false)
const handleSubmitting = ref(false)
const handleFormRef = ref()

const handleForm = reactive({
  id: null,
  title: '',
  handleRemark: ''
})

const handleRules = {
  handleRemark: [{ required: true, message: '请填写处理备注', trigger: 'blur' }]
}

function resetHandleForm() {
  handleForm.id = null
  handleForm.title = ''
  handleForm.handleRemark = ''
  handleFormRef.value?.clearValidate()
}

function openHandle(row) {
  resetHandleForm()
  handleForm.id = row.id
  handleForm.title = row.title
  handleDialogVisible.value = true
}

async function handleWorkOrderSubmit() {
  try {
    await handleFormRef.value.validate()
  } catch {
    return
  }
  handleSubmitting.value = true
  try {
    await handleWorkOrder(handleForm.id, { handleRemark: handleForm.handleRemark })
    ElMessage.success('工单处理完成')
    handleDialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    handleSubmitting.value = false
  }
}

/* ---------------- 初始化 ---------------- */

onMounted(() => {
  loadData()
  loadAssignees()
})

// 切换子菜单（告警工单/运维创建工单）时重新加载
watch(() => route.meta?.type, () => {
  loadData()
})
</script>

<style scoped>
.workorder-page {
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
