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
 * 人工扫码充值申请，记录用户付款后等待管理员人工审核的生命周期。
 */
@Data
@Table("credit_recharge_application")
public class CreditRechargeApplication implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 后端生成的唯一充值申请号。
     */
    private String rechargeNo;

    /**
     * 发起申请的用户 id。
     */
    private Long userId;

    /**
     * 创建申请时选择的套餐 id。
     */
    private String packageId;

    /**
     * 创建申请时的套餐金额快照。
     */
    private BigDecimal amount;

    /**
     * 创建申请时的应发积分快照。
     */
    private BigDecimal credits;

    /**
     * 用户选择或管理员识别的支付方式。
     */
    private String payMethod;

    /**
     * 申请状态：PENDING/APPROVED/REJECTED。
     */
    private String status;

    /**
     * 用户提交的付款备注或说明。
     */
    private String userRemark;

    /**
     * 管理员审核备注，拒绝时保存拒绝原因。
     */
    private String adminRemark;

    /**
     * 管理员审核时间。
     */
    private LocalDateTime auditTime;

    /**
     * 审核人账号。
     */
    private String auditor;

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
