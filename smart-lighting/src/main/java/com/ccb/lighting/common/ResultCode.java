package com.ccb.lighting.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 状态码枚举
 * 统一管理所有返回状态码，避免代码里到处写魔法数字
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),

    UNAUTHORIZED(401, "未登录或token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),

    PARAM_ERROR(400, "参数错误"),
    PARAM_NULL(40001, "必填参数为空"),

    USER_NOT_FOUND(10001, "用户不存在"),
    USER_PASSWORD_ERROR(10002, "用户名或密码错误"),
    USER_ACCOUNT_DISABLED(10003, "账号已禁用"),
    USER_ALREADY_EXISTS(10004, "用户已存在"),

    TOKEN_INVALID(20001, "token无效"),
    TOKEN_EXPIRED(20002, "token已过期"),

    DATA_NOT_FOUND(30001, "数据不存在"),
    DATA_ALREADY_EXISTS(30002, "数据已存在"),
    DATA_IN_USE(30003, "数据已被引用无法删除"),

    DEVICE_OFFLINE(40001, "设备离线"),
    DEVICE_NOT_FOUND(40002, "设备不存在");

    private final Integer code;
    private final String message;
}
