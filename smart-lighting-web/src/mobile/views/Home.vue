<template>
  <div class="home-page">
    <!-- 顶部问候 -->
    <div class="home-header">
      <div class="home-greeting">👋 欢迎回来</div>
      <div class="home-user">{{ userStore.nickname || userStore.username }}</div>
      <div class="home-actions">
        <span class="home-refresh" @click="refresh">⟳</span>
        <span class="home-logout" @click="doLogout">⏻</span>
      </div>
    </div>

    <!-- 统计卡片网格 -->
    <div class="stats-grid">
      <div
        class="glass-card stat-card"
        v-for="s in stats"
        :key="s.label"
        @click="s.to && router.push(s.to)"
      >
        <div class="stat-icon" v-html="s.icon"></div>
        <div class="stat-value">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <!-- 待处理告警 -->
    <div class="section-label">⚠️ 待处理告警</div>
    <div class="glass-card alarm-section">
      <van-empty v-if="alarms.length === 0" description="暂无告警" />
      <div v-else>
        <div
          class="alarm-item"
          v-for="(a, i) in alarms"
          :key="a.id"
          @click="handleAlarm(a)"
        >
          <div class="alarm-dot" :class="severityClass(a.severity)"></div>
          <div class="alarm-info">
            <div class="alarm-title">{{ a.content || a.deviceName || '未知告警' }}</div>
            <div class="alarm-time">{{ a.createTime }}</div>
          </div>
          <van-tag round plain :class="'severity-' + (a.severity ?? 0)">{{ severityText(a.severity) }}</van-tag>
        </div>
      </div>
    </div>

    <!-- 快速入口 -->
    <div class="section-label">🔧 快速入口</div>
    <div class="quick-grid">
      <div class="glass-card quick-item" @click="router.push('/control')">
        <div class="quick-icon">⚡</div>
        <span>照明控制</span>
      </div>
      <div class="glass-card quick-item" @click="router.push('/pole')">
        <div class="quick-icon">📍</div>
        <span>灯杆查询</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getOverview, getLatestAlarm } from '@/api/other'
import { showToast, showConfirmDialog } from 'vant'
import { handleAlarm as handleAlarmApi } from '@/api/other'

const router = useRouter()
const userStore = useUserStore()

async function doLogout() {
  const ok = await showConfirmDialog({
    title: '退出登录',
    message: '确定要退出当前账号？',
    confirmButtonText: '退出'
  }).catch(() => false)
  if (ok) {
    await userStore.logout()
    router.replace('/login')
  }
}

const stats = ref([])
const alarms = ref([])
let timer = null

function severityClass(s) {
  return s === 2 ? 'severity-error' : s === 1 ? 'severity-warn' : 'severity-info'
}
function severityText(s) {
  return s === 2 ? '严重' : s === 1 ? '一般' : '提示'
}

async function loadData() {
  try {
    const [overviewRes, alarmRes] = await Promise.all([
      getOverview().catch(() => ({ data: {} })),
      getLatestAlarm(5).catch(() => ({ data: [] }))
    ])
    const d = overviewRes.data || {}
    stats.value = [
      { icon: '📡', label: '设备总数', value: d.deviceTotal ?? '-', to: '' },
      { icon: '⚠️', label: '待处理告警', value: d.pendingAlarm ?? '-', to: '/alarm' },
      { icon: '🏮', label: '灯杆总数', value: d.poleTotal ?? '-', to: '/pole' },
      { icon: '📋', label: '今日工单', value: d.workOrderToday ?? '-', to: '' }
    ]
    alarms.value = (alarmRes.data || []).slice(0, 5)
  } catch { /* 静默 */ }
}

async function handleAlarm(a) {
  const confirm = await showConfirmDialog({
    title: '处理告警',
    message: `确认处理「${a.content || a.deviceName}」？`,
    confirmButtonText: '标记已处理'
  }).catch(() => false)
  if (confirm) {
    try {
      await handleAlarmApi({ id: a.id, status: 2, handleResult: '移动端处理' })
      showToast('已处理')
      await loadData()
    } catch (e) {
      showToast(e.message)
    }
  }
}

function refresh() {
  showToast('刷新中...')
  loadData()
}

onMounted(() => {
  loadData()
  timer = setInterval(loadData, 30000)
})
onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style scoped>
.home-page { padding: 20px 16px; }
.home-header { display: flex; align-items: center; gap: 8px; margin-bottom: 20px; }
.home-greeting { font-size: 14px; color: rgba(255,255,255,.5); }
.home-user { font-size: 20px; font-weight: 600; color: rgba(255,255,255,.92); flex:1; }
.home-actions { display: flex; gap: 4px; }
.home-refresh, .home-logout {
  font-size: 20px; color: rgba(255,255,255,.4); cursor: pointer;
  padding: 4px 8px; transition: color .2s;
}
.home-logout { font-size: 18px; }
.home-logout:active { color: #f56c6c; }

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 20px;
}
.stat-card { padding: 16px; text-align: center; }
.stat-icon { font-size: 24px; margin-bottom: 6px; }
.stat-value { font-size: 26px; font-weight: 700; color: rgba(255,255,255,.92); }
.stat-label { font-size: 12px; color: rgba(255,255,255,.45); margin-top: 2px; }

.section-label { font-size: 14px; font-weight: 600; color: rgba(255,255,255,.65); margin-bottom: 10px; }
.alarm-section { padding: 0; overflow: hidden; margin-bottom: 20px; }
.alarm-item {
  display: flex; align-items: center; padding: 13px 16px;
  border-bottom: 0.5px solid rgba(255,255,255,.04);
}
.alarm-item:last-child { border-bottom: none; }
.alarm-dot { width: 7px; height: 7px; border-radius: 50%; margin-right: 10px; flex-shrink: 0; }
.severity-error { background: #f56c6c; }
.severity-warn { background: #e6a23c; }
.severity-info { background: #909399; }
.alarm-info { flex: 1; min-width: 0; }
.alarm-title { font-size: 14px; color: rgba(255,255,255,.85); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.alarm-time { font-size: 11px; color: rgba(255,255,255,.35); margin-top: 2px; }
:deep(.severity-2) { color: #f56c6c !important; }
:deep(.severity-1) { color: #e6a23c !important; }
:deep(.severity-0) { color: rgba(255,255,255,.45) !important; }

.quick-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 10px;
}
.quick-item {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 8px; padding: 20px 12px;
  font-size: 13px; color: rgba(255,255,255,.85);
}
.quick-icon { font-size: 28px; }
</style>
