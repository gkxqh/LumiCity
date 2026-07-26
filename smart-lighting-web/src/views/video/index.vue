<!--
  视频监控页
  - 表格：摄像头名称、所属灯杆、流地址、状态、是否支持云台、操作
  - 新增 / 编辑弹窗
  - 简单 CRUD，调 pageCamera / addCamera / updateCamera / deleteCamera
  - 分页
  - 预览：按摄像头 status 分级展示
      * 故障(status=2)：弹窗告警 + 画面红色故障遮罩
      * 离线(status=0)：展示本地静态占位图（不连流）
      * 在线(status=1)：流地址可连接则显示实时监控画面
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

    <!-- ============ 视频预览弹窗 ============ -->
    <el-dialog
      v-model="previewVisible"
      :title="`视频预览 - ${previewRow.cameraName}`"
      width="880px"
      @closed="stopPlay"
    >
      <!-- 工具条 -->
      <div class="preview-toolbar">
        <div class="toolbar-left">
          <el-button-group>
            <el-button :icon="ArrowLeft" @click="navigate(-1)">上一个</el-button>
            <el-button @click="navigate(1)">下一个<i class="el-icon"><ArrowRight /></i></el-button>
          </el-button-group>
          <el-button :icon="Camera" :disabled="!canCapture" @click="captureSnapshot">抓拍</el-button>
          <el-button :icon="FullScreen" @click="toggleFullscreen">全屏</el-button>
        </div>
        <div class="toolbar-right">
          <el-button-group>
            <el-button @click="zoomOut">－</el-button>
            <el-button disabled>{{ Math.round(zoom * 100) }}%</el-button>
            <el-button @click="zoomIn">＋</el-button>
          </el-button-group>
        </div>
      </div>

      <!-- 播放区域 -->
      <div ref="playerWrap" class="player-wrap">
        <!-- 在线：HLS / MP4 用原生 video 直连 -->
        <video
          v-if="playMode === 'live' && (streamType === 'hls' || streamType === 'mp4')"
          class="player-media"
          :src="liveSrc"
          controls
          autoplay
          muted
          playsinline
        ></video>

        <!-- 在线：MJPEG 实时流 / 未知类型回退为 img 直连 -->
        <img
          v-else-if="playMode === 'live'"
          class="player-media"
          :src="liveSrc"
          :style="mediaStyle"
          alt="监控画面"
          @error="onMediaError"
        />

        <!-- 在线：静态快照地址轮询 -->
        <img
          v-else-if="playMode === 'snapshot'"
          class="player-media"
          :src="snapshotSrc"
          :style="mediaStyle"
          alt="监控画面"
          @error="onMediaError"
        />

        <!-- 离线：本地静态占位图（不连流） -->
        <img
          v-else-if="playMode === 'offline'"
          class="player-media"
          :src="staticOfflineSrc"
          :style="mediaStyle"
          alt="摄像头离线"
        />

        <!-- RTSP：浏览器无法直连的提示 -->
        <div v-else-if="playMode === 'rtsp'" class="player-tip">
          <el-icon :size="48" color="#e6a23c"><WarningFilled /></el-icon>
          <div class="tip-title">该摄像头为 RTSP 流，浏览器无法直接播放</div>
          <div class="tip-sub">
            需经流媒体网关（如 ZLMediaKit / SRS）转封装为 HLS / WebRTC 后，将 streamUrl 指向网关地址即可在此播放。
          </div>
        </div>

        <!-- 故障：红色遮罩 -->
        <div v-else-if="playMode === 'fault'" class="player-fault">
          <el-icon :size="54" color="#f56c6c"><WarningFilled /></el-icon>
          <div class="fault-title">摄像头故障</div>
          <div class="fault-sub">请及时维修或更换</div>
        </div>

        <!-- CCTV 风格叠加层（仅实时画面时显示） -->
        <div v-if="playMode === 'live' || playMode === 'snapshot'" class="cctv-overlay">
          <div class="cctv-top">
            <span class="cctv-name">{{ previewRow.cameraName }}</span>
            <el-tag size="small" :type="statusTagType(previewRow.status)" effect="dark">
              {{ statusMap[previewRow.status] }}
            </el-tag>
          </div>
          <div class="cctv-rec"><span class="rec-dot"></span>REC {{ recTime }}</div>
        </div>

        <!-- 兼容性提示条 -->
        <div v-if="playerHint" class="player-hint">{{ playerHint }}</div>
      </div>

      <!-- 流地址展示 -->
      <div class="preview-url">流地址：{{ previewRow.streamUrl }}</div>
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
          <el-input v-model="form.streamUrl" placeholder="如 rtsp://xxx / http://xxx.m3u8 / http://xxx.mjpg" />
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
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, Plus, Edit, Delete, VideoPlay,
  Camera, FullScreen, WarningFilled, ArrowLeft, ArrowRight
} from '@element-plus/icons-vue'
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

/* ---------------- 视频预览（状态分级） ---------------- */

const previewVisible = ref(false)
const previewRow = reactive({ id: null, cameraName: '', streamUrl: '', status: 1 })

