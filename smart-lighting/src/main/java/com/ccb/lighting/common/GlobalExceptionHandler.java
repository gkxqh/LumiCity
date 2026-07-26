package com.ccb.lighting.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 所有 Controller 抛出的异常在这里统一捕获，转成友好的 Result 返回前端
 * 好处：Controller/Service 里只管 throw，不用每个方法 try-catch
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 —— 最常见，Service 主动抛出的 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数校验异常 —— @Valid 校验失败时自动抛出 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /** 表单绑定异常 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /** 约束违反异常 —— @Validated 校验 query 参数失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("; "));
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /** 上传文件大小超限 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        long maxSize = e.getMaxUploadSize();
        String sizeStr = maxSize > 0 ? (maxSize / (1024 * 1024) + "MB") : "未知";
        log.warn("上传文件大小超过限制（上限={}）", sizeStr);
        return Result.error("文件大小超过上限（" + sizeStr + "），请压缩后重试");
    }

    /** 客户端提前断开连接（预览视频时关闭页面/弹窗）—— 静默忽略，无需返回 */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(ClientAbortException e) {
        log.debug("客户端提前断开连接: {}", e.getMessage());
    }

    /** 兜底：所有未捕获的异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error("系统繁忙，请稍后重试");
    }
}
