package com.sakura.passage_creator.notification.enums;

/**
 * 通知接收端范围枚举。
 *
 * @author Sakura
 */
public enum NotificationReceiverTypeEnum {

    /**
     * 系统后台用户。
     */
    ADMIN("admin"),

    /**
     * 用户端用户。
     */
    APP("app"),

    /**
     * 全部接收端。
     */
    ALL("all");

    /**
     * 存库值。
     */
    private final String value;

    NotificationReceiverTypeEnum(String value) {
        this.value = value;
    }

    /**
     * 判断当前接收端是否命中配置范围。
     *
     * @param receiverType 配置范围
     * @param currentReceiverType 当前访问端
     * @return 是否命中
     */
    public static boolean matches(String receiverType, String currentReceiverType) {
        return ALL.value.equals(receiverType) || receiverType != null && receiverType.equals(currentReceiverType);
    }

    public String getValue() {
        return value;
    }
}
