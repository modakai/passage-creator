package com.sakura.passage_creator.article.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 文章分页查询请求。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArticleQueryRequest extends PageRequest implements Serializable {

    /**
     * 文章 id。
     */
    private Long id;

    /**
     * 任务 ID。
     */
    private String taskId;

    /**
     * 用户 id，管理员可用于后台筛选。
     */
    private Long userId;

    /**
     * 文章选题关键字。
     */
    private String topic;

    /**
     * 标题关键字。
     */
    private String title;

    /**
     * 文章状态。
     */
    private String status;

    private static final long serialVersionUID = 1L;
}
