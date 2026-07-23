<!--
  数据大盘页
  - 顶部 4 个指标卡片：设备总数 / 在线数 / 告警数 / 今日能耗，用不同主题色区分
  - 下方左侧：ECharts 折线图，近 7 天告警趋势，调 getAlarmTrend()
  - 下方右侧：ECharts 饼图，设备状态占比（在线 / 离线 / 故障）
  - ECharts 完整生命周期：init → setOption → resize 监听 → onUnmounted dispose
-->
<template>
  <div class="dashboard-page">
    <!-- ============ 顶部指标卡片 ============ -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat--total">
          <div class="stat-label">设备总数</div>
          <div class="stat-value">{{ overview.deviceTotal ?? 0 }}</div>
          <div class="stat-extra">在线率 {{ overview.onlineRate || '0%' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat--online">
          <div class="stat-label">在线设备</div>
          <div class="stat-value">{{ overview.deviceOnline ?? 0 }}</div>
          <div class="stat-extra">实时在线状态</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat--alarm">
          <div class="stat-label">告警数量</div>
          <div class="stat-value">{{ overview.alarmPending ?? 0 }}</div>
          <div class="stat-extra">待处理 {{ overview.alarmPending ?? 0 }} 条</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat--energy">
          <div class="stat-label">今日能耗</div>
          <div class="stat-value">{{ overview.todayRecords ?? 0 }}</div>
          <div class="stat-extra">今日能耗记录数</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ============ 图表区 ============ -->
    <el-row :gutter="16" class="chart-row">
      <!-- 左侧：告警趋势折线图 -->
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>近 7 天告警趋势</span>
              <el-tag size="small" type="warning" effect="plain">告警数 / 天</el-tag>
            </div>
          </template>
          <div ref="alarmChartRef" class="chart-box"></div>
        </el-card>
      </el-col>

      <!-- 右侧：设备状态饼图 -->
      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>设备状态分布</span>
              <el-tag size="small" type="success" effect="plain">占比</el-tag>
            </div>
          </template>
          <div ref="statusChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getOverview, getAlarmTrend } from '@/api/other'

/* ---------------- 指标卡片数据 ---------------- */

// overview 由后端返回，结构示例：
// { deviceTotal, deviceOnline, alarmPending, todayRecords, onlineRate }
const overview = reactive({
  deviceTotal: 0,
  deviceOnline: 0,
  alarmPending: 0,
  todayRecords: 0,
  onlineRate: '0%'
})

const offlineDevices = computed(() => Math.max((overview.deviceTotal || 0) - (overview.deviceOnline || 0), 0))

// 加载大盘概览数据
async function loadOverview() {
  try {
    const res = await getOverview()
    Object.assign(overview, res.data || {})
    // 概览数据更新后，同步刷新饼图
    updateStatusChart()
  } catch (e) {
    // 接口失败时静默处理（拦截器已弹错误提示）
  }
}

/* ---------------- ECharts 实例管理 ---------------- */

// 折线图 / 饼图的 DOM 引用
const alarmChartRef = ref(null)
const statusChartRef = ref(null)

// ECharts 实例（init 之后赋值，dispose 之后置空）
let alarmChart = null
let statusChart = null

// 告警趋势数据（getAlarmTrend 返回 [{ date, count }]）
const alarmTrendData = ref([])

// 加载近 7 天告警趋势
async function loadAlarmTrend() {
  try {
    const res = await getAlarmTrend()
    alarmTrendData.value = res.data || []
    updateAlarmChart()
  } catch (e) {
    /* 静默 */
  }
}

/* ---------------- 折线图：告警趋势 ---------------- */

function updateAlarmChart() {
  if (!alarmChart) return
  const dates = alarmTrendData.value.map(i => i.date)
  const counts = alarmTrendData.value.map(i => i.count)
  alarmChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '告警数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: counts,
        itemStyle: { color: '#e6a23c' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(230,162,60,0.4)' },
            { offset: 1, color: 'rgba(230,162,60,0.02)' }
          ])
        },
        lineStyle: { width: 2 }
      }
    ]
  })
}

/* ---------------- 饼图：设备状态分布 ---------------- */

function updateStatusChart() {
  if (!statusChart) return
  const data = [
    { name: '在线', value: overview.deviceOnline || 0, itemStyle: { color: '#67c23a' } },
    { name: '离线', value: offlineDevices.value, itemStyle: { color: '#909399' } },
    { name: '故障', value: 0, itemStyle: { color: '#f56c6c' } }
  ]
  statusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, left: 'center' },
    series: [
      {
        name: '设备状态',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%' },
        data
      }
    ]
  })
}

/* ---------------- 窗口尺寸变化自适应 ---------------- */

// 节流处理窗口大小变化，使图表自适应
function handleResize() {
  alarmChart?.resize()
  statusChart?.resize()
}

/* ---------------- 生命周期 ---------------- */

onMounted(async () => {
  // 等待 DOM 渲染完成再初始化 ECharts 实例
  await nextTick()
  if (alarmChartRef.value) {
    alarmChart = echarts.init(alarmChartRef.value)
  }
  if (statusChartRef.value) {
    statusChart = echarts.init(statusChartRef.value)
  }
  // 加载数据
  await Promise.all([loadOverview(), loadAlarmTrend()])
  // 数据可能比实例先 ready，所以加载完后再渲染一次
  updateAlarmChart()
  updateStatusChart()
  // 监听窗口变化
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  // 释放 ECharts 实例，避免内存泄漏
  window.removeEventListener('resize', handleResize)
  alarmChart?.dispose()
  statusChart?.dispose()
  alarmChart = null
  statusChart = null
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

/* 指标卡片行 */
.stat-row {
  margin-bottom: 16px;
}

/* 卡片整体样式 */
.stat-card {
  border-left: 4px solid #409eff;
  position: relative;
  overflow: hidden;
}
.stat-card.stat--total { border-left-color: #409eff; }
.stat-card.stat--online { border-left-color: #67c23a; }
.stat-card.stat--alarm { border-left-color: #e6a23c; }
.stat-card.stat--energy { border-left-color: #f56c6c; }

.stat-label {
  font-size: 14px;
  color: #909399;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 8px 0 4px;
}
.stat-extra {
  font-size: 12px;
  color: #c0c4cc;
}

/* 图表行 */
.chart-row {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart-box {
  width: 100%;
  height: 320px;
}
</style>
