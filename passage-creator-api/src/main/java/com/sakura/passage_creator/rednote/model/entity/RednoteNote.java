package com.sakura.passage_creator.rednote.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小红书爆款笔记实体，承载 rednote workflow 的输入、阶段产物和最终结果。
 */
@Data
@Table("rednote_note")
public class RednoteNote implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 对外任务 ID，和 workflow_task.task_id 保持一致。
     */
    private String taskId;

    /**
     * 创建任务的用户 id。
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
     * 标签数量，默认 5。
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
    private String hookText;

    /**
     * 小红书正文主体。
     */
    private String bodyContent;

    /**
     * 行动引导文案。
     */
    private String callToAction;

    /**
     * 标签列表 JSON 文本。
     */
    private String tags;

    /**
     * 封面标题。
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
     * 配图结果列表 JSON 文本，包含 URL、状态和失败原因。
     */
    private String images;

    /**
     * 封面图 URL。
     */
    private String coverImage;

    /**
     * 任务状态：PENDING/PROCESSING/COMPLETED/FAILED。
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

    /**
     * 逻辑删除标记。
     */
    @Column(isLogicDelete = true)
    private Integer isDelete;

    /**
     * 序列化版本号。
     */
    @Column(ignore = true)
    private static final long serialVersionUID = 1L;
}
