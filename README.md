# 智慧城市照明综合控制系统

基于 **Spring Boot 3.x + Vue 3** 的智慧城市照明综合控制系统，涵盖路灯、摄像头、传感器、LED 屏等城市照明基础设施的统一管理。

## 技术栈

### 后端

| 类别 | 技术 | 版本 |
| --- | --- | --- |
| 框架 | Spring Boot | 3.2.5 |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.x |
| 认证 | JWT (jjwt) | 0.12.5 |
| 实时通信 | WebSocket (Spring Boot Starter) | — |
| 导入导出 | EasyExcel | 3.3.4 |
| 接口文档 | SpringDoc OpenAPI | 2.3.0 |
| 构建工具 | Maven | 3.8+ |
| JDK | OpenJDK | 17 |

### 前端

| 类别 | 技术 | 版本 |
| --- | --- | --- |
| 框架 | Vue 3 (Composition API) | ^3.4.0 |
| 构建 | Vite | ^5.2.0 |
| UI 组件 | Element Plus | ^2.6.0 |
| 状态管理 | Pinia | ^2.1.0 |
| 路由 | Vue Router 4 | ^4.3.0 |
| 图表 | ECharts | ^5.5.0 |
| 地图 | Leaflet | ^1.9.4 |
| 3D 可视化 | Three.js | ^0.185.1 |
| 移动端 | Vant 4 | ^4.10.0 |
| HTTP 请求 | Axios | ^1.6.0 |

### 工具

| 类别 | 技术 |
| --- | --- |
| 数据库初始化 | Python 3 + Flask + PyMySQL |
| 测试数据生成 | Python (random + bcrypt) |

## 项目结构

```
LumiCity/
├── smart-lighting/               # Spring Boot 后端
│   ├── pom.xml                   # Maven 依赖
│   ├── sql/
│   │   └── schema.sql            # 建表脚本 + 初始数据
│   └── src/main/java/com/ccb/lighting/
│       ├── SmartLightingApplication.java  # 启动类
│       ├── common/                # 通用基础（统一返回、异常处理、分页、文件上传）
│       ├── config/                # 配置类（MyBatis-Plus 自动填充、分页、CORS、WebSocket）
│       ├── security/              # JWT 认证（拦截器、工具、自定义注解）
│       ├── handler/               # WebSocket 处理器
│       └── module/                # 10 大业务模块
│           ├── system/            # 系统管理（用户/角色/菜单）
│           ├── device/            # 设备管理（灯杆/设备/区域）
│           ├── lighting/          # 智能照明（策略/实时控制）
│           ├── energy/            # 能耗管理
│           ├── alarm/             # 故障告警
│           ├── video/             # 视频监控
│           ├── publish/           # 信息发布（LED 节目）
│           ├── workorder/         # 工单运维
│           └── dashboard/         # 数据大盘（聚合查询）
│
├── smart-lighting-web/           # Vue 3 前端
│   ├── index.html                # PC 端入口
│   ├── mobile.html               # 移动端独立入口
│   ├── vite.config.js            # Vite 配置（代理 /api → 后端）
│   ├── package.json
│   └── src/
│       ├── api/                  # API 接口层（Axios 封装 + 6 个模块）
│       ├── store/                # Pinia 状态管理（用户/背景主题）
│       ├── router/               # 路由配置 + 路由守卫
│       ├── layout/               # 后台布局（侧边栏+顶栏）
│       ├── components/           # 通用组件（Leaflet 地图 / Three.js 3D）
│       ├── views/                # 13 个 PC 页面
│       └── mobile/               # 移动端（5 个页面，Vant UI）
│
└── tools/
    └── db-tool/                  # 数据库初始化工具（Python Flask）
        ├── app.py                # Flask Web 面板
        ├── data_generator.py     # 测试数据生成器
        ├── schema.sql            # 建表脚本（自包含）
        └── run.sh                # 启动脚本
```

### 后端模块化组织

每个业务模块内部结构统一：

```
module/<业务名>/
├── entity/              # 实体类（继承 BaseEntity）
├── mapper/              # Mapper 接口（继承 BaseMapper）
├── service/             # Service 接口
│   └── impl/            # Service 实现类
├── controller/          # Controller（RESTful 接口）
└── dto/                 # 数据传输对象（查询条件等，按需创建）
```

## 数据库

### 设计约定

- 主键统一使用 `BIGINT AUTO_INCREMENT`
- 所有实体继承 `BaseEntity`（id、create_time、update_time、create_by、update_by、deleted）
- `deleted` 字段实现逻辑删除（MyBatis-Plus @TableLogic）
- `create_time` / `update_time` 由 MetaObjectHandler 自动填充
- 字符集 `utf8mb4`

