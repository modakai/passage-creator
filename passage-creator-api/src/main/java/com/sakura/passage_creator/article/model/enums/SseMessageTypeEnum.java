package com.sakura.passage_creator.article.model.enums;

import lombok.Getter;

/**
 * 文章生成 SSE 消息类型枚举。
 *
 * @author sakura
 * @create 2026-04
 */
@Getter
public enum SseMessageTypeEnum {

    /**
     * 标题方案已生成，等待用户选择标题。
     */
    TITLES_GENERATED("TITLES_GENERATED", "标题方案已生成"),

    /**
     * 大纲流式输出。
     */
    OUTLINE_STREAMING("OUTLINE_STREAMING", "大纲流式输出"),

    /**
     * 大纲已生成，等待用户确认。
     */
    OUTLINE_GENERATED("OUTLINE_GENERATED", "大纲已生成"),

    /**
     * 正文流式输出。
     */
    CONTENT_STREAMING("CONTENT_STREAMING", "正文流式输出"),

    /**
     * 全部完成。
     */
    ALL_COMPLETE("ALL_COMPLETE", "全部完成"),

    /**
     * 生成失败。
     */
    ERROR("ERROR", "错误");

    /**
     * 消息类型值。
     */
    private final String value;

    /**
     * 消息类型描述。
     */
    private final String description;

    SseMessageTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
