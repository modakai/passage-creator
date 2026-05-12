package com.sakura.passage_creator.billing.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 人工充值套餐视图。
 */
@Data
public class ManualRechargePackageVO implements Serializable {

    /**
     * 套餐 ID。
     */
    private String packageId;

    /**
     * 套餐名称。
     */
    private String name;

    /**
     * 应支付金额，单位元。
     */
    private BigDecimal amount;

    /**
     * 可获得积分。
     */
    private BigDecimal credits;

    /**
     * 展示排序。
     */
    private Integer sortOrder;

    private static final long serialVersionUID = 1L;
}
