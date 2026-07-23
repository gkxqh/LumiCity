# 智慧城市照明综合控制系统 - 前端

> Vue 3 + Element Plus + ECharts 后台管理系统，对接后端 Spring Boot API。

## 快速启动

```bash
cd smart-lighting-web
npm install        # 安装依赖
npm run dev        # 启动开发服务器（默认 5173 端口）
```

确保后端已启动（8080 端口），前端通过 Vite 代理转发 `/api` 到后端。

## 技术栈与使用方式

### 1. Vue 3（核心框架）
- **语法**：Composition API + `<script setup>` 语法糖
- **响应式**：`ref()` 管理基本类型/数组，`reactive()` 管理对象
- **生命周期**：`onMounted` 加载数据，`onUnmounted` 清理 ECharts
- **用法示例**：
```vue
<script setup>
import { ref, reactive, onMounted } from 'vue'
const list = ref([])                    // 响应式数组
const query = reactive({ name: '' })   // 响应式对象
onMounted(() => { loadData() })        // 页面加载时调接口
</script>
```

### 2. Element Plus（UI 组件库）
- **全局注册**：在 main.js 里 `app.use(ElementPlus)`
- **常用组件**：
  - `ElTable` + `ElTableColumn` — 数据表格
  - `ElPagination` — 分页
  - `ElForm` + `ElFormItem` + `ElInput`/`ElSelect` — 表单
  - `ElDialog` — 弹窗（新增/编辑）
  - `ElButton`/`ElTag`/`ElCard`/`ElMenu` — 按钮/标签/卡片/菜单
  - `ElMessage`/`ElMessageBox` — 消息提示/确认框
- **图标**：`@element-plus/icons-vue`，已全局注册，用 `<el-icon><User /></el-icon>`

### 3. ECharts 5（数据可视化）
- **用在**：数据大盘（折线图+饼图）、能耗管理（趋势图）、环境监测（趋势图）
- **用法**：
```javascript
import * as echarts from 'echarts'
const chartRef = ref(null)
let chart = null
onMounted(() => {
  chart = echarts.init(chartRef.value)    // 初始化
  chart.setOption({ ... })                // 配置图表
  window.addEventListener('resize', () => chart.resize())
})
onUnmounted(() => {
  chart.dispose()                         // 销毁，防止内存泄漏
  window.removeEventListener('resize', ...)
})
```

### 4. Axios（HTTP 请求）
- **封装位置**：`src/api/request.js`
- **请求拦截器**：自动从 localStorage 取 token，加到请求头 `Authorization: Bearer xxx`
- **响应拦截器**：统一处理后端 Result 结构，code=200 返回 data，401 跳登录页
- **使用方式**：
```javascript
import request from '@/api/request'
export function pageDevice(params) {
  return request({ url: '/device/page', method: 'get', params })
}
// 调用时：const res = await pageDevice(query); res.data 就是后端返回的 data
```

### 5. Vue Router 4（路由）
- **配置**：`src/router/index.js`
- **路由守卫**：`router.beforeEach` 检查 token，没 token 跳登录页
- **布局路由**：`/` 下挂载 layout 组件，子路由用 `<router-view>` 渲染

### 6. Pinia（状态管理）
- **用户 Store**：`src/store/user.js`，管理 token/username/nickname
- **用法**：
```javascript
import { useUserStore } from '@/store/user'
const userStore = useUserStore()
await userStore.login(loginForm)    // 登录
userStore.logout()                  // 登出
```

### 7. Vite（构建工具）
- **开发代理**：`vite.config.js` 里配置 `/api` 代理到 `localhost:8080`，解决跨域
- **路径别名**：`@` 指向 `src` 目录

## 项目结构

```
smart-lighting-web/
├── package.json              依赖配置
├── vite.config.js            Vite 配置（代理+别名）
├── index.html                入口 HTML
└── src/
    ├── main.js               入口文件（注册 Element Plus）
    ├── App.vue               根组件
    ├── style.css             全局样式
    ├── api/                  接口封装
    │   ├── request.js        Axios 封装（拦截器+token）
    │   ├── auth.js           登录接口
    │   ├── device.js         设备/灯杆接口
    │   └── other.js          其余模块接口
    ├── router/index.js       路由配置（含守卫）
    ├── store/user.js         Pinia 用户状态
    ├── utils/auth.js         token 存取工具
    ├── layout/index.vue      布局组件（侧边栏+顶栏）
    └── views/                页面
        ├── login/index.vue   登录页
        ├── dashboard/        数据大盘（ECharts）
        ├── device/           设备管理 + 灯杆管理
        ├── lighting/         照明控制
        ├── energy/           能耗管理（ECharts）
        ├── alarm/            故障告警
        ├── video/            视频监控
        ├── environment/      环境监测（ECharts）
        ├── publish/          信息发布
        ├── workorder/        工单运维
        └── system/           系统管理
```

## 页面与技术对照

| 页面 | 用到的技术 | 核心功能 |
|------|-----------|----------|
| 登录页 | ElForm + Pinia | 表单校验 + JWT 登录 |
| 数据大盘 | ECharts + ElCard | 指标卡片 + 折线图 + 饼图 |
| 设备管理 | ElTable + ElDialog + Axios | 完整 CRUD（查增改删） |
| 灯杆管理 | ElTable + ElDialog | CRUD |
| 照明控制 | ElSlider + ElSwitch | 实时控制 + 策略管理 |
| 能耗管理 | ECharts + ElTable | 趋势图 + 记录列表 |
| 故障告警 | ElTable + ElTag | 告警列表 + 处理 |
| 视频监控 | ElTable + ElDialog | 摄像头 CRUD |
| 环境监测 | ECharts + ElCard | 数据卡片 + 趋势图 |
| 信息发布 | ElTable + ElDialog | 节目 CRUD + 发布 |
| 工单运维 | ElTable + ElDialog | 工单流转（派单/处理/验收） |
| 系统管理 | ElTable + ElDialog | 用户 CRUD |

## 默认登录账号

```
用户名：admin
密码：123456
```
