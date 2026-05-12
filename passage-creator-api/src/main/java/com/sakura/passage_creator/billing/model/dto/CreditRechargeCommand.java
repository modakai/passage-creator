package com.sakura.passage_creator.billing.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 积分充值内部命令，用于关联具体业务来源，避免申请审核复用管理端表单 DTO。
 */
@Data
public class CreditRechargeCommand implements Serializable {

    /**
     * 充值目标用户 id。
     */
    private Long userId;

    /**
     * 充值积分数量。
     */
    private BigDecimal amount;

    /**
     * 业务类型，例如 MANUAL_RECHARGE。
     */
    private String bizType;

    /**
     * 业务 id，例如人工充值申请号。
     */
    private String bizId;

    /**
     * 积分流水说明。
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
