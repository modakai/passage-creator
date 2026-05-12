package com.sakura.passage_creator.billing.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户创建人工扫码充值申请请求。
 */
@Data
public class ManualRechargeCreateRequest implements Serializable {

    /**
     * 后端配置中的套餐 ID，前端不得传金额和积分。
     */
    @NotBlank(message = "套餐不能为空")
    private String packageId;

    /**
     * 用户选择的付款方式，非法值会降级为 UNKNOWN。
     */
    private String payMethod;

    /**
     * 用户备注，例如付款说明。
     */
    private String userRemark;

    private static final long serialVersionUID = 1L;
}
