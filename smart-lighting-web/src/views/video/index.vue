<!--
  视频监控页
  - 表格：摄像头名称、所属灯杆、流地址、状态、是否支持云台、操作
  - 新增 / 编辑弹窗
  - 简单 CRUD，调 pageCamera / addCamera / updateCamera / deleteCamera
  - 分页
-->
<template>
  <div class="video-page">
    <!-- ============ 搜索栏 ============ -->
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="摄像头名称">
          <el-input
            v-model="query.cameraName"
            placeholder="请输入名称"
            clearable
            style="width: 200px"
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
          <el-button type="success" :icon="Plus" @click="openAdd">新增</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ============ 表格 ============ -->
    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="cameraName" label="摄像头名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="poleName" label="所属灯杆" width="140" show-overflow-tooltip />
        <el-table-column prop="streamUrl" label="流地址" min-width="240" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ptzEnable" label="云台支持" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.ptzEnable ? 'success' : 'info'" effect="plain">
              {{ row.ptzEnable ? '支持' : '不支持' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="VideoPlay" @click="handlePreview(row)">预览</el-button>
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

    <!-- ============ 视频预览弹窗（简易演示） ============ -->
    <el-dialog v-model="previewVisible" :title="`视频预览 - ${previewRow.cameraName}`" width="640px">
      <div class="preview-box">
        <!-- 实际项目里接入 flv.js / hls.js / EasyPlayer 播放 streamUrl -->
        <el-empty description="此处接入流媒体播放器（flv.js / hls.js）" />
        <div class="preview-url">流地址：{{ previewRow.streamUrl }}</div>
      </div>
    </el-dialog>

    <!-- ============ 新增/编辑弹窗 ============ -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="摄像头名称" prop="cameraName">
          <el-input v-model="form.cameraName" placeholder="请输入摄像头名称" />
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
        <el-form-item label="流地址" prop="streamUrl">
          <el-input v-model="form.streamUrl" placeholder="如 rtsp://xxx / http://xxx.m3u8" />
        </el-form-item>
        <el-form-item label="云台支持" prop="ptzEnable">
          <el-switch v-model="form.ptzEnable" :active-value="1" :inactive-value="0" active-text="支持" inactive-text="不支持" />
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
import { Search, Refresh, Plus, Edit, Delete, VideoPlay } from '@element-plus/icons-vue'
import { pageCamera, addCamera, updateCamera, deleteCamera } from '@/api/other'
import { listPole } from '@/api/device'

/* ---------------- 字典数据 ---------------- */

// 摄像头状态选项：value 用数字，对齐后端 Integer status（0离线 1在线 2故障）
const statusOptions = [
  { label: '离线', value: 0 },
  { label: '在线', value: 1 },
  { label: '故障', value: 2 }
]
const statusMap = Object.fromEntries(statusOptions.map(i => [i.value, i.label]))

// 状态 Tag 颜色：在线-绿、离线-灰、故障-红
function statusTagType(status) {
  return { 0: 'info', 1: 'success', 2: 'danger' }[status] || 'info'
}

/* ---------------- 灯杆下拉选项 ---------------- */

const poleOptions = ref([])
async function loadPoleOptions() {
  try {
    const res = await listPole()
    poleOptions.value = res.data || []
  } catch (e) {
    /* 静默 */
  }
}

/* ---------------- 查询 & 表格 ---------------- */

// 分页参数用 current/size，对齐后端 PageQuery
const query = reactive({
  current: 1,
  size: 10,
  cameraName: '',
  status: null
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await pageCamera(query)
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
  query.cameraName = ''
  query.status = null
  query.current = 1
  loadData()
}

/* ---------------- 视频预览 ---------------- */

const previewVisible = ref(false)
const previewRow = reactive({ cameraName: '', streamUrl: '' })

function handlePreview(row) {
  Object.assign(previewRow, { cameraName: row.cameraName, streamUrl: row.streamUrl })
  previewVisible.value = true
}

/* ---------------- 新增/编辑弹窗 ---------------- */

const dialogVisible = ref(false)
const dialogTitle = ref('新增摄像头')
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  cameraName: '',
  poleId: '',
  streamUrl: '',
  ptzEnable: 0,  // 对齐后端 Integer ptzEnable（0否 1是）
  status: null   // 对齐后端 Integer status（0离线 1在线 2故障）
})

const formRules = {
  cameraName: [{ required: true, message: '请输入摄像头名称', trigger: 'blur' }],
  poleId: [{ required: true, message: '请选择所属灯杆', trigger: 'change' }],
  streamUrl: [{ required: true, message: '请输入流地址', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function resetForm() {
  form.id = null
  form.cameraName = ''
  form.poleId = ''
  form.streamUrl = ''
  form.ptzEnable = 0
  form.status = null
  formRef.value?.clearValidate()
}

function openAdd() {
  dialogTitle.value = '新增摄像头'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  dialogTitle.value = '编辑摄像头'
  resetForm()
  Object.assign(form, {
    id: row.id,
    cameraName: row.cameraName,
    poleId: row.poleId,
    streamUrl: row.streamUrl,
    ptzEnable: row.ptzEnable ?? 0,
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
      await updateCamera({ ...form })
      ElMessage.success('修改成功')
    } else {
      await addCamera({ ...form })
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
    `确定删除摄像头「${row.cameraName}」吗？`,
    '提示',
    { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
  )
  await deleteCamera(row.id)
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
.video-page {
  padding: 0;
}
.search-card {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.preview-box {
  text-align: center;
}
.preview-url {
  margin-top: 12px;
  color: #909399;
  font-size: 13px;
  word-break: break-all;
}
</style>
