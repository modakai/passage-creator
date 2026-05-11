package com.sakura.passage_creator.billing.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分流水视图对象。
 */
@Data
public class CreditTransactionVO implements Serializable {

    private Long id;

    private Long userId;

    private String transactionType;

    private String status;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    private String bizType;

    private String bizId;

    private String description;

    private String operator;

    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
