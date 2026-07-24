package com.ccb.lighting.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 系统菜单实体 SysMenu
 *
 * <p>对应数据库表 sys_menu，这里的"菜单"泛化了三类资源：
 * - 目录(M)：左侧导航的一级分组，如"系统管理"
 * - 菜单(C)：具体的页面，如"用户管理"，有路由 path 和前端组件 component
 * - 按钮(F)：页面内的操作按钮，如"新增""删除"，用 perms 权限标识控制显隐</p>
 *
 * <p>菜单是树形结构：通过 parentId 指向父菜单 id，顶层菜单 parentId=0。
 * 前端拿到菜单列表后自己组装成树渲染侧边栏。</p>
 *
 * <p>表结构参考：
 * CREATE TABLE sys_menu (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
 *   parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID，0为顶层',
 *   path VARCHAR(200) COMMENT '路由路径',
 *   component VARCHAR(200) COMMENT '前端组件路径',
 *   perms VARCHAR(100) COMMENT '权限标识，如 system:user:add',
 *   menu_type CHAR(1) COMMENT '类型：M目录 C菜单 F按钮',
 *   icon VARCHAR(50) COMMENT '图标',
 *   order_num INT DEFAULT 0 COMMENT '排序号，越小越靠前',
 *   create_time DATETIME, update_time DATETIME,
 *   create_by BIGINT, update_by BIGINT, deleted TINYINT DEFAULT 0
 * );</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity implements Serializable {

    /** 菜单名称：展示用，如"用户管理" */
    private String menuName;

    /** 父菜单 ID：0 表示顶层菜单。通过它构建菜单树 */
    private Long parentId;

    /** 路由路径：前端 Vue Router 的 path，如 "user"；目录类型可只写 "system" */
    private String path;

    /** 前端组件路径：如 "system/user/index"，目录(M)和按钮(F)通常为空 */
    private String component;

    /** 权限标识：如 "system:user:add"，按钮(F)类型必填，用于后端鉴权和前端按钮显隐 */
    private String perms;

    /** 菜单类型：M=目录，C=菜单，F=按钮 */
    private String menuType;

    /** 图标：菜单左侧图标名，前端图标库的名称 */
    private String icon;

    /** 排序号：同级菜单按此字段升序排列，越小越靠前（数据库列名 sort） */
    @com.baomidou.mybatisplus.annotation.TableField("sort")
    private Integer orderNum;

    /** 子菜单列表（树形展示用，数据库无此列） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<SysMenu> children;
}
