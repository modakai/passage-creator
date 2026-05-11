package com.sakura.passage_creator.billing.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 管理员手动充值请求。
 */
@Data
public class CreditRechargeRequest implements Serializable {

    /**
     * 充值目标用户 id。
     */
    @NotNull(message = "用户 id 不能为空")
    private Long userId;

    /**
     * 充值积分金额。
     */
    @NotNull(message = "充值积分不能为空")
    @DecimalMin(value = "0.0001", message = "充值积分必须大于 0")
    private BigDecimal amount;

    /**
     * 充值备注。
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