### 表结构（13 张表）

**系统基础表（5 张）**

| 表名 | 说明 |
| --- | --- |
| `sys_user` | 系统用户（用户名/BCrypt 密码/昵称/手机/邮箱/状态） |
| `sys_role` | 系统角色（编码/名称/状态） |
| `sys_user_role` | 用户-角色关联 |
| `sys_menu` | 系统菜单/权限（目录/菜单/按钮三级类型，树形结构） |
| `sys_role_menu` | 角色-菜单关联 |

**设备基础表（3 张）**

| 表名 | 说明 |
| --- | --- |
| `region` | 行政区划（扁平化区级，如锦江区、武侯区） |
| `dev_pole` | 灯杆（编号/名称/区域/道路/编号/经纬度/高度/在线状态/照明状态/亮度） |
| `dev_device` | 设备（编号/名称/类型/所属灯杆/型号/厂商/在线状态） |

设备类型：`LIGHT`（照明灯）、`CAMERA`（摄像头）、`SENSOR`（传感器）、`LED_SCREEN`（LED 屏）、`BROADCAST`（广播）

**业务表（5 张）**

| 表名 | 说明 |
| --- | --- |
| `light_strategy` | 照明策略（TIME/LIGHT/TRAFFIC 三类，含亮度/时段/星期/启停） |
| `energy_record` | 能耗记录（电压/电流/功率/用电量，时序数据） |
| `alarm_record` | 告警记录（OFFLINE/OVERVOLTAGE/OVERCURRENT/ABNORMAL，三级状态流转） |
| `video_camera` | 视频摄像头（名称/所属灯杆/RTSP 流地址/状态/云台/分辨率） |
| `led_program` | LED 节目（名称/内容/媒体类型/播放模式/发布状态） |
| `work_order` | 工单（编号/类型/标题/告警关联/优先级/三级状态流转） |

**日志表（2 张）**

| 表名 | 说明 |
| --- | --- |
| `led_publish_log` | LED 节目发布记录（每次发布的详细信息追溯） |
| `light_command_log` | 照明控制指令日志（每灯杆指令结果记录） |

## 快速启动

### 1. 准备环境

- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Node.js 18+
- Python 3.10+（仅数据库初始化工具需要）

### 2. 初始化数据库

**方式一：使用数据库初始化工具（推荐）**

```bash
cd tools/db-tool
./run.sh
# 浏览器打开 http://localhost:5050
# 点击"一键全流程"按钮即可完成建库、建表和测试数据生成
```

**方式二：手动执行 SQL**

```bash
mysql -uroot -p < smart-lighting/sql/schema.sql
```

脚本会自动：
- 创建 `smart_lighting` 数据库
- 创建全部 13 张表
- 插入初始数据：admin 用户（密码 `123456`）、ADMIN/OPERATOR 两个角色、8 个区域、9 个一级目录菜单及按钮级权限

### 3. 启动后端

修改 `smart-lighting/src/main/resources/application.yml` 中的数据库连接配置（用户名/密码）：

```yaml
spring:
  datasource:
    username: root
    password: 123456    # 改为你的 MySQL 密码
```

然后在项目目录执行：

```bash
cd smart-lighting
mvn spring-boot:run
```

或用 IDE 打开项目，运行启动类 `SmartLightingApplication`。

启动后访问：
- 接口文档：http://localhost:8080/api/swagger-ui.html
- 默认登录账号：`admin` / `123456`

### 4. 启动前端

```bash
cd smart-lighting-web
npm install
npm run dev
```

开发服务器默认运行在 http://localhost:5173，Vite 自动将 `/api` 请求代理到后端 8080 端口。

### 5. 生成测试数据（可选，如已通过工具初始化则跳过）

启动后端后，在数据大盘页面可查看已生成的统计数据。如需更多测试数据，可使用数据库初始化工具的"生成测试数据"功能生成全套随机测试数据。

## 功能模块

### 后端 API

