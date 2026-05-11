package com.sakura.passage_creator.article.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

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

    /**
     * 用户允许使用的配图方式，后端会过滤非法值并在失败时允许降级到 PICSUM。
     */
    private List<String> enabledImageMethods;
}
