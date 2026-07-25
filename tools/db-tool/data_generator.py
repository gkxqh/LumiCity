# -*- coding: utf-8 -*-
"""
智慧城市照明系统 - 测试数据生成器

设计目标：
  - 覆盖各种情况：每种设备类型、每种告警类型/级别/状态、每种工单状态、时序数据（含昼夜变化）
  - 具有随机性：每次运行结果不同（未固定随机种子）
  - 幂等：先清空业务表与测试用户（保留 admin 及初始角色/菜单），再重新生成
"""
import random
import math
from datetime import datetime, timedelta, time as dtime


# ---------- 静态数据池 ----------


def bcrypt_hash(text: str) -> str:
    """用 Python 的 bcrypt 生成哈希，对标 Java BCryptPasswordEncoder"""
    import bcrypt as _bcrypt
    return _bcrypt.hashpw(text.encode("utf-8"), _bcrypt.gensalt()).decode("utf-8")


# ---------- 静态数据池 ----------
AREAS = [
    (1, "南山区", 113.93, 22.53),
    (2, "福田区", 114.06, 22.54),
    (3, "罗湖区", 114.13, 22.55),
    (4, "宝安区", 113.88, 22.55),
    (5, "龙华区", 114.02, 22.65),
]
STREETS = ["深南大道", "科技中一路", "后海大道", "滨海大道", "北环大道",
           "南海大道", "科苑路", "侨城东路", "民治大道", "龙华大道",
           "前海路", "沙河西路", "彩田路", "新洲路", "梅观路"]
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
ORDER_STATUS = [0, 1, 2, 3]  # 待处理/处理中/已完成/已验收
ORDER_STATUS_WEIGHTS = [20, 30, 30, 20]

