package com.sakura.passage_creator.article.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建文章
 *
 * @author sakura
 * @create 2026-04
 */
@Data
public class ArticleCreateRequest implements Serializable {

    /**
     * 文章选题。
     */
    @NotBlank(message = "{validation.article.topic.not_blank}")
    private String topic;
}
