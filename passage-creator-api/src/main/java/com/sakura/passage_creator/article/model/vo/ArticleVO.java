package com.sakura.passage_creator.article.model.vo;

import com.sakura.passage_creator.article.model.entity.Article;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章展示对象。
 */
@Data
@AutoMapper(target = Article.class)
public class ArticleVO implements Serializable {

    /**
     * 文章 id。
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
     * 文章状态。
     */
    private String status;

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
