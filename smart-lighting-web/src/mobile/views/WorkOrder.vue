<!--
  移动端工单运维（表单）页 — 参照电脑端 views/workorder/index.vue
  - 顶部 Tab：运维工单 / 告警工单
  - 筛选：状态 + 设备编号搜索
  - 列表：下拉刷新 + 上拉加载
  - 表单（底部弹层）：
      新增工单（运维Tab）：标题 / 类型 / 优先级 / 关联设备 / 问题描述
      新建告警表单（告警Tab）：选择待处理告警 + 委派处理人
        → 调 /alarm/handle(status=1)，告警变「处理中」，后端自动生成关联工单
      派单：处理人（下拉）/ 备注
      处理：处理备注；若为告警关联工单，提交后自动闭环告警（已完成）
  流转逻辑与电脑端一致：
      运维工单：待处理→派单，处理中→处理，已完成→终点
      告警闭环：告警待处理→新建表单(委派) 处理中→表单模块-告警填备注提交→告警已完成
-->
<template>
  <div class="wo-page">
    <!-- Tab：运维工单 / 告警工单 -->
    <van-tabs v-model:active="activeTab" @change="onTabChange">
      <van-tab title="运维工单" name="manual" />
      <van-tab title="告警工单" name="alarm" />
    </van-tabs>

    <!-- 搜索 + 筛选 -->
    <div class="section-padding">
      <van-search
        v-model="query.deviceId"
        placeholder="搜索设备编号"
        shape="round"
        @search="onSearch"
        @clear="onSearch"
      />
    </div>
    <div class="section-padding filter-row">
      <div class="glass-card filter-chip">
        <span class="filter-label">状态</span>
        <select v-model="statusVal" class="native-select" @change="onSearch">
          <option value="">全部</option>
          <option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
        </select>
        <span class="filter-value">{{ statusLabel }}</span>
        <span class="filter-arrow">›</span>
      </div>
      <div class="glass-card filter-chip">
        <span class="filter-label">类型</span>
        <select v-model="query.orderType" class="native-select" @change="onSearch">
          <option value="">全部</option>
          <option v-for="t in typeOptions" :key="t.value" :value="t.value">{{ t.label }}</option>
        </select>
        <span class="filter-value">{{ typeMap[query.orderType] || '全部' }}</span>
        <span class="filter-arrow">›</span>
      </div>
    </div>

    <!-- 列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadData"
      >
        <div class="list-padding">
          <van-empty v-if="finished && list.length === 0" description="暂无工单" />
          <div class="glass-card wo-card" :class="'wo-card--' + (w.priority ?? 3)" v-for="w in list" :key="w.id">
            <div class="wo-head">
              <span class="wo-title">{{ w.title }}</span>
              <van-tag round plain :class="'wo-s-' + (w.status ?? 0)">
                {{ statusMap[w.status] || '-' }}
              </van-tag>
            </div>
            <div class="wo-meta">
              <span class="wo-no">{{ w.orderNo }}</span>
              <van-tag round plain :class="'wo-p-' + (w.priority ?? 3)">
                {{ priorityMap[w.priority] || '-' }}优先级
              </van-tag>
            </div>
            <div class="wo-info">
              <span>{{ typeMap[w.orderType] || w.orderType }}</span>
              <span v-if="w.deviceId">设备 {{ w.deviceId }}</span>
            </div>
            <div class="wo-info">
              <span>{{ w.createTime }}</span>
            </div>
            <!-- 操作按钮：待处理→派单；处理中→处理 -->
            <div class="wo-actions" v-if="w.status === 0 || w.status === 1">
              <button
                v-if="w.status === 0"
                class="glass-btn wo-btn wo-btn-primary"
                @click="openAssign(w)"
              >派 单</button>
              <button
                v-else-if="w.status === 1"
                class="glass-btn wo-btn wo-btn-warn"
                @click="openHandle(w)"
              >处 理</button>
            </div>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>

    <!-- 悬浮新增按钮：运维Tab=新增工单；告警Tab=从待处理告警新建表单 -->
    <div class="wo-fab glass-card" @click="onFabClick">＋</div>

    <!-- ============ 新增工单表单 ============ -->
    <van-popup
      v-model:show="addShow"
      position="bottom"
      round
      teleport="body"
      :style="{ maxHeight: '85%' }"
      @closed="resetAddForm"
    >
      <div class="sheet">
        <div class="sheet-title">新增工单</div>
        <van-form @submit="handleAddSubmit">
          <van-cell-group inset>
            <van-field
              v-model="addForm.title"
              label="工单标题"
              placeholder="请输入工单标题"
              :rules="[{ required: true, message: '请输入工单标题' }]"
            />
            <van-field
              :model-value="typeMap[addForm.orderType] || ''"
              label="工单类型"
              placeholder="请选择工单类型"
              is-link
              readonly
              :rules="[{ required: true, message: '请选择工单类型' }]"
              @click="typePickerShow = true"
            />
            <van-field
              :model-value="priorityMap[addForm.priority] || ''"
              label="优先级"
              placeholder="请选择优先级"
              is-link
              readonly
              :rules="[{ required: true, message: '请选择优先级' }]"
              @click="priorityPickerShow = true"
            />
            <van-field
              v-model="addForm.deviceId"
              label="关联设备"
              placeholder="设备编号，如 L-2024-0001（可选）"
            />
            <van-field
              v-model="addForm.description"
              label="问题描述"
              type="textarea"
              rows="3"
              autosize
              placeholder="请描述工单问题"
              :rules="[{ required: true, message: '请描述工单问题' }]"
            />
          </van-cell-group>
          <div class="sheet-btns">
            <button type="button" class="glass-btn sheet-btn" @click="addShow = false">取 消</button>
            <van-button
              class="sheet-btn"
              type="primary"
              round
              block
              native-type="submit"
              :loading="addSubmitting"
            >确 定</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <!-- 工单类型选择器 -->
    <van-popup v-model:show="typePickerShow" position="bottom" round teleport="body">
      <van-picker
        :columns="typeOptions.map(t => ({ text: t.label, value: t.value }))"
        @confirm="({ selectedOptions }) => { addForm.orderType = selectedOptions[0].value; typePickerShow = false }"
        @cancel="typePickerShow = false"
      />
    </van-popup>

    <!-- 优先级选择器 -->
    <van-popup v-model:show="priorityPickerShow" position="bottom" round teleport="body">
      <van-picker
        :columns="priorityOptions.map(p => ({ text: p.label, value: p.value }))"
        @confirm="({ selectedOptions }) => { addForm.priority = selectedOptions[0].value; priorityPickerShow = false }"
        @cancel="priorityPickerShow = false"
      />
    </van-popup>

    <!-- ============ 新建告警表单（告警 → 表单 → 委派处理人） ============ -->
    <van-popup
      v-model:show="alarmCreateShow"
      position="bottom"
      round
      teleport="body"
      :style="{ maxHeight: '85%' }"
      @closed="resetAlarmCreateForm"
    >
      <div class="sheet">
        <div class="sheet-title">新建告警表单</div>
        <div class="sheet-tip">选择待处理告警并委派处理人，提交后告警进入「处理中」，系统自动生成关联工单</div>
        <van-form @submit="handleAlarmCreateSubmit">
          <van-cell-group inset>
            <van-field
              :model-value="alarmLabel"
              label="待处理告警"
              placeholder="请选择待处理告警"
              is-link
              readonly
              :rules="[{ required: true, message: '请选择待处理告警' }]"
              @click="openAlarmPicker"
            />
            <van-field
              v-if="selectedAlarm"
              :model-value="selectedAlarm.alarmContent || selectedAlarm.deviceName || ''"
              label="告警内容"
              type="textarea"
              rows="2"
              autosize
              readonly
            />
            <van-field
              :model-value="handleUserLabel"
              label="委派处理人"
              placeholder="请选择处理人"
              is-link
              readonly
              :rules="[{ required: true, message: '请选择处理人' }]"
              @click="handleUserPickerShow = true"
            />
          </van-cell-group>
          <div class="sheet-btns">
            <button type="button" class="glass-btn sheet-btn" @click="alarmCreateShow = false">取 消</button>
            <van-button
              class="sheet-btn"
              type="primary"
              round
              block
              native-type="submit"
              :loading="alarmCreateSubmitting"
            >创建表单</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <!-- 待处理告警选择器 -->
    <van-popup v-model:show="alarmPickerShow" position="bottom" round teleport="body">
      <van-picker
        :columns="pendingAlarmOptions"
        @confirm="({ selectedOptions }) => { alarmCreateForm.alarmId = selectedOptions[0]?.value ?? null; alarmPickerShow = false }"
        @cancel="alarmPickerShow = false"
      />
    </van-popup>

    <!-- 委派处理人选择器（告警表单用，值为用户名） -->
    <van-popup v-model:show="handleUserPickerShow" position="bottom" round teleport="body">
      <van-picker
        :columns="inspectorUserOptions"
        @confirm="({ selectedOptions }) => { alarmCreateForm.handleUser = selectedOptions[0]?.value ?? ''; handleUserPickerShow = false }"
        @cancel="handleUserPickerShow = false"
      />
    </van-popup>

    <!-- ============ 派单表单 ============ -->
    <van-popup
      v-model:show="assignShow"
      position="bottom"
      round
      teleport="body"
      @closed="resetAssignForm"
    >
      <div class="sheet">
        <div class="sheet-title">派 单</div>
        <van-form @submit="handleAssignSubmit">
          <van-cell-group inset>
            <van-field :model-value="assignForm.title" label="工单标题" readonly />
            <van-field
              :model-value="assigneeLabel"
              label="处理人"
              placeholder="请选择处理人"
              is-link
              readonly
              :rules="[{ required: true, message: '请选择处理人' }]"
              @click="assigneePickerShow = true"
            />
            <van-field
              v-model="assignForm.remark"
              label="备注"
              type="textarea"
              rows="2"
              autosize
              placeholder="派单备注（可选）"
            />
          </van-cell-group>
          <div class="sheet-btns">
            <button type="button" class="glass-btn sheet-btn" @click="assignShow = false">取 消</button>
            <van-button
              class="sheet-btn"
              type="primary"
              round
              block
              native-type="submit"
              :loading="assignSubmitting"
            >派 单</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <!-- 处理人选择器 -->
    <van-popup v-model:show="assigneePickerShow" position="bottom" round teleport="body">
      <van-picker
        :columns="assigneeOptions.map(a => ({ text: a.label, value: a.value }))"
        @confirm="({ selectedOptions }) => { assignForm.assigneeId = selectedOptions[0]?.value ?? null; assigneePickerShow = false }"
        @cancel="assigneePickerShow = false"
      />
    </van-popup>

    <!-- ============ 处理表单 ============ -->
    <van-popup
      v-model:show="handleShow"
      position="bottom"
      round
      teleport="body"
      @closed="resetHandleForm"
    >
      <div class="sheet">
        <div class="sheet-title">处理工单</div>
        <van-form @submit="handleWorkOrderSubmit">
          <van-cell-group inset>
            <van-field :model-value="handleForm.title" label="工单标题" readonly />
            <van-field
              v-model="handleForm.handleRemark"
              label="处理备注"
              type="textarea"
              rows="3"
              autosize
              placeholder="请填写处理过程 / 结果"
              :rules="[{ required: true, message: '请填写处理备注' }]"
            />
          </van-cell-group>
          <div class="sheet-btns">
            <button type="button" class="glass-btn sheet-btn" @click="handleShow = false">取 消</button>
            <van-button
              class="sheet-btn"
              type="primary"
              round
              block
              native-type="submit"
              :loading="handleSubmitting"
            >提交处理</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import {
  pageWorkOrder,
  addWorkOrder,
  assignWorkOrder,
  handleWorkOrder,
  listUsersByRole,
  pageAlarm,
  handleAlarm as handleAlarmApi
} from '@/api/other'

