package com.sakura.passage_creator.billing.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 人工充值申请视图。
 */
@Data
public class ManualRechargeApplicationVO implements Serializable {

    /**
     * 申请 id。
     */
    private Long id;

    /**
     * 充值申请号。
     */
    private String rechargeNo;

    /**
     * 用户 id。
     */
    private Long userId;

    /**
     * 套餐 id。
     */
    private String packageId;

    /**
     * 支付金额。
     */
    private BigDecimal amount;

    /**
     * 应发积分。
     */
    private BigDecimal credits;

    /**
     * 支付方式。
     */
    private String payMethod;

    /**
     * 申请状态。
     */
    private String status;

    /**
     * 用户备注。
     */
    private String userRemark;

    /**
     * 管理员审核备注。
     */
    private String adminRemark;

    /**
     * 审核时间。
     */
    private LocalDateTime auditTime;

    /**
     * 审核人。
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
     * 收款信息，详情和创建成功后返回。
     */
    private ManualRechargePaymentVO payment;

    private static final long serialVersionUID = 1L;
}