WIND_DIRS = ["N", "NE", "E", "SE", "S", "SW", "W", "NW"]
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

    # ---------- 清空（保留 admin 与初始角色/菜单） ----------
    def clear(self):
        # 先删业务表（引用设备/灯杆），再删设备/灯杆
        for t in ["energy_record", "alarm_record", "env_sensor_data", "work_order",
                  "led_program", "light_strategy", "video_camera", "dev_device", "dev_pole"]:
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
            # 给巡检角色绑定业务菜单（排除系统管理相关及按钮权限）
            # 系统管理目录(ID=1)及其子菜单(ID=2/3/4)均排除
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
            status = 0 if random.random() < 0.15 else 1  # 约15%禁用
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

    # ---------- 灯杆 ----------
    def gen_poles(self, n=30):
        poles = []
        for i in range(n):
            area = random.choice(AREAS)
            area_id, area_name, clng, clat = area
            lng = round(clng + random.uniform(-0.012, 0.012), 7)
            lat = round(clat + random.uniform(-0.010, 0.010), 7)
            street = random.choice(STREETS)
            no = random.randint(1, 200)
            pole_code = f"P-A{area_id}-{i+1:03d}"
            pole_name = f"{area_name}{street}{no}号灯杆"
            address = f"{area_name}{street}{no}号"
            # 状态分布：在线70% / 离线20% / 故障10%
            status = _weighted([1, 0, 2], [70, 20, 10])
            height = round(random.uniform(6, 12), 2)
            install = (self.now - timedelta(days=random.randint(30, 720))).date()
            pid = self._insert(
                "dev_pole",
                ["pole_code", "pole_name", "area_id", "address", "lng", "lat", "height",
                 "status", "install_time", "create_time", "update_time",
                 "create_by", "update_by", "deleted"],
                (pole_code, pole_name, area_id, address, lng, lat, height,
                 status, install, *self._audit()),
            )
            poles.append({"id": pid, "code": pole_code, "area_id": area_id,
                          "status": status, "lng": lng, "lat": lat})
        self.conn.commit()
        self.counts["dev_pole"] = n
        self._poles = poles
        return poles

    # ---------- 设备 ----------
    def gen_devices(self, per_pole_max=4):
        devices = []
        code_seq = 1
        for pole in self._poles:
            cnt = random.randint(1, per_pole_max)
            # 故障灯杆上的设备大概率故障，离线灯杆设备离线
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
                # 处理中 / 已闭环 均已分配处理人
                handle_user = random.choice(users)[1] if users else "admin"
            if status == 2:
                # 已闭环：填写处理意见 + 完成时间
                handle_time = alarm_time + timedelta(minutes=random.randint(10, 600))
                handle_result = random.choice(HANDLE_RESULTS)
            self._insert(
                "alarm_record",
                ["device_id", "pole_id", "alarm_type", "alarm_level", "alarm_content",
                 "alarm_time", "status", "handle_user", "handle_time", "handle_result",
                 "create_time", "update_time", "create_by", "update_by", "deleted"],
                (dev["code"], dev["pole_id"], atype, level, content_map[atype],
                 alarm_time, status, handle_user, handle_time, handle_result, *self._audit()),
            )
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

    # ---------- 环境传感器数据（时序，含昼夜变化） ----------
    def gen_env(self, days=7):
        sensors = [d for d in self._devices if d["type"] == "SENSOR"]
        sample = random.sample(sensors, min(10, len(sensors))) if sensors else []
        rows = []
        base = self.now.replace(hour=0, minute=0, second=0, microsecond=0)
        for dev in sample:
            for d in range(days, 0, -1):
                day = base - timedelta(days=d - 1)
                day_temp_base = random.uniform(24, 30)
                for h in range(0, 24, 1):
                    t = day + timedelta(hours=h)
                    # 温度：6-18点正弦上升，夜间下降
                    temp = round(day_temp_base + 4 * math.sin(math.pi * (h - 6) / 12)
                                 + random.uniform(-1.5, 1.5), 2) if 6 <= h <= 18 \
                        else round(day_temp_base - 3 + random.uniform(-1, 1), 2)
                    humidity = round(random.uniform(45, 92) - (temp - 25), 2)
                    humidity = max(20, min(99, humidity))
                    # PM2.5 偶发污染高峰
                    pm25 = round(random.uniform(100, 250), 2) if random.random() < 0.05 \
                        else round(random.uniform(8, 75), 2)
                    pm10 = round(pm25 + random.uniform(5, 40), 2)
                    noise = round(random.uniform(35, 55) if h < 7 or h > 22
                                  else random.uniform(50, 72), 2)
                    # 光照：白天正弦，夜间近 0
                    if 6 <= h <= 18:
                        illum = round(max(0, 80000 * math.sin(math.pi * (h - 6) / 12))
                                      + random.uniform(-2000, 2000), 2)
                    else:
                        illum = round(random.uniform(0, 500), 2)
                    wind = round(random.uniform(0, 8), 2)
                    wdir = random.choice(WIND_DIRS)
                    rows.append((dev["pole_id"], temp, humidity, pm25, pm10, noise,
                                 illum, wind, wdir, t, self.now, self.now, 1, 1, 0))
        sql = ("INSERT INTO env_sensor_data (pole_id, temperature, humidity, pm25, pm10, noise, "
               "illumination, wind_speed, wind_direction, record_time, create_time, update_time, "
               "create_by, update_by, deleted) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)")
        self.cur.executemany(sql, rows)
        self.conn.commit()
        self.counts["env_sensor_data"] = len(rows)

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
        for _ in range(n):
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
            if status in (2, 3):
                finish = create_t + timedelta(hours=random.randint(2, 48))
            self._insert(
                "work_order",
                ["order_no", "order_type", "title", "description", "device_id", "pole_id",
                 "assignee_id", "priority", "status", "finish_time", "create_time",
                 "update_time", "create_by", "update_by", "deleted"],
                (order_no, otype, f"{dev['code']}-{title}", f"针对设备 {dev['code']} 的{title}任务",
                 dev["code"], dev["pole_id"], assignee, random.choice([1, 2, 2, 3]),
                 status, finish, create_t, self.now, 1, 1, 0),
            )
        self.conn.commit()
        self.counts["work_order"] = n

    # ---------- 总入口 ----------
    def run(self):
        self.clear()
        self.gen_roles()
        self.gen_users()
        self.gen_poles()
        self.gen_devices()
        self.gen_strategies()
        self.gen_energy()
        self.gen_alarms()
        self.gen_cameras()
        self.gen_env()
        self.gen_led()
        self.gen_workorders()
        self.conn.commit()
        return self.counts
