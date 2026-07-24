package com.ccb.lighting.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具类
 * 生成 token、解析 token、校验 token
 * 登录成功后生成 token 返回前端，后续请求带 token 验身份
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 token（携带角色与权限，供接口级鉴权使用）
     *
     * @param userId   用户 ID（subject）
     * @param username 用户名
     * @param roles    角色编码列表（如 ["admin"]）
     * @param perms    权限标识列表（如 ["system:user:list"]），从用户角色→菜单聚合而来
     */
    public String createToken(Long userId, String username, List<String> roles, List<String> perms) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + jwtProperties.getExpire() * 1000);
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expire)
                .signWith(getKey());
        // 角色/权限写入 claim，拦截器据此做接口级鉴权（避免每次请求查库）
        if (roles != null) {
            builder.claim("roles", roles);
        }
        if (perms != null) {
            builder.claim("perms", perms);
        }
        return builder.compact();
    }

    /** 解析 token，返回 Claims（里面含 userId、username 等） */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 从 token 取 userId */
    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    /** 从 token 取 username */
    public String getUsername(String token) {
        return parseToken(token).get("username", String.class);
    }

    /** 校验 token 是否有效（未过期且签名正确） */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
