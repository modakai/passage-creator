package com.sakura.passage_creator.billing.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * AI 模型费率保存请求，id 为空时新增，id 存在时更新。
 */
@Data
public class AiModelPricingSaveRequest implements Serializable {

    /**
     * 费率配置 id。
     */
    private Long id;

    /**
     * 模型供应商。
     */
    @NotBlank(message = "模型供应商不能为空")
    private String provider;

    /**
     * 模型名称。
     */
    @NotBlank(message = "模型名称不能为空")
    private String model;

    /**
     * 请求类型：TEXT/IMAGE。
     */
    @NotBlank(message = "请求类型不能为空")
    private String requestType;

    /**
     * 输入 Token 每千 Token 积分单价。
     */
    @NotNull(message = "输入 Token 单价不能为空")
    @DecimalMin(value = "0.0000", message = "输入 Token 单价不能小于 0")
    private BigDecimal promptTokenPricePer1k;

    /**
     * 输出 Token 每千 Token 积分单价。
     */
    @NotNull(message = "输出 Token 单价不能为空")
    @DecimalMin(value = "0.0000", message = "输出 Token 单价不能小于 0")
    private BigDecimal completionTokenPricePer1k;

    /**
     * 固定调用积分成本。
     */
    @NotNull(message = "固定调用成本不能为空")
    @DecimalMin(value = "0.0000", message = "固定调用成本不能小于 0")
    private BigDecimal fixedCredits;

    /**
     * 调用前预扣积分。
     */
    @NotNull(message = "预扣积分不能为空")
    @DecimalMin(value = "0.0000", message = "预扣积分不能小于 0")
    private BigDecimal reserveCredits;

    /**
     * 是否启用。
     */
    @NotNull(message = "启用状态不能为空")
    private Integer enabled;

    private static final long serialVersionUID = 1L;
}
