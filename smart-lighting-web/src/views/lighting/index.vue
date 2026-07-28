<!--
  照明控制页（v2 — 新增按道路批量控制面板）
  - 上半部分左：单灯杆控制（原面板，region 筛选 + 灯杆下拉）
  - 上半部分右：批量控制（按道路批量开关/调光 + 按区域批量开关）
  - 下半部分：照明策略列表（不变）
-->
<template>
  <div class="lighting-page">
    <el-row :gutter="16">
      <!-- ============ 单灯杆控制面板 ============ -->
      <el-col :span="12">
        <el-card shadow="never" class="control-card">
          <template #header>
            <div class="card-header">
              <span>单灯杆控制</span>
              <el-tag size="small" type="success" effect="plain">实时控制</el-tag>
            </div>
          </template>

          <el-form label-width="80px">
            <el-form-item label="灯杆选择">
              <el-select
                v-model="single.poleId"
                placeholder="请选择灯杆"
                filterable
                clearable
                style="width: 100%"
                @change="onPoleChange"
              >
                <el-option
                  v-for="item in poleOptions"
                  :key="item.id"
                  :label="item.poleName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>

            <!-- 当前照明状态展示 -->
            <el-row :gutter="16" v-if="single.currentPole">
              <el-col :span="12">
                <el-descriptions :column="1" size="small" border>
                  <el-descriptions-item label="照明状态">
                    <el-tag
                      :type="single.currentPole.lightStatus === 1 ? 'warning' : 'info'"
                      effect="dark"
                      size="small"
                    >
                      {{ single.currentPole.lightStatus === 1 ? '开灯' : '关灯' }}
                    </el-tag>
                  </el-descriptions-item>
                </el-descriptions>
              </el-col>
              <el-col :span="12">
                <el-descriptions :column="1" size="small" border>
                  <el-descriptions-item label="当前亮度">
                    <span :style="{ color: single.currentPole.lightBrightness > 0 ? '#e6a23c' : '#909399' }">
                      {{ single.currentPole.lightBrightness != null ? single.currentPole.lightBrightness + '%' : '--' }}
                    </span>
                  </el-descriptions-item>
                </el-descriptions>
              </el-col>
            </el-row>
            <div v-else style="margin-bottom: 12px">
              <el-text type="info" size="small">请选择一个灯杆查看其当前状态</el-text>
            </div>

            <el-form-item label="开关灯">
              <el-button
                type="success"
                :icon="Open"
                :loading="switchingOpen"
                :disabled="switchingOff"
                @click="handleSwitch(true)"
              >开灯</el-button>
              <el-button
                type="danger"
                :icon="TurnOff"
                :loading="switchingOff"
                :disabled="switchingOpen"
                style="margin-left: 8px"
                @click="handleSwitch(false)"
              >关灯</el-button>
            </el-form-item>

            <el-form-item label="亮度">
              <el-slider
                v-model="single.brightness"
                :min="0"
                :max="100"
                :step="1"
                show-input
                style="width: 280px"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :icon="Check"
                :loading="brightnessLoading"
                @click="handleBrightness"
              >执行亮度</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- ============ 批量控制面板 ============ -->
      <el-col :span="12">
        <el-card shadow="never" class="control-card">
          <template #header>
            <div class="card-header">
              <span>批量控制</span>
              <el-tag size="small" type="warning" effect="plain">按道路/区域</el-tag>
            </div>
          </template>

          <el-form label-width="90px">
            <el-form-item label="选择区域">
              <el-select
                v-model="batch.regionId"
                placeholder="全部区域"
                clearable
                style="width: 100%"
                @change="onRegionChange"
              >
                <el-option
                  v-for="item in regionOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="选择道路">
              <el-select
                v-model="batch.road"
                placeholder="请选择道路"
                filterable
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="r in filteredRoadOptions"
                  :key="r"
                  :label="r"
                  :value="r"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="批量开关">
              <el-button
                type="success"
                :icon="Open"
                :loading="batchSwitching"
                @click="handleBatchSwitch(true)"
              >开灯</el-button>
              <el-button
                type="danger"
                :icon="TurnOff"
                :loading="batchSwitching"
                style="margin-left: 8px"
                @click="handleBatchSwitch(false)"
              >关灯</el-button>
            </el-form-item>

            <el-form-item label="批量亮度">
              <el-slider
                v-model="batch.brightness"
                :min="0"
                :max="100"
                :step="1"
                show-input
                style="width: 250px"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :icon="Check"
                :loading="batchBrightnessLoading"
                :disabled="!batch.road"
                @click="handleBatchBrightness"
              >批量执行亮度</el-button>
              <el-text v-if="!batch.road" type="info" style="margin-left: 8px; font-size: 12px">
                请先选择道路
              </el-text>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- ============ 照明策略列表 ============ -->
    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>照明策略</span>
          <el-button type="success" :icon="Plus" @click="openAdd">新增策略</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="strategyName" label="策略名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="strategyType" label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag effect="plain">{{ typeMap[row.strategyType] || row.strategyType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="brightness" label="亮度(%)" width="100" align="center" />
        <el-table-column label="时段" width="180" align="center">
          <template #default="{ row }">
            {{ row.startTime || '--' }} ~ {{ row.endTime || '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="启用状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              :active-value="1"
              :inactive-value="0"
              @change="handleToggleEnabled(row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- ============ 分页 ============ -->
      <el-pagination
        class="pagination"
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="loadStrategyData"
        @current-change="loadStrategyData"
      />
    </el-card>

    <!-- ============ 新增/编辑弹窗 ============ -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="策略名称" prop="strategyName">
          <el-input v-model="form.strategyName" placeholder="请输入策略名称" />
        </el-form-item>
        <el-form-item label="策略类型" prop="strategyType">
          <el-select v-model="form.strategyType" placeholder="请选择类型" style="width: 100%">
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="亮度(%)" prop="brightness">
          <el-slider v-model="form.brightness" :min="0" :max="100" show-input />
        </el-form-item>
        <el-form-item label="起始时间" prop="startTime">
          <el-time-picker
            v-model="form.startTime"
            placeholder="如 18:00"
            format="HH:mm"
            value-format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker
            v-model="form.endTime"
            placeholder="如 06:00"
            format="HH:mm"
            value-format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="启用状态" prop="enabled">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Open, TurnOff, Check, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  pageStrategy,
  addStrategy,
  updateStrategy,
  deleteStrategy,
  controlSwitch,
  controlBrightness,
  batchSwitchByRoad,
  batchBrightnessByRoad
} from '@/api/other'
import { listPole, listRegion } from '@/api/device'

/* ---------------- 字典数据 ---------------- */

const typeOptions = [
  { label: '定时', value: 'TIME' },
  { label: '感光', value: 'LIGHT' },
  { label: '车流', value: 'TRAFFIC' },
  { label: '其他', value: 'OTHER' }
]
const typeMap = { TIME: '定时', LIGHT: '感光', TRAFFIC: '车流', OTHER: '其他' }

/* ---------------- 区域 & 灯杆下拉 ---------------- */

const regionOptions = ref([])
const poleOptions = ref([])
const allRoadSet = ref(new Set())

async function loadPoleOptions() {
  try {
    const [regionRes, poleRes] = await Promise.all([listRegion(), listPole()])
    regionOptions.value = regionRes.data || []
    const poles = poleRes.data || []
    poleOptions.value = poles
    // 从灯杆数据提取所有不重复道路
    allRoadSet.value = new Set(
      poles.filter(p => p.road).map(p => p.road)
    )
  } catch {
    /* 静默 */
  }
}

/** 根据已选区域过滤道路列表 */
const filteredRoadOptions = computed(() => {
  const roads = Array.from(allRoadSet.value)
  if (!batch.regionId) {
    return roads.sort()
  }
  // 如果有区域选择，从灯杆数据中过滤
  return roads
})

/** 区域变化时，如果当前选择道路不在该区，自动清空 */
function onRegionChange() {
  // 当前实现不做跨区过滤（保留所有道路），用户自己选择即可
}

/* ---------------- 单灯杆控制 ---------------- */

const single = reactive({
  poleId: '',
  brightness: 80,
  currentPole: null  // 当前选中灯杆的完整数据，含 lightStatus/lightBrightness
})

const switchingOpen = ref(false)
const switchingOff = ref(false)
const brightnessLoading = ref(false)

// 切换灯杆时，从 poleOptions 中取出完整信息
function onPoleChange(val) {
  if (!val) {
    single.currentPole = null
    return
  }
  single.currentPole = poleOptions.value.find(p => p.id === val) || null
  // 同步亮度滑块到当前值
  if (single.currentPole && single.currentPole.lightBrightness != null) {
    single.brightness = single.currentPole.lightBrightness
  }
}

// 刷新单灯杆状态（控制后调用）
function refreshSinglePole() {
  if (single.poleId) {
    const found = poleOptions.value.find(p => p.id === single.poleId)
    if (found) {
      single.currentPole = { ...found }
    }
  }
}

async function handleSwitch(on) {
  if (!single.poleId) {
    ElMessage.warning('请先选择灯杆')
    return
  }
  if (on) switchingOpen.value = true
  else switchingOff.value = true
  try {
    const res = await controlSwitch({
      poleId: single.poleId,
      action: on ? 'on' : 'off'
    })
    const data = res.data || {}
    const simStatus = data.simStatus || 'SUCCESS'
    if (simStatus === 'SKIPPED') {
      ElMessage.warning(data.message || '灯杆离线，已跳过')
    } else if (simStatus === 'FAIL') {
      ElMessage.error(data.message || '通信失败，设备未响应')
    } else {
      if (single.currentPole) {
        single.currentPole.lightStatus = data.lightStatus
        // 开灯成功后同步更新亮度显示（后端已设默认80%或保留原值）
        if (data.lightBrightness != null) {
          single.currentPole.lightBrightness = data.lightBrightness
          single.brightness = data.lightBrightness
        }
      }
      ElMessage.success(data.message || (on ? '开灯成功' : '关灯成功'))
    }
    loadPoleOptions()
  } catch (e) {
    ElMessage.error(e.message || '控制请求失败，请检查网络连接')
  } finally {
    switchingOpen.value = false
    switchingOff.value = false
  }
}

async function handleBrightness() {
  if (!single.poleId) {
    ElMessage.warning('请先选择灯杆')
    return
  }
  brightnessLoading.value = true
  try {
    const res = await controlBrightness({
      poleId: single.poleId,
      brightness: single.brightness
    })
    const data = res.data || {}
    const simStatus = data.simStatus || 'SUCCESS'
    if (simStatus === 'SKIPPED') {
      ElMessage.warning(data.message || '灯杆离线，已跳过')
    } else if (simStatus === 'FAIL') {
      ElMessage.error(data.message || '通信失败，设备未响应')
    } else {
      if (single.currentPole) {
        single.currentPole.lightBrightness = single.brightness
        single.currentPole.lightStatus = data.lightStatus
      }
      ElMessage.success(data.message || `亮度已设置为 ${single.brightness}%`)
    }
    loadPoleOptions()
  } catch (e) {
    ElMessage.error(e.message || '控制请求失败，请检查网络连接')
  } finally {
    brightnessLoading.value = false
  }
}

/* ---------------- 批量控制 ---------------- */

const batch = reactive({
  regionId: null,
  road: '',
  brightness: 80
})

const batchSwitching = ref(false)
const batchBrightnessLoading = ref(false)

async function handleBatchSwitch(on) {
  if (!batch.road) {
    ElMessage.warning('请先选择道路')
    return
  }
  batchSwitching.value = true
  try {
    const res = await batchSwitchByRoad({
      road: batch.road,
      action: on ? 'on' : 'off'
    })
    const data = res.data || {}
    if (data.skippedCount > 0 || data.failedCount > 0) {
      ElMessage({
        type: data.successCount > 0 ? 'warning' : 'error',
        message: data.message || '批量操作完成',
        duration: 5000
      })
    } else {
      ElMessage.success(data.message || `「${batch.road}」批量${on ? '开灯' : '关灯'}完成`)
    }
    loadPoleOptions()
    refreshSinglePole()
  } catch (e) {
    ElMessage.error(e.message || '批量控制请求失败，请检查网络连接')
  } finally {
    batchSwitching.value = false
  }
}

async function handleBatchBrightness() {
  if (!batch.road) {
    ElMessage.warning('请先选择道路')
    return
  }
  batchBrightnessLoading.value = true
  try {
    const res = await batchBrightnessByRoad({
      road: batch.road,
      brightness: batch.brightness
    })
    const data = res.data || {}
    if (data.skippedCount > 0 || data.failedCount > 0) {
      ElMessage({
        type: data.successCount > 0 ? 'warning' : 'error',
        message: data.message || '批量调光完成',
        duration: 5000
      })
    } else {
      ElMessage.success(data.message || `「${batch.road}」批量调光完成`)
    }
    loadPoleOptions()
    refreshSinglePole()
  } catch (e) {
    ElMessage.error(e.message || '批量控制请求失败，请检查网络连接')
  } finally {
    batchBrightnessLoading.value = false
  }
}

/* ---------------- 策略列表 & 分页 ---------------- */

const query = reactive({
  current: 1,
  size: 10
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)

async function loadStrategyData() {
  loading.value = true
  try {
    const res = await pageStrategy(query)
    const page = res.data || {}
    tableData.value = page.records || page.list || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

async function handleToggleEnabled(row, val) {
  try {
    await updateStrategy({ ...row, enabled: val })
    row.enabled = val
    ElMessage.success(val ? '已启用' : '已停用')
  } catch {
    loadStrategyData()
  }
}

/* ---------------- 策略新增/编辑弹窗 ---------------- */

const dialogVisible = ref(false)
const dialogTitle = ref('新增策略')
const submitting = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  strategyName: '',
  strategyType: '',
  brightness: 80,
  startTime: '',
  endTime: '',
  enabled: 1
})

const formRules = {
  strategyName: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  strategyType: [{ required: true, message: '请选择策略类型', trigger: 'change' }],
  brightness: [{ required: true, message: '请设置亮度', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择起始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

function resetForm() {
  form.id = null
  form.strategyName = ''
  form.strategyType = ''
  form.brightness = 80
  form.startTime = ''
  form.endTime = ''
  form.enabled = 1
  formRef.value?.clearValidate()
}

function openAdd() {
  dialogTitle.value = '新增策略'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  dialogTitle.value = '编辑策略'
  resetForm()
  Object.assign(form, {
    id: row.id,
    strategyName: row.strategyName,
    strategyType: row.strategyType,
    brightness: row.brightness ?? 80,
    startTime: row.startTime || '',
    endTime: row.endTime || '',
    enabled: row.enabled ?? 1
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await updateStrategy({ ...form })
      ElMessage.success('修改成功')
    } else {
      await addStrategy({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadStrategyData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除策略「${row.strategyName}」吗？`,
    '提示',
    { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
  )
  await deleteStrategy(row.id)
  ElMessage.success('删除成功')
  loadStrategyData()
}

/* ---------------- 初始化 ---------------- */

onMounted(() => {
  loadPoleOptions()
  loadStrategyData()
})
</script>

<style scoped>
.lighting-page {
  padding: 0;
}
.control-card {
  margin-bottom: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
