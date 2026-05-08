package com.sakura.passage_creator.prompt.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新 Prompt 模板草稿请求。
 */
@Data
public class PromptTemplateUpdateRequest implements Serializable {

    /**
     * 模板版本 id。
     */
    @NotNull(message = "模板 id 不能为空")
    private Long id;

    /**
     * Prompt 模板内容。
     */
    private String content;

    /**
     * 变量定义 JSON。
     */
    private String variablesSchema;

    /**
     * 本版本变更说明。
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
