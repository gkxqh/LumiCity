#!/usr/bin/env bash
# 启动数据库工具。双击或在终端执行：./run.sh
set -e
cd "$(dirname "$0")"

if [ ! -d ".venv" ]; then
  echo "首次运行：创建虚拟环境并安装依赖..."
  python3 -m venv .venv
  .venv/bin/pip install --quiet --upgrade pip
  .venv/bin/pip install -r requirements.txt
fi

echo "启动数据库工具 → http://localhost:5050"
.venv/bin/python app.py