| 模块 | 路径前缀 | 接口数 | 说明 |
| --- | --- | --- | --- |
| 认证 | `/auth` | 4 | 登录、注册、登出、获取用户信息 |
| 系统管理 | `/system/user` | 6 | 用户 CRUD + 角色分配 |
| 系统角色 | `/system/role` | 6 | 角色 CRUD + 菜单分配 |
| 系统菜单 | `/system/menu` | 5 | 菜单管理 + 树形结构 |
| 灯杆管理 | `/device/pole` | 5 | 灯杆 CRUD + 列表 |
| 设备管理 | `/device` | 7 | 设备 CRUD + 导入导出 |
| 区域管理 | `/device/region` | 5 | 区域 CRUD |
| 照明策略 | `/lighting/strategy` | 5 | 策略 CRUD |
| 照明控制 | `/lighting/control` | 5 | 单灯/批量开关与调光 |
| 能耗管理 | `/energy` | 5 | 记录查询/趋势/统计/导出 |
| 故障告警 | `/alarm` | 5 | 告警 CRUD/处理/统计 |
| 视频监控 | `/video/camera` | 6 | 摄像头 CRUD/流地址/抓拍 |
| 环境监测 | `/env` | 3 | 环境数据查询/最新值/趋势 |
| 信息发布 | `/publish/program` | 6 | 节目 CRUD/发布/历史 |
| 工单运维 | `/workorder` | 6 | 工单创建/派单/处理/完成 |
| 数据大盘 | `/dashboard` | 7 | 聚合统计/趋势/分布 |
| 文件上传 | `/common/upload` | 1 | 通用文件上传 |

### 前端页面

**PC 端（15 页面）**

| 页面 | 路径 | 核心功能 |
| --- | --- | --- |
| 登录页 | `/login` | 液态玻璃风格登录，记住我功能 |
| 注册页 | `/register` | 用户自助注册 |
| 数据大盘 | `/dashboard` | Leaflet 地图 + Three.js 3D + 统计卡片 + ECharts 图表 + WebSocket 实时告警 (每 30s 轮询) |
| 设备管理 | `/device` | 设备 CRUD + Excel 批量导入/导出 |
| 灯杆管理 | `/pole` | 灯杆 CRUD + 区域/道路筛选 + 照明状态/亮度展示 |
| 照明控制 | `/lighting` | 单灯控制 + 按道路批量控制 + 照明策略管理 |
| 能耗管理 | `/energy` | 四维统计卡片 + ECharts 趋势图 + 导出报表 |
| 故障告警 | `/alarm` | 告警展示 + 模拟告警 + 处理闭环 + WebSocket 实时推送 |
| 视频监控 | `/video` | 摄像头 CRUD + HLS/MP4/MJPEG/RTSP 多协议播放 + 抓拍 |
| 环境监测 | `/environment` | 环境数据展示 + 图表趋势 |
| 信息发布 | `/publish` | LED 节目 CRUD + 发布 + 文件上传 + 内容预览 + 发布历史 |
| 工单运维 | `/workorder` | 告警工单/手动工单 + 派单/处理/完成流转 |
| 系统管理 | `/system` | 用户管理 CRUD + 角色分配 |
| 权限管理 | `/system/permission` | 角色管理 + 菜单树形管理 + 分配菜单 |
| 背景设置 | 侧边栏入口 | 5 套渐变色主题 + 自定义图片背景 |

**移动端（5 页面，独立入口）**

| 页面 | 核心功能 |
| --- | --- |
| 首页 | 统计卡片 + 待处理告警 + 快速入口 |
| 告警管理 | Tabs 筛选 + 标记处理 + 浮动新增 |
| 照明控制 | 按道路批量 + 单灯开关/调光 |
| 灯杆查询 | 搜索 + 区域筛选 + 上拉加载 |

### 关键技术要点

- **JWT 无状态认证**：jjwt 0.12.5 生成/解析 token，自定义 `@RequiresPerms` 注解实现方法级权限校验，ADMIN 角色自动放行
- **WebSocket 实时推送**：告警创建/处理/工单完成时广播推送，前端断线 3 秒自动重连
- **照明控制模拟通信**：控制指令发出后，离线灯杆自动跳过并记录 SKIPPED 日志
- **灯杆地址模型**：由 `region_id + road + number` 三个独立字段存储地址，`pole_name` 和 `address` 由服务层自动拼接
- **批量操作**：基于 `road` 字段实现按道路批量控制，零 join 零递归
- **FastExcel 导入导出**：行级监听器实现 100 条批量插入，配合设备编号查重校验
- **数据大盘**：Leaflet 暗色地图 + Three.js 3D 发光立柱 + ECharts 真实数据库聚合图表
- **RBAC 权限体系**：用户 → 角色 → 菜单三级模型，目录/菜单/按钮三级粒度，支持按钮级权限控制
- **告警与工单联动**：告警处理闭环时自动创建维修工单，形成完整的故障流转链路

## 开发环境

```bash
# 后端启动
cd smart-lighting
mvn spring-boot:run

# 前端启动
cd smart-lighting-web
npm run dev

# 数据库工具
cd tools/db-tool
./run.sh
```

- 后端端口：8080（API 路径前缀 `/api`）
- 前端端口：5173（Vite 反向代理 `/api` 到 8080）
- 启动类：`com.ccb.lighting.SmartLightingApplication`
- Swagger 文档：http://localhost:8080/api/swagger-ui.html
