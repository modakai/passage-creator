package com.sakura.passage_creator.rednote.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 小红书爆款笔记 workflow 阶段枚举。
 */
@Getter
public enum RednotePhaseEnum {

    PENDING("PENDING", "等待处理"),
    SEARCH_AGENT("SEARCH_AGENT", "搜索整理中"),
    COPY_GENERATING("COPY_GENERATING", "生成文案中"),
    IMAGE_PROMPT_GENERATING("IMAGE_PROMPT_GENERATING", "生成图片提示词中"),
    IMAGE_GENERATING("IMAGE_GENERATING", "生成配图中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败");

    /**
     * 持久化阶段值。
     */
    private final String value;

    /**
     * 阶段展示描述。
     */
    private final String description;

    RednotePhaseEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 获取自动 workflow 的下一个阶段。
     *
     * @return 下一个阶段的持久化值
     */
    public String next() {
        RednotePhaseEnum phaseEnum = switch (this) {
            case PENDING -> SEARCH_AGENT;
            case SEARCH_AGENT -> COPY_GENERATING;
            case COPY_GENERATING -> IMAGE_PROMPT_GENERATING;
            case IMAGE_PROMPT_GENERATING -> IMAGE_GENERATING;
            case IMAGE_GENERATING -> COMPLETED;
            case COMPLETED -> COMPLETED;
            default -> FAILED;
        };
        return phaseEnum.getValue();
    }

    /**
     * 根据持久化值查找阶段枚举。
     *
     * @param value 持久化阶段值
     * @return 匹配的阶段枚举，未匹配时返回 null
     */
    public static RednotePhaseEnum getByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (RednotePhaseEnum phaseEnum : values()) {
            if (phaseEnum.getValue().equals(value)) {
                return phaseEnum;
            }
        }
        return null;
    }
}
