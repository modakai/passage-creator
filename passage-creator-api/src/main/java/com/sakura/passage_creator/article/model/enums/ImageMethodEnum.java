package com.sakura.passage_creator.article.model.enums;

import lombok.Getter;

/**
 * 文章配图方式枚举。第一阶段只开放 GPT_IMAGE，其他方式留到策略化阶段再接入。
 */
@Getter
public enum ImageMethodEnum {

    /**
     * OpenAI GPT Image 2 文生图。
     */
    GPT_IMAGE("GPT_IMAGE", "GPT Image 2 配图");

    /**
     * 配图方式值，持久化和前后端传输均使用该值。
     */
    private final String value;

    /**
     * 配图方式说明。
     */
    private final String description;

    ImageMethodEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
