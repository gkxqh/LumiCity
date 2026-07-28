# -*- coding: utf-8 -*-
"""
智慧城市照明系统 - 测试数据生成器

设计目标：
  - 覆盖各种情况：每种设备类型、每种告警类型/级别/状态、每种工单状态、时序数据（含昼夜变化）
  - 具有随机性：每次运行结果不同（未固定随机种子）
  - 幂等：先清空业务表与测试用户（保留 admin 及初始角色/菜单/区域），再重新生成
  
v2 变更（2026-07-28）：
  - area 树形 → region 扁平表
  - dev_pole: area_id → region_id, 新增 road/number 字段
  - pole_name/address 改为拼接生成（{区}{路}{号}灯杆 / {区}{路}{号}）
  - 区→路白名单映射，消除不可能的区路组合
"""
import random
from datetime import datetime, timedelta, time as dtime


# ---------- 静态数据池 ----------


def bcrypt_hash(text: str) -> str:
    """用 Python 的 bcrypt 生成哈希，对标 Java BCryptPasswordEncoder"""
    import bcrypt as _bcrypt
    return _bcrypt.hashpw(text.encode("utf-8"), _bcrypt.gensalt()).decode("utf-8")


# ---------- 道路线段坐标（v4: 绕城高速以内成都主干道全覆盖） ----------
# 格式: {区名: {路名: (lng_start, lat_start, lng_end, lat_end), ...}}
# 基于腾讯地图/高德/天地图等公开 POI 数据标定（WGS-84），覆盖绕城高速(G4202)以内全部主要干道
#
# 绕城高速边界参考:
#   北界 (~30.75): 北星立交/大丰
#   南界 (~30.54): 锦城湖立交/白家
#   西界 (~103.93): 文家场立交
#   东界 (~104.20): 成渝立交/成龙立交
#
# 三环路参考坐标:
#   北段(金牛): 104.02~104.08, 30.720~30.721
#   西段(青羊): 103.993~104.005, 30.620~30.679
#   南段(武侯/高新): 104.00~104.09, 30.590~30.618
#   东段(锦江/成华): 104.09~104.16, 30.590~30.720
ROAD_SEGMENTS = {
    "锦江区": {
        # 环线
        "一环路":   (104.086, 30.649, 104.092, 30.660),
        "二环路":   (104.075, 30.647, 104.095, 30.660),
        "中环路":   (104.115, 30.614, 104.140, 30.628),
        "三环路":   (104.088, 30.590, 104.145, 30.618),
        # 南北向
        "人民南路": (104.066, 30.654, 104.066, 30.647),
        "红星路":   (104.088, 30.660, 104.089, 30.645),
        # 东西向/放射线
        "蜀都大道": (104.072, 30.663, 104.098, 30.660),
        "东大街":   (104.072, 30.652, 104.092, 30.648),
        "锦华路":   (104.080, 30.646, 104.098, 30.634),
        "成龙大道": (104.098, 30.607, 104.140, 30.600),
        "驿都大道": (104.100, 30.618, 104.145, 30.611),
    },
    "武侯区": {
        # 环线
        "一环路":   (104.065, 30.640, 104.055, 30.626),
        "二环路":   (104.045, 30.638, 104.060, 30.616),
        "中环路":   (104.014, 30.634, 104.025, 30.622),
        "三环路":   (104.000, 30.617, 104.055, 30.599),
        # 南北向
        "人民南路": (104.066, 30.645, 104.070, 30.610),
        "科华北路": (104.072, 30.634, 104.074, 30.616),
        "高攀路":   (104.075, 30.620, 104.078, 30.608),
        # 东西向/放射线
        "二环路":   (104.045, 30.638, 104.060, 30.616),
        "武侯大道": (103.983, 30.630, 104.040, 30.613),
        "川藏路":   (104.000, 30.620, 104.015, 30.608),
        # 西南放射
        "草金路":   (104.000, 30.635, 103.980, 30.640),
    },
    "青羊区": {
        # 环线
        "一环路":   (104.050, 30.676, 104.040, 30.664),
        "二环路":   (104.035, 30.678, 104.030, 30.658),
        "中环路":   (104.008, 30.677, 104.008, 30.658),
        "三环路":   (103.993, 30.679, 104.005, 30.647),
        # 东西向/放射线
        "蜀都大道": (104.042, 30.672, 104.068, 30.668),
        "光华大道": (103.980, 30.672, 104.050, 30.665),
        "日月大道": (103.970, 30.680, 104.040, 30.670),
        # 南北向
        "人民中路": (104.068, 30.676, 104.069, 30.666),
    },
    "金牛区": {
        # 环线
        "一环路":   (104.048, 30.686, 104.065, 30.692),
        "二环路":   (104.033, 30.693, 104.055, 30.696),
        "中环路":   (104.044, 30.716, 104.030, 30.704),
        "三环路":   (104.020, 30.721, 104.080, 30.720),
        # 南北向
        "人民北路": (104.069, 30.694, 104.071, 30.682),
        "北星大道": (104.072, 30.698, 104.075, 30.730),
        # 东西向/放射线
        "交大路":   (104.030, 30.690, 104.045, 30.692),
        "沙湾路":   (104.040, 30.685, 104.050, 30.690),
        "金府路":   (104.044, 30.716, 104.030, 30.704),
    },
    "成华区": {
        # 环线
        "一环路":   (104.095, 30.662, 104.100, 30.672),
        "二环路":   (104.108, 30.655, 104.112, 30.675),
        "中环路":   (104.140, 30.644, 104.120, 30.671),
        "三环路":   (104.090, 30.720, 104.162, 30.665),
        # 南北向
        "府青路":   (104.090, 30.685, 104.095, 30.700),
        "蓉都大道": (104.085, 30.700, 104.095, 30.730),
        # 东西向/放射线
        "成华大道": (104.098, 30.658, 104.150, 30.675),
        "建设路":   (104.100, 30.666, 104.110, 30.658),
        "猛追湾街": (104.092, 30.665, 104.098, 30.655),
        "成洛大道": (104.120, 30.656, 104.185, 30.649),
        "杉板桥路": (104.110, 30.668, 104.130, 30.664),
    },
    "高新区": {
        # 环线（三环以南至绕城）
        "三环路":   (104.055, 30.599, 104.088, 30.589),
        "中环路":   (104.040, 30.607, 104.070, 30.605),
        # 南北向主干道（三环→绕城）
        "天府大道": (104.069, 30.598, 104.065, 30.560),
        "益州大道": (104.053, 30.598, 104.050, 30.560),
        "剑南大道": (104.042, 30.597, 104.038, 30.560),
        # 区域道路
        "吉泰路":   (104.050, 30.590, 104.055, 30.580),
        "天府二街": (104.050, 30.585, 104.070, 30.585),
        "天府四街": (104.045, 30.578, 104.070, 30.578),
    },
    "天府新区": {
        # 绕城高速以南区域（维持原有，更新坐标）
        "天府大道": (104.065, 30.550, 104.062, 30.520),
        "益州大道": (104.050, 30.550, 104.048, 30.520),
        "剑南大道": (104.038, 30.550, 104.035, 30.520),
        "沈阳路":   (104.043, 30.505, 104.060, 30.503),
        "武汉路":   (104.048, 30.518, 104.070, 30.515),
    },
    "龙泉驿区": {
        # 绕城高速以东区域（三环至绕城之间）
        "成华大道": (104.155, 30.678, 104.185, 30.660),
        "驿都大道": (104.150, 30.611, 104.195, 30.565),
        "成龙大道": (104.145, 30.600, 104.195, 30.555),
        "成洛大道": (104.190, 30.649, 104.220, 30.648),
        "车城大道": (104.270, 30.570, 104.275, 30.545),
    },
}

