package com.sakura.passage_creator.article.model.dto;

import com.sakura.passage_creator.article.model.entity.Article;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增文章请求。
 */
@Data
@AutoMapper(target = Article.class, reverseConvertGenerate = false)
public class ArticleAddRequest implements Serializable {

    /**
     * 文章选题。
     */
    @NotBlank(message = "文章选题不能为空")
    @Size(max = 500, message = "文章选题不能超过 500 个字符")
    private String topic;

    /**
     * 主标题。
     */
    @Size(max = 200, message = "主标题不能超过 200 个字符")
    private String mainTitle;

    /**
     * 副标题。
     */
    @Size(max = 300, message = "副标题不能超过 300 个字符")
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
    @Size(max = 512, message = "封面图地址不能超过 512 个字符")
    private String coverImage;

    /**
     * 配图列表 JSON 文本。
     */
    private String images;

    /**
     * 文章状态。
     */
    @Size(max = 20, message = "文章状态不能超过 20 个字符")
    private String status;

    /**
     * 错误信息。
     */
    private String errorMessage;

    private static final long serialVersionUID = 1L;
}
