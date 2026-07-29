<template>
  <div class="pole-page">
    <!-- 搜索栏 -->
    <div class="section-padding">
      <van-search
        v-model="keyword"
        placeholder="搜索灯杆名称 / 道路"
        shape="round"
        @search="onSearch"
        @clear="onSearch"
        @update:model-value="onKeywordInput"
      />
    </div>

    <!-- 区域筛选（原生 select） -->
    <div class="section-padding" style="padding-top:0">
      <div class="glass-card filter-bar">
        <span class="filter-label">区域筛选</span>
        <select
          v-model="regionVal"
          class="native-select"
          @change="onSearch"
        >
          <option value="">全部区域</option>
          <option v-for="r in regions" :key="r.id" :value="r.id">{{ r.name }}</option>
        </select>
        <span class="filter-value">{{ regionLabel || '全部区域' }}</span>
        <span class="filter-arrow">›</span>
      </div>
    </div>

    <!-- 列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadPoles"
      >
        <div class="list-padding">
          <div class="glass-card pole-card" :class="'pole-card--' + (p.status || 0)" v-for="p in list" :key="p.id">
            <div class="pole-title">{{ p.poleName }}</div>
            <div class="pole-info">
              <span>{{ p.address || '-' }}</span>
              <van-tag round plain :class="'pole-s-' + (p.status || 0)">
                {{ p.status === 1 ? '在线' : p.status === 2 ? '故障' : '离线' }}
              </van-tag>
            </div>
            <div class="pole-info" v-if="p.lightStatus !== undefined" style="margin-top:4px">
              <span>💡 {{ p.lightStatus === 1 ? '已开启' : '已关闭' }}</span>
              <span>亮度 {{ p.lightBrightness ?? '-' }}%</span>
            </div>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { pagePole, listRegion } from '@/api/device'

const router = useRouter()
const keyword = ref('')
const regionVal = ref('')
const regions = ref([])
const list = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)

const regionLabel = computed(() => {
  if (!regionVal.value) return '全部区域'
  return regions.value.find(r => r.id === regionVal.value)?.name || '全部区域'
})

async function loadRegions() {
  try {
    const res = await listRegion()
    regions.value = res.data || []
  } catch {}
}

async function loadPoles(replace = false) {
  if (replace) { pageNum.value = 1; finished.value = false }
  loading.value = true
  try {
    const res = await pagePole({
      current: pageNum.value, size: 20,
      // 后端按 poleName 做 LIKE 模糊匹配（poleName = "{区}{路}{号}灯杆"），
      // 因此输入灯杆名称或道路片段都能命中，比原先的 road 精确匹配更贴合搜索框文案
      poleName: keyword.value || undefined,
      regionId: regionVal.value || undefined
    })
    const rows = (res.data?.records || res.data?.list || [])
    // replace=true 用于搜索/筛选/下拉刷新：整页替换；否则为滚动到底的增量追加
    if (replace) { list.value = rows }
    else { list.value = [...list.value, ...rows] }
    finished.value = rows.length < 20
    pageNum.value++
  } catch (e) { showToast(e.message); finished.value = true }
  finally { loading.value = false; if (replace) refreshing.value = false }
}

// 搜索框：输入即搜（轻量防抖），并支持回车/点击键盘“搜索”立即搜、清空即还原
let searchTimer = null
function onKeywordInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { loadPoles(true) }, 300)
}
function onSearch() { if (searchTimer) clearTimeout(searchTimer); loadPoles(true) }
function onRefresh() { loadPoles(true) }

onMounted(() => { loadRegions(); loadPoles() })
</script>

<style scoped>
.pole-page { min-height: 100vh; }
.section-padding { padding: 8px 16px; }
.list-padding { padding: 0 16px 8px; }

.filter-bar {
  position: relative; display: flex; align-items: center; padding: 12px 16px;
}
.filter-label { font-size: 13px; color: rgba(255,255,255,.6); margin-right: 10px; }
.filter-value { flex: 1; text-align: right; font-size: 13px; color: rgba(255,255,255,.85); }
.filter-arrow { font-size: 18px; color: rgba(255,255,255,.3); margin-left: 6px; }

/* 原生 select — 透明覆盖层 */
.native-select {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  font-size: 16px;
  cursor: pointer;
  z-index: 2;
}

.pole-card { padding: 14px; margin-bottom: 8px; position: relative; overflow: hidden; }
/* 右侧按状态渐变半透明染色：在线绿 / 故障红 / 离线灰；右边缘最深、向左渐隐（与照明控制一致） */
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
.pole-title { font-size: 15px; font-weight: 600; color: rgba(255,255,255,.88); }
.pole-info { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; font-size: 12px; color: rgba(255,255,255,.55); }
:deep(.pole-s-1) { color: #67c23a !important; }
:deep(.pole-s-2) { color: #f56c6c !important; }
/* 离线：原 rgba(255,255,255,.4) 与深色玻璃卡片背景太接近，几乎看不出。
   对齐“照明控制”的离线样式：提亮文字+边框色，并加微弱背景填充让椭圆轮廓清晰可见 */
:deep(.pole-s-0) {
  color: #e6e8ec !important;
  border-color: #e6e8ec !important;
  background-color: rgba(230,232,236,.16) !important;
}
/* 状态标签（椭圆胶囊）整体放大 1.2 倍；从右中点放大避免与左侧地址重叠 */
:deep(.pole-s-1),
:deep(.pole-s-2),
:deep(.pole-s-0) {
  transform: scale(1.2);
  transform-origin: right center;
  font-weight: 600;
}
</style>
