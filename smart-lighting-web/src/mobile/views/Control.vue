<template>
  <div class="control-page">
    <van-nav-bar title="照明控制" left-text="返回" left-arrow @click-left="router.push('/home')" />

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
        <div class="glass-card pole-card" v-for="p in poles" :key="p.id">
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
              class="glass-btn"
              style="height:30px;padding:0 12px;font-size:12px"
              @click="togglePole(p)"
            >{{ p.lightStatus === 1 ? '关闭' : '开启' }}</button>
            <van-slider
              v-model="p._brightness"
              style="width:120px"
              :min="1"
              :max="100"
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
  const target = p.lightStatus === 1 ? 0 : 1
  try {
    const res = await controlSwitch({ poleId: p.id, status: target })
    showToast(res.message || (target ? '已开启' : '已关闭'))
    p.lightStatus = target
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
.pole-card { padding: 14px; margin-bottom: 10px; }
.pole-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.pole-name { font-size: 14px; font-weight: 600; color: rgba(255,255,255,.88); }
:deep(.pole-status-1) { color: #67c23a !important; }
:deep(.pole-status-2) { color: #f56c6c !important; }
:deep(.pole-status-0) { color: rgba(255,255,255,.4) !important; }
.pole-detail { font-size: 12px; color: rgba(255,255,255,.5); margin-bottom: 10px; display: flex; gap: 16px; }
.pole-actions { display: flex; align-items: center; gap: 12px; }
</style>
