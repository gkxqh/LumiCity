<!--
  移动端告警管理页 — 处理流程对齐电脑端 views/alarm/index.vue（三段式闭环）：
    未处理(0) →[分配处理人]→ 处理中(1) →[工单模块处理完工单]→ [填写处理意见]→ 已闭环(2) →[查看结果]
  - 分配处理人：status=1 + handleUser，后端自动创建告警工单并指派
  - 填写处理意见：先校验关联工单已完成（未完成则引导去工单模块），再 status=2 + handleResult
  - 查看结果：只读展示 处理人 / 完成时间 / 处理结果
-->
<template>
  <div class="alarm-page">
    <van-tabs v-model:active="tabActive" sticky @change="() => loadAlarms(true)">
      <van-tab title="全部" />
      <van-tab title="未处理" />
      <van-tab title="处理中" />
      <van-tab title="已闭环" />
    </van-tabs>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadAlarms"
      >
        <div class="alarm-list">
          <div class="glass-card alarm-card" v-for="a in list" :key="a.id">
            <div class="alarm-header">
              <van-tag round plain class="alarm-tag" :class="'status-' + a.status">{{ statusText(a.status) }}</van-tag>
              <van-tag round plain class="alarm-tag" :class="'sev-' + (a.alarmLevel ?? 3)" style="margin-left:4px">
                {{ sevText(a.alarmLevel) }}
              </van-tag>
              <span class="alarm-date">{{ formatTime(a.createTime) }}</span>
            </div>
            <div class="alarm-body">{{ a.alarmContent || a.deviceName || '告警' }}</div>
            <!-- 操作按钮：按状态走不同流程（与电脑端一致） -->
            <div class="alarm-footer">
              <button
                v-if="a.status === 0"
                class="glass-btn handle-btn btn-assign"
                @click="openAssign(a)"
              >分配处理人</button>
              <button
                v-else-if="a.status === 1"
                class="glass-btn handle-btn btn-handle"
                @click="openHandle(a)"
              >填写处理意见</button>
              <button
                v-else
                class="glass-btn handle-btn btn-view"
                @click="openView(a)"
              >查看结果</button>
            </div>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>

    <!-- 浮动新增 -->
    <div class="fab" @click="showAdd = true">
      <span class="fab-icon">+</span>
    </div>

    <!-- 新增弹窗 -->
    <van-action-sheet v-model:show="showAdd" title="新增告警" round>
      <div class="sheet-body">
        <van-form @submit="onAdd">
          <van-cell-group inset>
            <van-field
              v-model="form.deviceName"
              label="设备名称"
              placeholder="输入设备名称"
              :rules="[{ required: true, message: '请填写设备名称' }]"
            />
            <van-field
              v-model="form.alarmContent"
              label="告警内容"
              type="textarea"
              rows="3"
              maxlength="200"
              show-word-limit
              placeholder="描述告警详情"
              :rules="[{ required: true, message: '请填写告警内容' }]"
            />
            <van-field name="alarmLevel" label="告警等级">
              <template #input>
                <van-radio-group v-model="form.alarmLevel" direction="horizontal">
                  <van-radio :name="1">严重</van-radio>
                  <van-radio :name="2">重要</van-radio>
                  <van-radio :name="3">一般</van-radio>
                </van-radio-group>
              </template>
            </van-field>
          </van-cell-group>
          <div style="margin:20px 24px 40px">
            <button class="glass-btn" style="width:100%;height:44px;font-size:15px" :disabled="submitting">
              {{ submitting ? '提交中...' : '提交告警' }}
            </button>
          </div>
        </van-form>
      </div>
    </van-action-sheet>

    <!-- ============ 分配处理人（未处理 → 处理中，后端自动生成告警工单） ============ -->
    <van-popup v-model:show="assignShow" position="bottom" round teleport="body" @closed="resetAssignForm">
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

    <!-- ============ 填写处理意见（处理中 → 已闭环，需关联工单已完成） ============ -->
    <van-popup v-model:show="handleShow" position="bottom" round teleport="body" @closed="resetHandleForm">
      <div class="sheet">
        <div class="sheet-title">填写处理意见</div>
        <van-form @submit="handleRemarkSubmit">
          <van-cell-group inset>
            <van-field
              :model-value="handleForm.alarmContent"
              label="告警内容"
              type="textarea"
              rows="2"
              autosize
              readonly
            />
            <van-field
              v-model="handleForm.handleRemark"
              label="处理意见"
              type="textarea"
              rows="3"
              autosize
              placeholder="请填写处理意见 / 处置过程"
              :rules="[{ required: true, message: '请填写处理意见' }]"
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
            >确认闭环</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <!-- ============ 查看结果（已闭环，只读） ============ -->
    <van-popup v-model:show="viewShow" position="bottom" round teleport="body">
      <div class="sheet">
        <div class="sheet-title">处理结果</div>
        <van-cell-group inset>
          <van-field :model-value="viewForm.alarmContent" label="告警内容" type="textarea" rows="2" autosize readonly />
          <van-field :model-value="viewForm.handleUser" label="处理人" readonly />
          <van-field :model-value="formatTime(viewForm.handleTime)" label="完成时间" readonly />
          <van-field :model-value="viewForm.handleResult" label="处理结果" type="textarea" rows="2" autosize readonly />
        </van-cell-group>
        <div class="sheet-btns">
          <button type="button" class="glass-btn sheet-btn" @click="viewShow = false">关 闭</button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showConfirmDialog } from 'vant'
