# -*- coding: utf-8 -*-
"""
智慧城市照明系统 - 数据库初始化与测试数据工具（独立小工具）

功能按钮：
  1. 检测并启动 MySQL   /api/start-mysql
  2. 初始化数据库        /api/init-db      （执行 schema.sql，drop & 重建）
  3. 生成测试数据        /api/gen-data     （随机覆盖各种情况）
  4. 一键全流程          /api/all
  5. 数据概览            /api/overview

运行：python app.py   然后浏览器打开 http://localhost:5050
"""
import os
import time
import socket
import shutil
import subprocess
from flask import Flask, request, jsonify, render_template_string

import pymysql
from data_generator import DataGenerator

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
SCHEMA_PATH = os.path.join(BASE_DIR, "schema.sql")
DEFAULT_DB = "smart_lighting"

app = Flask(__name__)


# ======================== 基础工具 ========================
def _cfg():
    """从请求体读取连接配置，带默认值"""
    body = request.get_json(silent=True) or {}
    return {
        "host": body.get("host", "localhost"),
        "port": int(body.get("port", 3306)),
        "user": body.get("user", "root"),
        "password": body.get("password", "123456"),
        "db": body.get("db", DEFAULT_DB),
    }


def port_open(host, port, timeout=2):
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True
    except OSError:
        return False


def connect(cfg, use_db=False):
    return pymysql.connect(
        host=cfg["host"], port=cfg["port"], user=cfg["user"],
        password=cfg["password"],
        db=cfg["db"] if use_db else None,
        charset="utf8mb4", autocommit=False,
        read_timeout=30, write_timeout=60,
    )


def split_sql(sql_text):
    """去掉 -- 注释行，按 ; 拆分语句"""
    buf = []
    for line in sql_text.splitlines():
        s = line.strip()
        if not s or s.startswith("--"):
            continue
        buf.append(line)
    text = "\n".join(buf)
    out = []
    for part in text.split(";"):
        p = part.strip()
        if p:
            out.append(p)
    return out


def db_exists_and_ok(cfg):
    """检查库是否存在且结构大致完整（至少有 sys_user / dev_pole 表）"""
    try:
        conn = connect(cfg, use_db=False)
    except pymysql.err.OperationalError as e:
        return False, f"MySQL 连接失败：{e}"
    try:
        cur = conn.cursor()
        cur.execute("SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME=%s",
                    (cfg["db"],))
        if not cur.fetchone():
            return False, f"数据库 {cfg['db']} 不存在"
        cur.execute("SELECT COUNT(*) FROM information_schema.tables "
                    "WHERE table_schema=%s AND table_name IN ('sys_user','dev_pole','alarm_record')",
                    (cfg["db"],))
        n = cur.fetchone()[0]
        if n < 3:
            return False, f"数据库 {cfg['db']} 结构不完整（核心表缺失）"
        return True, "ok"
    finally:
        conn.close()


# ======================== 接口 ========================
@app.post("/api/start-mysql")
def start_mysql():
    cfg = _cfg()
    logs = []
    if port_open(cfg["host"], cfg["port"], 2):
        logs.append(f"✓ MySQL 端口 {cfg['host']}:{cfg['port']} 已开放，疑似正在运行")
        return jsonify({"ok": True, "logs": logs})

    brew = shutil.which("brew") or shutil.which("brew",
        path="/opt/homebrew/bin:/usr/local/bin")
    candidates = []
    if brew:
        candidates += [[brew, "services", "start", "mysql"],
                       [brew, "services", "start", "mysql@8.0"]]
    candidates += [["mysql.server", "start"],
                   ["/usr/local/mysql/support-files/mysql.server", "start"]]
    started = False
    for cmd in candidates:
        try:
            logs.append(f"尝试执行: {' '.join(cmd)}")
            r = subprocess.run(cmd, capture_output=True, text=True, timeout=25)
            out = (r.stdout or "") + (r.stderr or "")
            if out.strip():
                logs.append(out.strip()[:400])
            time.sleep(2.5)
            if port_open(cfg["host"], cfg["port"], 2):
                logs.append("✓ 启动成功")
                started = True
                break
            else:
                logs.append("端口仍未开放，继续尝试下一种方式")
        except FileNotFoundError:
            logs.append("命令不存在，跳过")
        except Exception as e:
            logs.append(f"执行出错: {e}")
    if not started:
        logs.append("✗ 自动启动未成功，请手动启动 MySQL（如 brew services start mysql）")
    return jsonify({"ok": started, "logs": logs})


