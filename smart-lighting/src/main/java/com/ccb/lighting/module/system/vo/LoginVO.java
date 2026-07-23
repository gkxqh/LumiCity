package com.ccb.lighting.module.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录返回 VO（View Object）
 *
 * <p>作用：登录成功后返回给前端的数据，只包含前端需要的字段。
 * 前端拿到 token 后，后续请求都带上它（放在 Header 里）以证明已登录。</p>
 *
 * <p>为什么不直接返回 SysUser？
 * 1. 不能把 password 等敏感字段返回前端
 * 2. 前端只需要 token、用户名、昵称、角色列表，多了的字段是冗余
 * 3. VO 专门给"视图层"用，与数据库实体解耦</p>
 *
 * <p>字段说明：
 * - token：JWT 令牌，前端存 localStorage，每次请求放 Header
 * - username：账号，前端展示用
 * - nickname：昵称，前端展示用
 * - roles：角色编码列表（如 ["admin","operator"]），前端用来做按钮级权限控制</p>
 */
@Data
public class LoginVO implements Serializable {

    /** JWT 令牌 */
    private String token;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 角色编码列表，用于前端权限控制 */
    private List<String> roles;
}