const router = useRouter()
const route = useRoute()

/* ---------------- 字典（与电脑端保持一致） ---------------- */

const typeOptions = [
  { label: '巡检工单', value: 'INSPECT' },
  { label: '维修工单', value: 'REPAIR' }
]
const typeMap = Object.fromEntries(typeOptions.map(i => [i.value, i.label]))

const statusOptions = [
  { label: '待处理', value: 0 },
  { label: '处理中', value: 1 },
  { label: '已完成', value: 2 }
]
const statusMap = { 0: '待处理', 1: '处理中', 2: '已完成' }

const priorityOptions = [
  { label: '高', value: 1 },
  { label: '中', value: 2 },
  { label: '低', value: 3 }
]
const priorityMap = Object.fromEntries(priorityOptions.map(i => [i.value, i.label]))

/* ---------------- Tab / 查询 / 列表 ---------------- */

const activeTab = ref('manual')   // manual=运维工单 alarm=告警工单

// statusVal 用字符串给原生 select 绑定，query.status 提交时转数字
const statusVal = ref('')
const statusLabel = computed(() =>
  statusVal.value === '' ? '全部' : statusMap[Number(statusVal.value)]
)

const query = reactive({
  current: 1,
  size: 10,
  orderType: '',
  deviceId: '',
  alarmId: 0
})

