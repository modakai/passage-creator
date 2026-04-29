package com.sakura.passage_creator.notification.enums;

/**
 * 通知内容类型枚举。
 *
 * @author Sakura
 */
public enum NotificationTypeEnum {

    /**
     * 普通消息通知。
     */
    MESSAGE("message"),

    /**
     * 系统公告。
     */
    ANNOUNCEMENT("announcement");

    /**
     * 存库值。
     */
    private final String value;

    NotificationTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
