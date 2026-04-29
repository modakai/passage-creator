package com.sakura.passage_creator.audit.enums;

/**
 * 审计日志结果枚举。
 *
 * @author Sakura
 */
public enum AuditLogResultEnum {

    /**
     * 执行成功。
     */
    SUCCESS("success"),

    /**
     * 执行失败。
     */
    FAILURE("failure");

    /**
     * 存储值。
     */
    private final String value;

    AuditLogResultEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
