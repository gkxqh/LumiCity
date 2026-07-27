<!--
  MapPanel.vue
  Leaflet 地图组件：CartoDB Dark Matter 暗色瓦片 + 灯杆 Marker（按状态着色）+ 点击弹窗
-->
<template>
  <div ref="mapContainer" class="map-container"></div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import '@/assets/map-theme.css'

const props = defineProps({
  poles: { type: Array, default: () => [] }
})

const emit = defineEmits(['map-ready'])

const mapContainer = ref(null)
let map = null
let markerLayer = null

// 状态映射
const STATUS_CLASS = { 1: 'online', 0: 'offline', 2: 'fault' }
const STATUS_TEXT = { 1: '在线', 0: '离线', 2: '故障' }

/* ---------- 初始化地图 ---------- */
function initMap() {
  if (map) return

  map = L.map(mapContainer.value, {
    center: [30.60, 104.07],       // 成都市中心
    zoom: 12,
    zoomControl: true,
    attributionControl: true,
    fadeAnimation: true,
    zoomAnimation: true,
  })

  // CartoDB Dark Matter 无标注瓦片（去掉 POI 蓝点）
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_nolabels/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/">CARTO</a>',
    subdomains: 'abcd',
    maxZoom: 19,
    minZoom: 3,
  }).addTo(map)

  // 标记图层组
  markerLayer = L.layerGroup().addTo(map)

  // 通知父组件 map 实例已就绪
  emit('map-ready', map)
}

/* ---------- 渲染 Marker ---------- */
function renderMarkers() {
  if (!markerLayer) return
  markerLayer.clearLayers()

  props.poles.forEach(pole => {
    const lng = Number(pole.lng)
    const lat = Number(pole.lat)
    if (!lng || !lat) return

    const statusClass = STATUS_CLASS[pole.status] || 'offline'
    const statusText = STATUS_TEXT[pole.status] || '未知'

    const icon = L.divIcon({
      className: 'pole-marker-wrapper',
      html: `<div class="pole-marker ${statusClass}" data-id="${pole.id}"></div>`,
      iconSize: [16, 16],
      iconAnchor: [8, 8],
    })

    const marker = L.marker([lat, lng], { icon })

    // 点击弹窗
    marker.bindPopup(`
      <div class="pole-popup-dark">
        <h4>${pole.poleName || pole.poleCode}</h4>
        <div class="popup-row"><span class="popup-label">编号</span><span class="popup-value">${pole.poleCode}</span></div>
        <div class="popup-row"><span class="popup-label">地址</span><span class="popup-value">${pole.address || '-'}</span></div>
        <div class="popup-row"><span class="popup-label">状态</span><span class="popup-value" style="color:${pole.status === 1 ? '#67c23a' : pole.status === 2 ? '#f56c6c' : '#909399'}">${statusText}</span></div>
        <div class="popup-row"><span class="popup-label">高度</span><span class="popup-value">${pole.height || '-'}m</span></div>
        <div class="popup-row"><span class="popup-label">安装时间</span><span class="popup-value">${pole.installTime || '-'}</span></div>
      </div>
    `, { className: 'pole-popup-dark', closeButton: true, maxWidth: 240 })

    markerLayer.addLayer(marker)
  })
}

/* ---------- 尺寸自适应 ---------- */
function invalidateSize() {
  if (map) setTimeout(() => map.invalidateSize(), 100)
}

/* ---------- 响应数据变化 ---------- */
watch(() => props.poles, () => {
  renderMarkers()
}, { deep: true })

/* ---------- 生命周期 ---------- */
onMounted(async () => {
  await nextTick()
  initMap()
  await nextTick()
  renderMarkers()
  // 首次进入延迟再 resize 一次（解决容器动画导致的偏移）
  setTimeout(() => invalidateSize(), 300)
})

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
    markerLayer = null
  }
})

// 暴露 map 引用给父组件（传给 ThreeDEffects）
defineExpose({ map })
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 100%;
  border-radius: 10px;
  overflow: hidden;
}
</style>
