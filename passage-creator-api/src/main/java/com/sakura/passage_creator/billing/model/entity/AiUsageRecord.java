package com.sakura.passage_creator.billing.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 用量记录，保存每次模型调用的 Token、成本和调用结果。
 */
@Data
@Table("ai_usage_record")
public class AiUsageRecord implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 调用用户 id。
     */
    private Long userId;

    /**
     * 文章任务 id。
     */
    private String taskId;

    /**
     * Agent 或策略名称。
     */
    private String agentName;

    /**
     * 创作阶段。
     */
    private String phase;

    /**
     * 模型供应商。
     */
    private String provider;

    /**
     * 模型名称。
     */
    private String model;

    /**
     * 请求类型：TEXT/IMAGE。
     */
    private String requestType;

    /**
     * 输入 Token 数。
     */
    private Long promptTokens;

    /**
     * 输出 Token 数。
     */
    private Long completionTokens;

    /**
     * 总 Token 数。
     */
    private Long totalTokens;

    /**
     * 本次调用折算积分成本。
     */
    private BigDecimal creditCost;

    /**
     * 调用耗时毫秒。
     */
    private Integer latencyMs;

    /**
     * 调用是否成功。
     */
    private Boolean responseOk;

    /**
     * 失败原因。
     */
    private String errorMessage;

    /**
     * 使用时间。
     */
    private LocalDateTime usedAt;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记。
     */
    @Column(isLogicDelete = true)
    private Integer isDelete;

    @Column(ignore = true)
    private static final long serialVersionUID = 1L;
}
