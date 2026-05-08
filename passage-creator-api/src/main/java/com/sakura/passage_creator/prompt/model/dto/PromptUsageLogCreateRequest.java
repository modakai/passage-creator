package com.sakura.passage_creator.prompt.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Prompt 使用日志创建命令。
 */
@Data
public class PromptUsageLogCreateRequest implements Serializable {

    /**
     * 模板版本 id。
     */
    private Long promptTemplateId;

    /**
     * 模板标识。
     */
    private String templateKey;

    /**
     * 版本号。
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
     * 任务 id。
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
     * 本次调用是否成功。
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

    private static final long serialVersionUID = 1L;
}
