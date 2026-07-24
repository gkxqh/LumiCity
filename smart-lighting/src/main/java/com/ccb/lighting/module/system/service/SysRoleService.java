package com.ccb.lighting.module.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.module.system.entity.SysRole;

import java.util.List;

/**
 * 系统角色 Service 接口
 *
 * <p>方法清单：
 * - list / pageList：角色列表 / 分页（支持角色名、状态筛选）
 * - getById：角色详情
 * - add / update / delete：角色增删改（删除会一并清理角色-菜单绑定）
 * - getMenuIds：查某角色绑定的菜单 ID 列表（回显勾选用）
 * - assignMenus：给角色重新分配菜单（重写 sys_role_menu）</p>
 */
public interface SysRoleService {

    /** 角色列表（全部，按创建时间倒序） */
    List<SysRole> list(String roleName, Integer status);

    /** 角色分页 */
    Page<SysRole> pageList(Integer current, Integer size, String roleName, Integer status);

    /** 角色详情 */
    SysRole getById(Long id);

    /** 新增角色 */
    void add(SysRole role);

    /** 修改角色 */
    void update(SysRole role);

    /** 删除角色（逻辑删除 + 清理菜单绑定） */
    void delete(Long id);

    /** 查询角色绑定的菜单 ID 列表 */
    List<Long> getMenuIds(Long roleId);

    /** 给角色分配菜单：先删后插，整体重写绑定关系 */
    void assignMenus(Long roleId, List<Long> menuIds);
}
