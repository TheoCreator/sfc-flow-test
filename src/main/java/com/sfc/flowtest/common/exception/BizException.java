package com.sfc.flowtest.common.exception;

import com.sfc.flowtest.common.enums.ErrorCode;

/**
 * 业务异常，携带统一错误码供全局异常处理器转换响应。
 */
public class BizException extends RuntimeException {
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