SURNAMES = ["张", "李", "王", "刘", "陈", "杨", "赵", "黄", "周", "吴",
            "徐", "孙", "马", "朱", "胡", "林", "郭", "何", "高", "罗"]
GIVEN = ["伟", "芳", "娜", "敏", "静", "强", "磊", "军", "洋", "勇",
         "艳", "杰", "娟", "涛", "明", "超", "霞", "平", "刚", "桂英"]

DEVICE_MODELS = {
    "LIGHT":      [("SL-100", "雷士照明"), ("SL-200", "欧普照明"), ("LED-Pro", "飞利浦")],
    "CAMERA":     [("CAM-720", "海康威视"), ("CAM-4K", "大华股份")],
    "SENSOR":     [("ENV-S1", "汉威科技"), ("ENV-S2", "聚光科技")],
    "LED_SCREEN": [("LED-P5", "洲明科技"), ("LED-P10", "艾比森")],
    "BROADCAST":  [("BC-30W", "声优科技")],
}
DEVICE_TYPE_WEIGHTS = [("LIGHT", 50), ("CAMERA", 20), ("SENSOR", 15),
                       ("LED_SCREEN", 10), ("BROADCAST", 5)]

ALARM_TYPES = ["OFFLINE", "OVERVOLTAGE", "OVERCURRENT", "ABNORMAL"]
ALARM_TYPE_WEIGHTS = [30, 20, 20, 30]
ALARM_LEVELS = [1, 2, 3]
ALARM_LEVEL_WEIGHTS = [20, 30, 50]
ALARM_STATUS = [0, 1, 2]  # 未处理/处理中/已闭环
ALARM_STATUS_WEIGHTS = [20, 25, 55]
HANDLE_RESULTS = [
    "已现场排查并复位，设备恢复正常",
    "更换故障模块后恢复，已记录备件更换",
    "远程重启设备后告警消除",
    "现场巡检确认误报，已闭环",
    "联系厂商技术支持后处理完毕",
    "调整阈值参数后告警不再触发",
]