const list = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

async function loadData() {
  // alarmId：与电脑端一致，1=告警工单 0=运维创建工单
  query.alarmId = activeTab.value === 'alarm' ? 1 : 0
  if (refreshing.value) { query.current = 1; finished.value = false }
  loading.value = true
  try {
    const res = await pageWorkOrder({
      ...query,
      status: statusVal.value === '' ? undefined : Number(statusVal.value),
      deviceId: query.deviceId || undefined,
      orderType: query.orderType || undefined
    })
    const rows = res.data?.records || res.data?.list || []
    if (refreshing.value || query.current === 1) {
      list.value = rows
      refreshing.value = false
    } else {
      list.value = [...list.value, ...rows]
    }
    finished.value = rows.length < query.size
    query.current++
  } catch (e) {
    showToast(e.message || '加载失败')
    finished.value = true
    refreshing.value = false
  } finally {
    loading.value = false
  }
}

function reload() {
  query.current = 1
  finished.value = false
  list.value = []
  loadData()
}
function onSearch() { reload() }
function onRefresh() { query.current = 1; finished.value = false; loadData() }
function onTabChange() { reload() }

/* ---------------- 处理人下拉 ---------------- */

const assigneeOptions = ref([])
const rawInspectors = ref([])   // 原始巡检人员列表（含 username，供告警表单委派用）
async function loadAssignees() {
  try {
    const res = await listUsersByRole('INSPECTOR')
    rawInspectors.value = res.data || []
    assigneeOptions.value = rawInspectors.value.map(u => ({
      label: u.nickname || u.username,
      value: u.id
    }))
  } catch { /* 静默 */ }
}

