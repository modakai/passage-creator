package com.sakura.passage_creator.article.model.dto;

import com.sakura.passage_creator.article.model.entity.Article;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * @author sakura
 * @create 2026-04
 */
@Data
@AutoMapper(target = Article.class, reverseConvertGenerate = false)
public class ArticleConfirmTitleRequest implements Serializable {

    /**
     * 对外任务 ID，使用 UUID 字符串避免暴露自增 id。
     */
    @NotBlank(message = "{validation.article.task_id.not_blank}")
    private String taskId;

    /**
     * 选择的主标题
     */
    @NotBlank(message = "{validation.article.main_title.not_blank}")
    private String selectedMainTitle;

    /**
     * 选择的副标题
     */
    @NotBlank(message = "{validation.article.sub_title.not_blank}")
    private String selectedSubTitle;

    /**
     * 描述
     */
    private String userDescription;
}
