package com.sakura.passage_creator.observability.enums;

/**
 * 运维事件类型。
 *
 * @author Sakura
 */
public enum ObservabilityEventTypeEnum {

    /**
     * 慢接口。
     */
    SLOW_API("slow_api"),

    /**
     * 接口错误。
     */
    API_ERROR("api_error"),

    /**
     * 登录失败。
     */
    LOGIN_FAILURE("login_failure"),

    /**
     * 异常 IP。
     */
    ABNORMAL_IP("abnormal_ip"),

    /**
     * 强制下线。
     */
    FORCE_LOGOUT("force_logout"),

    /**
     * 安全告警。
     */
    SECURITY_ALERT("security_alert");

    /**
     * 存库值。
     */
    private final String value;

    ObservabilityEventTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