/* ---------------- 新增工单 ---------------- */

const addShow = ref(false)
const addSubmitting = ref(false)
const typePickerShow = ref(false)
const priorityPickerShow = ref(false)

const addForm = reactive({
  title: '',
  orderType: '',
  priority: null,
  deviceId: '',
  description: ''
})

function resetAddForm() {
  addForm.title = ''
  addForm.orderType = ''
  addForm.priority = null
  addForm.deviceId = ''
  addForm.description = ''
}

function openAdd() {
  resetAddForm()
  addShow.value = true
}

/* 悬浮按钮：按 Tab 分流 */
function onFabClick() {
  if (activeTab.value === 'alarm') openAlarmCreate()
  else openAdd()
}

async function handleAddSubmit() {
  addSubmitting.value = true
  try {
    await addWorkOrder({
      ...addForm,
      orderNo: 'TEMP-' + Date.now(),
      status: 0
    })
    showSuccessToast('工单创建成功')
    addShow.value = false
    reload()
  } catch (e) {
    showToast(e.message || '创建失败')
  } finally {
    addSubmitting.value = false
  }
}

/* ---------------- 新建告警表单（待处理告警 → 表单 + 委派 → 处理中） ---------------- */

const alarmCreateShow = ref(false)
const alarmCreateSubmitting = ref(false)
const alarmPickerShow = ref(false)
const handleUserPickerShow = ref(false)

