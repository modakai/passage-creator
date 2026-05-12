package com.sakura.passage_creator.billing.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员审核人工充值申请请求。
 */
@Data
public class ManualRechargeReviewRequest implements Serializable {

    /**
     * 充值申请 id。
     */
    @NotNull(message = "申请 id 不能为空")
    private Long id;

    /**
     * 管理员审核备注，拒绝时作为拒绝原因。
     */
    private String adminRemark;

    private static final long serialVersionUID = 1L;
}
