package com.ccb.lighting.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 系统用户实体 SysUser
 *
 * <p>对应数据库表 sys_user，存储平台登录账号信息。
 * 继承 BaseEntity 自动拥有 id、createTime、updateTime、createBy、updateBy、deleted 字段，
 * 这里只声明本表特有的业务字段。</p>
 *
 * <p>注解说明：
 * - @TableName("sys_user")：指定数据库表名（类名与表名不一致时必须写，一致也可省略，这里显式写更清晰）
 * - @EqualsAndHashCode(callSuper = true)：Lombok 生成 equals/hashCode 时把父类 BaseEntity 的字段也算进去，
 *   继承场景下必须加，否则可能只比子类字段导致 id 相同的两个对象判不相等</p>
 *
 * <p>表结构：
 * CREATE TABLE sys_user (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   username VARCHAR(50) NOT NULL COMMENT '用户名',
 *   password VARCHAR(100) NOT NULL COMMENT '密码',
 *   nickname VARCHAR(50) COMMENT '昵称',
 *   phone VARCHAR(20) COMMENT '手机号',
 *   email VARCHAR(50) COMMENT '邮箱',
 *   status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
 *   create_time DATETIME, update_time DATETIME,
 *   create_by BIGINT, update_by BIGINT, deleted TINYINT DEFAULT 0
 * );</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity implements Serializable {

    /** 用户名（登录账号），唯一，新增时需查重 */
    private String username;

    /** 密码：存储 BCrypt 加密后的值 */
    private String password;

    /** 昵称：用于前端页面展示的真实姓名 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 状态：0=禁用（无法登录），1=启用（正常登录）。登录时需校验此字段 */
    private Integer status;
}
