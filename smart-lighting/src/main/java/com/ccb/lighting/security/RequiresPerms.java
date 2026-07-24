package com.ccb.lighting.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级权限注解
 *
 * <p>标注在 Controller 方法上，声明该接口需要的权限标识（对应 sys_menu.perms）。
 * JwtInterceptor 在请求到达时校验当前用户是否拥有其中至少一个权限（OR 语义），
 * 没有则抛出 FORBIDDEN(403)。</p>
 *
 * <p>用法示例：
 * <pre>
 *   &#64;RequiresPerms("system:role:list")
 *   public Result&lt;?&gt; list() { ... }
 *
 *   // 满足任意一个即可
 *   &#64;RequiresPerms({"system:user:add", "system:user:edit"})
 *   public Result&lt;?&gt; save() { ... }
 * </pre>
 * 不标注该注解的接口，任何已登录用户均可访问（权限校验为“按需开启”）。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPerms {
    /** 需要的权限标识列表，满足任意一个即通过 */
    String[] value();
}
