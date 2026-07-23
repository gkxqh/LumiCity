<!--
  设备管理页（完整 CRUD）
  - 顶部搜索栏：设备名称 / 设备类型 / 状态 / 查询 / 重置 / 新增
  - 中间表格：设备编号、设备名称、设备类型、所属灯杆、状态(Tag)、创建时间、操作
  - 底部分页
  - 新增/编辑弹窗：设备编号、设备名称、设备类型、所属灯杆(下拉)、状态

  字段对齐后端 DevDevice 实体：
  - deviceType 是 String 枚举：LIGHT/CAMERA/SENSOR/LED_SCREEN/BROADCAST
  - status 是 Integer：0离线 1在线 2故障
  - poleId 是 Long
  - 分页参数 current/size（对齐 PageQuery）
-->
<template>
  <div class="device-page">
    <!-- ============ 搜索栏 ============ -->
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="设备名称">
          <el-input
            v-model="query.deviceName"
            placeholder="请输入设备名称"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="设备类型">
          <el-select
            v-model="query.deviceType"
            placeholder="全部"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 130px"
          >
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
          <el-button type="success" :icon="Plus" @click="openAdd">新增</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ============ 表格 ============ -->
    <el-card shadow="never">
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="deviceCode" label="设备编号" width="150" />
        <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="deviceType" label="设备类型" width="100" align="center">
          <template #default="{ row }">
            {{ typeMap[row.deviceType] || row.deviceType }}
          </template>
        </el-table-column>
        <el-table-column prop="poleId" label="所属灯杆ID" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
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
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="90px"
      >
        <el-form-item label="设备编号" prop="deviceCode">
          <el-input v-model="form.deviceCode" placeholder="请输入设备编号" />
        </el-form-item>
        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="form.deviceName" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="设备类型" prop="deviceType">
          <el-select v-model="form.deviceType" placeholder="请选择设备类型" style="width: 100%">
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属灯杆" prop="poleId">
          <el-select
            v-model="form.poleId"
            placeholder="请选择所属灯杆"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in poleOptions"
              :key="item.id"
              :label="item.poleName"
              :value="item.id"
            />
          </el-select>
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
import {
  pageDevice,
  addDevice,
  updateDevice,
  deleteDevice,
  listPole
} from '@/api/device'

/* ---------------- 字典数据 ---------------- */

// 设备类型选项：value 用英文枚举，对齐后端 String deviceType
const typeOptions = [
  { label: '照明灯', value: 'LIGHT' },
  { label: '摄像头', value: 'CAMERA' },
  { label: '传感器', value: 'SENSOR' },
  { label: 'LED屏', value: 'LED_SCREEN' },
  { label: '广播', value: 'BROADCAST' }
]
// value -> label 映射，表格展示用
const typeMap = Object.fromEntries(typeOptions.map(i => [i.value, i.label]))

// 状态选项：value 用数字，对齐后端 Integer status（0离线 1在线 2故障）
const statusOptions = [
  { label: '在线', value: 1 },
  { label: '离线', value: 0 },
  { label: '故障', value: 2 }
]
// 状态值 → 中文显示（key 是数字）
const statusMap = { 0: '离线', 1: '在线', 2: '故障' }

// 状态对应的 Tag 颜色：在线-绿、离线-灰、故障-红
function statusTagType(status) {
  return { 1: 'success', 0: 'info', 2: 'danger' }[status] || 'info'
}

/* ---------------- 查询 & 表格 ---------------- */

// 查询条件：分页参数用 current/size，对齐后端 PageQuery
const query = reactive({
  current: 1,
  size: 10,
  deviceName: '',
  deviceType: '',
  status: null
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)

// 加载设备分页数据
async function loadData() {
  loading.value = true
  try {
    const res = await pageDevice(query)
    const page = res.data || {}
    tableData.value = page.records || page.list || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

// 点击查询：回到第一页后重新加载
function handleSearch() {
  query.current = 1
  loadData()
}

// 重置查询条件
function handleReset() {
  query.deviceName = ''
  query.deviceType = ''
  query.status = null
  query.current = 1
  loadData()
}

/* ---------------- 灯杆下拉选项 ---------------- */

const poleOptions = ref([])
async function loadPoleOptions() {
  const res = await listPole()
  poleOptions.value = res.data || []
}

/* ---------------- 新增/编辑弹窗 ---------------- */

const dialogVisible = ref(false)
const dialogTitle = ref('新增设备')
const submitting = ref(false)
const formRef = ref()

// 弹窗表单数据：字段名和类型对齐后端 DevDevice 实体
const form = reactive({
  id: null,
  deviceCode: '',
  deviceName: '',
  deviceType: '',
  poleId: null,
  status: null
})

// 弹窗表单校验规则
const formRules = {
  deviceCode: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  deviceType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  poleId: [{ required: true, message: '请选择所属灯杆', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 重置表单（弹窗关闭时触发）
function resetForm() {
  form.id = null
  form.deviceCode = ''
  form.deviceName = ''
  form.deviceType = ''
  form.poleId = null
  form.status = null
  formRef.value?.clearValidate()
}

// 打开新增弹窗
function openAdd() {
  dialogTitle.value = '新增设备'
  resetForm()
  dialogVisible.value = true
}

// 打开编辑弹窗：把当前行数据回填到表单
function openEdit(row) {
  dialogTitle.value = '编辑设备'
  resetForm()
  Object.assign(form, {
    id: row.id,
    deviceCode: row.deviceCode,
    deviceName: row.deviceName,
    deviceType: row.deviceType,
    poleId: row.poleId,
    status: row.status
  })
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
    if (form.id) {
      await updateDevice({ ...form })
      ElMessage.success('修改成功')
    } else {
      await addDevice({ ...form })
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
    `确定删除设备「${row.deviceName}」吗？`,
    '提示',
    { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
  )
  await deleteDevice(row.id)
  ElMessage.success('删除成功')
  loadData()
}

/* ---------------- 初始化 ---------------- */

onMounted(() => {
  loadPoleOptions()
  loadData()
})
</script>

<style scoped>
.device-page {
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
