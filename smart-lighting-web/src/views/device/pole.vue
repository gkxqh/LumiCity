<!--
  灯杆管理页（完整 CRUD）
  - 顶部搜索栏：灯杆名称 / 灯杆编号 / 状态 / 查询 / 重置 / 新增
  - 中间表格：灯杆编号、灯杆名称、地址、经度、纬度、状态、操作
  - 底部分页
  - 新增/编辑弹窗：灯杆编号、灯杆名称、地址、经度、纬度、状态

  字段对齐后端 DevPole 实体：
  - 经度 lng（BigDecimal）、纬度 lat（BigDecimal）
  - status 是 Integer：0离线 1在线 2故障
  - 分页参数 current/size（对齐 PageQuery）
-->
<template>
  <div class="pole-page">
    <!-- ============ 搜索栏 ============ -->
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="灯杆名称">
          <el-input
            v-model="query.poleName"
            placeholder="请输入灯杆名称"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="灯杆编号">
          <el-input
            v-model="query.poleCode"
            placeholder="请输入灯杆编号"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
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
        <el-table-column prop="poleCode" label="灯杆编号" width="150" />
        <el-table-column prop="poleName" label="灯杆名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="lng" label="经度" width="120" align="center" />
        <el-table-column prop="lat" label="纬度" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
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
      width="540px"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="90px"
      >
        <el-form-item label="灯杆编号" prop="poleCode">
          <el-input v-model="form.poleCode" placeholder="请输入灯杆编号" />
        </el-form-item>
        <el-form-item label="灯杆名称" prop="poleName">
          <el-input v-model="form.poleName" placeholder="请输入灯杆名称" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="经度" prop="lng">
              <el-input v-model="form.lng" placeholder="如 116.404" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="lat">
              <el-input v-model="form.lat" placeholder="如 39.915" />
            </el-form-item>
          </el-col>
        </el-row>
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
  pagePole,
  addPole,
  updatePole,
  deletePole
} from '@/api/device'

/* ---------------- 字典数据 ---------------- */

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
  poleName: '',
  poleCode: '',
  status: null
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)

// 加载灯杆分页数据
async function loadData() {
  loading.value = true
  try {
    const res = await pagePole(query)
    // 兼容 MyBatis-Plus(records) 与 PageHelper(list) 两种返回结构
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
  query.poleName = ''
  query.poleCode = ''
  query.status = null
  query.current = 1
  loadData()
}

/* ---------------- 新增/编辑弹窗 ---------------- */

const dialogVisible = ref(false)
const dialogTitle = ref('新增灯杆')
const submitting = ref(false)
const formRef = ref()

// 弹窗表单数据：字段名对齐后端 DevPole 实体
const form = reactive({
  id: null,
  poleCode: '',
  poleName: '',
  address: '',
  lng: '',
  lat: '',
  status: null
})

// 经纬度数字校验：允许小数
const numberValidator = (rule, value, callback) => {
  if (value === '' || value === null || value === undefined) {
    callback(new Error('请输入数值'))
    return
  }
  if (isNaN(Number(value))) {
    callback(new Error('请输入合法数字'))
    return
  }
  callback()
}

// 弹窗表单校验规则
const formRules = {
  poleCode: [{ required: true, message: '请输入灯杆编号', trigger: 'blur' }],
  poleName: [{ required: true, message: '请输入灯杆名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  lng: [{ required: true, validator: numberValidator, trigger: 'blur' }],
  lat: [{ required: true, validator: numberValidator, trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 重置表单（弹窗关闭时触发）
function resetForm() {
  form.id = null
  form.poleCode = ''
  form.poleName = ''
  form.address = ''
  form.lng = ''
  form.lat = ''
  form.status = null
  formRef.value?.clearValidate()
}

// 打开新增弹窗
function openAdd() {
  dialogTitle.value = '新增灯杆'
  resetForm()
  dialogVisible.value = true
}

// 打开编辑弹窗：把当前行数据回填到表单
function openEdit(row) {
  dialogTitle.value = '编辑灯杆'
  resetForm()
  Object.assign(form, {
    id: row.id,
    poleCode: row.poleCode,
    poleName: row.poleName,
    address: row.address,
    lng: row.lng,
    lat: row.lat,
    status: row.status
  })
  dialogVisible.value = true
}

// 提交新增/编辑
async function handleSubmit() {
  // 1. 表单校验
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  // 2. 根据 id 判断新增还是修改；提交时把经纬度转成数字
  submitting.value = true
  try {
    const payload = {
      ...form,
      lng: Number(form.lng),
      lat: Number(form.lat)
    }
    if (form.id) {
      await updatePole(payload)
      ElMessage.success('修改成功')
    } else {
      await addPole(payload)
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
  // 二次确认
  await ElMessageBox.confirm(
    `确定删除灯杆「${row.poleName}」吗？`,
    '提示',
    { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
  )
  await deletePole(row.id)
  ElMessage.success('删除成功')
  loadData()
}

/* ---------------- 初始化 ---------------- */

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.pole-page {
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
