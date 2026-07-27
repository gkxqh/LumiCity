<!--
  数据大盘（大屏版）- 重构布局
  顶部栏 + 左(Leaflet地图+Three.js 3D叠加) | 右(3×2统计卡片+实时告警) + 底(4张ECharts图表)
-->
<template>
  <div class="dashboard-page">
    <!-- ============ 顶部栏 ============ -->
    <div class="d-header">
      <div class="d-title">
        <svg class="d-logo" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/>
          <path d="M12 2v20M2 12h20"/>
          <circle cx="12" cy="12" r="3" fill="currentColor" stroke="none"/>
        </svg>
        <span>智慧城市照明综合管控平台</span>
      </div>
      <div class="d-header-right">
        <div class="d-sys-info">
          <span class="d-sys-dot"></span>
          系统运行中
        </div>
        <span class="d-clock">{{ clockText }}</span>
        <button class="d-sidebar-btn" @click="toggleSidebar">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <rect x="3" y="3" width="18" height="18" rx="2"/>
            <line x1="9" y1="3" x2="9" y2="21"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- ============ 中间区域：左地图 + 右统计/告警 ============ -->
    <div class="mid-section">
      <!-- 左：地图 + 3D 叠加 -->
      <div class="mid-left">
        <div class="map-wrapper">
          <MapPanel ref="mapPanelRef" :poles="poleList" @map-ready="onMapReady" />
          <ThreeDEffects v-if="leafletMap" :map="leafletMap" :poles="poleList" />
        </div>
      </div>

      <!-- 右：统计卡片 + 告警 -->
      <div class="mid-right">
        <!-- 3×2 统计卡片网格 -->
        <div class="stat-grid">
          <div v-for="card in statCards" :key="card.key" :class="['stat-card', 'stat--' + card.color]">
            <div class="stat-watermark">{{ card.icon }}</div>
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value">{{ formatStat(card.key, overview[card.key]) }}</div>
            <div v-if="card.extra" class="stat-extra">{{ card.extra }}</div>
          </div>
        </div>

        <!-- 实时告警 -->
        <div class="alarm-section">
          <div class="alarm-title-bar">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2L2 18h20L12 2zM12 6v6M12 16h.01"/>
            </svg>
            <span>实时告警</span>
            <span class="alarm-badge" v-if="alarmList.length">{{ alarmList.length }} 条</span>
          </div>
          <div class="alarm-scroll" ref="alarmScrollRef">
            <div v-for="(a, i) in alarmList" :key="a.id || i" class="alarm-item">
              <span class="alarm-dot" :class="'alarm-dot--' + (a.alarmLevel || 3)"></span>
              <span class="alarm-level" :class="'level-' + (a.alarmLevel || 3)">
                {{ levelText[a.alarmLevel] || '一般' }}
              </span>
              <span class="alarm-type">{{ typeText[a.alarmType] || a.alarmType }}</span>
              <span class="alarm-device">{{ a.deviceName || a.deviceId }}</span>
              <span class="alarm-time">{{ a.alarmTime ? a.alarmTime.substring(11, 19) : '' }}</span>
            </div>
            <div v-if="!alarmList.length" class="empty-hint">暂无告警记录</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 底部：图表行 ============ -->
    <el-row :gutter="12" class="chart-row">
      <el-col :xs="24" :md="12" :lg="6">
        <div class="chart-card-item">
          <div class="chart-title-bar">
            <span>告警趋势</span>
            <span class="chart-subtitle">近7天</span>
          </div>
          <div ref="alarmChartRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="12" :lg="6">
        <div class="chart-card-item">
          <div class="chart-title-bar">
            <span>能耗趋势</span>
            <span class="chart-subtitle">近7天</span>
          </div>
          <div ref="energyChartRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="12" :lg="6">
        <div class="chart-card-item">
          <div class="chart-title-bar">
            <span>设备类型分布</span>
          </div>
          <div ref="typeChartRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="12" :lg="6">
        <div class="chart-card-item">
          <div class="chart-title-bar">
            <span>告警分类</span>
          </div>
          <div ref="categoryChartRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, inject, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElNotification } from 'element-plus'
