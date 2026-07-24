-- =============================================================================
-- 智慧城市照明综合控制系统 - 数据库建表脚本
-- 项目：smart-lighting
-- 数据库：MySQL 8.x
-- 字符集：utf8mb4（支持 emoji 等四字节字符）
--
-- 建表顺序：
--   1. 系统基础表：sys_user / sys_role / sys_user_role / sys_menu / sys_role_menu
--   2. 设备基础表：dev_pole / dev_device
--   3. 业务表：light_strategy / energy_record / alarm_record
--              video_camera / env_sensor_data / led_program / work_order
--
-- 通用约定：
--   - 主键 id BIGINT AUTO_INCREMENT
--   - create_time / update_time：创建时间、更新时间（MetaObjectHandler 自动填充）
--   - create_by / update_by：创建人、更新人（MetaObjectHandler 自动填充）
--   - deleted TINYINT DEFAULT 0：逻辑删除字段（@TableLogic 自动过滤）
-- =============================================================================

-- 创建数据库
DROP DATABASE IF EXISTS smart_lighting;
CREATE DATABASE smart_lighting DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE smart_lighting;

-- =============================================================================
-- 1. 系统基础表（用户、角色、菜单、权限）
-- =============================================================================

-- -----------------------------------------------------------------------------
-- sys_user 系统用户表
-- 存储登录系统的用户：管理员、运维人员、巡检人员
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名（登录账号，唯一）',
    password    VARCHAR(100) NOT NULL COMMENT '密码（MD5 加密存储）',
    nickname    VARCHAR(50)            COMMENT '昵称（中文名，用于展示）',
    phone       VARCHAR(20)            COMMENT '手机号',
    email       VARCHAR(100)          COMMENT '邮箱',
    status      TINYINT      DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time DATETIME              COMMENT '创建时间',
    update_time DATETIME              COMMENT '更新时间',
    create_by   BIGINT                COMMENT '创建人',
    update_by   BIGINT                COMMENT '更新人',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- -----------------------------------------------------------------------------
-- sys_role 系统角色表
-- 角色是权限的集合，用户通过角色关联获得权限
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_code   VARCHAR(50)  NOT NULL COMMENT '角色编码（唯一，如 ADMIN/OPERATOR）',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称（如 系统管理员/运维人员）',
    description VARCHAR(200)          COMMENT '角色描述',
    status      TINYINT      DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time DATETIME              COMMENT '创建时间',
    update_time DATETIME              COMMENT '更新时间',
    create_by   BIGINT                COMMENT '创建人',
    update_by   BIGINT                COMMENT '更新人',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- -----------------------------------------------------------------------------
-- sys_user_role 用户-角色关联表
-- 用户与角色多对多关系（一个用户可有多个角色，一个角色可分配给多个用户）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id          BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT  NOT NULL COMMENT '用户ID',
    role_id     BIGINT  NOT NULL COMMENT '角色ID',
    create_time DATETIME         COMMENT '创建时间',
    update_time DATETIME         COMMENT '更新时间',
    create_by   BIGINT           COMMENT '创建人',
    update_by   BIGINT           COMMENT '更新人',
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- -----------------------------------------------------------------------------
-- sys_menu 系统菜单表
-- 菜单即权限，前端按用户拥有的菜单渲染左侧导航；后端按菜单 code 校验接口权限
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    parent_id   BIGINT       DEFAULT 0 COMMENT '父菜单ID（0表示根菜单）',
    menu_name   VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    menu_type   VARCHAR(20)  NOT NULL COMMENT '类型：DIRECTORY目录/MENU菜单/BUTTON按钮',
    path        VARCHAR(200)          COMMENT '前端路由路径',
    component   VARCHAR(200)          COMMENT '前端组件路径',
    perms       VARCHAR(100)          COMMENT '权限标识（如 lighting:strategy:add）',
    icon        VARCHAR(50)           COMMENT '菜单图标',
    sort        INT          DEFAULT 0 COMMENT '排序值（升序）',
    visible     TINYINT      DEFAULT 1 COMMENT '是否可见：0隐藏 1显示',
    status      TINYINT      DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time DATETIME              COMMENT '创建时间',
    update_time DATETIME              COMMENT '更新时间',
    create_by   BIGINT                COMMENT '创建人',
    update_by   BIGINT                COMMENT '更新人',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

