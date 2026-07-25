<!--
  能耗管理页
  - 顶部 4 个统计卡片：今日用电 / 昨日用电 / 本月用电 / 累计用电
  - 中间 ECharts 折线图：用电趋势（日 / 周 / 月切换），调 energyTrend
  - 下方表格：用电记录列表，调 pageEnergy
  - 底部分页
  - ECharts 完整生命周期：init → setOption → resize → onUnmounted dispose
-->
<template>
  <div class="energy-page">
    <!-- ============ 顶部统计卡片 ============ -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat--today">
          <div class="stat-label">今日用电</div>
          <div class="stat-value">{{ stats.today ?? 0 }}</div>
          <div class="stat-extra">kWh</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat--yesterday">
          <div class="stat-label">昨日用电</div>
          <div class="stat-value">{{ stats.yesterday ?? 0 }}</div>
          <div class="stat-extra">kWh</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat--month">
          <div class="stat-label">本月用电</div>
          <div class="stat-value">{{ stats.month ?? 0 }}</div>
          <div class="stat-extra">kWh</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card stat--total">
          <div class="stat-label">累计用电</div>
          <div class="stat-value">{{ stats.total ?? 0 }}</div>
          <div class="stat-extra">kWh</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ============ 用电趋势折线图 ============ -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>用电趋势</span>
          <!-- 日 / 周 / 月切换 -->
          <el-radio-group v-model="trendType" size="small" @change="loadTrend">
            <el-radio-button label="day">日</el-radio-button>
            <el-radio-button label="week">周</el-radio-button>
            <el-radio-button label="month">月</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendChartRef" class="chart-box"></div>
    </el-card>

    <!-- ============ 用电记录表格 ============ -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>用电记录</span>
          <el-button type="info" @click="handleExport">导出报表</el-button>
        </div>
      </template>
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="deviceId" label="设备ID" min-width="140" show-overflow-tooltip />
        <el-table-column prop="poleId" label="所属灯杆ID" width="120" align="center" />
        <el-table-column prop="consumption" label="用电量(kWh)" width="130" align="right" />
        <el-table-column prop="voltage" label="电压(V)" width="110" align="right" />
        <el-table-column prop="current" label="电流(A)" width="110" align="right" />
        <el-table-column prop="power" label="功率(W)" width="110" align="right" />
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
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { pageEnergy, energyTrend, energyStatistics, exportEnergyReport } from '@/api/other'

/* ---------------- 顶部统计卡片 ---------------- */

// 后端返回：{ today, yesterday, month, total }
const stats = reactive({
  today: 0,
  yesterday: 0,
  month: 0,
  total: 0
})

// 加载统计数据
async function loadStatistics() {
  try {
    const res = await energyStatistics({})
    Object.assign(stats, res.data || {})
  } catch (e) {
    /* 静默 */
  }
}

/* ---------------- 用电趋势折线图 ---------------- */

const trendChartRef = ref(null)
let trendChart = null

// 趋势类型：day / week / month
const trendType = ref('day')
// 趋势数据：[{ date, energy }]
const trendData = ref([])

// 加载趋势数据
// 后端需要 deviceId/startTime/endTime，前端简化处理：查第一个设备最近7天
async function loadTrend() {
  try {
    // 先获取一条能耗记录，拿到 deviceId
    const pageRes = await pageEnergy({ current: 1, size: 1 })
    const page = pageRes.data || {}
    const list = page.records || page.list || []
    if (list.length === 0) {
      trendData.value = []
      updateTrendChart()
      return
    }
    const deviceId = list[0].deviceId || 'D-001'

    // 根据 trendType 计算时间范围
    const now = new Date()
    const endTime = now.toISOString().slice(0, 19)  // 2026-07-22T16:00:00
    let startTime
    if (trendType.value === 'day') {
      startTime = new Date(now - 24 * 60 * 60 * 1000).toISOString().slice(0, 19)
    } else if (trendType.value === 'week') {
      startTime = new Date(now - 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 19)
    } else {
      startTime = new Date(now - 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 19)
    }

    const res = await energyTrend({ deviceId, startTime, endTime })
    // 后端返回 List<EnergyRecord>，转为前端需要的 [{ date, energy }]
    const records = res.data || []
    trendData.value = records.map(r => ({
      date: r.recordTime,
      energy: r.consumption
    }))
    updateTrendChart()
  } catch (e) {
    trendData.value = []
    updateTrendChart()
  }
}

// 渲染折线图
function updateTrendChart() {
  if (!trendChart) return
  const dates = trendData.value.map(i => i.date)
  const values = trendData.value.map(i => i.energy)
  trendChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: v => v + ' kWh' },
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value', name: 'kWh' },
    series: [
      {
        name: '用电量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: values,
        itemStyle: { color: '#409eff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.4)' },
            { offset: 1, color: 'rgba(64,158,255,0.02)' }
          ])
        },
        lineStyle: { width: 2 }
      }
    ]
  })
}

/* ---------------- 表格 & 分页 ---------------- */

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
    const res = await pageEnergy(query)
    // 兼容 MyBatis-Plus(records) 与 PageHelper(list) 两种返回结构
    const page = res.data || {}
    tableData.value = page.records || page.list || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

/* ---------------- 导出报表 ---------------- */

async function handleExport() {
  try {
    // 获取设备ID（如果有的话）
    let deviceId = null
    if (tableData.value.length > 0) {
      deviceId = tableData.value[0].deviceId
    }
    
    const res = await exportEnergyReport({ deviceId })
    if (!res || !res.data) {
      ElMessage.error('导出失败')
      return
    }
    
    // 创建下载链接
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '能耗报表.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + (e.message || '未知错误'))
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
  await Promise.all([loadStatistics(), loadTrend(), loadData()])
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
.energy-page {
  padding: 0;
}

.stat-row {
  margin-bottom: 16px;
}
.stat-card {
  border-left: 4px solid #409eff;
}
.stat-card.stat--today { border-left-color: #409eff; }
.stat-card.stat--yesterday { border-left-color: #67c23a; }
.stat-card.stat--month { border-left-color: #e6a23c; }
.stat-card.stat--total { border-left-color: #f56c6c; }
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