import {
  getOverview,
  getAlarmTrend,
  getEnergyTrend,
  getDeviceTypeDist,
  getAlarmCategory,
  getLatestAlarm,
} from '@/api/other'
import { listPole } from '@/api/device'
import { connectAlarmWS, onAlarmMessage, disconnectAlarmWS } from '@/api/ws'
import MapPanel from '@/components/MapPanel.vue'
import ThreeDEffects from '@/components/ThreeDEffects.vue'

/* ---------------- 指标卡片配置 ---------------- */

const statCards = [
  { key: 'deviceTotal', label: '设备总数', icon: '🔧', color: 'blue', extra: '' },
  { key: 'onlineRate', label: '在线率', icon: '📡', color: 'green', extra: '' },
  { key: 'todayEnergy', label: '今日能耗', icon: '⚡', color: 'orange', extra: 'kWh' },
  { key: 'alarmPending', label: '待处理告警', icon: '🚨', color: 'red', extra: '' },
  { key: 'poleTotal', label: '灯杆总数', icon: '💡', color: 'purple', extra: '' },
  { key: 'workOrderToday', label: '今日工单', icon: '📋', color: 'cyan', extra: '' }
]

/* ---------------- 地图 / 3D ---------------- */

const mapPanelRef = ref(null)
const leafletMap = ref(null)
const poleList = ref([])

function onMapReady(mapInstance) {
  leafletMap.value = mapInstance
}

async function loadPoles() {
  try {
    const res = await listPole()
    poleList.value = res.data || []
  } catch (e) { /* 静默 */ }
}

/* ---------------- 顶栏 ---------------- */

const clockText = ref('')
let clockTimer = null

const sidebarCollapse = inject('sidebarCollapse', null)
function toggleSidebar() {
  if (sidebarCollapse && sidebarCollapse.value !== undefined) {
    sidebarCollapse.value = !sidebarCollapse.value
  }
}

function updateClock() {
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  clockText.value = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

function formatStat(key, val) {
  if (val == null) return 0
  if (key === 'onlineRate') return val
  if (typeof val === 'number' || typeof val === 'string') {
    const n = Number(val)
    if (!isNaN(n)) {
      if (Number.isInteger(n)) return n
      return n.toFixed(1)
    }
  }
  return val
}

/* ---------------- 指标卡片数据 ---------------- */

const overview = reactive({
  deviceTotal: 0,
  deviceOnline: 0,
  deviceFault: 0,
  alarmPending: 0,
  todayEnergy: 0,
  poleTotal: 0,
  workOrderToday: 0,
  onlineRate: '0%'
})

async function loadOverview() {
  try {
    const res = await getOverview()
    Object.assign(overview, res.data || {})
  } catch (e) { /* 静默 */ }
}

/* ---------------- 实时告警 ---------------- */

const alarmList = ref([])
const alarmScrollRef = ref(null)

const typeText = { OFFLINE: '离线告警', OVERVOLTAGE: '过压告警', OVERCURRENT: '过流告警', ABNORMAL: '其他异常' }
const levelText = { 1: '严重', 2: '重要', 3: '一般' }

async function loadLatestAlarm() {
  try {
    const res = await getLatestAlarm(15)
    alarmList.value = res.data || []
  } catch (e) { /* 静默 */ }
}

function onAlarmWsMessage(msg) {
  if (!msg || msg.event !== 'alarm_new') return
  const d = msg.data || {}
  ElNotification({
    title: `新告警 · ${typeText[d.alarmType] || ''}（${levelText[d.alarmLevel] || ''}）`,
    message: d.alarmContent || `设备 ${d.deviceId} 触发告警`,
    type: 'error',
    duration: 6000
  })
  alarmList.value.unshift(d)
  if (alarmList.value.length > 20) alarmList.value.length = 20
  nextTick(() => {
    const el = alarmScrollRef.value
    if (el) el.scrollTop = 0
  })
  overview.alarmPending = (overview.alarmPending || 0) + 1
  loadOverview()
}

/* ---------------- ECharts 通用深色主题配置 ---------------- */

const darkTextColor = '#c8d6e5'
const darkAxisColor = 'rgba(200,214,229,0.2)'
const darkSplitColor = 'rgba(200,214,229,0.08)'

/* ---------------- ECharts 图表 ---------------- */

const alarmChartRef = ref(null)
const energyChartRef = ref(null)
const typeChartRef = ref(null)
const categoryChartRef = ref(null)
let alarmChart = null
let energyChart = null
let typeChart = null
let categoryChart = null

async function loadAlarmTrend() {
  try {
    const res = await getAlarmTrend(7)
    const data = res.data || []
    if (!alarmChart) return
    alarmChart.setOption({
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'rgba(200,214,229,0.15)', textStyle: { color: darkTextColor, fontSize: 11 } },
      grid: { left: 30, right: 8, top: 16, bottom: 20 },
      xAxis: { type: 'category', data: data.map(i => i.date.substring(5)), boundaryGap: false, axisLine: { lineStyle: { color: darkAxisColor } }, axisLabel: { color: darkTextColor, fontSize: 10 }, splitLine: { show: false } },
      yAxis: { type: 'value', minInterval: 1, axisLine: { show: false }, axisLabel: { color: darkTextColor, fontSize: 10 }, splitLine: { lineStyle: { color: darkSplitColor, type: 'dashed' } } },
      series: [{
        name: '告警数', type: 'line', smooth: true, symbol: 'circle', symbolSize: 4,
        data: data.map(i => i.count),
        itemStyle: { color: '#e6a23c' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(230,162,60,0.3)' },
          { offset: 1, color: 'rgba(230,162,60,0.01)' }
        ])},
        lineStyle: { width: 2 }
      }]
    })
  } catch (e) { /* 静默 */ }
}

