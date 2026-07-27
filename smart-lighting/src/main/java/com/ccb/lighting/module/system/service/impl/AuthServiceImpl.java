package com.ccb.lighting.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.system.dto.LoginDTO;
import com.ccb.lighting.module.system.dto.RegisterDTO;
import com.ccb.lighting.module.system.entity.SysRole;
import com.ccb.lighting.module.system.entity.SysUser;
import com.ccb.lighting.module.system.mapper.SysUserMapper;
import com.ccb.lighting.module.system.service.AuthService;
import com.ccb.lighting.module.system.vo.LoginVO;
import com.ccb.lighting.security.JwtProperties;
import com.ccb.lighting.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证 Service 实现类
 *
 * <p>同时注入 SysUserMapper（查用户/查角色）和 JwtUtil（生成 token）。
 * 这就是构造器注入多依赖的写法：声明两个 final 字段，@RequiredArgsConstructor 一次性搞定。</p>
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 用户 Mapper：查用户信息、查用户角色 */
    private final SysUserMapper sysUserMapper;

    /** JWT 工具：登录成功后生成 token */
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    /**
     * 登录认证核心逻辑
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 根据用户名查用户
        SysUser user = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
        );

        // 2. 用户不存在：抛业务异常（学习蓝本简化，提示"用户名或密码错误"更安全，避免泄露用户是否存在）
        if (user == null) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 3. 账号被禁用：状态 0 为禁用，禁止登录
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_ACCOUNT_DISABLED);
        }

        // 4. 校验密码：用 BCrypt matches 比对明文与库中哈希
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if(!encoder.matches(dto.getPassword(), user.getPassword())){
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 5. 查询用户角色列表，提取角色编码（如 admin、operator）放入 LoginVO
        List<SysRole> roleList = sysUserMapper.selectUserRoleList(user.getId());
        List<String> roles = roleList.stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());

        // 6. 聚合权限标识：用户 → 角色 → 菜单，取去重后的 perms（对应按钮/接口权限）
        List<String> perms = sysUserMapper.selectUserPerms(user.getId());

        // 7. 生成 JWT token：把 userId、username、roles、perms 一起写入，供接口级鉴权
        long expireSeconds;
        if (dto.getRememberMe() != null && dto.getRememberMe()) {
            expireSeconds = 7 * 24 * 3600;  // 7天
        } else {
            expireSeconds = jwtProperties.getExpire();  // 用配置文件默认值（2小时）
        }
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), roles, perms, expireSeconds);

        // 8. 组装 LoginVO 返回（不返回密码等敏感字段）
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRoles(roles);
        vo.setPerms(perms);
        vo.setExpireSeconds(expireSeconds);
        return vo;
    }

    @Override
    public void register(RegisterDTO registerDTO){
        //1.校验两次密码是否一致
        if(!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())){
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        //2.检查用户名是否存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername,registerDTO.getUsername());
        if(sysUserMapper.selectOne(wrapper) != null){
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        //3.对数据库写入用户对象并保存
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(registerDTO.getPassword()));
        user.setNickname(registerDTO.getNickname());
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());
        user.setStatus(1);

        sysUserMapper.insert(user);
    }
}