import {
  pageAlarm,
  addAlarm,
  handleAlarm as handleAlarmApi,
  listUsersByRole,
  getWorkOrderByAlarm
} from '@/api/other'

const router = useRouter()

const tabActive = ref(0)
const list = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const showAdd = ref(false)
const submitting = ref(false)

const form = reactive({ deviceName: '', alarmContent: '', alarmLevel: 3 })

function statusText(s) { return ['未处理', '处理中', '已闭环'][s] || '未知' }
function sevText(s) { const v = Number(s);
  return v === 1 ? '严重' : v === 2 ? '重要' : v === 3 ? '一般' : '未知'}
function formatTime(t) { return t ? t.slice(0, 16).replace('T', ' ') : '' }

function statusFilter() { return [null, 0, 1, 2][tabActive.value] }

async function loadAlarms(reset = false) {
  if (refreshing.value || reset) { pageNum.value = 1; finished.value = false }
  if (reset) { list.value = [] }
  loading.value = true
  try {
    const res = await pageAlarm({ current: pageNum.value, size: 20, status: statusFilter() })
    const rows = (res.data || {}).records || (res.data || {}).list || []
    if (refreshing.value || reset) { list.value = rows; if (refreshing.value) refreshing.value = false }
    else { list.value = [...list.value, ...rows] }
    finished.value = rows.length < 20
    pageNum.value++
  } catch (e) { showToast(e.message); finished.value = true }
  finally { loading.value = false }
}

function onRefresh() { pageNum.value = 1; finished.value = false; loadAlarms() }

/* ---------------- 处理人下拉（INSPECTOR 角色） ---------------- */

const assigneeOptions = ref([])
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

/* ---------------- ① 分配处理人：未处理(0) → 处理中(1) ---------------- */

const assignShow = ref(false)
const assignSubmitting = ref(false)
const assigneePickerShow = ref(false)

const assignForm = reactive({ id: null, alarmContent: '', handleUser: '' })

const assigneeLabel = computed(() =>
  assigneeOptions.value.find(u => u.value === assignForm.handleUser)?.label || ''
)

function resetAssignForm() {
  assignForm.id = null
  assignForm.alarmContent = ''
  assignForm.handleUser = ''
}

