package com.sakura.passage_creator.shared.common;

/**
 * 错误码定义。
 * <p>
 * 作者：Sakura
 */
public enum ErrorCode {

    SUCCESS(0, "success.ok", "ok"),
    PARAMS_ERROR(40000, "error.params", "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "error.not_login", "未登录"),
    NO_AUTH_ERROR(40101, "error.no_auth", "无权限"),
    NOT_FOUND_ERROR(40400, "error.not_found", "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "error.forbidden", "禁止访问"),
    SYSTEM_ERROR(50000, "error.system", "系统内部异常"),
    OPERATION_ERROR(50001, "error.operation", "操作失败"),
    AGENT_ERROR(50002, "error.agent", "Agent执行异常");

    /**
     * 业务码。
     */
    private final int code;

    /**
     * 错误信息。
     */
    private final String messageKey;

    /**
     * 默认消息，用于消息 key 缺失时兜底。
     */
    private final String defaultMessage;

    ErrorCode(int code, String messageKey, String defaultMessage) {
        this.code = code;
        this.messageKey = messageKey;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
