<!--
  灯杆管理页（v2 — region + road + number 拼接）
  - 搜索栏：灯杆名称 / 灯杆编号 / 所属区域 / 道路 / 状态
  - 表格：灯杆编号、灯杆名称、区域、道路、编号、经纬度、状态
  - pole_name 和 address 由后端自动拼接，前端不提供编辑入口
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
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="灯杆编号">
          <el-input
            v-model="query.poleCode"
            placeholder="请输入灯杆编号"
            clearable
            style="width: 140px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="所属区域">
          <el-select
            v-model="query.regionId"
            placeholder="全部"
            clearable
            style="width: 130px"
          >
            <el-option
              v-for="item in regionOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="道路">
          <el-input
            v-model="query.road"
            placeholder="按路名筛选"
            clearable
            style="width: 140px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 110px"
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
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="poleCode" label="灯杆编号" width="130" />
        <el-table-column prop="poleName" label="灯杆名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="区域" width="100" align="center">
          <template #default="{ row }">{{ row.regionName || '--' }}</template>
        </el-table-column>
        <el-table-column prop="road" label="道路" width="130" show-overflow-tooltip />
        <el-table-column prop="number" label="编号" width="100" />
        <el-table-column prop="lng" label="经度" width="110" align="center" />
        <el-table-column prop="lat" label="纬度" width="110" align="center" />
        <el-table-column prop="status" label="在线状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="照明状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.lightStatus === 1 ? 'warning' : 'info'" effect="dark">
              {{ row.lightStatus === 1 ? '开灯' : '关灯' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前亮度" width="80" align="center">
          <template #default="{ row }">
            <span>{{ row.lightBrightness != null ? row.lightBrightness + '%' : '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right" align="center">
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
        <el-form-item label="灯杆编号" prop="poleCode">
          <el-input v-model="form.poleCode" placeholder="请输入灯杆编号" />
        </el-form-item>

        <el-form-item label="所属区域" prop="regionId">
          <el-select v-model="form.regionId" placeholder="请选择区域" style="width: 100%">
            <el-option
              v-for="item in regionOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="道路名称" prop="road">
          <el-input v-model="form.road" placeholder="如：科华北路、天府大道" />
        </el-form-item>

        <el-form-item label="编号" prop="number">
          <el-input v-model="form.number" placeholder="如：88号、29号院" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="经度" prop="lng">
              <el-input v-model="form.lng" placeholder="如 104.05" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="lat">
              <el-input v-model="form.lat" placeholder="如 30.63" />
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

        <!-- 提示：pole_name 和 address 由后端自动拼接 -->
        <el-form-item label=" ">
          <el-text type="info" size="small">灯杆名称和地址由「区域 + 道路 + 编号」自动生成</el-text>
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
  deletePole,
  listRegion
} from '@/api/device'

/* ---------------- 字典数据 ---------------- */

const statusOptions = [
  { label: '在线', value: 1 },
  { label: '离线', value: 0 },
  { label: '故障', value: 2 }
]

const statusMap = { 0: '离线', 1: '在线', 2: '故障' }

function statusTagType(status) {
  return { 1: 'success', 0: 'info', 2: 'danger' }[status] || 'info'
}

/* ---------------- 区域选项 ---------------- */

const regionOptions = ref([])

async function loadRegionOptions() {
  try {
    const res = await listRegion()
    regionOptions.value = res.data || []
  } catch {
    regionOptions.value = []
  }
}

/* ---------------- 查询 & 表格 ---------------- */

const query = reactive({
  current: 1,
  size: 10,
  poleName: '',
  poleCode: '',
  regionId: null,
  road: '',
  status: null
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await pagePole(query)
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
  query.poleName = ''
  query.poleCode = ''
  query.regionId = null
  query.road = ''
  query.status = null
  query.current = 1
  loadData()
}

/* ---------------- 新增/编辑弹窗 ---------------- */

const dialogVisible = ref(false)
const dialogTitle = ref('新增灯杆')
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  poleCode: '',
  regionId: null,
  road: '',
  number: '',
  lng: '',
  lat: '',
  status: null
})

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

const formRules = {
  poleCode: [{ required: true, message: '请输入灯杆编号', trigger: 'blur' }],
  regionId: [{ required: true, message: '请选择所属区域', trigger: 'change' }],
  road: [{ required: true, message: '请输入道路名称', trigger: 'blur' }],
  lng: [{ required: true, validator: numberValidator, trigger: 'blur' }],
  lat: [{ required: true, validator: numberValidator, trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function resetForm() {
  form.id = null
  form.poleCode = ''
  form.regionId = null
  form.road = ''
  form.number = ''
  form.lng = ''
  form.lat = ''
  form.status = null
  formRef.value?.clearValidate()
}

function openAdd() {
  dialogTitle.value = '新增灯杆'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  dialogTitle.value = '编辑灯杆'
  resetForm()
  Object.assign(form, {
    id: row.id,
    poleCode: row.poleCode,
    regionId: row.regionId,
    road: row.road,
    number: row.number || '',
    lng: row.lng,
    lat: row.lat,
    status: row.status
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
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
  loadRegionOptions()
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
