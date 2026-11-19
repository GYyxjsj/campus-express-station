package com.campus.express.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理：业务异常/参数校验/兜底。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        log.warn("业务异常 code={} msg={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return R.fail(ErrorCodes.PARAM_INVALID, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraint(ConstraintViolationException e) {
        return R.fail(ErrorCodes.PARAM_INVALID, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(ErrorCodes.PARAM_INVALID, "缺少参数: " + e.getParameterName());
    }

    /** 请求体不可读（JSON 格式错误/编码非法/类型不匹配）：返回 HTTP 400 参数错误，
     *  而不是被兜底成 500/90000（否则会掩盖客户端错误、污染系统异常告警） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败 msg={}", e.getMessage());
        return R.fail(ErrorCodes.PARAM_INVALID, "请求体格式错误，请检查 JSON 内容与编码");
    }

    /** 请求方法不支持：返回 HTTP 405，而不是被兜底成 500/90000（否则会掩盖客户端错误、污染告警） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持 method={} msg={}", e.getMethod(), e.getMessage());
        return R.fail(ErrorCodes.METHOD_NOT_ALLOWED,
                "该接口不支持 " + e.getMethod() + " 请求");
    }

    /** 路径未匹配任何处理器：返回 HTTP 404 + 统一响应体，避免落到容器默认错误页 */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNotFound(NoHandlerFoundException e) {
        log.warn("接口不存在 method={} path={}", e.getHttpMethod(), e.getRequestURL());
        return R.fail(ErrorCodes.API_NOT_FOUND, "接口不存在: " + e.getRequestURL());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return R.fail(ErrorCodes.INTERNAL_ERROR, "系统繁忙，请稍后重试");
    }
}