package com.sakura.passage_creator.billing.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * AI 用量记录命令，由计费门面统一构造。
 */
@Data
public class RecordAiUsageCommand implements Serializable {

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

    private static final long serialVersionUID = 1L;
}
