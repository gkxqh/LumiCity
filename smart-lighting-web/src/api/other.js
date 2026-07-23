import request from './request'

// ========== 数据大盘 ==========
export function getOverview() {
  return request({ url: '/dashboard/overview', method: 'get' })
}
export function getAlarmTrend() {
  return request({ url: '/dashboard/alarm/trend', method: 'get' })
}

// ========== 告警管理 ==========
export function pageAlarm(params) {
  return request({ url: '/alarm/page', method: 'get', params })
}
export function addAlarm(data) {
  return request({ url: '/alarm', method: 'post', data })
}
export function handleAlarm(params) {
  return request({ url: '/alarm/handle', method: 'put', params })
}
export function alarmStatistics() {
  return request({ url: '/alarm/statistics', method: 'get' })
}

// ========== 能耗管理 ==========
export function pageEnergy(params) {
  return request({ url: '/energy/page', method: 'get', params })
}
export function energyTrend(params) {
  return request({ url: '/energy/trend', method: 'get', params })
}
export function energyStatistics(params) {
  return request({ url: '/energy/statistics', method: 'get', params })
}

// ========== 照明控制 ==========
export function pageStrategy(params) {
  return request({ url: '/lighting/strategy/page', method: 'get', params })
}
export function addStrategy(data) {
  return request({ url: '/lighting/strategy', method: 'post', data })
}
export function updateStrategy(data) {
  return request({ url: '/lighting/strategy', method: 'put', data })
}
export function deleteStrategy(id) {
  return request({ url: `/lighting/strategy/${id}`, method: 'delete' })
}
export function controlSwitch(params) {
  return request({ url: '/lighting/control/switch', method: 'post', params })
}
export function controlBrightness(params) {
  return request({ url: '/lighting/control/brightness', method: 'post', params })
}

// ========== 视频监控 ==========
export function pageCamera(params) {
  return request({ url: '/video/camera/page', method: 'get', params })
}
export function addCamera(data) {
  return request({ url: '/video/camera', method: 'post', data })
}
export function updateCamera(data) {
  return request({ url: '/video/camera', method: 'put', data })
}
export function deleteCamera(id) {
  return request({ url: `/video/camera/${id}`, method: 'delete' })
}

// ========== 环境监测 ==========
export function pageEnv(params) {
  return request({ url: '/env/page', method: 'get', params })
}
export function latestEnv(poleId) {
  return request({ url: `/env/latest/${poleId}`, method: 'get' })
}
export function envTrend(params) {
  return request({ url: '/env/trend', method: 'get', params })
}

// ========== 信息发布 ==========
export function pageProgram(params) {
  return request({ url: '/publish/program/page', method: 'get', params })
}
export function addProgram(data) {
  return request({ url: '/publish/program', method: 'post', data })
}
export function updateProgram(data) {
  return request({ url: '/publish/program', method: 'put', data })
}
export function deleteProgram(id) {
  return request({ url: `/publish/program/${id}`, method: 'delete' })
}
export function publishProgram(id) {
  return request({ url: `/publish/program/${id}/publish`, method: 'put' })
}

// ========== 工单运维 ==========
export function pageWorkOrder(params) {
  return request({ url: '/workorder/page', method: 'get', params })
}
export function addWorkOrder(data) {
  return request({ url: '/workorder', method: 'post', data })
}
export function assignWorkOrder(id, params) {
  return request({ url: `/workorder/assign/${id}`, method: 'put', params })
}
export function handleWorkOrder(id) {
  return request({ url: `/workorder/handle/${id}`, method: 'put' })
}
export function finishWorkOrder(id) {
  return request({ url: `/workorder/finish/${id}`, method: 'put' })
}

// ========== 系统管理 ==========
export function pageUser(params) {
  return request({ url: '/system/user/page', method: 'get', params })
}
export function addUser(data) {
  return request({ url: '/system/user', method: 'post', data })
}
export function updateUser(data) {
  return request({ url: '/system/user', method: 'put', data })
}
export function deleteUser(id) {
  return request({ url: `/system/user/${id}`, method: 'delete' })
}