const pendingAlarms = ref([])   // 待处理告警列表（status=0）

const alarmCreateForm = reactive({
  alarmId: null,
  handleUser: ''
})

const pendingAlarmOptions = computed(() =>
  pendingAlarms.value.map(a => ({
    text: `${a.alarmContent || a.deviceName || '告警'}（${(a.createTime || '').slice(5, 16).replace('T', ' ')}）`,
    value: a.id
  }))
)

const inspectorUserOptions = computed(() =>
  rawInspectors.value.map(u => ({
    text: u.nickname || u.username,
    value: u.username
  }))
)

const selectedAlarm = computed(() =>
  pendingAlarms.value.find(a => a.id === alarmCreateForm.alarmId) || null
)
const alarmLabel = computed(() =>
  selectedAlarm.value
    ? (selectedAlarm.value.alarmContent || selectedAlarm.value.deviceName || `告警 #${selectedAlarm.value.id}`)
    : ''
)
const handleUserLabel = computed(() =>
  inspectorUserOptions.value.find(u => u.value === alarmCreateForm.handleUser)?.text || ''
)

async function loadPendingAlarms() {
  try {
    const res = await pageAlarm({ current: 1, size: 50, status: 0 })
    pendingAlarms.value = res.data?.records || res.data?.list || []
  } catch { pendingAlarms.value = [] }
}

function resetAlarmCreateForm() {
  alarmCreateForm.alarmId = null
  alarmCreateForm.handleUser = ''
}

async function openAlarmCreate(presetAlarmId) {
  resetAlarmCreateForm()
  await loadPendingAlarms()
  if (pendingAlarms.value.length === 0 && !presetAlarmId) {
    showToast('当前没有待处理告警')
    return
  }
  if (presetAlarmId != null) {
    const id = Number(presetAlarmId)
    alarmCreateForm.alarmId = pendingAlarms.value.some(a => a.id === id) ? id : null
  }
  alarmCreateShow.value = true
}

function openAlarmPicker() {
  if (pendingAlarms.value.length === 0) {
    showToast('当前没有待处理告警')
    return
  }
  alarmPickerShow.value = true
}

async function handleAlarmCreateSubmit() {
  alarmCreateSubmitting.value = true
  try {
    // 与电脑端一致：/alarm/handle status=1 + handleUser
    // 后端置告警「处理中」并自动生成关联工单（已委派给处理人）
    await handleAlarmApi({
      id: alarmCreateForm.alarmId,
      status: 1,
      handleUser: alarmCreateForm.handleUser
    })
    showSuccessToast('表单已创建，告警进入处理中')
    alarmCreateShow.value = false
    activeTab.value = 'alarm'
    reload()
  } catch (e) {
    showToast(e.message || '创建失败')
  } finally {
    alarmCreateSubmitting.value = false
  }
}

/* ---------------- 派单 ---------------- */

