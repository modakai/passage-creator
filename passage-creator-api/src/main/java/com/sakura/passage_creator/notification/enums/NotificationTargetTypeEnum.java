package com.sakura.passage_creator.notification.enums;

/**
 * 通知目标范围枚举。
 *
 * @author Sakura
 */
public enum NotificationTargetTypeEnum {

    /**
     * 接收端范围内全部用户。
     */
    ALL("all"),

    /**
     * 指定角色。
     */
    ROLE("role"),

    /**
     * 指定用户。
     */
    USER("user");

    /**
     * 存库值。
     */
    private final String value;

    NotificationTargetTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
