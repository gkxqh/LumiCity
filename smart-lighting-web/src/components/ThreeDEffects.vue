<!--
  ThreeDEffects.vue
  Three.js 3D 叠加层：发光立柱 + 地面网格
  覆盖在 Leaflet 地图上方，pointer-events: none 穿透鼠标事件
-->
<template>
  <div ref="overlayRef" class="three-overlay"></div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as THREE from 'three'

const props = defineProps({
  map: { type: Object, default: null },
  poles: { type: Array, default: () => [] }
})

const overlayRef = ref(null)

let scene = null
let camera = null
let renderer = null
let animationId = null
let pillars = []
let gridHelper = null

// 颜色映射
const STATUS_COLOR = { 1: 0x67c23a, 0: 0x909399, 2: 0xf56c6c }
const STATUS_EMISSIVE = { 1: 0x67c23a, 0: 0x555555, 2: 0xf56c6c }

/* ---------- 初始化 Three.js 场景 ---------- */
function initScene() {
  if (!overlayRef.value) return

  const w = overlayRef.value.clientWidth
  const h = overlayRef.value.clientHeight
  if (w === 0 || h === 0) return

  // 场景（透明背景）
  scene = new THREE.Scene()

  // 正交相机——坐标直接映射到像素空间
  camera = new THREE.OrthographicCamera(-w / 2, w / 2, h / 2, -h / 2, 0.1, 1000)
  camera.position.z = 100

  // 渲染器（透明、抗锯齿）
  renderer = new THREE.WebGLRenderer({
    alpha: true,
    antialias: true,
  })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setClearColor(0x000000, 0)
  overlayRef.value.appendChild(renderer.domElement)

  // 环境光
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambientLight)

  const dirLight = new THREE.DirectionalLight(0xffffff, 0.4)
  dirLight.position.set(0, 1, 1)
  scene.add(dirLight)

  // 地面网格
  const gridSize = Math.max(w, h) * 1.2
  gridHelper = new THREE.GridHelper(gridSize, 40, 0x409eff, 0x1a3a6a)
  gridHelper.material.transparent = true
  gridHelper.material.opacity = 0.12
  scene.add(gridHelper)
}

/* ---------- 创建发光立柱 ---------- */
function createPillars() {
  // 清理旧立柱
  pillars.forEach(p => scene.remove(p))
  pillars = []

  if (!props.map || !props.poles.length) return

  props.poles.forEach(pole => {
    const lng = Number(pole.lng)
    const lat = Number(pole.lat)
    if (!lng || !lat) return

    const color = STATUS_COLOR[pole.status] || STATUS_COLOR[0]
    const emissive = STATUS_EMISSIVE[pole.status] || STATUS_EMISSIVE[0]

    // 柱体
    const height = 2 + (pole.height || 8) * 0.3  // 高度关联灯杆实际高度
    const geo = new THREE.CylinderGeometry(0.6, 0.8, height, 8)
    const mat = new THREE.MeshPhongMaterial({
      color,
      emissive,
      emissiveIntensity: 0.3,
      transparent: true,
      opacity: 0.85,
    })
    const mesh = new THREE.Mesh(geo, mat)

    // 顶部发光球体
    const topGeo = new THREE.SphereGeometry(0.4, 8, 8)
    const topMat = new THREE.MeshBasicMaterial({
      color: emissive,
      transparent: true,
      opacity: 0.7,
    })
    const topMesh = new THREE.Mesh(topGeo, topMat)
    topMesh.position.y = height / 2 + 0.3

    // 组合
    const group = new THREE.Group()
    group.add(mesh)
    group.add(topMesh)

    // 初始位置（稍后在动画循环中更新）
    group.userData = { lng, lat, poleId: pole.id }
    group.position.set(0, 0, 0)

    scene.add(group)
    pillars.push(group)
  })
}

/* ---------- 更新立柱坐标（随地图平移/缩放） ---------- */
let prevMapCenter = null

function updatePillarPositions() {
  if (!props.map || !pillars.length) return

  const overlayW = overlayRef.value?.clientWidth || 0
  const overlayH = overlayRef.value?.clientHeight || 0

  pillars.forEach(group => {
    const { lng, lat } = group.userData
    const cp = props.map.latLngToContainerPoint([lat, lng])
    // 转换到 Three.js 坐标系：中心为 (0,0)
    const x = cp.x - overlayW / 2
    const y = overlayH / 2 - cp.y
    group.position.set(x, y, 0)
  })
}

/* ---------- 更新飞线粒子位置 ---------- */
/* ---------- 动画循环 ---------- */
let frameCount = 0

function animate() {
  animationId = requestAnimationFrame(animate)

  // 每隔一帧渲染一次（~30fps），减少 CPU 占用
  frameCount++
  if (frameCount % 2 !== 0) return

  if (!props.map || !renderer || !scene || !camera) return

  // 检查地图容器尺寸变化
  const el = overlayRef.value
  if (el) {
    const w = el.clientWidth
    const h = el.clientHeight
    const curW = renderer.domElement.width
    const curH = renderer.domElement.height
    if (Math.abs(w - curW) > 1 || Math.abs(h - curH) > 1) {
      renderer.setSize(w, h)
      camera.left = -w / 2
      camera.right = w / 2
      camera.top = h / 2
      camera.bottom = -h / 2
      camera.updateProjectionMatrix()
      // 网格也重设
      if (gridHelper) {
        gridHelper.geometry.dispose()
        scene.remove(gridHelper)
        const gridSize = Math.max(w, h) * 1.2
        gridHelper = new THREE.GridHelper(gridSize, 40, 0x409eff, 0x1a3a6a)
        gridHelper.material.transparent = true
        gridHelper.material.opacity = 0.12
        scene.add(gridHelper)
      }
    }
  }

  updatePillarPositions()
  renderer.render(scene, camera)
}

/* ---------- 响应数据变化 ---------- */
watch(() => props.poles, () => {
  createPillars()
}, { deep: true })

watch(() => props.map, (newMap) => {
  if (newMap) {
    newMap.on('moveend', updatePillarPositions)
    createPillars()
  }
})

/* ---------- 生命周期 ---------- */
onMounted(async () => {
  await nextTick()
  initScene()
  if (props.map) {
    createPillars()
  }
  animate()
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
  if (renderer) {
    renderer.dispose()
    if (renderer.domElement && renderer.domElement.parentNode) {
      renderer.domElement.parentNode.removeChild(renderer.domElement)
    }
  }
  // 清理 Three.js 资源
  pillars.forEach(p => {
    p.children.forEach(c => {
      if (c.geometry) c.geometry.dispose()
      if (c.material) c.material.dispose()
    })
  })
  pillars = []
  if (gridHelper) {
    if (gridHelper.geometry) gridHelper.geometry.dispose()
    if (gridHelper.material) gridHelper.material.dispose()
  }
  if (props.map) {
    props.map.off('moveend')
  }
})
</script>