const assignShow = ref(false)
const assignSubmitting = ref(false)
const assigneePickerShow = ref(false)

const assignForm = reactive({
  id: null,
  title: '',
  assigneeId: null,
  remark: ''
})

const assigneeLabel = computed(() =>
  assigneeOptions.value.find(a => a.value === assignForm.assigneeId)?.label || ''
)

function resetAssignForm() {
  assignForm.id = null
  assignForm.title = ''
  assignForm.assigneeId = null
  assignForm.remark = ''
}

function openAssign(row) {
  resetAssignForm()
  assignForm.id = row.id
  assignForm.title = row.title
  assignShow.value = true
}

async function handleAssignSubmit() {
  assignSubmitting.value = true
  try {
    await assignWorkOrder(assignForm.id, { assigneeId: assignForm.assigneeId })
    showSuccessToast('派单成功')
    assignShow.value = false
    reload()
  } catch (e) {
    showToast(e.message || '派单失败')
  } finally {
    assignSubmitting.value = false
  }
}

/* ---------------- 处理 ---------------- */

const handleShow = ref(false)
const handleSubmitting = ref(false)

const handleForm = reactive({
  id: null,
  title: '',
  alarmId: null,     // 关联告警 id（告警工单才有）
  handleRemark: ''
})

function resetHandleForm() {
  handleForm.id = null
  handleForm.title = ''
  handleForm.alarmId = null
  handleForm.handleRemark = ''
}

function openHandle(row) {
  resetHandleForm()
  handleForm.id = row.id
  handleForm.title = row.title
  handleForm.alarmId = row.alarmId ?? null
  handleShow.value = true
}

async function handleWorkOrderSubmit() {
  handleSubmitting.value = true
  try {
    // 1. 工单处理完成（处理中 → 已完成）
    await handleWorkOrder(handleForm.id, { handleRemark: handleForm.handleRemark })
    // 2. 告警关联工单：以处理备注闭环告警（处理中 → 已完成）
    if (handleForm.alarmId) {
      try {
        await handleAlarmApi({
          id: handleForm.alarmId,
          status: 2,
          handleResult: handleForm.handleRemark
        })
        showSuccessToast('处理完成，告警已闭环')
      } catch (e) {
        // 工单已完成但告警闭环失败（如权限/状态原因），提示但不回滚
        showToast(e.message || '工单已完成，但告警闭环失败')
      }
    } else {
      showSuccessToast('工单处理完成')
    }
    handleShow.value = false
    reload()
  } catch (e) {
    showToast(e.message || '操作失败')
  } finally {
    handleSubmitting.value = false
  }
}

/* ---------------- 初始化 ---------------- */

onMounted(async () => {
  await loadAssignees()
  // 支持从主页/告警页跳转进来直接建表单：/workorder?tab=alarm&create=1&alarmId=xx
  if (route.query.tab === 'alarm') activeTab.value = 'alarm'
  if (route.query.create === '1') {
    openAlarmCreate(route.query.alarmId)
  }
  // 列表由 van-list 首次 @load 自动触发，无需手动调用
})
</script>

<style scoped>
.wo-page { min-height: 100vh; padding-bottom: 80px; }
.section-padding { padding: 8px 16px; }
.list-padding { padding: 0 16px 8px; }

/* 筛选行 */
.filter-row { display: flex; gap: 10px; padding-top: 0; }
.filter-chip {
  position: relative; flex: 1;
  display: flex; align-items: center; padding: 10px 14px;
}
.filter-label { font-size: 12px; color: rgba(255,255,255,.5); margin-right: 8px; }
.filter-value { flex: 1; text-align: right; font-size: 12px; color: rgba(255,255,255,.85); }
.filter-arrow { font-size: 16px; color: rgba(255,255,255,.3); margin-left: 4px; }
.native-select {
  position: absolute; inset: 0; width: 100%; height: 100%;
  opacity: 0; font-size: 16px; cursor: pointer; z-index: 2;
}