-- -----------------------------------------------------------------------------
-- sys_role_menu 角色-菜单关联表
-- 角色与菜单多对多关系（一个角色拥有多个菜单权限，一个菜单可被多个角色拥有）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    id          BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_id     BIGINT  NOT NULL COMMENT '角色ID',
    menu_id     BIGINT  NOT NULL COMMENT '菜单ID',
    create_time DATETIME         COMMENT '创建时间',
    update_time DATETIME         COMMENT '更新时间',
    create_by   BIGINT           COMMENT '创建人',
    update_by   BIGINT           COMMENT '更新人',
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    KEY idx_role_id (role_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- =============================================================================
-- 2. 设备基础表（灯杆、设备）
-- =============================================================================

-- -----------------------------------------------------------------------------
-- dev_pole 灯杆表
-- 灯杆是物理载体，所有设备（灯、摄像头、传感器、LED屏）都挂载在灯杆上
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS dev_pole;
CREATE TABLE dev_pole (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    pole_code    VARCHAR(50)   NOT NULL COMMENT '灯杆编号（唯一）',
    pole_name    VARCHAR(100)  NOT NULL COMMENT '灯杆名称',
    area_id      BIGINT                  COMMENT '所属区域ID',
    address      VARCHAR(255)            COMMENT '安装地址',
    lng          DECIMAL(10,7)            COMMENT '经度（地图坐标）',
    lat          DECIMAL(10,7)            COMMENT '纬度（地图坐标）',
    height       DECIMAL(5,2)             COMMENT '灯杆高度(米)',
    status       TINYINT       DEFAULT 0 COMMENT '状态：0离线 1在线 2故障',
    install_time DATE                     COMMENT '安装时间',
    create_time  DATETIME                COMMENT '创建时间',
    update_time  DATETIME                COMMENT '更新时间',
    create_by    BIGINT                  COMMENT '创建人',
    update_by    BIGINT                  COMMENT '更新人',
    deleted      TINYINT       DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pole_code (pole_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灯杆表';

-- -----------------------------------------------------------------------------
-- dev_device 设备表
-- 设备是挂载在灯杆上的各类终端：照明灯、摄像头、传感器、LED屏、广播
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS dev_device;
CREATE TABLE dev_device (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    device_code      VARCHAR(50)  NOT NULL COMMENT '设备编号（唯一）',
    device_name      VARCHAR(100) NOT NULL COMMENT '设备名称',
    device_type      VARCHAR(20)  NOT NULL COMMENT '设备类型：LIGHT照明/CAMERA摄像头/SENSOR传感器/LED_SCREEN屏幕/BROADCAST广播',
    pole_id          BIGINT                 COMMENT '所属灯杆ID',
    model            VARCHAR(100)           COMMENT '设备型号',
    vendor           VARCHAR(100)           COMMENT '厂商',
    status           TINYINT      DEFAULT 0 COMMENT '状态：0离线 1在线 2故障',
    last_online_time DATETIME               COMMENT '最后在线时间',
    create_time      DATETIME               COMMENT '创建时间',
    update_time      DATETIME               COMMENT '更新时间',
    create_by        BIGINT                 COMMENT '创建人',
    update_by        BIGINT                 COMMENT '更新人',
    deleted          TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_code (device_code),
    KEY idx_pole_id (pole_id),
    KEY idx_device_type (device_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

-- =============================================================================
-- 3. 业务表（照明策略、能耗、告警、视频、环境、LED节目、工单）
-- =============================================================================

-- -----------------------------------------------------------------------------
-- light_strategy 照明策略表
-- 控制灯杆亮灭与亮度的策略规则（定时/感光/车流）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS light_strategy;
CREATE TABLE light_strategy (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    strategy_name VARCHAR(100) NOT NULL COMMENT '策略名称',
    strategy_type VARCHAR(20)  NOT NULL COMMENT '类型：TIME定时/LIGHT感光/TRAFFIC车流',
    pole_id       BIGINT                 COMMENT '灯杆ID（空表示群组策略）',
    brightness    INT          DEFAULT 100 COMMENT '亮度0-100',
    start_time    TIME                   COMMENT '开始时间',
    end_time      TIME                   COMMENT '结束时间',
    week_days     VARCHAR(20)            COMMENT '周几（1-7逗号分隔）',
    enabled       TINYINT      DEFAULT 1 COMMENT '是否启用：0禁用 1启用',
    create_time   DATETIME               COMMENT '创建时间',
    update_time   DATETIME               COMMENT '更新时间',
    create_by     BIGINT                 COMMENT '创建人',
    update_by     BIGINT                 COMMENT '更新人',
    deleted       TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    KEY idx_strategy_type (strategy_type),
    KEY idx_pole_id (pole_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='照明策略表';

-- -----------------------------------------------------------------------------
-- energy_record 能耗记录表
-- 设备上报的电压、电流、功率、用电量数据（时序数据）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS energy_record;
CREATE TABLE energy_record (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    device_id   VARCHAR(50)   NOT NULL COMMENT '设备ID',
    pole_id     BIGINT                 COMMENT '灯杆ID',
    record_time DATETIME      NOT NULL COMMENT '记录时间',
    voltage     DECIMAL(10,2)          COMMENT '电压(V)',
    current     DECIMAL(10,2)          COMMENT '电流(A)',
    power       DECIMAL(10,2)          COMMENT '功率(W)',
    consumption DECIMAL(10,3)          COMMENT '用电量(kWh)',
    create_time DATETIME               COMMENT '创建时间',
    update_time DATETIME               COMMENT '更新时间',
    create_by   BIGINT                 COMMENT '创建人',
    update_by   BIGINT                 COMMENT '更新人',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    KEY idx_device_id (device_id),
    KEY idx_pole_id (pole_id),
    KEY idx_record_time (record_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能耗记录表';

-- -----------------------------------------------------------------------------
-- alarm_record 告警记录表
-- 设备异常时产生的告警，运维人员处理闭环
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS alarm_record;
CREATE TABLE alarm_record (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    device_id    VARCHAR(50)  NOT NULL COMMENT '设备ID',
    pole_id      BIGINT                 COMMENT '灯杆ID',
    alarm_type   VARCHAR(20)  NOT NULL COMMENT '告警类型：OFFLINE离线/OVERVOLTAGE过压/OVERCURRENT过流/ABNORMAL异常',
    alarm_level  TINYINT      DEFAULT 3 COMMENT '级别：1严重 2重要 3一般',
    alarm_content VARCHAR(500)         COMMENT '告警内容',
    alarm_time   DATETIME     NOT NULL COMMENT '告警时间',
    status       TINYINT      DEFAULT 0 COMMENT '状态：0未处理 1处理中 2已闭环',
    handle_time  DATETIME               COMMENT '处理时间（完成时间，闭环时写入）',
    handle_user  VARCHAR(50)            COMMENT '处理人（分配处理人时写入）',
    handle_result VARCHAR(500)          COMMENT '处理结果/处理意见（处理人闭环时填写）',
    create_time  DATETIME               COMMENT '创建时间',
    update_time  DATETIME               COMMENT '更新时间',
    create_by    BIGINT                 COMMENT '创建人',
    update_by    BIGINT                 COMMENT '更新人',
    deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    KEY idx_device_id (device_id),
    KEY idx_pole_id (pole_id),
    KEY idx_alarm_type (alarm_type),
    KEY idx_status (status),
    KEY idx_alarm_time (alarm_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- -----------------------------------------------------------------------------
-- video_camera 视频摄像头表
-- 灯杆挂载的摄像头信息与 RTSP 流地址
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS video_camera;
CREATE TABLE video_camera (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    camera_name VARCHAR(100) NOT NULL COMMENT '摄像头名称',
    pole_id     BIGINT                 COMMENT '灯杆ID',
    stream_url  VARCHAR(500) NOT NULL COMMENT 'RTSP流地址',
    status      TINYINT      DEFAULT 0 COMMENT '状态：0离线 1在线 2故障',
    ptz_enable  TINYINT      DEFAULT 0 COMMENT '是否支持云台：0否 1是',
    resolution  VARCHAR(20)            COMMENT '分辨率（如 1080P/4K）',
    create_time DATETIME               COMMENT '创建时间',
    update_time DATETIME               COMMENT '更新时间',
    create_by   BIGINT                 COMMENT '创建人',
    update_by   BIGINT                 COMMENT '更新人',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    KEY idx_pole_id (pole_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频摄像头表';

-- -----------------------------------------------------------------------------
-- env_sensor_data 环境传感器数据表
-- 灯杆挂载的环境传感器采集的温湿度、PM2.5、噪声等数据（时序数据）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS env_sensor_data;
CREATE TABLE env_sensor_data (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    pole_id        BIGINT        NOT NULL COMMENT '灯杆ID',
    temperature    DECIMAL(5,2)           COMMENT '温度(℃)',
    humidity      DECIMAL(5,2)            COMMENT '湿度(%)',
    pm25          DECIMAL(6,2)            COMMENT 'PM2.5(μg/m³)',
    pm10          DECIMAL(6,2)            COMMENT 'PM10(μg/m³)',
    noise         DECIMAL(6,2)            COMMENT '噪声(dB)',
    illumination  DECIMAL(8,2)            COMMENT '光照(lux)',
    wind_speed    DECIMAL(5,2)            COMMENT '风速(m/s)',
    wind_direction VARCHAR(10)           COMMENT '风向(如 N/NE/E)',
    record_time   DATETIME      NOT NULL COMMENT '记录时间',
    create_time   DATETIME                COMMENT '创建时间',
    update_time   DATETIME                COMMENT '更新时间',
    create_by     BIGINT                  COMMENT '创建人',
    update_by     BIGINT                  COMMENT '更新人',
    deleted       TINYINT      DEFAULT 0  COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    KEY idx_pole_id (pole_id),
    KEY idx_record_time (record_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环境传感器数据表';

-- -----------------------------------------------------------------------------
-- led_program LED节目表
-- LED 屏播放的节目内容、播放模式、发布状态
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS led_program;
CREATE TABLE led_program (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    program_name VARCHAR(100) NOT NULL COMMENT '节目名称',
    content      TEXT                  COMMENT '节目内容',
    media_type  VARCHAR(20)  NOT NULL COMMENT '媒体类型：TEXT文本/IMAGE图片/VIDEO视频',
    screen_id   BIGINT                 COMMENT '屏幕ID',
    play_mode   VARCHAR(20)  DEFAULT 'LOOP' COMMENT '播放模式：LOOP循环/ONCE单次',
    start_time  DATETIME               COMMENT '开始时间',
    end_time    DATETIME               COMMENT '结束时间',
    status      TINYINT      DEFAULT 0 COMMENT '状态：0待发布 1已发布 2已下线',
    create_time DATETIME               COMMENT '创建时间',
    update_time DATETIME               COMMENT '更新时间',
    create_by   BIGINT                 COMMENT '创建人',
    update_by   BIGINT                 COMMENT '更新人',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    KEY idx_screen_id (screen_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LED节目表';

-- -----------------------------------------------------------------------------
-- work_order 工单表
-- 设备故障/巡检任务的运维工单，形成 创建→派单→处理→完成→验收 闭环
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS work_order;
CREATE TABLE work_order (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no     VARCHAR(50)   NOT NULL COMMENT '工单编号（唯一）',
    order_type   VARCHAR(20)   NOT NULL COMMENT '类型：INSPECT巡检/REPAIR维修',
    title        VARCHAR(200)  NOT NULL COMMENT '标题',
    description  VARCHAR(1000)           COMMENT '描述',
    device_id    VARCHAR(50)            COMMENT '设备ID',
    pole_id      BIGINT                  COMMENT '灯杆ID',
    assignee_id  BIGINT                  COMMENT '指派人ID',
    priority     TINYINT       DEFAULT 2 COMMENT '优先级：1高 2中 3低',
    status       TINYINT       DEFAULT 0 COMMENT '状态：0待处理 1处理中 2已完成 3已验收',
    finish_time  DATETIME                COMMENT '完成时间',
    create_time  DATETIME                COMMENT '创建时间',
    update_time  DATETIME                COMMENT '更新时间',
    create_by    BIGINT                  COMMENT '创建人',
    update_by    BIGINT                  COMMENT '更新人',
    deleted      TINYINT       DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_type (order_type),
    KEY idx_assignee_id (assignee_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- =============================================================================
-- 4. 初始数据
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 4.1 初始用户
-- admin 用户，密码 123456 的 MD5：e10adc3949ba59abbe56e057f20f883e
-- -----------------------------------------------------------------------------
INSERT INTO sys_user (username, password, nickname, phone, email, status, create_time, update_time)
VALUES ('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', '13800000000', 'admin@ccb.com', 1, NOW(), NOW());

-- -----------------------------------------------------------------------------
-- 4.2 初始角色
-- -----------------------------------------------------------------------------
INSERT INTO sys_role (role_code, role_name, description, status, create_time, update_time)
VALUES ('ADMIN', '系统管理员', '拥有系统全部权限', 1, NOW(), NOW());
INSERT INTO sys_role (role_code, role_name, description, status, create_time, update_time)
VALUES ('OPERATOR', '运维人员', '负责设备运维、工单处理', 1, NOW(), NOW());

-- -----------------------------------------------------------------------------
-- 4.3 用户-角色关联
-- admin 用户绑定 ADMIN 角色
-- -----------------------------------------------------------------------------
INSERT INTO sys_user_role (user_id, role_id, create_time, update_time)
VALUES (1, 1, NOW(), NOW());

-- -----------------------------------------------------------------------------
-- 4.4 基础菜单数据
-- 一级目录 + 二级菜单，对应前端左侧导航
-- -----------------------------------------------------------------------------
-- 一级目录：系统管理
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (0, '系统管理', 'DIRECTORY', '/system', NULL, NULL, 'Setting', 1, 1, 1, NOW(), NOW());
SET @sys_menu_id = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@sys_menu_id, '用户管理', 'MENU', '/system/user', 'system/user/index', 'system:user:list', 'User', 1, 1, 1, NOW(), NOW());
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@sys_menu_id, '角色管理', 'MENU', '/system/role', 'system/role/index', 'system:role:list', 'UserFilled', 2, 1, 1, NOW(), NOW());
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@sys_menu_id, '菜单管理', 'MENU', '/system/menu', 'system/menu/index', 'system:menu:list', 'Menu', 3, 1, 1, NOW(), NOW());

-- 一级目录：设备管理
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (0, '设备管理', 'DIRECTORY', '/device', NULL, NULL, 'Monitor', 2, 1, 1, NOW(), NOW());
SET @device_menu_id = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@device_menu_id, '灯杆管理', 'MENU', '/device/pole', 'device/pole/index', 'device:pole:list', 'Location', 1, 1, 1, NOW(), NOW());
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@device_menu_id, '设备管理', 'MENU', '/device/list', 'device/list/index', 'device:list:list', 'Cpu', 2, 1, 1, NOW(), NOW());

-- 一级目录：智能照明
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (0, '智能照明', 'DIRECTORY', '/lighting', NULL, NULL, 'Sunny', 3, 1, 1, NOW(), NOW());
SET @lighting_menu_id = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@lighting_menu_id, '照明策略', 'MENU', '/lighting/strategy', 'lighting/strategy/index', 'lighting:strategy:list', 'SetUp', 1, 1, 1, NOW(), NOW());
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@lighting_menu_id, '实时控制', 'MENU', '/lighting/control', 'lighting/control/index', 'lighting:control:list', 'SwitchButton', 2, 1, 1, NOW(), NOW());

-- 一级目录：能耗管理
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (0, '能耗管理', 'DIRECTORY', '/energy', NULL, NULL, 'Lightning', 4, 1, 1, NOW(), NOW());
SET @energy_menu_id = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@energy_menu_id, '能耗记录', 'MENU', '/energy/record', 'energy/record/index', 'energy:record:list', 'DataLine', 1, 1, 1, NOW(), NOW());

-- 一级目录：故障告警
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (0, '故障告警', 'DIRECTORY', '/alarm', NULL, NULL, 'BellFilled', 5, 1, 1, NOW(), NOW());
SET @alarm_menu_id = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@alarm_menu_id, '告警记录', 'MENU', '/alarm/record', 'alarm/record/index', 'alarm:record:list', 'Warning', 1, 1, 1, NOW(), NOW());

-- 一级目录：视频监控
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (0, '视频监控', 'DIRECTORY', '/video', NULL, NULL, 'VideoCamera', 6, 1, 1, NOW(), NOW());
SET @video_menu_id = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@video_menu_id, '摄像头管理', 'MENU', '/video/camera', 'video/camera/index', 'video:camera:list', 'VideoCamera', 1, 1, 1, NOW(), NOW());

-- 一级目录：环境监测
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (0, '环境监测', 'DIRECTORY', '/env', NULL, NULL, 'Aim', 7, 1, 1, NOW(), NOW());
SET @env_menu_id = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@env_menu_id, '环境数据', 'MENU', '/env/data', 'env/data/index', 'env:data:list', 'DataAnalysis', 1, 1, 1, NOW(), NOW());

-- 一级目录：信息发布
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (0, '信息发布', 'DIRECTORY', '/publish', NULL, NULL, 'ChatDotRound', 8, 1, 1, NOW(), NOW());
SET @publish_menu_id = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@publish_menu_id, 'LED节目', 'MENU', '/publish/program', 'publish/program/index', 'publish:program:list', 'Film', 1, 1, 1, NOW(), NOW());

-- 一级目录：工单运维
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (0, '工单运维', 'DIRECTORY', '/workorder', NULL, NULL, 'Tickets', 9, 1, 1, NOW(), NOW());
SET @workorder_menu_id = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@workorder_menu_id, '工单管理', 'MENU', '/workorder/list', 'workorder/list/index', 'workorder:list:list', 'Document', 1, 1, 1, NOW(), NOW());

-- 一级目录：数据大盘
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (0, '数据大盘', 'DIRECTORY', '/dashboard', NULL, NULL, 'Odometer', 10, 1, 1, NOW(), NOW());
SET @dashboard_menu_id = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
VALUES (@dashboard_menu_id, '首页大盘', 'MENU', '/dashboard/index', 'dashboard/index/index', 'dashboard:index:list', 'DataBoard', 1, 1, 1, NOW(), NOW());

-- -----------------------------------------------------------------------------
-- 4.5 角色-菜单关联
-- ADMIN 角色绑定所有菜单（这里简化，可手动按需调整）
-- -----------------------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id, create_time, update_time)
SELECT 1, id, NOW(), NOW() FROM sys_menu WHERE deleted = 0;
