package com.sakura.passage_creator.prompt.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Prompt 模板版本视图对象。
 */
@Data
public class PromptTemplateVO implements Serializable {

    /**
     * 主键 id。
     */
    private Long id;

    /**
     * 模板标识。
     */
    private String templateKey;

    /**
     * 版本号。
     */
    private String version;

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

    /**
     * 状态。
     */
    private String status;

    /**
     * 运行环境。
     */
    private String environment;

    /**
     * 创建人账号。
     */
    private String createdBy;

    /**
     * 发布人账号。
     */
    private String publishedBy;

    /**
     * 发布时间。
     */
    private LocalDateTime publishedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    private static final long serialVersionUID = 1L;
}
