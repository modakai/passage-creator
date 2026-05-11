package com.sakura.passage_creator.billing.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 用量记录视图对象。
 */
@Data
public class AiUsageRecordVO implements Serializable {

    private Long id;

    private Long userId;

    private String taskId;

    private String agentName;

    private String phase;

    private String provider;

    private String model;

    private String requestType;

    private Long promptTokens;

    private Long completionTokens;

    private Long totalTokens;

    private BigDecimal creditCost;

    private Integer latencyMs;

    private Boolean responseOk;

    private String errorMessage;

    private LocalDateTime usedAt;

    private static final long serialVersionUID = 1L;
}
