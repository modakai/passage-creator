package com.sakura.passage_creator.billing.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户积分账户余额视图。
 */
@Data
public class CreditAccountVO implements Serializable {

    private Long id;

    private Long userId;

    private BigDecimal balance;

    private BigDecimal totalRecharge;

    private BigDecimal totalConsume;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
