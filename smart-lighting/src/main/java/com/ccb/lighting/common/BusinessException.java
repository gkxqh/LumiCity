package com.ccb.lighting.common;

import lombok.Getter;

/**
 * 自定义业务异常
 * 业务逻辑出错时抛出，被全局异常处理器捕获转成 Result 返回前端
 * 用法：throw db_tool.py BusinessException("设备编号已存在");
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
