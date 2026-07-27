<!--
  数据大盘（大屏版）
  - 顶部：标题 + 实时时钟
  - 第二行：6 个核心指标卡片（带图标水印 + 等宽数字）
  - 中间：环境快照（左）+ 实时告警滚动（右，带状态指示灯）
  - 底部：告警趋势 / 能耗趋势 / 设备类型 / 告警分类 四张图表（深色主题适配）
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

    <!-- ============ 指标卡片行 ============ -->
    <el-row :gutter="12" class="stat-row">
      <el-col :xs="8" :sm="8" :md="4" v-for="card in statCards" :key="card.key">
        <div :class="['stat-card', 'stat--' + card.color]">
          <div class="stat-watermark">{{ card.icon }}</div>
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value">{{ formatStat(card.key, overview[card.key]) }}</div>
          <div v-if="card.extra" class="stat-extra">{{ card.extra }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- ============ 中间区域：环境 + 告警 ============ -->
    <el-row :gutter="12" class="mid-row">
      <!-- 左侧：环境快照 -->
      <el-col :xs="24" :md="10">
        <div class="section-card">
          <div class="section-title-bar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
            </svg>
            <span>环境监测快照</span>
          </div>
          <div class="env-grid">
            <div class="env-item" v-for="e in envItems" :key="e.key">
              <span class="env-label">{{ e.label }}</span>
              <span class="env-value" :style="{ color: e.color }">{{ envData[e.key] ?? '-' }}{{ e.unit }}</span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧：实时告警 -->
      <el-col :xs="24" :md="14">
        <div class="section-card">
          <div class="section-title-bar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
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
      </el-col>
    </el-row>

    <!-- ============ 图表行 ============ -->
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
  getLatestEnv
} from '@/api/other'
import { connectAlarmWS, onAlarmMessage, disconnectAlarmWS } from '@/api/ws'

/* ---------------- 指标卡片配置 ---------------- */

const statCards = [
  { key: 'deviceTotal', label: '设备总数', icon: '🔧', color: 'blue', extra: '' },
  { key: 'onlineRate', label: '在线率', icon: '📡', color: 'green', extra: '' },
  { key: 'todayEnergy', label: '今日能耗', icon: '⚡', color: 'orange', extra: 'kWh' },
  { key: 'alarmPending', label: '待处理告警', icon: '🚨', color: 'red', extra: '' },
  { key: 'poleTotal', label: '灯杆总数', icon: '💡', color: 'purple', extra: '' },
  { key: 'workOrderToday', label: '今日工单', icon: '📋', color: 'cyan', extra: '' }
]

/* ---------------- 顶栏 ---------------- */

const clockText = ref('')
let clockTimer = null

// 从 layout 注入侧边栏折叠控制
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


// 数据格式化：数字保留 1 位小数，百分数原样显示
function formatStat(key, val) {
  if (val == null) return 0
  if (key === 'onlineRate') return val // 已经是 "95.00%"
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

/* ---------------- 环境快照 ---------------- */

const envItems = [
  { key: 'temperature', label: '温度', unit: '℃', color: '#e6a23c' },
  { key: 'humidity', label: '湿度', unit: '%', color: '#409eff' },
  { key: 'pm25', label: 'PM2.5', unit: '', color: '#67c23a' },
  { key: 'noise', label: '噪声', unit: 'dB', color: '#f56c6c' }
]
const envData = reactive({ temperature: '-', humidity: '-', pm25: '-', noise: '-' })

async function loadLatestEnv() {
  try {
    const res = await getLatestEnv()
    if (res.data) {
      Object.keys(envData).forEach(k => {
        envData[k] = res.data[k] != null ? res.data[k] : '-'
      })
    }
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

// WebSocket 新告警回调
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

// ---------- 告警趋势 ----------
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

// ---------- 能耗趋势 ----------
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

// ---------- 设备类型分布（饼图） ----------
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

// ---------- 告警分类（柱状图） ----------
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
    loadLatestEnv()
  }, 30000)
}

/* ---------------- 生命周期 ---------------- */

onMounted(async () => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)

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
    loadLatestAlarm(),
    loadLatestEnv()
  ])

  loadAlarmTrend()
  loadEnergyTrend()
  loadDeviceTypeDist()
  loadAlarmCategory()

  window.addEventListener('resize', handleResize)
  connectAlarmWS()
  unsubAlarm = onAlarmMessage(onAlarmWsMessage)
  startPolling()
})

let unsubAlarm = null

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
  padding: 16px 20px;
  background: linear-gradient(135deg, #0a1628 0%, #111d2e 50%, #0d1a2c 100%);
  min-height: calc(100vh - 84px);
  color: #c8d6e5;
  position: relative;
  overflow: auto;
}

