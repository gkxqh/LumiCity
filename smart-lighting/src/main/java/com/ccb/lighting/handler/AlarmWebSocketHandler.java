package com.ccb.lighting.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 告警 WebSocket 处理器
 *
 * <p>维护所有已连接的客户端会话，提供 broadcast 方法向全部在线客户端广播消息。
 * 当告警产生/处理时，由 AlarmRecordServiceImpl 调用 broadcast 推送实时告警事件。</p>
 *
 * <p>设计要点：
 * - 会话集合用 CopyOnWriteArraySet，保证遍历广播时的线程安全
 * - sendMessage 非线程安全，对单个 session 加锁后再发
 * - alarm-push-enabled 开关：application.yml 中 lighting.alarm-push-enabled=false 时关闭推送
 *   （WebSocket 连接仍可建立，只是不广播业务消息，便于联调/压测时静默）</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    /** 推送开关，对应 application.yml 的 lighting.alarm-push-enabled */
    @Value("${lighting.alarm-push-enabled:true}")
    private boolean pushEnabled;

    /** 在线客户端会话集合（线程安全） */
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        Object username = session.getAttributes().get("username");
        log.info("告警 WebSocket 已连接：{}（用户={}），当前在线={}", session.getId(), username, sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("告警 WebSocket 已断开：{}，当前在线={}", session.getId(), sessions.size());
    }

    /**
     * 广播消息给所有在线客户端
     *
     * @param payload 任意对象，内部用 ObjectMapper 序列化为 JSON
     */
    public void broadcast(Object payload) {
        if (!pushEnabled || sessions.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    // WebSocketSession.sendMessage 不是线程安全，对同一 session 加锁
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("告警 WebSocket 广播失败：{}", e.getMessage());
        }
    }

    /** 当前在线连接数（供调试/统计） */
    public int onlineCount() {
        return sessions.size();
    }

    /**
     * 定向推送给指定用户（通过握手时注入的 username 匹配）
     */
    public void sendToUser(String username, Object payload) {
        if (!pushEnabled || sessions.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    Object sessionUser = session.getAttributes().get("username");
                    if (username.equals(sessionUser)) {
                        synchronized (session) {
                            session.sendMessage(message);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("告警 WS 定向推送失败（目标={}）：{}", username, e.getMessage());
        }
    }

}
