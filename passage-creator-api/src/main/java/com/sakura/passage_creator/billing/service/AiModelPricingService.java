package com.sakura.passage_creator.billing.service;

import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.billing.model.entity.AiModelPricing;

import java.math.BigDecimal;

/**
 * AI 模型费率服务。
 */
public interface AiModelPricingService extends IService<AiModelPricing> {

    /**
     * 获取启用的费率配置，数据库未配置时返回代码默认费率。
     */
    AiModelPricing resolvePricing(String provider, String model, String requestType);

    /**
     * 按费率和 Token 用量计算积分成本。
     */
    BigDecimal calculateTextCost(AiModelPricing pricing, Long promptTokens, Long completionTokens);

    /**
     * 获取固定调用成本，主要用于图片模型。
     */
    BigDecimal resolveFixedCost(AiModelPricing pricing);
}
