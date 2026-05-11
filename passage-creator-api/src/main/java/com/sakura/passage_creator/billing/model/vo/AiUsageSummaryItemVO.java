package com.sakura.passage_creator.billing.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * AI 用量聚合条目。
 */
@Data
public class AiUsageSummaryItemVO implements Serializable {

    private String label;

    private Long callCount;

    private Long totalTokens;

    private BigDecimal creditCost;

    private static final long serialVersionUID = 1L;
}
