package com.sakura.passage_creator.observability.enums;

/**
 * 运维事件级别。
 *
 * @author Sakura
 */
public enum ObservabilityEventLevelEnum {

    /**
     * 普通信息。
     */
    INFO("info"),

    /**
     * 需要管理员关注。
     */
    WARNING("warning"),

    /**
     * 已经出现错误或安全风险。
     */
    ERROR("error");

    /**
     * 存库值。
     */
    private final String value;

    ObservabilityEventLevelEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
