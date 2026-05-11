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
 * 用户积分账户，保存当前余额和累计充值/消费。
 */
@Data
@Table("credit_account")
public class CreditAccount implements Serializable {

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
     * 当前积分余额。
     */
    private BigDecimal balance;

    /**
     * 累计充值积分。
     */
    private BigDecimal totalRecharge;

    /**
     * 累计消费积分。
     */
    private BigDecimal totalConsume;

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