async function loadEnergyTrend() {
  try {
    const res = await getEnergyTrend(7)
    const data = res.data || []
    if (!energyChart) return
    energyChart.setOption({
      tooltip: { trigger: 'axis', valueFormatter: v => (v || 0).toFixed(1) + ' kWh', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'rgba(200,214,229,0.15)', textStyle: { color: darkTextColor, fontSize: 11 } },
      grid: { left: 38, right: 8, top: 16, bottom: 20 },
      xAxis: { type: 'category', data: data.map(i => i.date.substring(5)), boundaryGap: false, axisLine: { lineStyle: { color: darkAxisColor } }, axisLabel: { color: darkTextColor, fontSize: 10 }, splitLine: { show: false } },
      yAxis: { type: 'value', name: 'kWh', nameTextStyle: { color: darkTextColor, fontSize: 10 }, axisLine: { show: false }, axisLabel: { color: darkTextColor, fontSize: 10 }, splitLine: { lineStyle: { color: darkSplitColor, type: 'dashed' } } },
      series: [{
        name: '用电量', type: 'line', smooth: true, symbol: 'circle', symbolSize: 4,
        data: data.map(i => i.totalEnergy),
        itemStyle: { color: '#409eff' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64,158,255,0.3)' },
          { offset: 1, color: 'rgba(64,158,255,0.01)' }
        ])},
        lineStyle: { width: 2 }
      }]
    })
  } catch (e) { /* 静默 */ }
}

async function loadDeviceTypeDist() {
  try {
    const res = await getDeviceTypeDist()
    const data = res.data || []
    if (!typeChart) return
    const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399']
    typeChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'rgba(200,214,229,0.15)', textStyle: { color: darkTextColor, fontSize: 11 } },
      legend: { bottom: 0, left: 'center', textStyle: { color: darkTextColor, fontSize: 10 } },
      series: [{
        type: 'pie', radius: ['38%', '63%'], center: ['50%', '43%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 4, borderColor: '#0f172a', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold', color: darkTextColor } },
        data: data.map((d, i) => ({
          name: d.typeName || d.typeKey,
          value: d.count,
          itemStyle: { color: colors[i % colors.length] }
        }))
      }]
    })
  } catch (e) { /* 静默 */ }
}

async function loadAlarmCategory() {
  try {
    const res = await getAlarmCategory()
    const data = res.data || []
    if (!categoryChart) return
    const barColors = ['#f56c6c', '#e6a23c', '#409eff', '#909399']
    categoryChart.setOption({
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'rgba(200,214,229,0.15)', textStyle: { color: darkTextColor, fontSize: 11 } },
      grid: { left: 35, right: 8, top: 12, bottom: 30 },
      xAxis: {
        type: 'category',
        data: data.map(d => d.typeName || d.typeKey),
        axisLine: { lineStyle: { color: darkAxisColor } },
        axisLabel: { color: darkTextColor, fontSize: 10, interval: 0 },
        axisTick: { alignWithLabel: true }
      },
      yAxis: { type: 'value', minInterval: 1, axisLine: { show: false }, axisLabel: { color: darkTextColor, fontSize: 10 }, splitLine: { lineStyle: { color: darkSplitColor, type: 'dashed' } } },
      series: [{
        type: 'bar',
        data: data.map((d, i) => ({
          value: d.count,
          itemStyle: { color: barColors[i % barColors.length], borderRadius: [3, 3, 0, 0] }
        })),
        barWidth: '55%'
      }]
    })
  } catch (e) { /* 静默 */ }
}

