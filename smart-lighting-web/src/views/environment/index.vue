<!--
  环境监测页
  - 顶部最新数据卡片：温度 / 湿度 / PM2.5 / PM10 / 噪声 / 光照（调 latestEnv 或 pageEnv 取第一条）
  - 中间 ECharts 折线图：环境数据趋势（可切换温度 / 湿度 / PM2.5），调 envTrend
  - 下方表格：历史数据列表，调 pageEnv
  - 底部分页
  - ECharts 完整生命周期：init → setOption → resize → onUnmounted dispose
-->
<template>
  <div class="env-page">
    <!-- ============ 顶部最新数据卡片 ============ -->
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="item in envMetrics" :key="item.key" :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card" :style="{ borderLeftColor: item.color }">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">
            {{ latest[item.key] ?? '--' }}
            <span class="stat-unit">{{ item.unit }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ============ 环境趋势折线图 ============ -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>环境数据趋势</span>
          <!-- 指标切换：温度 / 湿度 / PM2.5 -->
          <el-radio-group v-model="trendMetric" size="small" @change="loadTrend">
            <el-radio-button label="temperature">温度</el-radio-button>
            <el-radio-button label="humidity">湿度</el-radio-button>
            <el-radio-button label="pm25">PM2.5</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendChartRef" class="chart-box"></div>
    </el-card>

    <!-- ============ 历史数据表格 ============ -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>历史数据</span>
          <el-button type="primary" :icon="Refresh" @click="loadData">刷新</el-button>
        </div>
      </template>
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="poleName" label="所属灯杆" width="140" show-overflow-tooltip />
        <el-table-column prop="temperature" label="温度(℃)" width="100" align="right" />
        <el-table-column prop="humidity" label="湿度(%)" width="100" align="right" />
        <el-table-column prop="pm25" label="PM2.5" width="100" align="right" />
        <el-table-column prop="pm10" label="PM10" width="100" align="right" />
        <el-table-column prop="noise" label="噪声(dB)" width="100" align="right" />
        <el-table-column prop="illumination" label="光照(lx)" width="100" align="right" />
        <el-table-column prop="recordTime" label="记录时间" width="170" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { pageEnv, latestEnv, envTrend } from '@/api/other'

/* ---------------- 顶部最新数据 ---------------- */

// 环境指标配置（label / 字段名 / 单位 / 主题色）
const envMetrics = [
  { key: 'temperature', label: '温度', unit: '℃', color: '#f56c6c' },
  { key: 'humidity', label: '湿度', unit: '%', color: '#409eff' },
  { key: 'pm25', label: 'PM2.5', unit: 'μg/m³', color: '#e6a23c' },
  { key: 'pm10', label: 'PM10', unit: 'μg/m³', color: '#909399' },
  { key: 'noise', label: '噪声', unit: 'dB', color: '#9c27b0' },
  { key: 'illumination', label: '光照', unit: 'lx', color: '#67c23a' }
]

// 最新环境数据
const latest = reactive({
  temperature: null,
  humidity: null,
  pm25: null,
  pm10: null,
  noise: null,
  illumination: null,
  poleId: null
})

// 加载最新数据：优先调 latestEnv，失败则从 pageEnv 第一条取
async function loadLatest() {
  try {
    // 默认取第一个灯杆的最新数据（poleId 传 1 或空，由后端约定）
    const res = await latestEnv(1)
    Object.assign(latest, res.data || {})
  } catch (e) {
    // 回退方案：从分页接口取第一条
    try {
      const res = await pageEnv({ current: 1, size: 1 })
      const page = res.data || {}
      const list = page.records || page.list || []
      if (list.length > 0) {
        Object.assign(latest, list[0])
      }
    } catch (err) {
      /* 静默 */
    }
  }
}

/* ---------------- 趋势折线图 ---------------- */

const trendChartRef = ref(null)
let trendChart = null

// 当前趋势指标：temperature / humidity / pm25
const trendMetric = ref('temperature')
// 趋势数据：[{ recordTime, temperature, humidity, pm25, ... }]
const trendData = ref([])

async function loadTrend() {
  try {
    // 后端需要 poleId/startTime/endTime，前端简化处理：查第一个灯杆最近7天
    const poleId = latest.poleId || 1

    // 计算时间范围：最近7天
    const now = new Date()
    const endTime = now.toISOString().slice(0, 19)
    const startTime = new Date(now - 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 19)

    const res = await envTrend({ poleId, startTime, endTime })
    trendData.value = res.data || []
    updateTrendChart()
  } catch (e) {
    trendData.value = []
    updateTrendChart()
  }
}

// 渲染折线图
function updateTrendChart() {
  if (!trendChart) return
  const times = trendData.value.map(i => i.recordTime || i.time)
  const values = trendData.value.map(i => Number(i[trendMetric.value]))
  // 当前指标配置（取单位用于 yAxis 名称）
  const cfg = envMetrics.find(m => m.key === trendMetric.value) || {}
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: times, boundaryGap: false },
    yAxis: { type: 'value', name: cfg.unit || '' },
    series: [
      {
        name: cfg.label || '',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: values,
        itemStyle: { color: cfg.color || '#409eff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: (cfg.color || '#409eff') + '66' },
            { offset: 1, color: (cfg.color || '#409eff') + '05' }
          ])
        },
        lineStyle: { width: 2 }
      }
    ]
  })
}

/* ---------------- 历史数据表格 ---------------- */

const query = reactive({
  current: 1,
  size: 10
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await pageEnv(query)
    const page = res.data || {}
    tableData.value = page.records || page.list || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

/* ---------------- 窗口自适应 ---------------- */

function handleResize() {
  trendChart?.resize()
}

/* ---------------- 生命周期 ---------------- */

onMounted(async () => {
  await nextTick()
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
  }
  await Promise.all([loadLatest(), loadTrend(), loadData()])
  updateTrendChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  trendChart = null
})
</script>

<style scoped>
.env-page {
  padding: 0;
}

.stat-row {
  margin-bottom: 16px;
}
.stat-card {
  border-left: 4px solid #409eff;
}
.stat-label {
  font-size: 14px;
  color: #909399;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-top: 8px;
}
.stat-unit {
  font-size: 12px;
  color: #c0c4cc;
  font-weight: normal;
  margin-left: 4px;
}

.chart-card {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart-box {
  width: 100%;
  height: 300px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
