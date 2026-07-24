package com.ccb.lighting.module.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 当前登录用户信息 VO
 *
 * <p>GET /auth/info 返回，包含用户名、昵称、角色编码、权限标识。
 * 前端拿到后写入用户 Store，用于侧边栏渲染与按钮级权限控制（v-hasPerm）。</p>
 */
@Data
public class UserInfoVO implements Serializable {

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 角色编码列表 */
    private List<String> roles;

    /** 权限标识列表 */
    private List<String> perms;
}