/* ---------------- 窗口自适应 ---------------- */

function handleResize() {
  alarmChart?.resize()
  energyChart?.resize()
  typeChart?.resize()
  categoryChart?.resize()
}

/* ---------------- 轮询定时器 ---------------- */

let pollTimer = null

function startPolling() {
  pollTimer = setInterval(() => {
    loadOverview()
    loadLatestAlarm()
    loadPoles()
  }, 30000)
}

/* ---------------- 生命周期 ---------------- */

let unsubAlarm = null

onMounted(async () => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)

  // 加载灯杆数据（地图用）
  await loadPoles()

  await nextTick()
  if (alarmChartRef.value) alarmChart = echarts.init(alarmChartRef.value)
  if (energyChartRef.value) energyChart = echarts.init(energyChartRef.value)
  if (typeChartRef.value) typeChart = echarts.init(typeChartRef.value)
  if (categoryChartRef.value) categoryChart = echarts.init(categoryChartRef.value)

  await Promise.all([
    loadOverview(),
    loadAlarmTrend(),
    loadEnergyTrend(),
    loadDeviceTypeDist(),
    loadAlarmCategory(),
    loadLatestAlarm()
  ])

  window.addEventListener('resize', handleResize)
  connectAlarmWS()
  unsubAlarm = onAlarmMessage(onAlarmWsMessage)
  startPolling()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  alarmChart?.dispose()
  energyChart?.dispose()
  typeChart?.dispose()
  categoryChart?.dispose()
  alarmChart = null
  energyChart = null
  typeChart = null
  categoryChart = null
  if (unsubAlarm) unsubAlarm()
  disconnectAlarmWS()
  if (clockTimer) clearInterval(clockTimer)
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
/* ============ 基础大屏背景 ============ */
.dashboard-page {
  padding: 12px 16px;
  background: linear-gradient(135deg, #0a1628 0%, #111d2e 50%, #0d1a2c 100%);
  color: #c8d6e5;
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.dashboard-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(64,158,255,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(64,158,255,0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
  z-index: 0;
}

.dashboard-page > * {
  position: relative;
  z-index: 1;
}

/* ============ 顶部栏 ============ */
@keyframes borderGlow {
  0%, 100% { border-color: rgba(64,158,255,0.2); box-shadow: 0 0 12px rgba(64,158,255,0.05); }
  50% { border-color: rgba(64,158,255,0.5); box-shadow: 0 0 20px rgba(64,158,255,0.12); }
}

.d-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 22px;
  background: linear-gradient(135deg, rgba(26,34,56,0.95), rgba(18,26,46,0.95));
  border: 1px solid rgba(64,158,255,0.2);
  border-radius: 10px;
  margin-bottom: 12px;
  color: #e8edf5;
  animation: borderGlow 4s ease-in-out infinite;
}
.d-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
}
.d-logo {
  color: #409eff;
  flex-shrink: 0;
}
.d-header-right {
  display: flex;
  align-items: center;
  gap: 18px;
  font-size: 13px;
}
.d-sys-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #67c23a;
}
.d-sys-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #67c23a;
  box-shadow: 0 0 6px #67c23a;
  animation: pulseDot 2s ease-in-out infinite;
}
@keyframes pulseDot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
.d-clock {
  font-family: 'Courier New', monospace;
  color: #7ba7e0;
  font-size: 14px;
  letter-spacing: 1px;
}
.d-sidebar-btn {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 6px;
  color: #a0b8d4;
  cursor: pointer;
  padding: 4px 8px;
  display: flex;
  align-items: center;
  transition: all 0.2s;
  font-size: 13px;
}
.d-sidebar-btn:hover {
  background: rgba(255,255,255,0.12);
  color: #e8edf5;
}

