<template>
  <div class="control-page">
    <!-- 道路选择（原生 select，触发系统级选择器） -->
    <div class="section-padding">
      <div class="glass-card road-selector">
        <span class="road-label">选择道路</span>
        <select
          v-model="selectedRoad"
          class="native-select"
          @change="selectedRoad && loadPoles()"
        >
          <option value="" disabled>请选择道路</option>
          <option v-for="r in roads" :key="r" :value="r">{{ r }}</option>
        </select>
        <span class="road-value">{{ selectedRoad || '请选择道路' }}</span>
        <span class="road-arrow">›</span>
      </div>
    </div>

    <!-- 道路控制面板 -->
    <div v-if="selectedRoad" class="section-padding">
      <div class="glass-card control-panel">
        <div class="panel-title">{{ selectedRoad }}</div>
        <div class="panel-info">{{ poles.length }} 盏灯杆 · {{ onlineCount }} 在线 · {{ offlineCount }} 离线</div>

        <div class="batch-actions">
          <button class="glass-btn batch-btn" @click="batchSwitch('on')">🔛 全部开启</button>
          <button class="glass-btn batch-btn" @click="batchSwitch('off')">🔛 全部关闭</button>
        </div>

        <div class="brightness-row">
          <span class="brightness-label">亮度 {{ globalBrightness }}%</span>
          <van-slider
            v-model="globalBrightness"
            style="flex:1;margin:0 12px"
            :disabled="poles.length === 0"
            @drag-end="batchBrightness"
          />
        </div>
      </div>

      <!-- 灯杆列表 -->
      <div class="pole-list">
        <div class="glass-card pole-card" :class="'pole-card--' + (p.status || 0)" v-for="p in poles" :key="p.id">
          <div class="pole-header">
            <span class="pole-name">{{ p.poleName }}</span>
            <van-tag round plain :class="'pole-status-' + (p.status || 0)">
              {{ p.status === 1 ? '在线' : p.status === 2 ? '故障' : '离线' }}
            </van-tag>
          </div>
          <div class="pole-detail">
            <span class="light-indicator">💡 {{ p.lightStatus === 1 ? '已开启' : '已关闭' }}</span>
            <span class="brightness-text">亮度: {{ p.lightBrightness ?? '-' }}%</span>
          </div>
          <div class="pole-actions">
            <button
              class="glass-btn pole-switch-btn"
              style="height:30px;padding:0 12px;font-size:12px"
              :disabled="p.status !== 1"
              @click="togglePole(p)"
            >{{ p.status === 1 ? (p.lightStatus === 1 ? '关闭' : '开启') : '不可控' }}</button>
            <van-slider
              v-model="p._brightness"
              style="width:120px"
              :min="0"
              :max="100"
              :disabled="p.status !== 1"
              @drag-end="setBrightness(p)"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { pagePole } from '@/api/device'
import { batchSwitchByRoad, batchBrightnessByRoad, controlSwitch, controlBrightness } from '@/api/other'

const router = useRouter()
const selectedRoad = ref('')
const roads = ref([])
const poles = ref([])
const globalBrightness = ref(80)

const onlineCount = computed(() => poles.value.filter(p => p.status === 1).length)
const offlineCount = computed(() => poles.value.filter(p => p.status !== 1).length)

async function loadRoads() {
  try {
    const res = await pagePole({ current: 1, size: 500 })
    const allPoles = (res.data?.records || res.data?.list || [])
    const unique = [...new Set(allPoles.filter(p => p.road).map(p => p.road))]
    roads.value = unique.sort()
  } catch { roads.value = ['科华北路', '人民南路三段', '天府大道', '剑南大道'] }
}

async function loadPoles() {
  if (!selectedRoad.value) return
  try {
    const res = await pagePole({ current: 1, size: 100, road: selectedRoad.value })
    const rows = (res.data?.records || res.data?.list || [])
    poles.value = rows.map(p => ({ ...p, _brightness: p.lightBrightness ?? 0 }))
  } catch { poles.value = [] }
}

async function batchSwitch(action) {
  try {
    const res = await batchSwitchByRoad({ road: selectedRoad.value, action })
    showToast(res.message || '操作成功')
    await loadPoles()
  } catch (e) { showToast(e.message) }
}

async function batchBrightness() {
  try {
    const res = await batchBrightnessByRoad({ road: selectedRoad.value, brightness: globalBrightness.value })
    showToast(res.message || '调光成功')
    await loadPoles()
  } catch (e) { showToast(e.message) }
}

