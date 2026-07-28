/**
 * 告警 WebSocket 客户端
 *
 * 连接地址：ws://host/api/ws/alarm?token=xxx
 * - token 从 localStorage 取（与 axios 请求头同一个 JWT）
 * - 收到消息后回调所有注册的监听器
 * - 断线自动重连（3s），便于长时间保持实时推送
 *
 * 消息结构（后端 AlarmRecordServiceImpl.buildMessage）：
 *   { event: "alarm_new" | "alarm_handled", data: { id, deviceId, alarmType, ... }, time }
 */
import { getToken } from '@/utils/auth'

let ws = null
let reconnectTimer = null
const listeners = []
// 防止重连风暴：连接中/已连接时不重复建连
let connecting = false

/** 建立告警 WebSocket 连接 */
export function connectAlarmWS() {
  if (connecting) return
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return

  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const token = getToken()
  // 严格模式：无 token 时不发起连接，避免被后端拒绝后陷入重连循环
  if (!token) return
  // 路径带 /api 前缀，开发环境由 Vite 代理（需 ws:true）转发到后端 8080
  const url = `${proto}://${window.location.host}/api/ws/alarm?token=${token}`

  connecting = true
  try {
    ws = new WebSocket(url)
  } catch (e) {
    connecting = false
    scheduleReconnect()
    return
  }

  ws.onopen = () => {
    connecting = false
    // 连接成功，可在此打日志
  }

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      listeners.forEach((fn) => fn(msg))
    } catch (e) {
      // 非 JSON 文本，忽略
    }
  }

  ws.onerror = () => {
    // 错误后通常紧跟 onclose，由 onclose 触发重连
  }

  ws.onclose = () => {
    connecting = false
    scheduleReconnect()
  }
}

/** 注册消息监听器，收到任意消息时回调 */
export function onAlarmMessage(fn) {
  if (!listeners.includes(fn)) listeners.push(fn)
  return () => {
    // 返回取消监听函数
    const i = listeners.indexOf(fn)
    if (i >= 0) listeners.splice(i, 1)
  }
}

/** 主动断开（页面卸载时调用） */
export function disconnectAlarmWS() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  connecting = false
  if (ws) {
    ws.onclose = null // 防止触发重连
    ws.close()
    ws = null
  }
}

function scheduleReconnect() {
  if (reconnectTimer) return
  // token 为空时不重连，避免无效重连循环
  if (!getToken()) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connectAlarmWS()
  }, 3000)
}
