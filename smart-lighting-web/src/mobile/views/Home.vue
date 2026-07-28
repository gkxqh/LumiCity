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
          <div class="alarm-dot" :class="severityClass(a.alarmLevel)"></div>
          <div class="alarm-info">
            <div class="alarm-title">{{ a.alarmContent || a.deviceName || '未知告警' }}</div>
            <div class="alarm-time">{{ a.createTime }}</div>
          </div>
          <van-tag round plain class="alarm-tag" :class="'severity-' + (a.alarmLevel ?? 3)">{{ severityText(a.alarmLevel) }}</van-tag>
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
      <div class="glass-card quick-item" @click="router.push('/workorder')">
        <div class="quick-icon">📋</div>
        <span>工单运维</span>
      </div>
    </div>

    <!-- ============ 分配处理人表单（待处理告警 → 处理中，后端自动生成告警工单） ============ -->
    <van-popup
      v-model:show="assignShow"
      position="bottom"
      round
      teleport="body"
      @closed="resetAssignForm"
    >
      <div class="sheet">
        <div class="sheet-title">分配处理人</div>
        <van-form @submit="handleAssignSubmit">
          <van-cell-group inset>
            <van-field
              :model-value="assignForm.alarmContent"
              label="告警内容"
              type="textarea"
              rows="2"
              autosize
              readonly
            />
            <van-field
              :model-value="assigneeLabel"
              label="处理人"
              placeholder="请选择运维人员"
              is-link
              readonly
              :rules="[{ required: true, message: '请选择处理人' }]"
              @click="assigneePickerShow = true"
            />
          </van-cell-group>
          <div class="sheet-tip">提交后告警进入「处理中」，并自动生成告警工单指派给处理人</div>
          <div class="sheet-btns">
            <button type="button" class="glass-btn sheet-btn" @click="assignShow = false">取 消</button>
            <van-button
              class="sheet-btn"
              type="primary"
              round
              block
              native-type="submit"
              :loading="assignSubmitting"
            >确 定</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <!-- 处理人选择器 -->
    <van-popup v-model:show="assigneePickerShow" position="bottom" round teleport="body">
      <van-picker
        :columns="assigneeOptions.map(u => ({ text: u.label, value: u.value }))"
        @confirm="({ selectedOptions }) => { assignForm.handleUser = selectedOptions[0]?.value ?? ''; assigneePickerShow = false }"
        @cancel="assigneePickerShow = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { showToast, showSuccessToast, showConfirmDialog } from 'vant'
import {
  getOverview,
  getLatestAlarm,
  handleAlarm as handleAlarmApi,
  listUsersByRole
} from '@/api/other'

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
  return s === 1 ? 'severity-error' : s === 2 ? 'severity-warn' : 'severity-info'
}
function severityText(s) {
  return s === 1 ? '严重' : s === 2 ? '重要' : '一般'
}

async function refreshOverview() {
  // 仅刷新顶部 4 个统计卡片（不重拉告警列表）
  try {
    const overviewRes = await getOverview().catch(() => ({ data: {} }))
    const d = overviewRes.data || {}
    const map = {
      '设备总数': d.deviceTotal,
      '待处理告警': d.alarmPending,   // 后端字段名是 alarmPending，不是 pendingAlarm
      '灯杆总数': d.poleTotal,
      '今日工单': d.workOrderToday
    }
    stats.value.forEach(s => {
      if (map[s.label] !== undefined && map[s.label] !== null) {
        s.value = map[s.label]
      }
    })
  } catch { /* 静默 */ }
}

async function loadData() {
  try {
    const [overviewRes, alarmRes] = await Promise.all([
      getOverview().catch(() => ({ data: {} })),
      // 多拉一些，客户端过滤掉已处理 / 处理中的，再取前 5 条"未处理"告警
      getLatestAlarm(20).catch(() => ({ data: [] }))
    ])
    const d = overviewRes.data || {}
    stats.value = [
      { icon: '📡', label: '设备总数', value: d.deviceTotal ?? '-', to: '' },
      { icon: '⚠️', label: '待处理告警', value: d.alarmPending ?? '-', to: '/alarm' },
      { icon: '🏮', label: '灯杆总数', value: d.poleTotal ?? '-', to: '/pole' },
      { icon: '📋', label: '今日工单', value: d.workOrderToday ?? '-', to: '/workorder' }
    ]
    // 只显示 status === 0（未处理）的前 5 条
    alarms.value = (alarmRes.data || [])
      .filter(a => (a.status ?? 0) === 0)
      .slice(0, 5)
  } catch { /* 静默 */ }
}

