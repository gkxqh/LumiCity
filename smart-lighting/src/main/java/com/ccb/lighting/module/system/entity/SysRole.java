package com.ccb.lighting.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 系统角色实体 SysRole
 *
 * <p>对应数据库表 sys_role，RBAC 权限模型中的"角色"。
 * 用户通过 sys_user_role 中间表与角色关联（一个用户可有多个角色）。</p>
 *
 * <p>RBAC 模型说明：
 * User(用户) —— sys_user_role(中间表) —— Role(角色) —— sys_role_menu(中间表) —— Menu(菜单/权限)
 * 即：用户绑角色，角色绑菜单，最终决定用户能看到哪些菜单、能点哪些按钮。</p>
 *
 * <p>表结构参考：
 * CREATE TABLE sys_role (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
 *   role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
 *   status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
 *   create_time DATETIME, update_time DATETIME,
 *   create_by BIGINT, update_by BIGINT, deleted TINYINT DEFAULT 0
 * );</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity implements Serializable {

    /** 角色名称：展示用，如"系统管理员"、"运维人员" */
    private String roleName;

    /** 角色编码：程序里判断权限用，如 admin、operator。登录时放入 LoginVO 的 roles 就是这个值 */
    private String roleCode;

    /** 状态：0=禁用，1=启用 */
    private Integer status;
}
