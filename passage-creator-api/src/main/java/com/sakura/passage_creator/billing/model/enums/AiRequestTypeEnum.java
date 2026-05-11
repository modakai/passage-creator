package com.sakura.passage_creator.billing.model.enums;

import lombok.Getter;

/**
 * AI 请求类型，用于区分文本 Token 计费和图片固定计费。
 */
@Getter
public enum AiRequestTypeEnum {

    TEXT("文本模型", "TEXT"),
    IMAGE("图片模型", "IMAGE");

    private final String text;

    private final String value;

    AiRequestTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }
}
