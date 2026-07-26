<!--
  信息发布页
  - 表格：节目名称、媒体类型、播放模式、状态、发布时间、操作(预览/发布/编辑/删除)
  - 新增/编辑弹窗：媒体类型选 TEXT → textarea 输入；选 IMAGE/VIDEO → 文件上传
  - 预览弹窗：按媒体类型展示内容（TEXT 渲染文本 / IMAGE 展示图片 / VIDEO 播放视频）
  - 发布时写入 led_publish_log，可查看发布历史
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
            <el-option v-for="item in mediaTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-table-column prop="programName" label="节目名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="mediaType" label="媒体类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag effect="plain">{{ mediaTypeMap[row.mediaType] || row.mediaType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="playMode" label="播放模式" width="100" align="center">
          <template #default="{ row }">
            {{ playModeMap[row.playMode] || row.playMode }}
          </template>
        </el-table-column>
        <el-table-column prop="screenName" label="目标设备" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.screenName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ statusMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170">
          <template #default="{ row }">
            {{ row.publishTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="360" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openPreview(row)">预览</el-button>
            <el-button
              type="success"
              link
              :icon="Promotion"
              :disabled="row.status === 1"
              @click="handlePublish(row)"
            >发布</el-button>
            <el-button type="warning" link :icon="Tickets" @click="openLogs(row)">历史</el-button>
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

    <!-- ============ 新增/编辑弹窗（文件上传版） ============ -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="节目名称" prop="programName">
          <el-input v-model="form.programName" placeholder="请输入节目名称" />
        </el-form-item>
        <el-form-item label="媒体类型" prop="mediaType">
          <el-select v-model="form.mediaType" placeholder="请选择媒体类型" style="width: 100%" @change="onMediaTypeChange">
            <el-option v-for="item in mediaTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="播放模式" prop="playMode">
          <el-select v-model="form.playMode" placeholder="请选择播放模式" style="width: 100%">
            <el-option v-for="item in playModeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标LED屏" prop="screenId">
          <el-select v-model="form.screenId" placeholder="请选择LED屏设备" filterable clearable style="width: 100%">
            <el-option v-for="item in screenOptions" :key="item.id" :label="item.deviceName" :value="item.id" />
          </el-select>
        </el-form-item>

        <!-- 文本内容：textarea 输入 -->
        <el-form-item v-if="showTextarea" label="节目内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="请输入节目文本内容"
          />
        </el-form-item>

        <!-- 图片/视频内容：文件上传 -->
        <el-form-item v-if="showUpload" :label="uploadLabel" prop="content">
          <div class="upload-wrap">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :show-file-list="false"
              :accept="uploadAccept"
              :on-change="onFileChange"
            >
              <el-button type="primary" :icon="Upload">选择文件</el-button>
              <template #tip>
                <span class="upload-hint">{{ uploadHint }}</span>
              </template>
            </el-upload>
            <div v-if="form.content" class="upload-preview">
              <img v-if="form.mediaType === 'IMAGE'" :src="form.content" class="preview-img" />
              <video v-if="form.mediaType === 'VIDEO'" :src="form.content" class="preview-video" controls />
              <el-button link type="danger" size="small" @click="form.content = ''">移除</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>

    <!-- ============ 内容预览弹窗 ============ -->
    <el-dialog v-model="previewVisible" :title="`节目预览 - ${previewRow.programName}`" width="700px" top="5vh">
      <div class="preview-body">
        <div class="preview-meta">
          <el-tag size="small" effect="plain">{{ mediaTypeMap[previewRow.mediaType] || previewRow.mediaType }}</el-tag>
          <el-tag size="small" :type="statusTagType(previewRow.status)" effect="light">{{ statusMap[previewRow.status] || previewRow.status }}</el-tag>
          <span v-if="previewRow.publishTime" class="preview-time">发布时间：{{ previewRow.publishTime }}</span>
        </div>
        <div class="preview-content">
          <!-- TEXT：大号字体渲染 -->
          <div v-if="previewRow.mediaType === 'TEXT'" class="preview-text">
            {{ previewRow.content }}
          </div>
          <!-- IMAGE：展示图片 -->
          <img v-if="previewRow.mediaType === 'IMAGE'" :src="previewRow.content" class="preview-img-full" />
          <!-- VIDEO：嵌入式播放 -->
          <video v-if="previewRow.mediaType === 'VIDEO'" :src="previewRow.content" class="preview-video-full" controls autoplay />
        </div>
        <div v-if="previewRow.content && previewRow.mediaType !== 'TEXT'" class="preview-url">
          资源地址：<el-link type="primary" :href="previewRow.content" target="_blank">{{ previewRow.content }}</el-link>
        </div>
      </div>
    </el-dialog>

    <!-- ============ 发布历史弹窗 ============ -->
    <el-dialog v-model="logVisible" :title="`发布历史 - ${logProgramName}`" width="700px">
      <el-table :data="logData" border stripe v-loading="logLoading" style="width: 100%">
        <el-table-column prop="publishTime" label="发布时间" width="170" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="pushStatus" label="推送状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.pushStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.pushStatus === 'SUCCESS' ? '已推送' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contentPreview" label="内容预览" min-width="180" show-overflow-tooltip />
        <el-table-column prop="pushMessage" label="推送结果" min-width="180" show-overflow-tooltip />
      </el-table>
      <el-pagination
        class="pagination"
        v-model:current-page="logQuery.current"
        v-model:page-size="logQuery.size"
        :total="logTotal"
        layout="total, prev, pager, next"
        background
        small
        @current-change="loadLogs"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Promotion, View, Upload, Tickets } from '@element-plus/icons-vue'
import {
  pageProgram,
  addProgram,
  updateProgram,
  deleteProgram,
  publishProgram,
  programLogs,
  uploadFile
} from '@/api/other'
import { pageDevice } from '@/api/device'

/* ---------------- 字典数据 ---------------- */

const mediaTypeOptions = [
  { label: '文本', value: 'TEXT' },
  { label: '图片', value: 'IMAGE' },
  { label: '视频', value: 'VIDEO' }
]
const mediaTypeMap = Object.fromEntries(mediaTypeOptions.map(i => [i.value, i.label]))

const playModeOptions = [
  { label: '循环播放', value: 'LOOP' },
  { label: '单次播放', value: 'ONCE' }
]
const playModeMap = Object.fromEntries(playModeOptions.map(i => [i.value, i.label]))

const statusOptions = [
  { label: '待发布', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已下线', value: 2 }
]
const statusMap = Object.fromEntries(statusOptions.map(i => [i.value, i.label]))

function statusTagType(status) {
  return { 0: 'info', 1: 'success', 2: 'danger' }[status] || 'info'
}

/* ---------------- 查询 & 表格 ---------------- */

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
const uploadRef = ref()

// LED 屏设备列表（用于下拉选择）
const screenOptions = ref([])

async function loadLedScreens() {
  try {
    const res = await pageDevice({ deviceType: 'LED_SCREEN', size: 999 })
    screenOptions.value = (res.data?.records || []).filter(d => d.status === 1) // 只显示在线的
  } catch {
    screenOptions.value = []
  }
}

const form = reactive({
  id: null,
  programName: '',
  mediaType: '',
  playMode: 'LOOP',
  content: '',
  status: 0,
  screenId: null
})

const formRules = {
  programName: [{ required: true, message: '请输入节目名称', trigger: 'blur' }],
  mediaType: [{ required: true, message: '请选择媒体类型', trigger: 'change' }],
  playMode: [{ required: true, message: '请选择播放模式', trigger: 'change' }],
  content: [{ required: true, message: '请上传文件或输入内容', trigger: 'blur' }]
}

// 根据媒体类型动态切换输入方式
const showTextarea = computed(() => form.mediaType === 'TEXT')
const showUpload = computed(() => form.mediaType === 'IMAGE' || form.mediaType === 'VIDEO')
const uploadLabel = computed(() => form.mediaType === 'IMAGE' ? '上传图片' : '上传视频')
const uploadAccept = computed(() => form.mediaType === 'IMAGE' ? '.jpg,.jpeg,.png,.gif,.bmp,.webp' : '.mp4,.avi,.mov,.wmv,.flv,.mkv')
const uploadHint = computed(() => form.mediaType === 'IMAGE' ? '支持 JPG/PNG/GIF/WebP，建议尺寸 1920x1080' : '支持 MP4/AVI/MOV/FLV，建议 10MB 以内')

function onMediaTypeChange() {
  // 切换媒体类型时清空已选文件
  form.content = ''
  if (form.mediaType === 'TEXT') {
    formRules.content = [{ required: true, message: '请输入节目内容', trigger: 'blur' }]
  } else {
    formRules.content = [{ required: true, message: '请上传文件', trigger: 'change' }]
  }
}

async function onFileChange(ev) {
  // 上传文件 → 获取 URL → 存到 form.content
  try {
    const res = await uploadFile(ev.raw)
    const data = res.data
    if (data.url) {
      form.content = data.url
      ElMessage.success('文件上传成功')
    } else {
      ElMessage.error('文件上传失败：返回数据异常')
    }
  } catch (e) {
    ElMessage.error(e.message || '文件上传失败，请重试')
  }
  // 清除 upload 组件的文件列表（避免重复触发）
  uploadRef.value?.clearFiles()
}

function resetForm() {
  form.id = null
  form.programName = ''
  form.mediaType = ''
  form.playMode = 'LOOP'
  form.content = ''
  form.status = 0
  form.screenId = null
  formRef.value?.clearValidate()
}

function openAdd() {
  dialogTitle.value = '新增节目'
  resetForm()
  loadLedScreens()
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
    status: 0,  // 编辑后自动回到"待发布"，需要重新发布
    screenId: row.screenId || null
  })
  loadLedScreens()
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

/* ---------------- 预览 ---------------- */

const previewVisible = ref(false)
const previewRow = ref({})

function openPreview(row) {
  previewRow.value = row
  previewVisible.value = true
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
  // 自动弹出发布历史
  openLogs(row)
}

/* ---------------- 发布历史弹窗 ---------------- */

const logVisible = ref(false)
const logProgramName = ref('')
const logData = ref([])
const logTotal = ref(0)
const logLoading = ref(false)
const logQuery = reactive({ current: 1, size: 10 })

let currentLogProgramId = null

async function loadLogs() {
  if (!currentLogProgramId) return
  logLoading.value = true
  try {
    const res = await programLogs(currentLogProgramId, { current: logQuery.current, size: logQuery.size })
    const page = res.data || {}
    logData.value = page.records || page.list || []
    logTotal.value = page.total || 0
  } finally {
    logLoading.value = false
  }
}

function openLogs(row) {
  currentLogProgramId = row.id
  logProgramName.value = row.programName
  logQuery.current = 1
  loadLogs()
  logVisible.value = true
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

/* 上传区域 */
.upload-wrap {
  width: 100%;
}
.upload-hint {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}
.upload-preview {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.preview-img {
  max-width: 200px;
  max-height: 120px;
  border-radius: 4px;
  border: 1px solid #eee;
}
.preview-video {
  max-width: 320px;
  max-height: 160px;
  border-radius: 4px;
}

/* 预览弹窗 */
.preview-body {
  min-height: 200px;
}
.preview-meta {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.preview-time {
  font-size: 13px;
  color: #999;
}
.preview-content {
  margin: 16px 0;
}
.preview-text {
  font-size: 32px;
  line-height: 1.6;
  color: #333;
  padding: 24px;
  background: #f9f9f9;
  border-radius: 8px;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.preview-img-full {
  max-width: 100%;
  max-height: 60vh;
  border-radius: 8px;
  display: block;
  margin: 0 auto;
}
.preview-video-full {
  max-width: 100%;
  max-height: 60vh;
  border-radius: 8px;
  display: block;
  margin: 0 auto;
}
.preview-url {
  margin-top: 12px;
  font-size: 12px;
  color: #999;
  word-break: break-all;
}
</style>
