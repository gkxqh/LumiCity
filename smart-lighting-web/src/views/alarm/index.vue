<!--
  告警管理页
  - 顶部搜索栏：告警类型 / 状态 / 查询
  - 中间表格：设备名称、告警类型、告警级别(Tag)、告警内容、告警时间、状态、操作
  - 底部分页
  - 处理流程（两步骤闭环）：
      未处理(0) →[分配处理人]→ 处理中(1) →[编写处理意见]→ 已闭环(2) →[查看结果]
    - 分配处理人：填写 handleUser，后端置 status=1（处理中），不填完成时间
    - 编写处理意见：填写 handleResult，后端置 status=2（已闭环）并记完成时间
    - 查看结果：只读展示 告警时间 / 完成时间 / 处理人 / 处理结果

  字段对齐后端 AlarmRecord 实体：
  - alarmType 是 String 枚举：OFFLINE/OVERVOLTAGE/OVERCURRENT/ABNORMAL
  - alarmLevel 是 Integer：1严重 2重要 3一般
  - status 是 Integer：0未处理 1处理中 2已闭环
  - alarmContent 是告警内容字段（非 content）
  - 分页参数 current/size（对齐 PageQuery）
-->
<template>
  <div class="alarm-page">
    <!-- ============ 搜索栏 ============ -->
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="告警类型">
          <el-select
            v-model="query.alarmType"
            placeholder="全部"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 130px"
          >
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="warning" :icon="Bell" @click="handleMockAlarm" :loading="mocking">
            模拟告警
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ============ 表格 ============ -->
    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="alarmType" label="告警类型" width="120" align="center">
          <template #default="{ row }">
            {{ typeMap[row.alarmType] || row.alarmType }}
          </template>
        </el-table-column>
        <el-table-column prop="alarmLevel" label="告警级别" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.alarmLevel)" effect="light">
              {{ levelMap[row.alarmLevel] || row.alarmLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmContent" label="告警内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="alarmTime" label="告警时间" width="170" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">
              {{ statusMap[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              type="primary"
              link
              :icon="Edit"
              @click="openAssign(row)"
            >
              分配处理人
            </el-button>
            <el-button
              v-else-if="row.status === 1"
              type="warning"
              link
              :icon="Edit"
              @click="openHandle(row)"
            >
              编写处理意见
            </el-button>
            <el-button
              v-else
              type="info"
              link
              :icon="View"
              @click="openView(row)"
            >
              查看结果
            </el-button>
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

    <!-- ============ 处理/查看弹窗 ============ -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="设备名称">
          <el-input :model-value="form.deviceName" disabled />
        </el-form-item>
        <el-form-item label="告警内容">
          <el-input :model-value="form.alarmContent" type="textarea" :rows="2" disabled />
        </el-form-item>

        <!-- 模式一：分配处理人（未处理 → 处理中） -->
        <template v-if="dialogMode === 'assign'">
          <el-form-item label="处理人" prop="handleUser">
            <el-input
              v-model="form.handleUser"
              placeholder="请输入处理人姓名 / 工号"
            />
          </el-form-item>
        </template>

        <!-- 模式二：编写处理意见（处理中 → 已闭环） -->
        <template v-if="dialogMode === 'handle'">
          <el-form-item label="处理意见" prop="handleRemark">
            <el-input
              v-model="form.handleRemark"
              type="textarea"
              :rows="4"
              placeholder="请填写处理意见 / 处置过程"
            />
          </el-form-item>
        </template>

        <!-- 模式三：查看处理结果（已闭环，只读） -->
        <template v-if="dialogMode === 'view'">
          <el-form-item label="告警时间">
            <el-input :model-value="form.alarmTime" disabled />
          </el-form-item>
          <el-form-item label="完成时间">
            <el-input :model-value="form.handleTime" disabled />
          </el-form-item>
          <el-form-item label="处理人">
            <el-input :model-value="form.handleUser" disabled />
          </el-form-item>
          <el-form-item label="处理结果">
            <el-input :model-value="form.handleResult" type="textarea" :rows="3" disabled />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button
          v-if="dialogMode !== 'view'"
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { Search, Refresh, Edit, Bell, View } from '@element-plus/icons-vue'
import { pageAlarm, addAlarm, handleAlarm } from '@/api/other'
import { connectAlarmWS, onAlarmMessage, disconnectAlarmWS } from '@/api/ws'

/* ---------------- 字典数据 ---------------- */

// 告警类型选项：value 用英文枚举，对齐后端 String alarmType
const typeOptions = [
  { label: '离线告警', value: 'OFFLINE' },
  { label: '过压告警', value: 'OVERVOLTAGE' },
  { label: '过流告警', value: 'OVERCURRENT' },
  { label: '其他异常', value: 'ABNORMAL' }
]
// value -> label 映射，表格展示用
const typeMap = Object.fromEntries(typeOptions.map(i => [i.value, i.label]))

// 告警级别：value 用数字，对齐后端 Integer alarmLevel（1严重 2重要 3一般）
const levelMap = { 1: '严重', 2: '重要', 3: '一般' }

// 告警状态选项：value 用数字，对齐后端 Integer status（0未处理 1处理中 2已闭环）
const statusOptions = [
  { label: '未处理', value: 0 },
  { label: '处理中', value: 1 },
  { label: '已闭环', value: 2 }
]
// 状态值 → 中文显示（key 是数字）
const statusMap = { 0: '未处理', 1: '处理中', 2: '已闭环' }

// 状态 Tag 颜色：未处理-红、处理中-橙、已闭环-绿
function statusTagType(status) {
  return { 0: 'danger', 1: 'warning', 2: 'success' }[status] || 'info'
}

// 告警级别 Tag 颜色：严重-红、重要-橙、一般-蓝
function levelTagType(level) {
  return { 1: 'danger', 2: 'warning', 3: 'primary' }[level] || 'info'
}

/* ---------------- 查询 & 表格 ---------------- */

// 查询条件：分页参数用 current/size，对齐后端 PageQuery
const query = reactive({
  current: 1,
  size: 10,
  alarmType: '',
  status: null
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)

// 加载告警分页数据
async function loadData() {
  loading.value = true
  try {
    const res = await pageAlarm(query)
    // 兼容 MyBatis-Plus(records) 与 PageHelper(list) 两种返回结构
    const page = res.data || {}
    tableData.value = page.records || page.list || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

// 点击查询：回到第一页后重新加载
function handleSearch() {
  query.current = 1
  loadData()
}

// 重置查询条件
function handleReset() {
  query.alarmType = ''
  query.status = null
  query.current = 1
  loadData()
}

/* ---------------- 处理弹窗（三模式） ---------------- */

const dialogVisible = ref(false)
const dialogMode = ref('assign')          // assign=分配处理人 / handle=编写处理意见 / view=查看结果
const submitting = ref(false)
const formRef = ref()

const dialogTitle = computed(() => ({
  assign: '分配处理人',
  handle: '编写处理意见',
  view: '处理结果'
}[dialogMode.value]))

// 弹窗表单数据：字段名对齐后端 AlarmRecord 实体
const form = reactive({
  id: null,
  deviceName: '',
  alarmContent: '',
  handleUser: '',     // 处理人（分配阶段写入 handle_user）
  handleRemark: '',   // 处理意见（闭环阶段写入 handle_result）
  alarmTime: '',      // 查看阶段展示
  handleTime: '',     // 查看阶段展示（完成时间）
  handleResult: ''    // 查看阶段展示（处理结果）
})

// 校验规则随模式变化：分配阶段校验处理人，编写阶段校验处理意见
const formRules = computed(() => {
  if (dialogMode.value === 'assign') {
    return { handleUser: [{ required: true, message: '请输入处理人', trigger: 'blur' }] }
  }
  if (dialogMode.value === 'handle') {
    return { handleRemark: [{ required: true, message: '请输入处理意见', trigger: 'blur' }] }
  }
  return {}
})

// 重置表单（弹窗关闭时触发）
function resetForm() {
  form.id = null
  form.deviceName = ''
  form.alarmContent = ''
  form.handleUser = ''
  form.handleRemark = ''
  form.alarmTime = ''
  form.handleTime = ''
  form.handleResult = ''
  formRef.value?.clearValidate()
}

// 打开「分配处理人」：未处理(0) → 处理中(1)
function openAssign(row) {
  resetForm()
  form.id = row.id
  form.deviceName = row.deviceName
  form.alarmContent = row.alarmContent
  dialogMode.value = 'assign'
  dialogVisible.value = true
}

// 打开「编写处理意见」：处理中(1) → 已闭环(2)
function openHandle(row) {
  resetForm()
  form.id = row.id
  form.deviceName = row.deviceName
  form.alarmContent = row.alarmContent
  dialogMode.value = 'handle'
  dialogVisible.value = true
}

// 打开「查看结果」：已闭环(2) 只读展示
function openView(row) {
  resetForm()
  form.id = row.id
  form.deviceName = row.deviceName
  form.alarmContent = row.alarmContent
  form.alarmTime = row.alarmTime
  form.handleTime = row.handleTime
  form.handleUser = row.handleUser
  form.handleResult = row.handleResult
  dialogMode.value = 'view'
  dialogVisible.value = true
}

// 提交：按当前模式走不同分支，绝不再把意见错写进处理人、也不再一步跳到已闭环
async function handleSubmit() {
  // 1. 表单校验
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  // 2. 按模式调用 handleAlarm 接口
  submitting.value = true
  try {
    if (dialogMode.value === 'assign') {
      // 分配处理人：status=1，写入 handleUser
      await handleAlarm({
        id: form.id,
        status: 1,
        handleUser: form.handleUser
      })
      ElMessage.success('已分配处理人，告警进入处理中')
    } else if (dialogMode.value === 'handle') {
      // 编写处理意见：status=2，写入 handleResult（后端记录完成时间）
      await handleAlarm({
        id: form.id,
        status: 2,
        handleResult: form.handleRemark
      })
      ElMessage.success('处理完成，告警已闭环')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

/* ---------------- WebSocket 实时推送 ---------------- */

// 告警类型/级别中文映射（供通知文案用）
const levelText = { 1: '严重', 2: '重要', 3: '一般' }
const typeText = { OFFLINE: '离线告警', OVERVOLTAGE: '过压告警', OVERCURRENT: '过流告警', ABNORMAL: '其他异常' }

let unsubAlarm = null

function onAlarmWsMessage(msg) {
  if (!msg || !msg.event) return
  if (msg.event === 'alarm_new') {
    const d = msg.data || {}
    ElNotification({
      title: `新告警 · ${typeText[d.alarmType] || d.alarmType || ''}（${levelText[d.alarmLevel] || ''}）`,
      message: d.alarmContent || `设备 ${d.deviceId} 触发告警`,
      type: 'error',
      duration: 6000
    })
    // 收到新告警：回到第一页刷新，确保最新告警可见
    query.current = 1
    loadData()
  } else if (msg.event === 'alarm_handled') {
    // 告警状态变更：静默刷新当前页
    loadData()
  }
}

/* ---------------- 模拟告警（演示用） ---------------- */

const mocking = ref(false)
async function handleMockAlarm() {
  const types = ['OFFLINE', 'OVERVOLTAGE', 'OVERCURRENT', 'ABNORMAL']
  const levels = [1, 2, 3]
  const atype = types[Math.floor(Math.random() * types.length)]
  const level = levels[Math.floor(Math.random() * levels.length)]
  mocking.value = true
  try {
    await addAlarm({
      deviceId: 'D-MOCK-' + Math.floor(Math.random() * 9000 + 1000),
      alarmType: atype,
      alarmLevel: level,
      alarmContent: `模拟${typeText[atype]}：用于演示 WebSocket 实时推送`
    })
    ElMessage.success('已触发模拟告警，观察实时推送')
  } finally {
    mocking.value = false
  }
}

/* ---------------- 初始化 ---------------- */

onMounted(() => {
  loadData()
  connectAlarmWS()
  unsubAlarm = onAlarmMessage(onAlarmWsMessage)
})

onUnmounted(() => {
  if (unsubAlarm) unsubAlarm()
  disconnectAlarmWS()
})
</script>

<style scoped>
.alarm-page {
  padding: 0;
}
.search-card {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
