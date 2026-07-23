package com.ccb.lighting.module.system.service;

import com.ccb.lighting.module.system.dto.LoginDTO;
import com.ccb.lighting.module.system.dto.RegisterDTO;
import com.ccb.lighting.module.system.vo.LoginVO;

/**
 * 认证 Service 接口
 *
 * <p>负责登录认证相关业务：校验账号密码、生成 JWT 令牌。
 * 与 SysUserService 分开，是因为"认证"和"用户管理"是两个独立的业务关注点，
 * 拆分后职责单一，后续加注册、改密、刷新 token 等也放这里。</p>
 */
public interface AuthService {
    /**
     * 登录认证
     *
     * <p>流程：根据用户名查用户 → 校验账号是否存在/是否禁用 → 校验密码 →
     * 查角色列表 → 生成 JWT token → 组装 LoginVO 返回。</p>
     *
     * @param dto 登录入参（用户名 + 密码）
     * @return 登录成功信息（token、用户名、昵称、角色编码列表）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 注册
     *
     * @param registerDTO 注册入参
     * @return 注册成功信息，写入数据库
     */
    void register(RegisterDTO registerDTO);
}
