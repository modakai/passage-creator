package com.sakura.passage_creator.prompt.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Prompt 模板版本实体。
 */
@Data
@Table("prompt_template")
public class PromptTemplate implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 模板标识，例如 article_title_generator_user。
     */
    private String templateKey;

    /**
     * 版本号，例如 1.0.0。
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
     * 状态：DRAFT/ACTIVE/ARCHIVED。
     */
    private String status;

    /**
     * 运行环境：production/staging/dev。
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

    /**
     * 序列化版本号。
     */
    @Column(ignore = true)
    private static final long serialVersionUID = 1L;
}
