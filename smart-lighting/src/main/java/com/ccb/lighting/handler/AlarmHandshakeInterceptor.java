package com.ccb.lighting.handler;

import com.ccb.lighting.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * 告警 WebSocket 握手拦截器
 *
 * <p>浏览器原生 WebSocket 无法自定义请求头，因此本项目约定：前端连接时把 JWT token
 * 放在 URL 查询参数 ?token=xxx 里。本拦截器在握手前从 URL 取出 token 并校验，
 * 校验通过则把 userId/username 放入会话属性，供 Handler 使用。</p>
 *
 * <p>学习蓝本策略：未携带或无效 token 也允许连接（保证联调顺畅），仅不设置用户信息；
 * 正式环境应改为校验失败即拒绝握手（beforeHandshake 返回 false）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                   org.springframework.http.server.ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request.getURI());
        if (StringUtils.hasText(token) && jwtUtil.isValid(token)) {
            try {
                attributes.put("userId", jwtUtil.getUserId(token));
                attributes.put("username", jwtUtil.getUsername(token));
            } catch (Exception e) {
                log.warn("WebSocket 握手解析 token 失败：{}", e.getMessage());
            }
        } else {
            log.warn("WebSocket 握手未携带有效 token，仍允许连接（学习蓝本宽松策略）");
        }
        return true;
    }

    @Override
    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request,
                               org.springframework.http.server.ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需后处理
    }

    /** 从 URL 查询串中提取 token 参数 */
    private String extractToken(URI uri) {
        String query = uri.getQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if ("token".equals(kv[0]) && kv.length == 2) {
                return kv[1];
            }
        }
        return null;
    }
}
