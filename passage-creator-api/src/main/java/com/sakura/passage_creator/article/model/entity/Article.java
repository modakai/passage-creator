package com.sakura.passage_creator.article.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章实体，承载创作任务和最终文章内容。
 */
@Data
@Table("article")
public class Article implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 对外任务 ID，使用 UUID 字符串避免暴露自增 id。
     */
    private String taskId;

    /**
     * 创建文章的用户 id。
     */
    private Long userId;

    /**
     * 文章选题。
     */
    private String topic;

    /**
     * 主标题。
     */
    private String mainTitle;

    /**
     * 副标题。
     */
    private String subTitle;

    /**
     * 大纲 JSON 文本。
     */
    private String outline;

    /**
     * 正文 Markdown。
     */
    private String content;

    /**
     * 完整图文 Markdown。
     */
    private String fullContent;

    /**
     * 封面图 URL。
     */
    private String coverImage;

    /**
     * 配图列表 JSON 文本。
     */
    private String images;

    /**
     * 状态：PENDING/PROCESSING/COMPLETED/FAILED。
     */
    private String status;

    /**
     * 状态：PENDING/PROCESSING/COMPLETED/FAILED。
     */
    private String phase;

    /**
     * AI 生成的标题候选 JSON 数组文本，对应 MySQL json 字段。
     */
    private String titleOptions;

    /**
     * 用户选择标题后补充的创作要求。
     */
    private String userDescription;

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
