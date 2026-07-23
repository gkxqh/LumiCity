# 照明系统 · 数据库初始化与测试数据工具

独立于主项目（Spring Boot / Vue）的小工具，用于一键管理本地 MySQL：启动、建库、灌测试数据。

## 快速使用

```bash
cd tools/db-tool
./run.sh          # 自动建虚拟环境、装依赖、启动
```

浏览器打开 http://localhost:5050

> 默认连接配置取自后端 `application.yml`：`localhost:3306 / root / 123456 / smart_lighting`，可在面板上改。

## 功能按钮（每个功能独立操作）

| 按钮 | 接口 | 作用 |
| --- | --- | --- |
| ① 检测并启动 MySQL | `/api/start-mysql` | 探测 3306 端口；未开放则依次尝试 `brew services start mysql` / `mysql@8.0` / `mysql.server start` / 官方安装目录脚本，启动后回探确认 |
| ② 初始化数据库 | `/api/init-db` | 执行 `schema.sql`：DROP 并重建 `smart_lighting`，建 13 张表 + 初始 admin/角色/菜单 |
| ③ 生成测试数据 | `/api/gen-data` | 清空业务表与测试用户（**保留 admin 及初始角色/菜单**），再随机生成覆盖各种情况的测试数据 |
| ⚡ 一键全流程 | `/api/all` | 依次执行 启动 → 初始化 → 生成数据 |
| 📊 数据概览 | `/api/overview` | 检测 MySQL/库状态，并返回各表数据量 |

## 生成的测试数据覆盖范围

- **用户/角色**：8 个随机用户（运维/巡检，约 15% 禁用），新增 INSPECTOR 角色；密码均为 `123456` 的 MD5
- **灯杆**：30 根，分布 5 个区域，经纬度围绕真实城市中心散布，状态 在线/离线/故障 ≈ 7:2:1
- **设备**：每杆 1~4 台，覆盖 LIGHT/CAMERA/SENSOR/LED_SCREEN/BROADCAST 全类型，状态与所属灯杆联动
- **照明策略**：TIME/LIGHT/TRAFFIC 三类，含亮度/时段/星期/启停
- **能耗记录**：15 台照明灯 × 7 天 × 8 次/天 时序数据（电压/电流/功率/用电量）
- **告警**：50 条，覆盖 4 种类型 × 3 级别 × 3 状态（未处理/处理中/已闭环），含处理人/处理时间
- **环境数据**：10 根传感器杆 × 7 天 × 24 小时时序，温度/湿度/PM2.5(含污染高峰)/光照(昼夜正弦变化)/风速风向
- **视频/LED/工单**：摄像头含 RTSP 与云台；LED 节目三种媒体三种状态；工单覆盖巡检/维修 × 4 种流转状态

每次生成均随机，且会先清空旧测试数据（幂等）。

## 文件说明

```
tools/db-tool/
├── app.py              Flask 应用 + 内嵌 Web 面板
├── data_generator.py   测试数据生成逻辑
├── schema.sql          建表脚本（从 smart-lighting/sql 拷贝，工具自包含）
├── requirements.txt    flask / pymysql
└── run.sh              启动脚本（自动管理虚拟环境）
```

## 依赖

- Python 3.10+
- 本地已安装 MySQL 8.x（启动功能支持 Homebrew / 官方 DMG 两种安装方式自动探测）
