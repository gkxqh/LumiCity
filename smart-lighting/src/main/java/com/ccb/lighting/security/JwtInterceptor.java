package com.ccb.lighting.security;

import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.common.SecurityContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * JWT 拦截器
 *
 * <p>职责：
 * 1. 校验 token（未带 / 过期 → 401/401）
 * 2. 解析 userId / username / roles / perms，写入 SecurityContext 供后续使用
 * 3. 接口级权限校验：若目标方法标注了 @RequiresPerms，则校验当前用户是否拥有所需权限，
 *    没有则抛 FORBIDDEN(403)（由全局异常处理器转成统一 Result 返回前端）
 * 4. 请求结束后清除 SecurityContext，避免线程复用串号</p>
 *
 * <p>在 WebMvcConfig 注册，排除登录/文档/WebSocket 握手等路径。</p>
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 取 token
        String token = request.getHeader(jwtProperties.getHeader());
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (token.startsWith(jwtProperties.getTokenPrefix())) {
            token = token.substring(jwtProperties.getTokenPrefix().length());
        }
        if (!jwtUtil.isValid(token)) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }

        // 2. 解析并写入 SecurityContext
        Claims claims = jwtUtil.parseToken(token);
        List<String> roles = castList(claims.get("roles"));
        List<String> perms = castList(claims.get("perms"));
        SecurityContext.SecurityInfo info = new SecurityContext.SecurityInfo();
        info.setUserId(Long.parseLong(claims.getSubject()));
        info.setUsername(claims.get("username", String.class));
        info.setRoles(roles);
        info.setPerms(perms);
        SecurityContext.set(info);

        request.setAttribute("userId", claims.getSubject());
        request.setAttribute("username", claims.get("username"));

        // 3. 接口级权限校验（仅对 Controller 方法生效）
        if (handler instanceof HandlerMethod hm) {
            RequiresPerms anno = hm.getMethodAnnotation(RequiresPerms.class);
            if (anno != null) {
                // ADMIN 视为超级用户，跳过权限校验（典型 RBAC 设计）
                boolean isAdmin = roles != null && roles.contains("ADMIN");
                boolean allowed = isAdmin
                        || Arrays.stream(anno.value()).anyMatch(perms::contains);
                if (!allowed) {
                    throw new BusinessException(ResultCode.FORBIDDEN);
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束清空，防止 Tomcat 线程池复用导致上下文串号
        SecurityContext.clear();
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object o) {
        if (o == null) {
            return java.util.Collections.emptyList();
        }
        return (List<String>) o;
    }
}