/* ---------------- 分配处理人（对齐 PC 端闭环流程） ----------------
 * 待处理告警不能直接"标记已处理"，正确流程：
 * ① 分配处理人（status=1，后端自动创建告警工单并指派）
 * ② 工单模块-告警工单 Tab 填处理备注提交（工单完成）
 * ③ 告警页填写处理意见（status=2 已闭环）
 */

const assignShow = ref(false)
const assignSubmitting = ref(false)
const assigneePickerShow = ref(false)
const assigneeOptions = ref([])

const assignForm = reactive({
  id: null,
  alarmContent: '',
  handleUser: ''   // 处理人 username（对齐 PC 端 alarm/handle 的 handleUser 参数）
})

const assigneeLabel = computed(() =>
  assigneeOptions.value.find(u => u.value === assignForm.handleUser)?.label || ''
)

function resetAssignForm() {
  assignForm.id = null
  assignForm.alarmContent = ''
  assignForm.handleUser = ''
}

async function loadAssignees() {
  try {
    const res = await listUsersByRole('INSPECTOR')
    // label 显示昵称，value 用 username（后端 handleUser 存的是用户名）
    assigneeOptions.value = (res.data || []).map(u => ({
      label: u.nickname || u.username,
      value: u.username
    }))
  } catch { /* 静默 */ }
}

// 点击待处理告警：打开分配处理人表单
function handleAlarm(a) {
  resetAssignForm()
  assignForm.id = a.id
  assignForm.alarmContent = a.alarmContent || a.deviceName || '未知告警'
  assignShow.value = true
}

async function handleAssignSubmit() {
  assignSubmitting.value = true
  try {
    // status=1：告警进入处理中，后端自动创建告警工单并指派给 handleUser
    await handleAlarmApi({ id: assignForm.id, status: 1, handleUser: assignForm.handleUser })
    assignShow.value = false
    // ① 立即从本地"待处理"列表移除（该告警已进入处理中）
    alarms.value = alarms.value.filter(x => x.id !== assignForm.id)
    // ② 刷新顶部"待处理告警"数字
    refreshOverview()
    showSuccessToast('已生成告警工单')
    // ③ 引导用户前往工单模块继续处理
    const go = await showConfirmDialog({
      title: '分配成功',
      message: '告警已进入「处理中」，并自动生成告警工单。\n是否前往工单模块处理？',
      confirmButtonText: '去处理',
      cancelButtonText: '稍后再说'
    }).catch(() => false)
    if (go) router.push('/workorder?tab=alarm')
  } catch (e) {
    showToast(e.message || '分配失败')
  } finally {
    assignSubmitting.value = false
  }
}

function refresh() {
  showToast('刷新中...')
  loadData()
}

onMounted(() => {
  loadData()
  loadAssignees()
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
.severity-info { background: #90caf9; }
.alarm-info { flex: 1; min-width: 0; }
.alarm-title { font-size: 14px; color: rgba(255,255,255,.85); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.alarm-time { font-size: 11px; color: rgba(255,255,255,.35); margin-top: 2px; }
:deep(.severity-1) { color: #f56c6c !important; }
:deep(.severity-2) { color: #e6a23c !important; }
:deep(.severity-3) { color: #90caf9 !important; }
:deep(.alarm-tag) { font-size: 18px; padding: 4px 12px; line-height: 1.4; }

.quick-grid {
  display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px;
}
.quick-item {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 8px; padding: 20px 12px;
  font-size: 13px; color: rgba(255,255,255,.85);
}
.quick-icon { font-size: 28px; }

/* 底部表单弹层（与工单模块风格一致） */
.sheet { padding: 20px 0 24px; }
.sheet-title {
  text-align: center; font-size: 16px; font-weight: 600;
  color: rgba(255,255,255,.9); margin-bottom: 14px;
}
.sheet-tip {
  padding: 10px 24px 0; font-size: 12px; line-height: 1.5;
  color: rgba(255,255,255,.45);
}
.sheet-btns { display: flex; gap: 12px; padding: 18px 16px 0; }
.sheet-btn { flex: 1; height: 44px; }
.sheet :deep(.van-cell-group--inset) {
  background: rgba(255,255,255,.04) !important;
  border-radius: 14px;
}
.sheet :deep(.van-cell::after) {
  border-color: rgba(255,255,255,.06);
}
</style>
