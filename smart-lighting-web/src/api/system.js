import request from './request'

// ========== 角色管理 ==========
export function listRole(params) {
  return request({ url: '/system/role/list', method: 'get', params })
}
export function pageRole(params) {
  return request({ url: '/system/role/page', method: 'get', params })
}
export function addRole(data) {
  return request({ url: '/system/role', method: 'post', data })
}
export function updateRole(data) {
  return request({ url: '/system/role', method: 'put', data })
}
export function deleteRole(id) {
  return request({ url: `/system/role/${id}`, method: 'delete' })
}
export function getRoleMenus(id) {
  return request({ url: `/system/role/${id}/menus`, method: 'get' })
}
export function assignRoleMenus(id, menuIds) {
  return request({ url: `/system/role/${id}/menus`, method: 'put', data: menuIds })
}

// ========== 菜单管理 ==========
export function listMenu() {
  return request({ url: '/system/menu/list', method: 'get' })
}
export function treeMenu() {
  return request({ url: '/system/menu/tree', method: 'get' })
}
export function addMenu(data) {
  return request({ url: '/system/menu', method: 'post', data })
}
export function updateMenu(data) {
  return request({ url: '/system/menu', method: 'put', data })
}
export function deleteMenu(id) {
  return request({ url: `/system/menu/${id}`, method: 'delete' })
}

// ========== 用户-角色绑定 ==========
export function getUserRoles(id) {
  return request({ url: `/system/user/${id}/roles`, method: 'get' })
}
export function assignUserRoles(id, roleIds) {
  return request({ url: `/system/user/${id}/roles`, method: 'put', data: roleIds })
}
