# 智慧城市照明综合控制系统

> 基于 Spring Boot 3.x 的智慧城市照明综合控制系统后端。
> 涵盖 IoC/DI/MVC 三层架构、MyBatis-Plus 单表 CRUD、JWT 认证、WebSocket 通信、EasyExcel 导入导出等常用技术。

## 一、项目简介

本项目模拟真实的"智慧城市照明综合控制系统"后端，覆盖路灯、摄像头、传感器、LED 屏等城市照明基础设施的统一管理：

- **三层架构**：Controller → Service(interface+impl) → Mapper 的标准分层
- **构造器注入**：`@RequiredArgsConstructor` + `private final` 字段
- **MyBatis-Plus**：BaseMapper 单表 CRUD + LambdaQueryWrapper 条件构造
- **统一返回**：Result 包装 + 全局异常处理 + 状态码枚举
- **基础实体**：BaseEntity + 自动填充字段 + 逻辑删除
- **业务模块化**：10 大功能模块按业务拆分包组织

## 二、技术栈

| 类别 | 技术 | 版本 | 说明 |
| --- | --- | --- | --- |
| 框架 | Spring Boot | 3.2.5 | 主框架 |
| ORM | MyBatis-Plus | 3.5.7 | 单表 CRUD + 分页 |
| 数据库 | MySQL | 8.x | 关系型数据库 |
| 认证 | JWT (jjwt) | 0.12.5 | 无状态登录 |
| 通信 | WebSocket | starter | 实时告警推送 |
| 导入导出 | EasyExcel | 3.3.4 | Excel 批量处理 |
| 接口文档 | springdoc | 2.3.0 | OpenAPI 3 |
| 工具 | Lombok | starter | 简化 POJO |
| 前端 | Vue3 | - | Element Plus + ECharts |

## 三、项目结构说明

```
smart-lighting/
├── pom.xml                          # Maven 依赖配置
├── sql/
│   └── schema.sql                   # 建表脚本 + 初始数据
├── src/main/
│   ├── java/com/ccb/lighting/
│   │   ├── SmartLightingApplication.java   # 启动类
│   │   ├── common/                  # 通用基础（已就绪）
│   │   │   ├── BaseEntity.java      # 基础实体（id/时间/创建人/逻辑删除）
│   │   │   ├── BusinessException.java# 业务异常
│   │   │   ├── GlobalExceptionHandler.java # 全局异常处理
│   │   │   ├── PageQuery.java       # 分页基类
│   │   │   ├── Result.java          # 统一返回
│   │   │   └── ResultCode.java      # 状态码枚举
│   │   ├── config/                  # 配置类（MyBatis-Plus / JWT / WebSocket）
│   │   ├── security/                # JWT 认证、拦截器
│   │   └── module/                  # 业务模块（按业务分包）
│   │       ├── system/              # 系统管理（用户/角色/菜单）
│   │       ├── device/              # 设备管理（灯杆/设备）
│   │       ├── lighting/            # 智能照明控制
│   │       ├── energy/              # 能耗管理
│   │       ├── alarm/               # 故障告警
│   │       ├── video/               # 视频监控
│   │       ├── environment/         # 环境监测
│   │       ├── publish/             # 信息发布
│   │       ├── workorder/          # 工单运维
│   │       └── dashboard/          # 数据大盘（聚合查询）
│   └── resources/
│       └── application.yml         # 应用配置
└── README.md                        # 本文件
```

### 模块化组织约定

每个业务模块内部结构统一：

```
module/<业务名>/
├── entity/              # 实体类（继承 BaseEntity）
├── mapper/              # Mapper 接口（继承 BaseMapper）
├── service/             # Service 接口
│   └── impl/           # Service 实现类
├── controller/         # Controller（RESTful 接口）
└── dto/                # 数据传输对象（查询条件等，按需创建）
```

## 四、快速启动步骤

### 1. 准备环境

- JDK 17+
- Maven 3.8+
- MySQL 8.x

### 2. 建库建表

```bash
# 登录 MySQL，执行建表脚本
mysql -uroot -p < sql/schema.sql
```

脚本会自动：
- 创建 `smart_lighting` 数据库
- 创建 13 张表（系统表 + 设备表 + 业务表）
- 插入初始 admin 用户（密码：`123456`）、管理员角色、基础菜单

### 3. 修改配置

打开 `src/main/resources/application.yml`，按需修改数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smart_lighting?...
    username: root
    password: 123456    # 改成你的 MySQL 密码
```

### 4. 启动项目

```bash
# 在项目根目录执行
mvn spring-boot:run
```

或用 IDE 打开项目，运行启动类 `SmartLightingApplication`。

### 5. 验证启动

启动成功后访问：
- 接口文档：http://localhost:8080/api/swagger-ui.html
- 健康检查：http://localhost:8080/api/actuator/health（需引入 actuator）

## 五、10 大功能模块说明

对应立项申请书的 10 大功能模块，分布如下：

| 序号 | 模块 | 包路径 | 路径前缀 | 说明 |
| --- | --- | --- | --- | --- |
| 1 | 用户登录鉴权 | module.system | /auth | JWT 登录、token 刷新 |
| 2 | 系统管理 | module.system | /system | 用户、角色、菜单管理 |
| 3 | 设备管理 | module.device | /device | 灯杆、设备 CRUD |
| 4 | 智能照明控制 | module.lighting | /lighting | 策略 CRUD + 实时开关/调光 |
| 5 | 能耗管理 | module.energy | /energy | 能耗记录、趋势、统计 |
| 6 | 故障告警 | module.alarm | /alarm | 告警查询、处理闭环、统计 |
| 7 | 视频监控 | module.video | /video/camera | 摄像头 CRUD、播放地址 |
| 8 | 环境监测 | module.environment | /env | 环境数据查询、最新值、趋势 |
| 9 | 信息发布 | module.publish | /publish/program | LED 节目 CRUD、发布 |
| 10 | 工单运维 | module.workorder | /workorder | 工单创建、派单、处理、完成 |

> 数据大盘（module.dashboard）为聚合查询模块，不单独立项，对应首页综合展示。

## 六.接口文档地址

启动项目后，访问 Swagger UI：

```
http://localhost:8080/api/swagger-ui.html
```

接口路径前缀统一为 `/api`（由 `application.yml` 的 `server.servlet.context-path` 配置）。
