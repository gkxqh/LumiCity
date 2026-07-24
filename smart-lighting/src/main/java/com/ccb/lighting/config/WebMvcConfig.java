package com.ccb.lighting.config;

import com.ccb.lighting.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 1. 注册 JWT 拦截器，排除登录/接口文档等路径
 * 2. 配置跨域（前后端分离必须）
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",         // 登录接口免 token
                        "/auth/logout",        // 登出接口免 token
                        "/auth/register",      // 注册接口免 token
                        "/ws/**",              // WebSocket 握手端点（鉴权交由 AlarmHandshakeInterceptor）
                        "/swagger-ui.html",    // Swagger UI 入口（重定向页）
                        "/swagger-ui/**",      // Swagger UI 静态资源
                        "/v3/api-docs/**",     // API 文档 JSON
                        "/error"               // 错误页
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
