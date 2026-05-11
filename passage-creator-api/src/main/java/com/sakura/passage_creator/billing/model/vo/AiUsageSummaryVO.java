package com.sakura.passage_creator.billing.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 用量总览。
 */
@Data
public class AiUsageSummaryVO implements Serializable {

    private Long callCount = 0L;

    private Long promptTokens = 0L;

    private Long completionTokens = 0L;

    private Long totalTokens = 0L;

    private BigDecimal creditCost = BigDecimal.ZERO;

    private List<AiUsageSummaryItemVO> modelItems = new ArrayList<>();

    private List<AiUsageSummaryItemVO> phaseItems = new ArrayList<>();

    private static final long serialVersionUID = 1L;
}
