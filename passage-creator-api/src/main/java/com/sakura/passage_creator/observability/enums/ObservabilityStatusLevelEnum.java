package com.sakura.passage_creator.observability.enums;

/**
 * 运维状态等级。
 *
 * @author Sakura
 */
public enum ObservabilityStatusLevelEnum {

    /**
     * 正常。
     */
    UP("up"),

    /**
     * 降级或接近阈值。
     */
    DEGRADED("degraded"),

    /**
     * 不可用或超过严重阈值。
     */
    DOWN("down"),

    /**
     * 未知状态。
     */
    UNKNOWN("unknown");

    /**
     * 前端展示值。
     */
    private final String value;

    ObservabilityStatusLevelEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 根据使用率和阈值计算状态等级。
     *
     * @param usagePercent 使用率百分比
     * @param warningPercent 警告阈值
     * @param criticalPercent 严重阈值
     * @return 状态等级
     */
    public static ObservabilityStatusLevelEnum fromUsage(double usagePercent, double warningPercent,
            double criticalPercent) {
        if (usagePercent >= criticalPercent) {
            return DOWN;
        }
        if (usagePercent >= warningPercent) {
            return DEGRADED;
        }
        return UP;
    }
}