/* 背景装饰网格线 */
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

/* ============ 顶部栏（带边框流光动画） ============ */
@keyframes borderGlow {
  0%, 100% { border-color: rgba(64,158,255,0.2); box-shadow: 0 0 12px rgba(64,158,255,0.05); }
  50% { border-color: rgba(64,158,255,0.5); box-shadow: 0 0 20px rgba(64,158,255,0.12); }
}

.d-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 24px;
  background: linear-gradient(135deg, rgba(26,34,56,0.95), rgba(18,26,46,0.95));
  border: 1px solid rgba(64,158,255,0.2);
  border-radius: 10px;
  margin-bottom: 14px;
  color: #e8edf5;
  animation: borderGlow 4s ease-in-out infinite;
}
.d-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 19px;
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
  gap: 20px;
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
  font-size: 15px;
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

/* ============ 指标卡片 ============ */
.stat-row {
  margin-bottom: 14px;
}
.stat-card {
  position: relative;
  overflow: hidden;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.06);
  backdrop-filter: blur(4px);
  transition: border-color 0.3s, background 0.3s;
  text-align: center;
  /* 所有卡片固定同高 + flex 居中，内容不一致也不影响 */
  height: 110px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.stat-card:hover {
  border-color: rgba(255,255,255,0.15);
  background: rgba(255,255,255,0.07);
}
.stat-watermark {
  position: absolute;
  right: 6px;
  bottom: 2px;
  font-size: 50px;
  opacity: 0.08;
  line-height: 1;
  pointer-events: none;
  user-select: none;
}
.stat-label {
  font-size: 12px;
  color: rgba(200,214,229,0.6);
  line-height: 1.4;
}
.stat-value {
  font-family: 'Courier New', monospace;
  font-size: 26px;
  font-weight: 700;
  color: #e8edf5;
  line-height: 1.3;
}
.stat-extra {
  font-size: 11px;
  color: rgba(200,214,229,0.35);
  line-height: 1.2;
}

/* ============ 中间区域卡片 ============ */
.mid-row {
  margin-bottom: 14px;
}
.section-card {
  height: 270px;
  border-radius: 10px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  padding: 0;
  display: flex;
  flex-direction: column;
}
.section-title-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #a0b8d4;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.section-title-bar svg {
  flex-shrink: 0;
}
.section-card .env-grid,
.section-card .alarm-scroll {
  flex: 1;
  padding: 12px 16px;
}

/* 环境网格 */
.env-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-content: center;
  height: 100%;
  padding: 0;
}
.env-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.04);
  border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.05);
  padding: 16px 8px;
}
.env-label {
  font-size: 12px;
  color: rgba(200,214,229,0.5);
  margin-bottom: 6px;
}
.env-value {
  font-size: 24px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
}

/* 告警列表 */
.alarm-scroll {
  overflow-y: auto;
  padding: 0;
  scrollbar-width: thin;
  scrollbar-color: rgba(255,255,255,0.1) transparent;
}
.alarm-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
  border-bottom: 1px solid rgba(255,255,255,0.04);
  font-size: 13px;
}
.alarm-item:last-child {
  border-bottom: none;
}
.alarm-dot {
  width: 7px;
  height: 7px;
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
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 3px;
  color: #fff;
}
.level-1 { background: #f56c6c; }
.level-2 { background: #e6a23c; }
.level-3 { background: rgba(200,214,229,0.15); color: #8096b0; }
.alarm-type {
  flex-shrink: 0;
  color: rgba(200,214,229,0.7);
  min-width: 56px;
}
.alarm-device {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #c8d6e5;
}
.alarm-time {
  flex-shrink: 0;
  color: rgba(200,214,229,0.35);
  font-size: 12px;
  font-family: 'Courier New', monospace;
}
.empty-hint {
  text-align: center;
  color: rgba(200,214,229,0.3);
  padding: 40px 0;
  font-size: 14px;
}
.alarm-badge {
  margin-left: auto;
  font-size: 11px;
  font-weight: 400;
  color: #f56c6c;
  background: rgba(245,108,108,0.12);
  padding: 2px 8px;
  border-radius: 10px;
}

/* ============ 图表区 ============ */
.chart-row {
  margin-bottom: 0;
}
.chart-card-item {
  margin-bottom: 14px;
  border-radius: 10px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  padding: 0;
}
.chart-title-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px 6px;
  font-size: 13px;
  font-weight: 500;
  color: #a0b8d4;
}
.chart-subtitle {
  font-size: 11px;
  font-weight: 400;
  color: rgba(200,214,229,0.35);
}
.chart-box {
  width: 100%;
  height: 200px;
}
</style>
