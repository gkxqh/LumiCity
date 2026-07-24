package com.ccb.lighting.config;

import com.ccb.lighting.handler.AlarmHandshakeInterceptor;
import com.ccb.lighting.handler.AlarmWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 *
 * <p>注册告警实时推送端点 /ws/alarm：
 * - 前端连接 ws://host/api/ws/alarm?token=xxx（context-path /api，故实际带 /api 前缀）
 * - 握手时由 AlarmHandshakeInterceptor 校验 token 并注入用户信息
 * - 业务消息由 AlarmWebSocketHandler 广播</p>
 *
 * <p>说明：由于 server.servlet.context-path=/api，客户端连接路径为 /api/ws/alarm；
 * WebMvcConfig 已将 /ws/** 排除出 JwtInterceptor，握手鉴权交由本拦截器处理。</p>
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AlarmWebSocketHandler alarmWebSocketHandler;
    private final AlarmHandshakeInterceptor alarmHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(alarmWebSocketHandler, "/ws/alarm")
                .addInterceptors(alarmHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
