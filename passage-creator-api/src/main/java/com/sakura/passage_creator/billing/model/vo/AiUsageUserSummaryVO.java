package com.sakura.passage_creator.billing.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 用户维度 AI 用量汇总。
 */
@Data
public class AiUsageUserSummaryVO implements Serializable {

    private Long userId;

    private Long callCount;

    private Long totalTokens;

    private BigDecimal creditCost;

    private static final long serialVersionUID = 1L;
}