ORDER_TYPES = ["INSPECT", "REPAIR"]
ORDER_STATUS = [0, 1, 2]  # 待处理/处理中/已完成
ORDER_STATUS_WEIGHTS = [20, 30, 50]

LED_MEDIA = ["TEXT", "IMAGE", "VIDEO"]
LED_STATUS = [0, 1, 2]  # 待发布/已发布/已下线


def _weighted(items, weights):
    return random.choices(items, weights=weights, k=1)[0]


class DataGenerator:
    def __init__(self, conn):
        self.conn = conn
        self.cur = conn.cursor()
        self.now = datetime.now().replace(microsecond=0)
        self.counts = {}

    # ---------- 底层工具 ----------
    def _audit(self):
        """返回审计字段通用取值"""
        return (self.now, self.now, 1, 1, 0)  # create_time, update_time, create_by, update_by, deleted

    def _insert(self, table, cols, vals):
        ph = ",".join(["%s"] * len(cols))
        sql = f"INSERT INTO {table} ({','.join(cols)}) VALUES ({ph})"
        self.cur.execute(sql, vals)
        return self.cur.lastrowid

    # ---------- 清空（保留 admin 与初始角色/菜单/区域） ----------
    def clear(self):
        # 先删业务表（引用设备/灯杆），再删设备/灯杆
        for t in ["energy_record", "alarm_record", "work_order",
                  "led_publish_log", "led_program", "light_strategy", "video_camera", "dev_device", "dev_pole"]:
            self.cur.execute(f"DELETE FROM {t}")
        # 清理角色菜单绑定并重置 OPERATOR 权限
        self.cur.execute("DELETE FROM sys_role_menu WHERE role_id > 2")
        self.cur.execute("DELETE FROM sys_role_menu WHERE role_id = 2")
        # 重新给 OPERATOR 绑定业务菜单（排除系统管理相关及按钮权限）
        self.cur.execute(
            "INSERT INTO sys_role_menu (role_id, menu_id, create_time, update_time, create_by, update_by, deleted) "
            "SELECT 2, id, NOW(), NOW(), 1, 1, 0 FROM sys_menu m "
            "WHERE m.deleted = 0 AND m.id NOT IN (1, 2, 3, 4) AND m.menu_type != 'BUTTON'"
        )
        self.cur.execute("DELETE FROM sys_user_role WHERE user_id > 1 OR role_id > 2")
        self.cur.execute("DELETE FROM sys_user WHERE id > 1")
        self.cur.execute("DELETE FROM sys_role WHERE id > 2")
        # 注意：region 数据是 schema.sql 中的初始数据，不清空
        self.conn.commit()
        self.counts["cleared"] = True

    # ---------- 角色 ----------
    def gen_roles(self):
        self.cur.execute("SELECT id, role_code FROM sys_role")
        existing = {code: rid for rid, code in self.cur.fetchall()}
        if "INSPECTOR" not in existing:
            rid = self._insert(
                "sys_role",
                ["role_code", "role_name", "description", "status", "create_time",
                 "update_time", "create_by", "update_by", "deleted"],
                ("INSPECTOR", "巡检人员", "负责日常巡检与工单执行", 1, *self._audit()),
            )
            self.cur.execute(
                "SELECT id FROM sys_menu WHERE deleted=0 AND menu_type != 'BUTTON'"
                " AND id NOT IN (1, 2, 3, 4)"
            )
            for (mid,) in self.cur.fetchall():
                self._insert("sys_role_menu", ["role_id", "menu_id", "create_time",
                                               "update_time", "create_by", "update_by", "deleted"],
                             (rid, mid, self.now, self.now, 1, 1, 0))
        self.conn.commit()

    # ---------- 用户 ----------
    def gen_users(self, n=8):
        self.cur.execute("SELECT id, role_code FROM sys_role WHERE deleted=0")
        role_map = {code: rid for rid, code in self.cur.fetchall()}
        used = set()
        cnt = 0
        while cnt < n:
            name = random.choice(SURNAMES) + random.choice(GIVEN)
            if name in used:
                continue
            used.add(name)
            username = "user" + str(100 + cnt)
            role_code = random.choice(["OPERATOR", "INSPECTOR", "OPERATOR"])  # 运维偏多
            status = 0 if random.random() < 0.15 else 1
            uid = self._insert(
                "sys_user",
                ["username", "password", "nickname", "phone", "email", "status",
                 "create_time", "update_time", "create_by", "update_by", "deleted"],
                (username, bcrypt_hash("123456"), name, f"13{random.randint(100000000,999999999)}",
                 f"{username}@ccb.com", status, *self._audit()),
            )
            rid = role_map.get(role_code, 2)
            self._insert("sys_user_role", ["user_id", "role_id", "create_time",
                                           "update_time", "create_by", "update_by", "deleted"],
                         (uid, rid, self.now, self.now, 1, 1, 0))
            cnt += 1
        self.conn.commit()
        self.counts["sys_user"] = n
        return cnt

    def _fetch_users(self):
        self.cur.execute("SELECT id, username, nickname FROM sys_user WHERE deleted=0 AND status=1")
        return self.cur.fetchall()  # [(id, username, nickname)]

    # ---------- 灯杆（v3: 沿道路线段插值生成坐标） ----------
    def gen_poles(self, n=300):
        # 从 region 表读取已有区域数据
        self.cur.execute("SELECT id, name FROM region WHERE deleted=0 AND status=1 ORDER BY id")
        db_regions = [(row[0], row[1]) for row in self.cur.fetchall()]
        if not db_regions:
            # 如果 region 表为空（比如初次运行但没初始化），用硬编码数据
            print("WARNING: region 表为空，使用硬编码区域数据")
            db_regions = [(1, "锦江区"), (2, "武侯区"), (3, "青羊区"), (4, "金牛区"),
                          (5, "成华区"), (6, "高新区"), (7, "天府新区"), (8, "龙泉驿区")]

        # 建立 region_id → (区名, {路名: (lng1,lat1,lng2,lat2)})
        region_segments = {}
        for rid, name in db_regions:
            segs = ROAD_SEGMENTS.get(name)
            if segs:
                region_segments[rid] = (name, segs)
            else:
                # 兜底：给一个默认的线段
                region_segments[rid] = (name, {"人民路": (104.06, 30.65, 104.07, 30.64)})

        # 收集所有路名列表，用于按道路分组
        all_road_names = []
        for rid, (rname, segs) in region_segments.items():
            for road in segs.keys():
                all_road_names.append((rid, rname, road))

        poles = []
        for i in range(n):
            # 随机选一条道路
            rid, region_name, road = random.choice(all_road_names)
            segs = region_segments[rid][1]
            lng1, lat1, lng2, lat2 = segs[road]

            # 沿线段随机插值
            t = random.random()
            lng = round(lng1 + (lng2 - lng1) * t, 7)
            lat = round(lat1 + (lat2 - lat1) * t, 7)

            # 编号采用序号（1~299），使同一路段的灯杆编号有序
            no = random.randint(1, 500)
            pole_code = f"P-A{rid}-{i+1:03d}"

            # pole_name 和 address 由拼接生成（模拟后端 fillNameAndAddress 逻辑）
            address = f"{region_name}{road}{no}号"
            pole_name = f"{address}灯杆"

            # 状态分布：在线70% / 离线20% / 故障10%
            status = _weighted([1, 0, 2], [70, 20, 10])
            # 照明状态：约60%开灯 40%关灯
            light_status = 1 if random.random() < 0.6 else 0
            # 如果开灯则随机亮度 40~100，关灯亮度=0
            light_brightness = random.randint(40, 100) if light_status == 1 else 0
            height = round(random.uniform(6, 12), 2)
            install = (self.now - timedelta(days=random.randint(30, 720))).date()
            pid = self._insert(
                "dev_pole",
                ["pole_code", "pole_name", "region_id", "road", "number", "address",
                 "lng", "lat", "height", "status", "light_status", "light_brightness", "install_time",
                 "create_time", "update_time", "create_by", "update_by", "deleted"],
                (pole_code, pole_name, rid, road, f"{no}号", address,
                 lng, lat, height, status, light_status, light_brightness, install, *self._audit()),
            )
            poles.append({"id": pid, "code": pole_code, "region_id": rid,
                          "status": status, "lng": lng, "lat": lat, "road": road})
        self.conn.commit()
        self.counts["dev_pole"] = n
        self._poles = poles
        return poles

    # ---------- 设备 ----------
    def gen_devices(self, per_pole_max=4):
        devices = []
        code_seq = 1
        for pole in self._poles:
            # 大部分灯杆只有1台设备（60%概率1台，25%概率2台，15%概率3台）
            cnt = _weighted([1, 2, 3], [60, 25, 15])
            for _ in range(cnt):
                dtype = _weighted([t for t, _ in DEVICE_TYPE_WEIGHTS],
                                  [w for _, w in DEVICE_TYPE_WEIGHTS])
                model, vendor = random.choice(DEVICE_MODELS[dtype])
                if pole["status"] == 2:
                    status = _weighted([2, 1, 0], [60, 20, 20])
                elif pole["status"] == 0:
                    status = _weighted([0, 1], [80, 20])
                else:
                    status = _weighted([1, 0, 2], [80, 12, 8])
                last_online = (self.now - timedelta(minutes=random.randint(1, 6000))
                               if status != 0 else self.now - timedelta(hours=random.randint(2, 72)))
                device_code = f"D-{dtype[:3]}-{code_seq:04d}"
                code_seq += 1
                did = self._insert(
                    "dev_device",
                    ["device_code", "device_name", "device_type", "pole_id", "model", "vendor",
                     "status", "last_online_time", "create_time", "update_time",
                     "create_by", "update_by", "deleted"],
                    (device_code, f"{pole['code']}-{dtype}", dtype, pole["id"], model, vendor,
                     status, last_online, *self._audit()),
                )
                devices.append({"id": did, "code": device_code, "type": dtype,
                                "pole_id": pole["id"], "status": status})
        self.conn.commit()
        self.counts["dev_device"] = len(devices)
        self._devices = devices
        return devices

    # ---------- 照明策略 ----------
    def gen_strategies(self, n=20):
        cnt = 0
        for _ in range(n):
            pole = random.choice(self._poles)
            stype = _weighted(["TIME", "LIGHT", "TRAFFIC"], [60, 25, 15])
            brightness = random.randint(30, 100) if stype != "LIGHT" else random.randint(50, 100)
            sh = random.randint(17, 20)
            eh = random.randint(4, 7)
            start_t = dtime(sh, random.choice([0, 30])).strftime("%H:%M:%S")
            end_t = dtime(eh, random.choice([0, 30])).strftime("%H:%M:%S")
            week = ",".join(str(d) for d in sorted(random.sample(range(1, 8), random.randint(5, 7))))
            enabled = 1 if random.random() < 0.8 else 0
            self._insert(
                "light_strategy",
                ["strategy_name", "strategy_type", "pole_id", "brightness", "start_time",
                 "end_time", "week_days", "enabled", "create_time", "update_time",
                 "create_by", "update_by", "deleted"],
                (f"{pole['code']}策略", stype, pole["id"], brightness, start_t, end_t,
                 week, enabled, *self._audit()),
            )
            cnt += 1
        self.conn.commit()
        self.counts["light_strategy"] = cnt

    # ---------- 能耗记录（时序） ----------
    def gen_energy(self, days=7, readings_per_day=8):
        lights = [d for d in self._devices if d["type"] == "LIGHT"]
        sample = random.sample(lights, min(15, len(lights))) if lights else []
        rows = []
        base = self.now.replace(hour=0, minute=0, second=0, microsecond=0)
        for dev in sample:
            for d in range(days, 0, -1):
                day = base - timedelta(days=d - 1)
                for r in range(readings_per_day):
                    t = day + timedelta(hours=r * (24 // readings_per_day) + random.randint(0, 1))
                    voltage = round(random.uniform(218, 225), 2)
                    power = round(random.uniform(30, 180), 2)
                    current = round(power / voltage, 2)
                    consumption = round(power * (24 / readings_per_day) / 1000, 3)
                    rows.append((dev["code"], dev["pole_id"], t, voltage, current, power,
                                 consumption, self.now, self.now, 1, 1, 0))
        sql = ("INSERT INTO energy_record (device_id, pole_id, record_time, voltage, current, "
               "power, consumption, create_time, update_time, create_by, update_by, deleted) "
               "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)")
        self.cur.executemany(sql, rows)
        self.conn.commit()
        self.counts["energy_record"] = len(rows)

    # ---------- 告警记录 ----------
    def gen_alarms(self, n=50):
        users = [u for u in self._fetch_users()]
        rows = 0
        self._alarms = []
        for _ in range(n):
            dev = random.choice(self._devices)
            atype = _weighted(ALARM_TYPES, ALARM_TYPE_WEIGHTS)
            level = _weighted(ALARM_LEVELS, ALARM_LEVEL_WEIGHTS)
            status = _weighted(ALARM_STATUS, ALARM_STATUS_WEIGHTS)
            alarm_time = self.now - timedelta(days=random.randint(0, 30),
                                              hours=random.randint(0, 23),
                                              minutes=random.randint(0, 59))
            content_map = {
                "OFFLINE": f"设备 {dev['code']} 离线超过阈值",
                "OVERVOLTAGE": f"设备 {dev['code']} 电压超限",
                "OVERCURRENT": f"设备 {dev['code']} 电流异常",
                "ABNORMAL": f"设备 {dev['code']} 运行参数异常",
            }
            handle_user = handle_time = handle_result = None
            if status in (1, 2):
                handle_user = random.choice(users)[1] if users else "admin"
            if status == 2:
                handle_time = alarm_time + timedelta(minutes=random.randint(10, 600))
                handle_result = random.choice(HANDLE_RESULTS)
            alarm_id = self._insert(
                "alarm_record",
                ["device_id", "pole_id", "alarm_type", "alarm_level", "alarm_content",
                 "alarm_time", "status", "handle_user", "handle_time", "handle_result",
                 "create_time", "update_time", "create_by", "update_by", "deleted"],
                (dev["code"], dev["pole_id"], atype, level, content_map[atype],
                 alarm_time, status, handle_user, handle_time, handle_result, *self._audit()),
            )
            if status in (1, 2):
                self._alarms.append((alarm_id, dev["code"], dev["pole_id"], atype, level, status))
            rows += 1
        self.conn.commit()
        self.counts["alarm_record"] = rows

    # ---------- 视频摄像头 ----------
    def gen_cameras(self):
        cams = [d for d in self._devices if d["type"] == "CAMERA"]
        cnt = 0
        for dev in cams:
            ptz = 1 if random.random() < 0.5 else 0
            res = random.choice(["720P", "1080P", "1080P", "4K"])
            stream = f"rtsp://admin:ccb123@cam-{dev['code']}:554/stream1"
            self._insert(
                "video_camera",
                ["camera_name", "pole_id", "stream_url", "status", "ptz_enable",
                 "resolution", "create_time", "update_time", "create_by", "update_by", "deleted"],
                (f"摄像头-{dev['code']}", dev["pole_id"], stream, dev["status"], ptz,
                 res, *self._audit()),
            )
            cnt += 1
        self.conn.commit()
        self.counts["video_camera"] = cnt

    # ---------- LED 节目 ----------
    def gen_led(self, n=15):
        leds = [d for d in self._devices if d["type"] == "LED_SCREEN"]
        if not leds:
            self.counts["led_program"] = 0
            return
        contents = {
            "TEXT": ["欢迎来到智慧城市", "文明出行 安全第一", "今日天气晴朗", "节约用电 人人有责"],
            "IMAGE": ["公益广告图片", "城市宣传海报", "交通提示图", "活动海报"],
            "VIDEO": ["城市宣传片", "安全警示视频", "节日庆祝视频", "环保宣传片"],
        }
        for _ in range(n):
            dev = random.choice(leds)
            media = random.choice(LED_MEDIA)
            status = _weighted(LED_STATUS, [30, 50, 20])
            start = self.now - timedelta(days=random.randint(0, 10))
            end = start + timedelta(days=random.randint(1, 30))
            self._insert(
                "led_program",
                ["program_name", "content", "media_type", "screen_id", "play_mode",
                 "start_time", "end_time", "status", "create_time", "update_time",
                 "create_by", "update_by", "deleted"],
                (f"节目-{dev['code']}", random.choice(contents[media]), media, dev["id"],
                 random.choice(["LOOP", "ONCE"]), start, end, status, *self._audit()),
            )
        self.conn.commit()
        self.counts["led_program"] = n

    # ---------- 工单 ----------
    def gen_workorders(self, n=25):
        users = self._fetch_users()
        if not users:
            self.counts["work_order"] = 0
            return
        titles_inspect = ["例行巡检", "季度设备检查", "灯杆安全巡检", "线路排查"]
        titles_repair = ["灯具更换", "摄像头维修", "传感器校准", "电源故障修复", "LED屏维修"]
        seq = 1

        self.cur.execute("SELECT order_no FROM work_order ORDER BY id DESC LIMIT 1")
        row = self.cur.fetchone()
        if row:
            last_seq = int(row[0].split("-")[-1])
            seq = last_seq + 1

        alarm_related = 0
        alarm_list = self._alarms if hasattr(self, '_alarms') and self._alarms else []
        for (alarm_id, dev_code, pole_id, alarm_type, alarm_level, alarm_status) in alarm_list:
            create_t = self.now - timedelta(days=random.randint(0, 15),
                                            hours=random.randint(0, 23))
            order_no = f"WO-{create_t.strftime('%Y%m%d')}-{seq:03d}"
            seq += 1

            type_text = {"OFFLINE": "离线", "OVERVOLTAGE": "过压",
                         "OVERCURRENT": "过流", "ABNORMAL": "异常"}.get(alarm_type, alarm_type)
            title = f"维修工单：{dev_code} - {type_text}告警"
            assignee = random.choice(users)
            finish = create_t + timedelta(hours=random.randint(2, 48))

            if alarm_status == 2:
                wo_status = 2
            else:
                wo_status = _weighted([1, 2], [40, 60])
            remark = random.choice(HANDLE_RESULTS) if wo_status == 2 else None
            self._insert(
                "work_order",
                ["order_no", "order_type", "title", "description", "device_id", "pole_id",
                 "assignee_id", "priority", "status", "finish_time", "handle_remark", "alarm_id",
                 "create_time", "update_time", "create_by", "update_by", "deleted"],
                (order_no, "REPAIR", title,
                 f"告警自动生成：{dev_code} 触发 {type_text}告警",
                 dev_code, pole_id, assignee[0],
                 alarm_level, wo_status,
                 finish if wo_status == 2 else None, remark, alarm_id,
                 create_t, self.now, 1, 1, 0),
            )
            alarm_related += 1

        independent_target = 10
        for _ in range(independent_target):
            dev = random.choice(self._devices)
            otype = _weighted(ORDER_TYPES, [40, 60])
            status = _weighted(ORDER_STATUS, ORDER_STATUS_WEIGHTS)
            create_t = self.now - timedelta(days=random.randint(0, 30),
                                            hours=random.randint(0, 23))
            order_no = f"WO-{create_t.strftime('%Y%m%d')}-{seq:03d}"
            seq += 1
            title = random.choice(titles_inspect if otype == "INSPECT" else titles_repair)
            assignee = random.choice(users)[0]
            finish = None
            remark = None
            if status == 2:
                finish = create_t + timedelta(hours=random.randint(2, 48))
                remark = random.choice(HANDLE_RESULTS)
            self._insert(
                "work_order",
                ["order_no", "order_type", "title", "description", "device_id", "pole_id",
                 "assignee_id", "priority", "status", "finish_time", "handle_remark", "alarm_id",
                 "create_time", "update_time", "create_by", "update_by", "deleted"],
                (order_no, otype, f"{dev['code']}-{title}",
                 f"针对设备 {dev['code']} 的{title}任务",
                 dev["code"], dev["pole_id"], assignee,
                 random.choice([1, 2, 2, 3]),
                 status, finish, remark, None,
                 create_t, self.now, 1, 1, 0),
            )
        self.conn.commit()
        self.counts["work_order"] = alarm_related + independent_target

    # ---------- 总入口 ----------
    def run(self):
        self.clear()
        self.gen_roles()
        self.gen_users()
        self.gen_poles()
        self.gen_devices(per_pole_max=3)
        self.gen_strategies()
        self.gen_energy()
        self.gen_alarms()
        self.gen_cameras()
        self.gen_led()
        self.gen_workorders()
        self.conn.commit()
        return self.counts
