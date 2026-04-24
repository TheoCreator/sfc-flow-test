package com.sfc.flowtest.common.enums;

/**
 * 统一业务错误码定义。
 */
public enum ErrorCode {
    SUCCESS("0", "success"),
    PARAM_INVALID("40001", "参数校验失败"),
    MANUSCRIPT_NOT_FOUND("40002", "稿件不存在"),
    ILLEGAL_STATE("40003", "当前状态不允许该操作"),
    REJECT_OPINION_REQUIRED("40004", "退回意见不能为空"),
    REJECT_LEVEL_INVALID("40005", "退回关卡数据异常"),
    ASSET_NOT_FOUND("40006", "素材不存在"),
    ASSET_INVALID_TYPE("40007", "不支持的素材类型"),
    ASSET_TOO_LARGE("40008", "素材文件过大"),
    INTERNAL_ERROR("50000", "系统内部错误");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
