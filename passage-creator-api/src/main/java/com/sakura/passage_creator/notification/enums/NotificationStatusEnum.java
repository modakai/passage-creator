package com.sakura.passage_creator.notification.enums;

/**
 * 通知发布状态枚举。
 *
 * @author Sakura
 */
public enum NotificationStatusEnum {

    /**
     * 草稿。
     */
    DRAFT("draft"),

    /**
     * 已发布。
     */
    PUBLISHED("published"),

    /**
     * 已撤回。
     */
    REVOKED("revoked"),

    /**
     * 已归档。
     */
    ARCHIVED("archived");

    /**
     * 存库值。
     */
    private final String value;

    NotificationStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
