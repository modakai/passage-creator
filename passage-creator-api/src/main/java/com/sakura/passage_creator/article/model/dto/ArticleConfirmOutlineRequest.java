package com.sakura.passage_creator.article.model.dto;

import com.sakura.passage_creator.article.agent.state.ArticleState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @author sakura
 * @create 2026-04
 */
@Data
public class ArticleConfirmOutlineRequest implements Serializable {

    /**
     * 对外任务 ID，使用 UUID 字符串避免暴露自增 id。
     */
    @NotBlank(message = "{validation.article.task_id.not_blank}")
    private String taskId;

    /**
     * 大纲
     */
    @NotNull(message = "{validation.article.outline.not_null}")
    private ArticleState.OutlineResult outline;

}
