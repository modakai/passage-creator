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
 * AI 模型费率配置，控制 Token 单价、图片固定成本和调用前预扣额度。
 */
@Data
@Table("ai_model_pricing")
public class AiModelPricing implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 模型供应商，例如 DASHSCOPE、OPENAI。
     */
    private String provider;

    /**
     * 模型名称。
     */
    private String model;

    /**
     * 请求类型：TEXT/IMAGE。
     */
    private String requestType;

    /**
     * 输入 Token 每千 Token 积分单价。
     */
    private BigDecimal promptTokenPricePer1k;

    /**
     * 输出 Token 每千 Token 积分单价。
     */
    private BigDecimal completionTokenPricePer1k;

    /**
     * 固定调用积分成本，图片模型优先使用。
     */
    private BigDecimal fixedCredits;

    /**
     * 调用前预扣积分，防止余额不足仍然调用模型。
     */
    private BigDecimal reserveCredits;

    /**
     * 是否启用。
     */
    private Integer enabled;

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
