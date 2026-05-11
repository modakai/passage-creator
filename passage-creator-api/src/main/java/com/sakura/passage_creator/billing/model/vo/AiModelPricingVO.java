package com.sakura.passage_creator.billing.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 模型费率配置视图。
 */
@Data
public class AiModelPricingVO implements Serializable {

    private Long id;

    private String provider;

    private String model;

    private String requestType;

    private BigDecimal promptTokenPricePer1k;

    private BigDecimal completionTokenPricePer1k;

    private BigDecimal fixedCredits;

    private BigDecimal reserveCredits;

    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
