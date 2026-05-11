package com.sakura.passage_creator.billing.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分流水，每一次余额变化都必须有对应记录。
 */
@Data
@Table("credit_transaction")
public class CreditTransaction implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 用户 id。
     */
    private Long userId;

    /**
     * 积分账户 id。
     */
    private Long accountId;

    /**
     * 流水类型。
     */
    private String transactionType;

    /**
     * 流水状态。
     */
    private String status;

    /**
     * 本次流水积分金额，消费和预扣也保存正数。
     */
    private BigDecimal amount;

    /**
     * 流水完成后的账户余额。
     */
    private BigDecimal balanceAfter;

    /**
     * 业务类型，例如 AI_CALL、ADMIN_RECHARGE。
     */
    private String bizType;

    /**
     * 业务 id，例如 taskId:agentName。
     */
    private String bizId;

    /**
     * 流水说明。
     */
    private String description;

    /**
     * 操作人，系统自动扣费使用 SYSTEM。
     */
    private String operator;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记。
     */
    @Column(isLogicDelete = true)
    private Integer isDelete;

    @Column(ignore = true)
    private static final long serialVersionUID = 1L;
}
