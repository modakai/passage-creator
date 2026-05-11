package com.sakura.passage_creator.billing.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 当前用户积分账户概览。
 */
@Data
public class CreditSummaryVO implements Serializable {

    private Long userId;

    private BigDecimal balance;

    private BigDecimal totalRecharge;

    private BigDecimal totalConsume;

    private static final long serialVersionUID = 1L;
}
