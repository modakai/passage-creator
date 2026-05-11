package com.sakura.passage_creator.billing.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 用量分页查询请求。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AiUsageQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户 id。
     */
    private Long userId;

    /**
     * 文章任务 id。
     */
    private String taskId;

    /**
     * Agent 名称。
     */
    private String agentName;

    /**
     * 模型供应商。
     */
    private String provider;

    /**
     * 模型名称。
     */
    private String model;

    /**
     * 创作阶段。
     */
    private String phase;

    /**
     * 请求类型：TEXT/IMAGE。
     */
    private String requestType;

    /**
     * 开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 结束时间。
     */
    private LocalDateTime endTime;

    private static final long serialVersionUID = 1L;
}
