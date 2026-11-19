package com.campus.express.common;

/**
 * 业务异常：Service 层抛出，由全局异常处理器转为统一响应。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}