@app.post("/api/init-db")
def init_db():
    cfg = _cfg()
    logs = []
    try:
        conn = connect(cfg, use_db=False)
    except pymysql.err.OperationalError as e:
        return jsonify({"ok": False, "logs": [f"连接失败：{e}",
                        "请先点【检测并启动 MySQL】"]})
    try:
        cur = conn.cursor()
        with open(SCHEMA_PATH, "r", encoding="utf-8") as f:
            sql_text = f.read()
        stmts = split_sql(sql_text)
        logs.append(f"共解析出 {len(stmts)} 条语句，开始执行 schema.sql ...")
        for i, s in enumerate(stmts, 1):
            cur.execute(s)
        conn.commit()
        logs.append(f"✓ 已执行 {len(stmts)} 条语句，数据库 {cfg['db']} 初始化完成")
        logs.append("  - 创建 13 张表 + 初始 admin 用户 + 角色/菜单")
        return jsonify({"ok": True, "logs": logs})
    except Exception as e:
        conn.rollback()
        logs.append(f"✗ 执行失败：{e}")
        return jsonify({"ok": False, "logs": logs})
    finally:
        conn.close()


@app.post("/api/gen-data")
def gen_data():
    cfg = _cfg()
    logs = []
    ok, msg = db_exists_and_ok(cfg)
    if not ok:
        return jsonify({"ok": False, "logs": [f"前置检查未通过：{msg}",
                        "请先点【初始化数据库】"]})
    try:
        conn = connect(cfg, use_db=True)
    except pymysql.err.OperationalError as e:
        return jsonify({"ok": False, "logs": [f"连接失败：{e}"]})
    try:
        gen = DataGenerator(conn)
        counts = gen.run()
        logs.append("✓ 测试数据生成完成（保留 admin 及初始角色/菜单）：")
        order = ["sys_user", "dev_pole", "dev_device", "light_strategy",
                 "energy_record", "alarm_record", "video_camera",
                 "env_sensor_data", "led_program", "work_order"]
        for k in order:
            if k in counts:
                logs.append(f"  - {k}: 新增 {counts[k]} 条")
        logs.append("提示：每次生成数据均随机，且会先清空旧测试数据")
        return jsonify({"ok": True, "logs": logs, "counts": counts})
    except Exception as e:
        conn.rollback()
        import traceback
        logs.append(f"✗ 生成失败：{e}")
        logs.append(traceback.format_exc().splitlines()[-1])
        return jsonify({"ok": False, "logs": logs})
    finally:
        conn.close()


@app.post("/api/all")
def run_all():
    """一键全流程：启动 → 初始化 → 生成数据"""
    cfg = _cfg()
    all_logs = []

    def merge(resp):
        all_logs.extend(resp.get("logs", []))

    # 1. start
    r = start_mysql()
    merge(r.get_json())
    if not r.get_json().get("ok"):
        return jsonify({"ok": False, "logs": all_logs})

    # 2. init
    r2 = init_db()
    merge(r2.get_json())
    if not r2.get_json().get("ok"):
        return jsonify({"ok": False, "logs": all_logs})

    # 3. gen
    r3 = gen_data()
    merge(r3.get_json())
    return jsonify({"ok": r3.get_json().get("ok", False), "logs": all_logs})


