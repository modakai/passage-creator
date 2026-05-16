package com.sakura.passage_creator.rednote.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 小红书爆款笔记更新请求，主要供后续节点落库和管理端修正使用。
 */
@Data
public class RednoteUpdateRequest implements Serializable {

    /**
     * 小红书笔记 id。
     */
    @NotNull(message = "小红书笔记 id 不能为空")
    private Long id;

    /**
     * 用户原始自然语言创作需求。
     */
    private String content;

    /**
     * SearchAgent 解析出的核心主体、产品或场景。
     */
    @Size(max = 300, message = "主体不能超过 300 个字符")
    private String subject;

    /**
     * SearchAgent 整理后的创作上下文。
     */
    private String context;

    /**
     * 篇幅档位：SHORT/MEDIUM/LONG。
     */
    @Size(max = 32, message = "篇幅档位不能超过 32 个字符")
    private String contentLength;

    /**
     * 目标字数。
     */
    private Integer targetWordCount;

    /**
     * 关键词列表 JSON 文本。
     */
    private String keywords;

    /**
     * 标签数量。
     */
    private Integer tagCount;

    /**
     * 普通配图数量，最多 5，不含封面。
     */
    private Integer imageCount;

    /**
     * 搜索结果摘要 JSON 文本。
     */
    private String searchResults;

    /**
     * 开头钩子文案。
     */
    @Size(max = 1000, message = "开头钩子不能超过 1000 个字符")
    private String hookText;

    /**
     * 小红书正文主体。
     */
    private String bodyContent;

    /**
     * 行动引导文案。
     */
    @Size(max = 1000, message = "行动引导不能超过 1000 个字符")
    private String callToAction;

    /**
     * 标签列表 JSON 文本。
     */
    private String tags;

    /**
     * 封面标题。
     */
    @Size(max = 200, message = "封面标题不能超过 200 个字符")
    private String coverTitle;

    /**
     * 封面图片提示词。
     */
    private String coverPrompt;

    /**
     * 普通配图提示词计划 JSON 文本。
     */
    private String imagePrompts;

    /**
     * 配图结果列表 JSON 文本，包含 URL、位置、状态和失败原因。
     */
    private String images;

    /**
     * 封面图 URL。
     */
    @Size(max = 512, message = "封面图 URL 不能超过 512 个字符")
    private String coverImage;

    /**
     * 任务状态。
     */
    private String status;

    /**
     * 当前生成阶段。
     */
    private String phase;

    /**
     * 失败错误信息。
     */
    private String errorMessage;

    private static final long serialVersionUID = 1L;
}
