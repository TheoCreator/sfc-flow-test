package com.sfc.flowtest.common.response;

import com.sfc.flowtest.common.enums.ErrorCode;

/**
 * 统一接口响应模型。
 *
 * @param <T> 数据载荷类型
 */
public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 构造成功响应。
     *
     * @param data 响应数据
     * @return 成功响应
     * @param <T> 数据类型
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getDefaultMessage(), data);
    }

    /**
     * 构造失败响应。
     *
     * @param errorCode 错误码
     * @param message   错误消息
     * @return 失败响应
     * @param <T> 数据类型
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
