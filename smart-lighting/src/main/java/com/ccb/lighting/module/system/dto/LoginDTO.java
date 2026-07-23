package com.ccb.lighting.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求 DTO（Data Transfer Object）
 *
 * <p>作用：接收前端登录请求的参数，与数据库实体解耦。
 * 前端 POST /auth/login 时传 JSON：{"username":"admin","password":"123456"}
 * Controller 用 @Valid @RequestBody LoginDTO 接收，校验通过后才进入 Service。</p>
 *
 * <p>校验说明：
 * - @NotBlank：字符串不能为 null、空串、纯空白，否则触发参数校验异常
 * - message 用中文，校验失败时由全局异常处理器返回给前端</p>
 *
 * <p>为什么单独建 DTO 而不直接用 SysUser？
 * 1. 登录只需要用户名+密码两个字段，用实体类会带一堆无关字段，暴露过多信息
 * 2. 实体类对应数据库表结构，DTO 对应接口入参，职责分离更清晰
 * 3. 校验规则可能不同（如登录不校验手机号格式，新增用户却要校验）</p>
 */
@Data
public class LoginDTO implements Serializable {

    /** 用户名：登录账号，不能为空 */
    @NotBlank(message = "用户名不能为空")
    private String username;
    /** 密码：明文传输*/
    @NotBlank(message = "密码不能为空")
    private String password;
}
