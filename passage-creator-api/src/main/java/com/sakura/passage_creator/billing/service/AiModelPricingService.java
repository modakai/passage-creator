package com.sakura.passage_creator.billing.service;

import com.mybatisflex.core.service.IService;
import com.mybatisflex.core.query.QueryWrapper;
import com.sakura.passage_creator.billing.model.dto.AiModelPricingQueryRequest;
import com.sakura.passage_creator.billing.model.dto.AiModelPricingSaveRequest;
import com.sakura.passage_creator.billing.model.entity.AiModelPricing;
import com.sakura.passage_creator.billing.model.vo.AiModelPricingVO;

import java.math.BigDecimal;
import java.util.List;

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

    /**
     * 构造管理端费率查询条件。
     */
    QueryWrapper getQueryWrapper(AiModelPricingQueryRequest request);

    /**
     * 新增或更新模型费率配置。
     */
    AiModelPricing savePricing(AiModelPricingSaveRequest request);

    /**
     * 转换费率配置视图。
     */
    List<AiModelPricingVO> getPricingVO(List<AiModelPricing> records);
}