@app.post("/api/overview")
def overview():
    cfg = _cfg()
    logs = []
    # MySQL 状态
    running = port_open(cfg["host"], cfg["port"], 2)
    logs.append(f"MySQL 端口 {cfg['port']}：{'运行中' if running else '未开放'}")
    if not running:
        return jsonify({"ok": False, "logs": logs, "running": False})

    ok, msg = db_exists_and_ok(cfg)
    logs.append(f"数据库检查：{msg}")
    if not ok:
        return jsonify({"ok": False, "logs": logs, "running": True, "db_ok": False})

    try:
        conn = connect(cfg, use_db=True)
    except pymysql.err.OperationalError as e:
        logs.append(f"连接失败：{e}")
        return jsonify({"ok": False, "logs": logs})
    try:
        cur = conn.cursor()
        tables = ["sys_user", "sys_role", "sys_menu", "dev_pole", "dev_device",
                  "light_strategy", "energy_record", "alarm_record", "video_camera",
                  "env_sensor_data", "led_program", "work_order"]
        counts = {}
        for t in tables:
            cur.execute(f"SELECT COUNT(*) FROM {t} WHERE deleted=0")
            counts[t] = cur.fetchone()[0]
        logs.append("✓ 各表数据量（deleted=0）：")
        for t in tables:
            logs.append(f"  {t}: {counts[t]}")
        return jsonify({"ok": True, "logs": logs, "counts": counts,
                        "running": True, "db_ok": True})
    except Exception as e:
        logs.append(f"✗ 查询失败：{e}")
        return jsonify({"ok": False, "logs": logs})
    finally:
        conn.close()


