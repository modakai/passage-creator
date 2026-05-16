package com.sakura.passage_creator.rednote.model.enums;

import lombok.Getter;

/**
 * 小红书爆款笔记任务状态枚举。
 */
@Getter
public enum RednoteStatusEnum {

    PENDING("PENDING", "等待处理"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败");

    /**
     * 持久化状态值。
     */
    private final String value;

    /**
     * 状态展示描述。
     */
    private final String description;

    RednoteStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 根据持久化值查找状态枚举。
     *
     * @param value 持久化状态值
     * @return 匹配的状态枚举，未匹配时返回 null
     */
    public static RednoteStatusEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (RednoteStatusEnum statusEnum : values()) {
            if (statusEnum.getValue().equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