/* 工单卡片 */
.wo-card { padding: 14px; margin-bottom: 10px; position: relative; overflow: hidden; }
/* 右侧按优先级渐变半透明染色：高红 / 中橙 / 低黄；右边缘最深、向左渐隐 */
.wo-card::before {
  content: '';
  position: absolute; inset: 0;
  pointer-events: none;
  z-index: 0;
}
.wo-card--1::before { background: linear-gradient(to left, rgba(245,108,108,.30), transparent 64%); }
.wo-card--2::before { background: linear-gradient(to left, rgba(239,138,47,.36), transparent 64%); }
.wo-card--3::before { background: linear-gradient(to left, rgba(255,216,77,.26), transparent 64%); }
/* 卡片内容抬到染色层之上，保证文字清晰可读 */
.wo-card > * { position: relative; z-index: 1; }
.wo-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.wo-title {
  flex: 1; font-size: 15px; font-weight: 600; color: rgba(255,255,255,.9);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.wo-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; }
.wo-no { font-size: 11px; color: rgba(255,255,255,.4); font-family: monospace; }
.wo-info {
  display: flex; justify-content: space-between; margin-top: 5px;
  font-size: 12px; color: rgba(255,255,255,.55);
}

/* 状态 / 优先级 颜色 */
:deep(.wo-s-0) { color: #f56c6c !important; }
:deep(.wo-s-1) { color: #e6a23c !important; }
:deep(.wo-s-2) { color: #67c23a !important; }
/* 优先级标签：与灯杆查询/照明控制一致，提亮文字+边框色+微弱背景填充，
   避免深色玻璃卡上看不清（低优先级原 rgba(255,255,255,.5) 几乎不可见，改成黄色） */
:deep(.wo-p-1) { color: #f56c6c !important; border-color: #f56c6c !important; background-color: rgba(245,108,108,.18) !important; }
:deep(.wo-p-2) { color: #ef8a2f !important; border-color: #ef8a2f !important; background-color: rgba(239,138,47,.18) !important; }
:deep(.wo-p-3) { color: #ffd84d !important; border-color: #ffd84d !important; background-color: rgba(255,216,77,.18) !important; }

/* 卡片内操作按钮 */
.wo-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 10px; }
.wo-btn { height: 32px; padding: 0 18px; font-size: 13px; border-radius: 10px; }
.wo-btn-primary { color: #7ec8e3; border-color: rgba(126,200,227,.35); }
.wo-btn-warn { color: #e6a23c; border-color: rgba(230,162,60,.35); }

/* 悬浮新增按钮 */
.wo-fab {
  position: fixed; right: 20px; bottom: 90px; z-index: 10;
  width: 52px; height: 52px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; color: #7ec8e3; cursor: pointer;
  background: rgba(126, 200, 227, 0.12);
  border: 0.5px solid rgba(126, 200, 227, 0.3);
}
.wo-fab:active { transform: scale(.94); }

/* 底部表单弹层 */
.sheet { padding: 20px 0 24px; }
.sheet-title {
  text-align: center; font-size: 16px; font-weight: 600;
  color: rgba(255,255,255,.9); margin-bottom: 14px;
}
.sheet-tip {
  font-size: 12px; color: rgba(255,255,255,.45);
  text-align: center; line-height: 1.5;
  padding: 0 24px; margin: -6px 0 12px;
}
.sheet-btns { display: flex; gap: 12px; padding: 18px 16px 0; }
.sheet-btn { flex: 1; height: 44px; }

/* 弹层内表单字段透明化，贴合玻璃风 */
.sheet :deep(.van-cell-group--inset) {
  background: rgba(255,255,255,.04) !important;
  border-radius: 14px;
}
.sheet :deep(.van-cell::after) {
  border-color: rgba(255,255,255,.06);
}
</style>
