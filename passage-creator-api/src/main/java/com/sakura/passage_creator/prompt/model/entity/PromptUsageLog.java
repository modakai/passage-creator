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
 * Prompt 使用日志实体，用于追溯每次 Agent 调用了哪个模板版本。
 */
@Data
@Table("prompt_usage_log")
public class PromptUsageLog implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 使用的 Prompt 模板版本 id，兜底模板没有数据库记录时为空。
     */
    private Long promptTemplateId;

    /**
     * 模板标识。
     */
    private String templateKey;

    /**
     * 模板版本号。
     */
    private String version;

    /**
     * 运行环境。
     */
    private String environment;

    /**
     * Agent 名称。
     */
    private String agentName;

    /**
     * 文章生成任务 id。
     */
    private String taskId;

    /**
     * 会话 id。
     */
    private String sessionId;

    /**
     * 用户 id。
     */
    private Long userId;

    /**
     * 使用时间。
     */
    private LocalDateTime usedAt;

    /**
     * 本次调用最终是否成功。
     */
    private Boolean responseOk;

    /**
     * 失败原因。
     */
    private String errorMessage;

    /**
     * 调用耗时毫秒。
     */
    private Integer latencyMs;

    /**
     * 用户反馈评分。
     */
    private Integer feedback;

    /**
     * 序列化版本号。
     */
    @Column(ignore = true)
    private static final long serialVersionUID = 1L;
}
