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
 * <p>未携带或无效 token 直接拒绝握手，防止未认证客户端接入。</p>
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
        String token = extractToken(request.getURI());//从 URL 提取 token
        if (!StringUtils.hasText(token) || !jwtUtil.isValid(token)) {
            log.warn("WebSocket 握手拒绝：未携带或无效 token，URI={}", request.getURI());
            return false;
        }
        try {
            attributes.put("userId", jwtUtil.getUserId(token));//解析出userId
            attributes.put("username", jwtUtil.getUsername(token));//从token解析出username
        } catch (Exception e) {
            log.warn("WebSocket 握手解析 token 失败：{}", e.getMessage());
            return false;
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
        String query = uri.getQuery();//获取 URL 中?后面的查询字符串部分
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {//按&拆分，得到所有键值对
            String[] kv = pair.split("=", 2);//将键值对按=分割，最多分割成两份
            if ("token".equals(kv[0]) && kv.length == 2) {//判断是否是token参数
                return kv[1];
            }
        }
        return null;
    }
}
