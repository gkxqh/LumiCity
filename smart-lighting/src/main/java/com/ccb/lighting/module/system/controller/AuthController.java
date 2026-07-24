package com.ccb.lighting.module.system.controller;

import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.system.dto.LoginDTO;
import com.ccb.lighting.module.system.dto.RegisterDTO;
import com.ccb.lighting.module.system.entity.SysUser;
import com.ccb.lighting.module.system.mapper.SysUserMapper;
import com.ccb.lighting.module.system.service.AuthService;
import com.ccb.lighting.module.system.service.SysUserService;
import com.ccb.lighting.module.system.vo.LoginVO;
import com.ccb.lighting.module.system.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 Controller
 *
 * <p>注解说明：
 * - @RestController = @Controller + @ResponseBody，所有方法返回值直接写进响应体（转 JSON）
 * - @RequestMapping("/auth")：类级路径，类内所有接口都以 /auth 开头
 * - @RequiredArgsConstructor：构造器注入 AuthService、SysUserService</p>
 *
 * <p>提供的接口：
 * - POST /auth/login  登录（无需 token，在 WebMvcConfig 里排除）
 * - POST /auth/logout 登出（学习蓝本只做简单响应，token 无状态由前端丢弃即可）
 * - GET  /auth/info   获取当前登录用户信息（需带 token，由 JwtInterceptor 校验）</p>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    /** 认证 Service：处理登录逻辑 */
    private final AuthService authService;

    /** 用户 Service：/auth/info 接口用它查用户详情 */
    private final SysUserService sysUserService;

    /** 用户 Mapper：/auth/info 聚合角色与权限标识 */
    private final SysUserMapper sysUserMapper;

    /**
     * 登录接口
     *
     * <p>@Valid：触发 LoginDTO 上的 @NotBlank 校验，失败时抛 MethodArgumentNotValidException，
     * 由全局异常处理器统一处理。
     * @RequestBody：把请求体 JSON 反序列化成 LoginDTO 对象。</p>
     *
     * @param dto 登录入参（username、password）
     * @return Result<LoginVO>，data 含 token、用户名、昵称、角色
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = authService.login(dto);
        return Result.success(vo);
    }
    /**
     * 登出接口
     *
     * <p>JWT 是无状态令牌，服务端不存储，登出通常由前端丢弃 token 实现。
     * 这里返回成功即可；如需强制失效，可引入 Redis 维护 token 黑名单。</p>
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    /**
     * 注册接口
     *
     * @param registerDTO 注册入参
     * @return Result<void>
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid RegisterDTO registerDTO){
        authService.register(registerDTO);
        return Result.success();
    }

    /**
     * 获取当前登录用户信息
     *
     * <p>请求到达此方法前，JwtInterceptor 已校验过 token，并把 userId、username
     * 存进 request 的 attribute 中（值为 String）。这里取出来查库返回用户信息。</p>
     *
     * @param request 请求对象，用于取拦截器放入的 userId
     * @return Result<SysUser>，密码字段会清空避免泄露
     */
    @GetMapping("/info")
    public Result<UserInfoVO> info(HttpServletRequest request) {
        // 拦截器存的 userId 是 String，这里转回 Long
        Object userIdAttr = request.getAttribute("userId");
        Long userId = Long.parseLong(userIdAttr.toString());

        // 查用户详情
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return Result.error(com.ccb.lighting.common.ResultCode.USER_NOT_FOUND);
        }

        // 组装返回信息（不返回密码等敏感字段），并带上角色与权限，供前端做权限控制
        UserInfoVO vo = new UserInfoVO();
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRoles(sysUserMapper.selectUserRoleList(userId).stream()
                .map(r -> r.getRoleCode()).collect(java.util.stream.Collectors.toList()));
        vo.setPerms(sysUserMapper.selectUserPerms(userId));
        return Result.success(vo);
    }
}
