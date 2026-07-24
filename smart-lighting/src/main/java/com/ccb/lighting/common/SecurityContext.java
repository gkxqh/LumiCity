package com.ccb.lighting.common;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 当前登录用户上下文（基于 ThreadLocal）
 *
 * <p>由 JwtInterceptor 在请求进来时写入，请求结束时清除（afterCompletion）。
 * Service / Controller 层可随时取当前用户 ID、角色、权限，避免层层传参。
 * 例如 MetaObjectHandler 可用 SecurityContext.getUserId() 填充 create_by / update_by。</p>
 */
public class SecurityContext {

    private static final ThreadLocal<SecurityInfo> HOLDER = new ThreadLocal<>();

    public static void set(SecurityInfo info) {
        HOLDER.set(info);
    }

    public static SecurityInfo get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static Long getUserId() {
        SecurityInfo info = HOLDER.get();
        return info == null ? null : info.getUserId();
    }

    public static List<String> getRoles() {
        SecurityInfo info = HOLDER.get();
        return info == null || info.getRoles() == null ? Collections.emptyList() : info.getRoles();
    }

    public static List<String> getPerms() {
        SecurityInfo info = HOLDER.get();
        return info == null || info.getPerms() == null ? Collections.emptyList() : info.getPerms();
    }

    /** 请求级安全信息载体 */
    @Data
    public static class SecurityInfo {
        private Long userId;
        private String username;
        private List<String> roles;
        private List<String> perms;
    }
}
