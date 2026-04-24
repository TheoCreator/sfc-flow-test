package com.sfc.flowtest.common.exception;

import com.sfc.flowtest.common.enums.ErrorCode;
import com.sfc.flowtest.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException ex) {
        log.warn("code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        return ApiResponse.failure(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public ApiResponse<Void> handleValidation(Exception ex) {
        String message = resolveValidationMessage(ex);
        log.warn("code={}, message={}", ErrorCode.PARAM_INVALID.getCode(), message);
        return ApiResponse.failure(ErrorCode.PARAM_INVALID, message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleSystem(Exception ex) {
        log.error("code={}, message={}", ErrorCode.INTERNAL_ERROR.getCode(), ex.getMessage(), ex);
        return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage());
    }

    private String resolveValidationMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException validEx) {
            return validEx.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        if (ex instanceof BindException bindEx) {
            return bindEx.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        if (ex instanceof ConstraintViolationException constraintEx) {
            return constraintEx.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .collect(Collectors.joining("; "));
        }
        return ErrorCode.PARAM_INVALID.getDefaultMessage();
    }
}
