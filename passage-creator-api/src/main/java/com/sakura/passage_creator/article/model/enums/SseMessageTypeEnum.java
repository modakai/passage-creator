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
     * 当前文章任务快照，SSE 连接建立后用于恢复页面状态。
     */
    PROGRESS("PROGRESS", "文章任务进度"),

    /**
     * 当前生成阶段已变更。
     */
    PHASE_CHANGED("PHASE_CHANGED", "生成阶段已变更"),

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
     * 配图需求已分析完成。
     */
    IMAGE_ANALYZED("IMAGE_ANALYZED", "配图需求已分析完成"),

    /**
     * 单张配图已生成。
     */
    IMAGE_COMPLETE("IMAGE_COMPLETE", "单张配图已生成"),

    /**
     * 所有配图已生成。
     */
    IMAGE_GENERATED("IMAGE_GENERATED", "所有配图已生成"),

    /**
     * 图文合成已完成。
     */
    MERGE_COMPLETE("MERGE_COMPLETE", "图文合成已完成"),

    /**
     * 全部完成。
     */
    ALL_COMPLETE("ALL_COMPLETE", "全部完成"),

    /**
     * 生成失败。
     */
    ERROR("ERROR", "错误"),

    /**
     * Workflow 等待过久，checkpoint 已过期。
     */
    WORKFLOW_EXPIRED("WORKFLOW_EXPIRED", "Workflow 已过期");

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