async function togglePole(p) {
  // 后端 /lighting/control/switch 要求 @RequestParam String action，取值 "on"/"off"
  // （service 内 boolean on = "on".equals(action)），不能用数字 0/1，参数名也不能用 status
  const turnOn = p.lightStatus !== 1
  const action = turnOn ? 'on' : 'off'
  try {
    const res = await controlSwitch({ poleId: p.id, action })
    // 后端 data.simStatus：SUCCESS=真正改库 / SKIPPED=离线跳过 / FAIL=通信失败
    const data = res.data || {}
    if (data.simStatus === 'SUCCESS') {
      p.lightStatus = turnOn ? 1 : 0
      if (turnOn) {
        // 开启：用后端返回的亮度同步滑块；未返回则给一个合理默认，避免滑块停在 0%
        const b = data.lightBrightness != null ? data.lightBrightness : 80
        p.lightBrightness = b
        p._brightness = b
      } else {
        // 关闭：亮度与右侧拖动条一起归零
        p.lightBrightness = 0
        p._brightness = 0
      }
    }
    showToast(data.message || res.message || (turnOn ? '已开启' : '已关闭'))
  } catch (e) { showToast(e.message) }
}

async function setBrightness(p) {
  try {
    const res = await controlBrightness({ poleId: p.id, brightness: p._brightness })
    showToast(res.message || `亮度已设为 ${p._brightness}%`)
  } catch (e) { showToast(e.message) }
}

loadRoads()
</script>

<style scoped>
.control-page { min-height: 100vh; }
.section-padding { padding: 8px 16px; }

.road-selector {
  position: relative; display: flex; align-items: center; padding: 14px 16px;
}
.road-label { font-size: 14px; color: rgba(255,255,255,.65); margin-right: 12px; }
.road-value { flex: 1; text-align: right; color: rgba(255,255,255,.85); font-size: 14px; }
.road-arrow { font-size: 20px; color: rgba(255,255,255,.3); margin-left: 8px; }

/* 原生 select — 透明盖在卡片上，触发系统选择器 */
.native-select {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  font-size: 16px;  /* 防止 iOS 缩放 */
  cursor: pointer;
  z-index: 2;
}

.control-panel { padding: 16px; position: relative; z-index: 1; }
.panel-title { font-size: 16px; font-weight: 600; color: rgba(255,255,255,.92); }
.panel-info { font-size: 12px; color: rgba(255,255,255,.45); margin: 4px 0 14px; }
.batch-actions { display: flex; gap: 12px; justify-content: center; margin-bottom: 14px; }
.batch-btn { flex: 1; height: 36px; font-size: 13px; }
.brightness-row { display: flex; align-items: center; }
.brightness-label { font-size: 13px; color: rgba(255,255,255,.55); white-space: nowrap; }

.pole-list { margin-top: 8px; }
.pole-card { padding: 14px; margin-bottom: 10px; position: relative; overflow: hidden; }
/* 右侧按状态渐变半透明染色：在线绿 / 故障红 / 离线灰；右边缘最深、向左渐隐 */
.pole-card::before {
  content: '';
  position: absolute; inset: 0;
  pointer-events: none;
  z-index: 0;
}
.pole-card--1::before { background: linear-gradient(to left, rgba(103,194,58,.22), transparent 62%); }
.pole-card--2::before { background: linear-gradient(to left, rgba(245,108,108,.28), transparent 62%); }
.pole-card--0::before { background: linear-gradient(to left, rgba(195,205,220,.18), transparent 62%); }
/* 卡片内容抬到染色层之上，保证文字清晰可读 */
.pole-card > * { position: relative; z-index: 1; }
/* 离线/故障灯的操作按钮与调光条禁用灰显 */
.pole-switch-btn:disabled {
  opacity: .38;
  cursor: not-allowed;
  filter: grayscale(.75);
}
.pole-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.pole-name { font-size: 14px; font-weight: 600; color: rgba(255,255,255,.88); }
:deep(.pole-status-1) { color: #67c23a !important; }
:deep(.pole-status-2) { color: #f56c6c !important; }
/* 离线：原 rgba(255,255,255,.4) 与深色玻璃卡片背景太接近，几乎看不出。
   提亮文字+边框色，并加微弱背景填充让椭圆轮廓清晰可见 */
:deep(.pole-status-0) {
  color: #e6e8ec !important;
  border-color: #e6e8ec !important;
  background-color: rgba(230,232,236,.16) !important;
}
/* 状态标签（椭圆胶囊）整体放大 1.3 倍；从右中点放大避免与左侧灯杆名重叠 */
:deep(.pole-status-1),
:deep(.pole-status-2),
:deep(.pole-status-0) {
  transform: scale(1.3);
  transform-origin: right center;
  font-weight: 600;
}
.pole-detail { font-size: 12px; color: rgba(255,255,255,.5); margin-bottom: 10px; display: flex; gap: 16px; }
.pole-actions { display: flex; align-items: center; gap: 12px; }
</style>
