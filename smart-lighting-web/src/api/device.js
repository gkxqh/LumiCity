import request from './request'

// 设备分页查询
export function pageDevice(params) {
  return request({ url: '/device/page', method: 'get', params })
}

// 设备详情
export function getDevice(id) {
  return request({ url: `/device/${id}`, method: 'get' })
}

// 新增设备
export function addDevice(data) {
  return request({ url: '/device', method: 'post', data })
}

// 修改设备
export function updateDevice(data) {
  return request({ url: '/device', method: 'put', data })
}

// 删除设备
export function deleteDevice(id) {
  return request({ url: `/device/${id}`, method: 'delete' })
}

// 灯杆分页查询
export function pagePole(params) {
  return request({ url: '/device/pole/page', method: 'get', params })
}

// 灯杆列表（下拉框用）
export function listPole() {
  return request({ url: '/device/pole/list', method: 'get' })
}

// 新增灯杆
export function addPole(data) {
  return request({ url: '/device/pole', method: 'post', data })
}

// 修改灯杆
export function updatePole(data) {
  return request({ url: '/device/pole', method: 'put', data })
}

// 删除灯杆
export function deletePole(id) {
  return request({ url: `/device/pole/${id}`, method: 'delete' })
}
