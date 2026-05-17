package com.sakura.passage_creator.rednote.model.vo;

import com.sakura.passage_creator.rednote.model.entity.RednoteNote;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小红书爆款笔记展示对象。
 */
@Data
@AutoMapper(target = RednoteNote.class)
public class RednoteNoteVO implements Serializable {

    /**
     * 小红书笔记 id。
     */
    private Long id;

    /**
     * 对外任务 ID。
     */
    private String taskId;

    /**
     * 创建用户 id。
     */
    private Long userId;

    /**
     * 用户原始自然语言创作需求。
     */
    private String content;

    /**
     * SearchAgent 解析出的核心主体、产品或场景。
     */
    private String subject;

    /**
     * SearchAgent 整理后的创作上下文。
     */
    private String context;

    /**
     * 篇幅档位：SHORT/MEDIUM/LONG。
     */
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
     * 小红书正文主体。
     */
    private String bodyContent;

    /**
     * 标签列表 JSON 文本。
     */
    private String tags;

    /**
     * 封面标题，由后续图片提示词节点生成。
     */
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
    private String coverImage;

    /**
     * 任务状态。
     */
    private String status;

    /**
     * 任务状态展示名称。
     */
    private String statusLabel;

    /**
     * 当前生成阶段。
     */
    private String phase;

    /**
     * 当前生成阶段展示名称。
     */
    private String phaseLabel;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 完成时间。
     */
    private LocalDateTime completedTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
