package com.sakura.passage_creator.audit.enums;

/**
 * 审计日志类型枚举。
 *
 * @author Sakura
 */
public enum AuditLogTypeEnum {

    /**
     * 登录日志。
     */
    LOGIN("login"),

    /**
     * 管理员操作日志。
     */
    ADMIN_OPERATION("admin_operation");

    /**
     * 存储值。
     */
    private final String value;

    AuditLogTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