function openAssign(a) {
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
    showSuccessToast('已生成告警工单')
    const target = list.value.find(x => x.id === assignForm.id)
    if (target) target.status = 1
    // 引导前往工单模块继续处理
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

/* ---------------- ② 填写处理意见：处理中(1) → 已闭环(2) ---------------- */

const handleShow = ref(false)
const handleSubmitting = ref(false)

const handleForm = reactive({ id: null, alarmContent: '', handleRemark: '' })

function resetHandleForm() {
  handleForm.id = null
  handleForm.alarmContent = ''
  handleForm.handleRemark = ''
}

// 打开前先校验关联工单是否已完成（与电脑端一致，后端也有兜底校验）
async function openHandle(a) {
  try {
    const res = await getWorkOrderByAlarm(a.id)
    const wo = res.data
    if (wo && wo.status < 2) {
      const go = await showConfirmDialog({
        title: '工单未完成',
        message: '关联工单尚未处理完毕，请先在工单模块完成处理，再回来填写处理意见。',
        confirmButtonText: '去工单模块',
        cancelButtonText: '知道了'
      }).catch(() => false)
      if (go) router.push('/workorder?tab=alarm')
      return
    }
  } catch { /* 查不到工单或接口失败，放行（后端仍会校验） */ }
  resetHandleForm()
  handleForm.id = a.id
  handleForm.alarmContent = a.alarmContent || a.deviceName || '未知告警'
  handleShow.value = true
}

async function handleRemarkSubmit() {
  handleSubmitting.value = true
  try {
    // status=2：写入处理结果，后端记录完成时间并闭环
    await handleAlarmApi({ id: handleForm.id, status: 2, handleResult: handleForm.handleRemark })
    handleShow.value = false
    showSuccessToast('处理完成，告警已闭环')
    // 刷新列表拿到 handleTime / handleResult 等最新字段
    loadAlarms(true)
  } catch (e) {
    showToast(e.message || '操作失败')
  } finally {
    handleSubmitting.value = false
  }
}

/* ---------------- ③ 查看结果：已闭环(2) 只读 ---------------- */

const viewShow = ref(false)
const viewForm = reactive({ alarmContent: '', handleUser: '', handleTime: '', handleResult: '' })

function openView(a) {
  viewForm.alarmContent = a.alarmContent || a.deviceName || ''
  viewForm.handleUser = a.handleUser || '-'
  viewForm.handleTime = a.handleTime || ''
  viewForm.handleResult = a.handleResult || '-'
  viewShow.value = true
}

/* ---------------- 新增告警 ---------------- */

async function onAdd() {
  submitting.value = true
  try {
    await addAlarm({ alarmContent: form.alarmContent, alarmLevel: form.alarmLevel, deviceName: form.deviceName })
    showToast('告警已提交')
    showAdd.value = false
    form.deviceName = ''; form.alarmContent = ''; form.alarmLevel = 3
    pageNum.value = 1; finished.value = false; loadAlarms()
  } catch (e) { showToast(e.message) }
  finally { submitting.value = false }
}

onMounted(() => {
  loadAssignees()
  // 列表由 van-list 首次 @load 自动触发
})
</script>

<style scoped>
.alarm-page { min-height: 100vh; }
.alarm-list { padding: 8px 16px; }
.alarm-card { padding: 14px; margin-bottom: 10px; }
.alarm-header { display: flex; align-items: center; gap: 4px; }
:deep(.status-0) { color: #f56c6c !important; }
:deep(.status-1) { color: #e6a23c !important; }
:deep(.status-2) { color: #67c23a !important; }
:deep(.sev-1) { color: #f56c6c !important; }
:deep(.sev-2) { color: #e6a23c !important; }
:deep(.sev-3) { color: #90caf9 !important; }
:deep(.alarm-tag) { font-size: 18px; padding: 4px 12px; line-height: 1.4; }
.alarm-date { margin-left: auto; font-size: 11px; color: rgba(255,255,255,.35); }
.alarm-body { font-size: 14px; margin: 8px 0; line-height: 1.4; color: rgba(255,255,255,.85); }
.alarm-footer { display: flex; justify-content: flex-end; }
.handle-btn { height: 32px; padding: 0 14px; font-size: 12px; }
.btn-assign { color: #7ec8e3; border-color: rgba(126,200,227,.35); }
.btn-handle { color: #e6a23c; border-color: rgba(230,162,60,.35); }
.btn-view { color: rgba(255,255,255,.6); }

.fab {
  position: fixed; right: 20px; bottom: 90px; z-index: 100;
  width: 50px; height: 50px; border-radius: 50%;
  background: rgba(255,255,255,.1);
  backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px);
  border: 0.5px solid rgba(255,255,255,.15);
  box-shadow: 0 4px 16px rgba(0,0,0,.3);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: transform .2s;
}
.fab:active { transform: scale(.92); }
.fab-icon { font-size: 28px; color: rgba(255,255,255,.85); line-height:1; }

.sheet-body { padding: 8px 0; min-height: 300px; }

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
