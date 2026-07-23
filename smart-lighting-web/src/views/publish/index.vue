<!--
  信息发布页
  - 表格：节目名称、媒体类型、播放模式、状态、操作(编辑/发布/删除)
  - 新增/编辑弹窗：节目名称、媒体类型(Select)、内容(Textarea)、播放模式(Select)
  - 发布按钮调 publishProgram
  - 分页
-->
<template>
  <div class="publish-page">
    <!-- ============ 搜索栏 ============ -->
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="节目名称">
          <el-input
            v-model="query.programName"
            placeholder="请输入节目名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="媒体类型">
          <el-select v-model="query.mediaType" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="item in mediaTypeOptions"
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
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="success" :icon="Plus" @click="openAdd">新增</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ============ 表格 ============ -->
    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="programName" label="节目名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="mediaType" label="媒体类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag effect="plain">{{ mediaTypeMap[row.mediaType] || row.mediaType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="playMode" label="播放模式" width="120" align="center">
          <template #default="{ row }">
            {{ playModeMap[row.playMode] || row.playMode }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              type="success"
              link
              :icon="Promotion"
              :disabled="row.status === 1"
              @click="handlePublish(row)"
            >发布</el-button>
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
      width="560px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="节目名称" prop="programName">
          <el-input v-model="form.programName" placeholder="请输入节目名称" />
        </el-form-item>
        <el-form-item label="媒体类型" prop="mediaType">
          <el-select v-model="form.mediaType" placeholder="请选择媒体类型" style="width: 100%">
            <el-option
              v-for="item in mediaTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="播放模式" prop="playMode">
          <el-select v-model="form.playMode" placeholder="请选择播放模式" style="width: 100%">
            <el-option
              v-for="item in playModeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="节目内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="请输入节目内容（文本 / JSON 配置）"
          />
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
import { Search, Refresh, Plus, Edit, Delete, Promotion } from '@element-plus/icons-vue'
import {
  pageProgram,
  addProgram,
  updateProgram,
  deleteProgram,
  publishProgram
} from '@/api/other'

/* ---------------- 字典数据 ---------------- */

// 媒体类型选项：value 对齐后端 String mediaType（TEXT/IMAGE/VIDEO）
const mediaTypeOptions = [
  { label: '文本', value: 'TEXT' },
  { label: '图片', value: 'IMAGE' },
  { label: '视频', value: 'VIDEO' }
]
const mediaTypeMap = Object.fromEntries(mediaTypeOptions.map(i => [i.value, i.label]))

// 播放模式选项：value 对齐后端 String playMode（LOOP/ONCE）
const playModeOptions = [
  { label: '循环播放', value: 'LOOP' },
  { label: '单次播放', value: 'ONCE' }
]
const playModeMap = Object.fromEntries(playModeOptions.map(i => [i.value, i.label]))

// 状态选项：value 对齐后端 Integer status（0待发布 1已发布 2已下线）
const statusOptions = [
  { label: '待发布', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已下线', value: 2 }
]
const statusMap = Object.fromEntries(statusOptions.map(i => [i.value, i.label]))

// 状态 Tag 颜色：待发布-灰、已发布-绿、已下线-红
function statusTagType(status) {
  return { 0: 'info', 1: 'success', 2: 'danger' }[status] || 'info'
}

/* ---------------- 查询 & 表格 ---------------- */

// 分页参数用 current/size，对齐后端 PageQuery
const query = reactive({
  current: 1,
  size: 10,
  programName: '',
  mediaType: '',
  status: null
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await pageProgram(query)
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
  query.programName = ''
  query.mediaType = ''
  query.status = null
  query.current = 1
  loadData()
}

/* ---------------- 新增/编辑弹窗 ---------------- */

const dialogVisible = ref(false)
const dialogTitle = ref('新增节目')
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  programName: '',
  mediaType: '',
  playMode: 'LOOP',
  content: '',
  status: 0
})

const formRules = {
  programName: [{ required: true, message: '请输入节目名称', trigger: 'blur' }],
  mediaType: [{ required: true, message: '请选择媒体类型', trigger: 'change' }],
  playMode: [{ required: true, message: '请选择播放模式', trigger: 'change' }],
  content: [{ required: true, message: '请输入节目内容', trigger: 'blur' }]
}

function resetForm() {
  form.id = null
  form.programName = ''
  form.mediaType = ''
  form.playMode = 'LOOP'
  form.content = ''
  form.status = 0
  formRef.value?.clearValidate()
}

function openAdd() {
  dialogTitle.value = '新增节目'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  dialogTitle.value = '编辑节目'
  resetForm()
  Object.assign(form, {
    id: row.id,
    programName: row.programName,
    mediaType: row.mediaType,
    playMode: row.playMode || 'LOOP',
    content: row.content || '',
    status: row.status ?? 0
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
    if (form.id) {
      await updateProgram({ ...form })
      ElMessage.success('修改成功')
    } else {
      await addProgram({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

/* ---------------- 发布 ---------------- */

async function handlePublish(row) {
  await ElMessageBox.confirm(
    `确定发布节目「${row.programName}」吗？发布后将推送到对应 LED 屏。`,
    '发布确认',
    { type: 'warning', confirmButtonText: '确定发布', cancelButtonText: '取消' }
  )
  await publishProgram(row.id)
  ElMessage.success('发布成功')
  loadData()
}

/* ---------------- 删除 ---------------- */

async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除节目「${row.programName}」吗？`,
    '提示',
    { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
  )
  await deleteProgram(row.id)
  ElMessage.success('删除成功')
  loadData()
}

/* ---------------- 初始化 ---------------- */

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.publish-page {
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
