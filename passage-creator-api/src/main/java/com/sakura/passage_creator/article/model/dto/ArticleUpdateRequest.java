package com.sakura.passage_creator.article.model.dto;

import com.sakura.passage_creator.article.model.entity.Article;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 更新文章请求。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AutoMapper(target = Article.class, reverseConvertGenerate = false)
public class ArticleUpdateRequest extends ArticleAddRequest implements Serializable {

    /**
     * 文章 id。
     */
    @NotNull(message = "文章 id 不能为空")
    @Positive(message = "文章 id 必须大于 0")
    private Long id;

    private static final long serialVersionUID = 1L;
}
