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
 * /ws/alarm 是一个 WebSocket 端点路径，不是普通的 HTTP 接口
 * 客户端连接实际完整路径是ws://host:port/api/ws/alarm?token=xxx
 * 与Http的请求一次就结束不同，websocket建立连接后保持长连接使得服务端能随时主动推消息给客户端
 * 握手时由 AlarmHandshakeInterceptor校验 token 并注入用户信息
 * 业务消息由 AlarmWebSocketHandler广播</p>
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

    @Override//在registerWebSocketHandlers方法中把告警WebSocket的处理器和拦截器注册到/ws/alarm这个路径上
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(alarmWebSocketHandler, "/ws/alarm")
                .addInterceptors(alarmHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
