package com.sakura.passage_creator.billing.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * AI 模型费率分页查询请求。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AiModelPricingQueryRequest extends PageRequest implements Serializable {

    /**
     * 模型供应商。
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
     * 是否启用。
     */
    private Integer enabled;

    private static final long serialVersionUID = 1L;
}
