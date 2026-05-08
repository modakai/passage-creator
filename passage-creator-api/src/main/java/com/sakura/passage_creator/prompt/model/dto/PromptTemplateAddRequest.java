package com.sakura.passage_creator.prompt.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增 Prompt 模板版本请求。
 */
@Data
public class PromptTemplateAddRequest implements Serializable {

    /**
     * 模板标识。
     */
    @NotBlank(message = "模板标识不能为空")
    @Size(max = 100, message = "模板标识不能超过 100 个字符")
    private String templateKey;

    /**
     * 版本号。
     */
    @NotBlank(message = "版本号不能为空")
    @Size(max = 20, message = "版本号不能超过 20 个字符")
    private String version;

    /**
     * Prompt 模板内容。
     */
    @NotBlank(message = "Prompt 内容不能为空")
    private String content;

    /**
     * 变量定义 JSON。
     */
    private String variablesSchema;

    /**
     * 本版本变更说明。
     */
    private String description;

    /**
     * 运行环境。
     */
    @Size(max = 20, message = "运行环境不能超过 20 个字符")
    private String environment;

    private static final long serialVersionUID = 1L;
}