# ======================== 页面 ========================
HTML = """<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>照明系统 - 数据库工具</title>
<style>
  :root{--bg:#f4f6fa;--card:#fff;--border:#e3e8ef;--text:#1f2937;--muted:#6b7280;
    --pri:#3b6fe0;--pri-h:#2f5fc4;--ok:#16a34a;--warn:#d97706;--err:#dc2626;--code:#0f172a}
  *{box-sizing:border-box}
  body{margin:0;font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif;
    background:var(--bg);color:var(--text);line-height:1.6}
  .wrap{max-width:920px;margin:0 auto;padding:28px 20px 60px}
  h1{font-size:22px;margin:0 0 4px}
  .sub{color:var(--muted);font-size:13px;margin-bottom:20px}
  .card{background:var(--card);border:1px solid var(--border);border-radius:12px;
    padding:18px 18px;margin-bottom:16px;box-shadow:0 1px 2px rgba(0,0,0,.03)}
  .card h2{font-size:15px;margin:0 0 12px;display:flex;align-items:center;gap:8px}
  .grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}
  .grid label{font-size:12px;color:var(--muted);display:block;margin-bottom:3px}
  .grid input{width:100%;padding:8px 10px;border:1px solid var(--border);border-radius:8px;
    font-size:13px;outline:none}
  .grid input:focus{border-color:var(--pri)}
  .btns{display:flex;flex-wrap:wrap;gap:10px}
  button{cursor:pointer;border:none;border-radius:9px;padding:11px 16px;font-size:14px;
    font-weight:600;color:#fff;background:var(--pri);transition:.15s;display:flex;align-items:center;gap:6px}
  button:hover{background:var(--pri-h)}
  button:disabled{opacity:.6;cursor:wait}
  button.secondary{background:#64748b}
  button.secondary:hover{background:#475569}
  button.success{background:#0ea568}
  button.success:hover{background:#0b8757}
  .log{background:var(--code);color:#e2e8f0;border-radius:10px;padding:14px 16px;
    font-family:"SF Mono",Menlo,Consolas,monospace;font-size:12.5px;line-height:1.7;
    min-height:120px;max-height:420px;overflow:auto;white-space:pre-wrap;word-break:break-all}
  .log .ok{color:#4ade80}.log .err{color:#f87171}.log .warn{color:#fbbf24}.log .dim{color:#94a3b8}
  .pill{font-size:11px;padding:2px 8px;border-radius:20px;background:#eef2ff;color:var(--pri)}
  .status{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px}
  .badge{font-size:12px;padding:4px 10px;border-radius:20px;border:1px solid var(--border);
    background:#fff;color:var(--muted)}
  .badge.on{background:#dcfce7;color:var(--ok);border-color:#bbf7d0}
  .badge.off{background:#fee2e2;color:var(--err);border-color:#fecaca}
  .spin{width:14px;height:14px;border:2px solid #fff;border-top-color:transparent;
    border-radius:50%;display:inline-block;animation:sp .7s linear infinite}
  @keyframes sp{to{transform:rotate(360deg)}}
  .hint{font-size:12px;color:var(--muted);margin-top:6px}
</style>
</head>
<body>
<div class="wrap">
  <h1>🧩 智慧城市照明系统 · 数据库工具</h1>
  <div class="sub">独立于主项目的小工具：管理 MySQL 启动 / 库初始化 / 测试数据生成</div>

  <div class="status">
    <span id="bMysql" class="badge">MySQL：未知</span>
    <span id="bDb" class="badge">数据库：未知</span>
  </div>

  <div class="card">
    <h2>⚙️ 连接配置</h2>
    <div class="grid">
      <div><label>主机</label><input id="host" value="localhost"></div>
      <div><label>端口</label><input id="port" value="3306"></div>
      <div><label>用户名</label><input id="user" value="root"></div>
      <div><label>密码</label><input id="password" value="123456" type="text"></div>
      <div><label>数据库名</label><input id="db" value="smart_lighting"></div>
    </div>
    <div class="hint">默认值取自后端 application.yml（root / 123456 / smart_lighting）</div>
  </div>

  <div class="card">
    <h2>🚀 操作（每个功能独立按钮）</h2>
    <div class="btns">
      <button onclick="call('/api/start-mysql')">① 检测并启动 MySQL</button>
      <button class="secondary" onclick="call('/api/init-db')">② 初始化数据库</button>
      <button class="success" onclick="call('/api/gen-data')">③ 生成测试数据</button>
      <button onclick="call('/api/all')">⚡ 一键全流程</button>
      <button class="secondary" onclick="call('/api/overview')">📊 数据概览</button>
    </div>
  </div>

  <div class="card">
    <h2>📋 执行日志 <span class="pill" id="lastTime"></span></h2>
    <div class="log" id="log"><span class="dim">等待操作...</span></div>
  </div>
</div>

<script>
function cfg(){return{host:v('host'),port:v('port'),user:v('user'),password:v('password'),db:v('db')}}
function v(id){return document.getElementById(id).value}
function append(lines,ok){
  const el=document.getElementById('log');
  if(el.querySelector('.dim'))el.innerHTML='';
  const t=new Date().toLocaleTimeString('zh-CN');
  document.getElementById('lastTime').textContent=t;
  lines.forEach(l=>{
    let cls='';
    if(/^✓/.test(l)||/成功|完成|运行中/.test(l))cls='ok';
    else if(/^✗/.test(l)||/失败|未开放|不存在/.test(l))cls='err';
    else if(/尝试|提示|未通过/.test(l))cls='warn';
    el.innerHTML+=`<div class="${cls}">${escapeHtml(l)}</div>`;
  });
  el.scrollTop=el.scrollHeight;
}
function escapeHtml(s){return String(s).replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]))}

async function call(url){
  const btns=[...document.querySelectorAll('button')];
  btns.forEach(b=>b.disabled=true);
  append([`▶ 调用 ${url} ...`],'');
  try{
    const r=await fetch(url,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(cfg())});
    const j=await r.json();
    append(j.logs||[],j.ok);
    if(j.running!==undefined)updateBadges(j);
  }catch(e){append(['✗ 请求失败：'+e],'err');}
  finally{btns.forEach(b=>b.disabled=false);}
}
function updateBadges(j){
  const m=document.getElementById('bMysql'),d=document.getElementById('bDb');
  if(j.running===true){m.className='badge on';m.textContent='MySQL：运行中';}
  else if(j.running===false){m.className='badge off';m.textContent='MySQL：未运行';}
  if(j.db_ok===true){d.className='badge on';d.textContent='数据库：就绪';}
  else if(j.db_ok===false){d.className='badge off';d.textContent='数据库：未就绪';}
}
window.addEventListener('load',()=>call('/api/overview'));
</script>
</body>
</html>"""

@app.get("/")
def index():
    return render_template_string(HTML)


if __name__ == "__main__":
    print("数据库工具已启动：http://localhost:5050")
    app.run(host="127.0.0.1", port=5050, debug=False)
