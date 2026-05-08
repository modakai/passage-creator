package com.sakura.passage_creator.prompt.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * Prompt 模板缓存刷新请求。
 */
@Data
public class PromptTemplateRefreshRequest implements Serializable {

    /**
     * 模板标识。
     */
    @NotBlank(message = "模板标识不能为空")
    private String templateKey;

    /**
     * 运行环境。
     */
    private String environment;

    private static final long serialVersionUID = 1L;
}
