package com.ccb.lighting.module.system.service;

import com.ccb.lighting.module.system.entity.SysMenu;

import java.util.List;

/**
 * 系统菜单 Service 接口
 *
 * <p>方法清单：
 * - list：全部菜单（扁平，按排序号升序）
 * - tree：菜单树（父→子递归组装，前端侧边栏/权限分配树用）
 * - getById / add / update / delete（删除含子项时拒绝）</p>
 */
public interface SysMenuService {

    /** 全部菜单（扁平） */
    List<SysMenu> list();

    /** 菜单树（已组装 children） */
    List<SysMenu> tree();

    /** 菜单详情 */
    SysMenu getById(Long id);

    /** 新增菜单 */
    void add(SysMenu menu);

    /** 修改菜单 */
    void update(SysMenu menu);

    /** 删除菜单（存在子项则拒绝） */
    void delete(Long id);
}
