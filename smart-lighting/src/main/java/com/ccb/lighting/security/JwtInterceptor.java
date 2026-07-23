package com.ccb.lighting.security;

import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.ResultCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器
 * 拦截请求校验 token，校验通过放行并把 userId 放进 request
 * 在 WebMvcConfig 注册，排除登录/文档等接口
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
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
        Claims claims = jwtUtil.parseToken(token);
        request.setAttribute("userId", claims.getSubject());
        request.setAttribute("username", claims.get("username"));
        return true;
    }
}