/* ============ 中间区域：左地图 + 右统计/告警 - flex:1 撑满剩余空间 ============ */
.mid-section {
  display: flex;
  gap: 12px;
  flex: 1;
  min-height: 0;
  padding-bottom: 12px;
}

.mid-left {
  flex: 0 0 60%;
  position: relative;
  min-height: 0;
}
.map-wrapper {
  width: 100%;
  height: 100%;
  border-radius: 10px;
  overflow: hidden;
  position: relative;
  border: 1px solid rgba(255,255,255,0.06);
  background: rgba(255,255,255,0.02);
}

.mid-right {
  flex: 0 0 calc(40% - 12px);
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: hidden;
  min-height: 0;
}

/* ============ 3×2 统计卡片网格 ============ */
.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr;
  gap: 8px;
  flex-shrink: 0;
}

.stat-card {
  position: relative;
  overflow: hidden;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.06);
  backdrop-filter: blur(4px);
  transition: border-color 0.3s, background 0.3s;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 72px;
}
.stat-card:hover {
  border-color: rgba(255,255,255,0.15);
  background: rgba(255,255,255,0.07);
}
.stat-watermark {
  position: absolute;
  right: 4px;
  bottom: 0;
  font-size: 36px;
  opacity: 0.08;
  line-height: 1;
  pointer-events: none;
  user-select: none;
}
.stat-label {
  font-size: 11px;
  color: rgba(200,214,229,0.6);
  line-height: 1.3;
}
.stat-value {
  font-family: 'Courier New', monospace;
  font-size: 20px;
  font-weight: 700;
  color: #e8edf5;
  line-height: 1.3;
}
.stat-extra {
  font-size: 10px;
  color: rgba(200,214,229,0.35);
  line-height: 1.1;
}

/* ============ 实时告警区 ============ */
.alarm-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  overflow: hidden;
}
.alarm-title-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px 8px;
  font-size: 13px;
  font-weight: 500;
  color: #a0b8d4;
  border-bottom: 1px solid rgba(255,255,255,0.05);
  flex-shrink: 0;
}
.alarm-title-bar svg {
  flex-shrink: 0;
}
.alarm-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 6px 14px 8px;
  scrollbar-width: thin;
  scrollbar-color: rgba(255,255,255,0.1) transparent;
}
.alarm-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 0;
  border-bottom: 1px solid rgba(255,255,255,0.04);
  font-size: 12px;
}
.alarm-item:last-child {
  border-bottom: none;
}
.alarm-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.alarm-dot--1 {
  background: #f56c6c;
  animation: blink 1.2s ease-in-out infinite;
}
.alarm-dot--2 {
  background: #e6a23c;
}
.alarm-dot--3 {
  background: rgba(200,214,229,0.3);
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.2; }
}
.alarm-level {
  flex-shrink: 0;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 3px;
  color: #fff;
}
.level-1 { background: #f56c6c; }
.level-2 { background: #e6a23c; }
.level-3 { background: rgba(200,214,229,0.15); color: #8096b0; }
.alarm-type {
  flex-shrink: 0;
  color: rgba(200,214,229,0.7);
  min-width: 48px;
  font-size: 11px;
}
.alarm-device {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #c8d6e5;
  font-size: 11px;
}
.alarm-time {
  flex-shrink: 0;
  color: rgba(200,214,229,0.35);
  font-size: 11px;
  font-family: 'Courier New', monospace;
}
.empty-hint {
  text-align: center;
  color: rgba(200,214,229,0.3);
  padding: 24px 0;
  font-size: 13px;
}
.alarm-badge {
  margin-left: auto;
  font-size: 10px;
  font-weight: 400;
  color: #f56c6c;
  background: rgba(245,108,108,0.12);
  padding: 2px 6px;
  border-radius: 10px;
}

/* ============ 底部图表行 ============ */
.chart-row {
  flex-shrink: 0;
  margin-bottom: 0;
}
.chart-card-item {
  border-radius: 10px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  padding: 0;
}
.chart-title-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px 4px;
  font-size: 12px;
  font-weight: 500;
  color: #a0b8d4;
}
.chart-subtitle {
  font-size: 10px;
  font-weight: 400;
  color: rgba(200,214,229,0.35);
}
.chart-box {
  width: 100%;
  height: 160px;
}
</style>
