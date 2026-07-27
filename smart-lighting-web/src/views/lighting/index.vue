<!--
  照明控制页
  - 上半部分：照明控制面板
    - 灯杆选择(Select) / 开灯按钮 / 关灯按钮 / 亮度滑块(0-100) / 执行按钮
    - 开关灯调 controlSwitch，亮度调 controlBrightness
  - 下半部分：照明策略列表
    - 表格：策略名称、类型、亮度、时段、启用状态(开关)、操作
    - 新增 / 编辑弹窗
    - 调 pageStrategy / addStrategy / updateStrategy / deleteStrategy
-->
<template>
  <div class="lighting-page">
    <!-- ============ 照明控制面板 ============ -->
    <el-card shadow="never" class="control-card">
      <template #header>
        <div class="card-header">
          <span>照明控制</span>
          <el-tag size="small" type="success" effect="plain">实时控制</el-tag>
        </div>
      </template>

      <el-form inline label-width="80px">
        <el-form-item label="灯杆选择">
          <el-select
            v-model="control.poleId"
            placeholder="请选择灯杆"
            filterable
            clearable
            style="width: 220px"
          >
            <el-option
              v-for="item in poleOptions"
              :key="item.id"
              :label="item.poleName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="开关灯">
          <el-button
            type="success"
            :icon="Open"
            :loading="switching"
            @click="handleSwitch(true)"
          >开灯</el-button>
          <el-button
            type="danger"
            :icon="TurnOff"
            :loading="switching"
            @click="handleSwitch(false)"
          >关灯</el-button>
        </el-form-item>

        <el-form-item label="亮度">
          <el-slider
            v-model="control.brightness"
            :min="0"
            :max="100"
            :step="1"
            show-input
            style="width: 320px"
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

    <!-- ============ 照明策略列表 ============ -->
    <el-card shadow="never">
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
        @size-change="loadData"
        @current-change="loadData"
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Open, TurnOff, Check, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  pageStrategy,
  addStrategy,
  updateStrategy,
  deleteStrategy,
  controlSwitch,
  controlBrightness
} from '@/api/other'
import { listPole } from '@/api/device'

/* ---------------- 字典数据 ---------------- */

// 策略类型选项：value 用英文枚举，对齐后端 String strategyType（TIME/LIGHT/TRAFFIC/OTHER）
const typeOptions = [
  { label: '定时', value: 'TIME' },
  { label: '感光', value: 'LIGHT' },
  { label: '车流', value: 'TRAFFIC' },
  { label: '其他', value: 'OTHER' }
]
// value -> label 映射，表格展示用
const typeMap = { TIME: '定时', LIGHT: '感光', TRAFFIC: '车流', OTHER: '其他' }

/* ---------------- 灯杆下拉选项 ---------------- */

const poleOptions = ref([])
async function loadPoleOptions() {
  try {
    const res = await listPole()
    poleOptions.value = res.data || []
  } catch (e) {
    /* 静默 */
  }
}

/* ---------------- 实时控制面板 ---------------- */

// 控制面板表单
const control = reactive({
  poleId: '',
  brightness: 80
})

const switching = ref(false)
const brightnessLoading = ref(false)

// 开 / 关灯
async function handleSwitch(on) {
  if (!control.poleId) {
    ElMessage.warning('请先选择灯杆')
    return
  }
  switching.value = true
  try {
    await controlSwitch({
      poleId: control.poleId,
      action: on ? 'on' : 'off'
    })
    ElMessage.success(on ? '开灯指令已下发' : '关灯指令已下发')
  } finally {
    switching.value = false
  }
}

// 亮度调节
async function handleBrightness() {
  if (!control.poleId) {
    ElMessage.warning('请先选择灯杆')
    return
  }
  brightnessLoading.value = true
  try {
    await controlBrightness({
      poleId: control.poleId,
      brightness: control.brightness
    })
    ElMessage.success(`亮度已设置为 ${control.brightness}%`)
  } finally {
    brightnessLoading.value = false
  }
}

/* ---------------- 策略列表 & 分页 ---------------- */

// 分页参数用 current/size，对齐后端 PageQuery
const query = reactive({
  current: 1,
  size: 10
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)

async function loadData() {
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

/* ---------------- 启用状态切换 ---------------- */

// 切换启用状态：调 updateStrategy 改 enabled 字段
// 注意后端 @Valid 校验需要传全所有必填字段，故把 row 展开传递
async function handleToggleEnabled(row, val) {
  try {
    await updateStrategy({ ...row, enabled: val })
    row.enabled = val
    ElMessage.success(val ? '已启用' : '已停用')
  } catch (e) {
    // 失败时状态回滚由表格刷新保证
    loadData()
  }
}

/* ---------------- 新增/编辑弹窗 ---------------- */

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
  enabled: 1  // 对齐后端 Integer enabled（0禁用 1启用）
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
    loadData()
  } finally {
    submitting.value = false
  }
}

/* ---------------- 删除 ---------------- */

async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除策略「${row.strategyName}」吗？`,
    '提示',
    { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
  )
  await deleteStrategy(row.id)
  ElMessage.success('删除成功')
  loadData()
}

/* ---------------- 初始化 ---------------- */

onMounted(() => {
  loadPoleOptions()
  loadData()
})
</script>

<style scoped>
.lighting-page {
  padding: 0;
}
.control-card {
  margin-bottom: 16px;
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
