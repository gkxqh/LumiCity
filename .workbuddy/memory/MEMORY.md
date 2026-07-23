# LumiCity 项目记忆

## 项目性质
- 仓库 = 「智慧城市照明综合控制系统」全栈**教学蓝本（学习蓝本）**，非生产系统。
- 用途推断：软件工程综合练习 / 考试配套示例（用户有软工备考背景）。
- pom.xml 与前端 package.json 描述均写「学习蓝本」。

## 技术栈
- 后端 `smart-lighting/`：Spring Boot 3.2.5 (Java 17) + MyBatis-Plus 3.5.7 + JWT(jjwt 0.12.5) + WebSocket + EasyExcel 3.3.4 + springdoc 2.3.0。包名 `com.ccb.lighting`，端口 8080，context-path `/api`，DB=`smart_lighting`(MySQL8)。
- 前端 `smart-lighting-web/`：Vue3 + Element Plus + ECharts5 + Pinia + Vue Router4 + Axios + Vite(5173，代理 /api→8080)。

## 模块（后端 10+1）
auth/system/device/lighting/energy/alarm/video/environment/publish/workorder + dashboard(聚合)。RBAC 五表 + 设备2 + 业务6 = 13 张表（sql/schema.sql）。初始 admin / 123456。

## 运行现状
- logs/lighting.log 显示 2026-07-23 13:04 后端成功启动并连上本地 MySQL。
- 本地 MySQL(root/123456) 已运行；smart_lighting 库已存在且结构完整，已有部分测试数据(50杆/200设备/8400能耗/30告警，但env/led/strategy/video/workorder表为空)。

## 数据库工具 tools/db-tool/（2026-07-23 新增，独立于主项目）
- Python+Flask 本地 Web 面板，端口 5050。本地 venv `.venv`，依赖 flask+pymysql。启动：`./run.sh`。
- 5 个按钮：①检测启动MySQL ②初始化库(跑schema.sql,drop重建) ③生成测试数据 ④一键全流程 ⑤数据概览。
- data_generator.py：清空业务表+测试用户(保留admin与初始角色菜单)→随机生成13表数据(8用户/30杆/79设备/20策略/840能耗/50告警/16摄像/1176环境/15LED/25工单)，含时序昼夜变化，每次随机幂等。已在临时库验证通过。
- schema.sql 是 smart-lighting/sql/schema.sql 的自包含拷贝。
