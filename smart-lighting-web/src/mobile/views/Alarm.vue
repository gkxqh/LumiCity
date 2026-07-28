<template>
  <div class="alarm-page">
    <van-nav-bar title="告警管理" left-text="返回" left-arrow @click-left="router.push('/home')" />

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
            <div class="alarm-footer" v-if="a.status < 2">
              <button class="glass-btn handle-btn" @click="handle(a)">标记处理</button>
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
              v-model="form.content"
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
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { pageAlarm, addAlarm, handleAlarm as handleAlarmApi } from '@/api/other'

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

async function handle(a) {
  const c = await showConfirmDialog({ title: '标记处理', message: `确认处理此告警？`, confirmButtonText: '标记已处理' }).catch(() => false)
  if (!c) return
  try {
    await handleAlarmApi({ id: a.id, status: 2, handleResult: '移动端处理' })
    showToast('处理成功'); a.status = 2
  } catch (e) { showToast(e.message) }
}

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
</style>