const playMode = ref('')        // live / snapshot / offline / rtsp / fault
const streamType = ref('')      // hls / mp4 / mjpeg / image / rtsp / unknown
const liveSrc = ref('')         // 直连流（video 或 img）
const snapshotSrc = ref('')     // 快照轮询地址
const staticOfflineSrc = ref('')// 离线静态图
const recTime = ref('')         // CCTV 时间码
const zoom = ref(1)             // 画面缩放
const playerWrap = ref(null)

let snapshotTimer = null
let clockTimer = null

// 离线静态占位图（SVG data URI）：摄像头离线时展示，不尝试连接流
const STATIC_OFFLINE_IMG =
  'data:image/svg+xml;utf8,' + encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="640" height="360" viewBox="0 0 640 360">
       <rect width="640" height="360" fill="#232323"/>
       <g transform="translate(235,118)">
         <rect x="20" y="30" width="120" height="80" rx="10" fill="#333" stroke="#888" stroke-width="3"/>
         <rect x="140" y="55" width="42" height="30" rx="6" fill="#333" stroke="#888" stroke-width="3"/>
         <circle cx="66" cy="70" r="20" fill="#232323" stroke="#888" stroke-width="3"/>
         <line x1="10" y1="20" x2="185" y2="125" stroke="#e06c6c" stroke-width="5" stroke-linecap="round"/>
       </g>
       <text x="320" y="248" fill="#cfcfcf" font-size="22" text-anchor="middle" font-family="sans-serif">摄像头已离线</text>
       <text x="320" y="280" fill="#8a8a8a" font-size="15" text-anchor="middle" font-family="sans-serif">暂无可显示画面</text>
     </svg>`
  )

// 媒体缩放样式
const mediaStyle = computed(() => ({
  transform: `scale(${zoom.value})`
}))

// 当前是否可用"抓拍"（仅实时画面时）
const canCapture = computed(() => playMode.value === 'live' || playMode.value === 'snapshot')

// 浏览器兼容性提示
function isSafari() {
  const ua = navigator.userAgent
  return /Safari/.test(ua) && !/Chrome/.test(ua)
}
const playerHint = computed(() => {
  if (playMode.value === 'rtsp') {
    return '当前为 RTSP 流，浏览器无法直接播放，需经流媒体网关转封装为 HLS / WebRTC 后接入。'
  }
  if (playMode.value === 'live' && streamType.value === 'hls' && !isSafari()) {
    return '当前为 HLS 流，建议使用 Safari 或引入 hls.js 以获得最佳兼容。'
  }
  return ''
})

// 识别流地址类型，自动选择播放方案（无需手动配置播放器）
function detectStreamType(url) {
  if (!url) return 'unknown'
  const u = url.toLowerCase()
  if (u.startsWith('rtsp://') || u.startsWith('rtsps://')) return 'rtsp'
  if (u.includes('.m3u8')) return 'hls'
  if (u.includes('.mp4') || u.includes('.webm')) return 'mp4'
  if (u.includes('.mjpg') || u.includes('.mjpeg') ||
      u.includes('getoneshot') || u.includes('nphmotionjpeg') ||
      u.includes('mjpeg') || u.includes('multipart')) return 'mjpeg'
  if (u.includes('.jpg') || u.includes('.jpeg') || u.includes('.png')) return 'image'
  return 'unknown'
}

// 停止并清理所有播放状态/定时器
function stopPlay() {
  if (snapshotTimer) { clearInterval(snapshotTimer); snapshotTimer = null }
  if (clockTimer) { clearInterval(clockTimer); clockTimer = null }
  liveSrc.value = ''
  snapshotSrc.value = ''
  staticOfflineSrc.value = ''
  recTime.value = ''
  zoom.value = 1
  playMode.value = ''
  streamType.value = ''
}

// 启动 CCTV 时间码
function startClock() {
  const tick = () => {
    recTime.value = new Date().toLocaleString('zh-CN', { hour12: false })
  }
  tick()
  clockTimer = setInterval(tick, 1000)
}

// 快照地址轮询（每秒刷新，加时间戳防缓存）
function startSnapshotLoop(url) {
  const tick = () => {
    snapshotSrc.value = url + (url.includes('?') ? '&' : '?') + '_t=' + Date.now()
  }
  tick()
  snapshotTimer = setInterval(tick, 1000)
}

// 开始预览：先按 status 分级
function startPlay(row) {
  stopPlay()
  previewRow.id = row.id
  previewRow.cameraName = row.cameraName
  previewRow.streamUrl = row.streamUrl
  previewRow.status = row.status

  // 故障：弹窗告警 + 红色遮罩，不尝试播放
  if (row.status === 2) {
    playMode.value = 'fault'
    nextTick(() => {
      ElMessageBox.alert('摄像头故障，请及时维修或更换。', '设备告警', {
        type: 'error',
        confirmButtonText: '我知道了',
        showClose: false
      })
    })
    return
  }

  // 离线：展示静态占位图，不连接流
  if (row.status === 0) {
    playMode.value = 'offline'
    staticOfflineSrc.value = STATIC_OFFLINE_IMG
    return
  }

  // 在线：流地址可连接则显示实时监控画面
  const type = detectStreamType(row.streamUrl)
  streamType.value = type
  startClock()

  if (type === 'rtsp') {
    // 浏览器原生不支持 RTSP，需网关转封装
    playMode.value = 'rtsp'
    return
  }
  if (type === 'mjpeg' || type === 'hls' || type === 'mp4') {
    // 直连播放：MJPEG 走 img，HLS/MP4 走 video
    playMode.value = 'live'
    liveSrc.value = row.streamUrl
    return
  }
  if (type === 'image') {
    // 静态快照地址：轮询刷新模拟实时
    playMode.value = 'snapshot'
    startSnapshotLoop(row.streamUrl)
    return
  }
  // 未知类型：尝试 img 直连
  playMode.value = 'live'
  liveSrc.value = row.streamUrl
}

function handlePreview(row) {
  previewVisible.value = true
  startPlay(row)
}

// 媒体加载失败（在线但流不可连接）
function onMediaError() {
  if (playMode.value === 'live' || playMode.value === 'snapshot') {
    ElMessage.warning('实时监控画面连接失败，请检查流地址是否可访问')
  }
}

// 上一个 / 下一个摄像头切换
function navigate(delta) {
  const list = tableData.value
  if (!list.length) return
  const idx = list.findIndex(d => d.id === previewRow.id)
  let next = idx + delta
  if (next < 0) next = list.length - 1
  if (next >= list.length) next = 0
  startPlay(list[next])
}

// 全屏
function toggleFullscreen() {
  const el = playerWrap.value
  if (!el) return
  if (!document.fullscreenElement) {
    el.requestFullscreen?.()
  } else {
    document.exitFullscreen?.()
  }
}

// 缩放
function zoomIn() { if (zoom.value < 2) zoom.value = +(zoom.value + 0.2).toFixed(2) }
function zoomOut() { if (zoom.value > 0.4) zoom.value = +(zoom.value - 0.2).toFixed(2) }

// 抓拍截图：将当前画面经 canvas 导出 PNG 并叠加时间码
function captureSnapshot() {
  if (!canCapture.value) {
    ElMessage.warning('当前无实时画面，无法抓拍')
    return
  }
  const src = playMode.value === 'live' ? liveSrc.value : snapshotSrc.value
  if (!src) return
  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.onload = () => {
    try {
      const canvas = document.createElement('canvas')
      canvas.width = img.naturalWidth
      canvas.height = img.naturalHeight
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0)
      ctx.fillStyle = 'rgba(0,0,0,0.55)'
      ctx.fillRect(0, canvas.height - 28, canvas.width, 28)
      ctx.fillStyle = '#fff'
      ctx.font = '16px sans-serif'
      ctx.fillText('抓拍 ' + new Date().toLocaleString('zh-CN', { hour12: false }), 10, canvas.height - 8)
      const link = document.createElement('a')
      link.download = `camera_${previewRow.id}_${Date.now()}.png`
      link.href = canvas.toDataURL('image/png')
      link.click()
      ElMessage.success('抓拍成功')
    } catch (e) {
      ElMessage.error('抓拍失败：画面跨域受限，无法导出')
    }
  }
  img.onerror = () => ElMessage.error('抓拍失败：画面加载异常')
  img.src = src
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

/* 预览工具条 */
.preview-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 播放区域 */
.player-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #000;
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.player-media {
  max-width: 100%;
  max-height: 100%;
  transform-origin: center center;
}
.player-tip,
.player-fault {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px;
  text-align: center;
  color: #fff;
}
.player-tip {
  background: rgba(20, 20, 20, 0.85);
}
.player-tip .tip-title {
  font-size: 17px;
  font-weight: 600;
  color: #e6a23c;
}
.player-tip .tip-sub {
  font-size: 13px;
  color: #cfcfcf;
  max-width: 520px;
  line-height: 1.6;
}
.player-fault {
  background: rgba(20, 20, 20, 0.72);
}
.player-fault .fault-title {
  font-size: 20px;
  font-weight: 600;
  color: #f56c6c;
}
.player-fault .fault-sub {
  font-size: 14px;
  color: #e6a23c;
}

/* CCTV 叠加层 */
.cctv-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.cctv-top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.cctv-name {
  color: #fff;
  font-size: 14px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}
.cctv-rec {
  align-self: flex-start;
  color: #fff;
  font-size: 14px;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  gap: 6px;
}
.rec-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f56c6c;
  animation: rec-blink 1s steps(2, start) infinite;
}
@keyframes rec-blink {
  to { opacity: 0.2; }
}

/* 兼容性提示条 */
.player-hint {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(230, 162, 60, 0.92);
  color: #1f1f1f;
  font-size: 12px;
  padding: 6px 12px;
  text-align: center;
}

.preview-url {
  margin-top: 12px;
  color: #909399;
  font-size: 13px;
  word-break: break-all;
}
</style>
