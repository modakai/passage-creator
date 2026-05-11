package com.sakura.passage_creator.billing.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.billing.model.entity.AiModelPricing;
import com.sakura.passage_creator.billing.model.enums.AiRequestTypeEnum;
import com.sakura.passage_creator.billing.repository.AiModelPricingMapper;
import com.sakura.passage_creator.billing.service.AiModelPricingService;
import com.sakura.passage_creator.billing.service.AiTokenCostCalculator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.sakura.passage_creator.billing.model.entity.table.AiModelPricingTableDef.AI_MODEL_PRICING;

/**
 * AI 模型费率服务实现。
 */
@Service
public class AiModelPricingServiceImpl extends ServiceImpl<AiModelPricingMapper, AiModelPricing>
        implements AiModelPricingService {

    private final AiTokenCostCalculator calculator = new AiTokenCostCalculator();

    @Override
    public AiModelPricing resolvePricing(String provider, String model, String requestType) {
        AiModelPricing pricing = this.getOne(QueryWrapper.create()
                .where(AI_MODEL_PRICING.PROVIDER.eq(provider))
                .and(AI_MODEL_PRICING.MODEL.eq(model))
                .and(AI_MODEL_PRICING.REQUEST_TYPE.eq(requestType))
                .and(AI_MODEL_PRICING.ENABLED.eq(1)));
        return pricing == null ? buildDefaultPricing(provider, model, requestType) : pricing;
    }

    @Override
    public BigDecimal calculateTextCost(AiModelPricing pricing, Long promptTokens, Long completionTokens) {
        AiModelPricing effectivePricing = pricing == null
                ? buildDefaultPricing("DASHSCOPE", "qwen3-max", AiRequestTypeEnum.TEXT.getValue())
                : pricing;
        return calculator.calculateTextCost(effectivePricing.getPromptTokenPricePer1k(),
                effectivePricing.getCompletionTokenPricePer1k(), promptTokens, completionTokens);
    }

    @Override
    public BigDecimal resolveFixedCost(AiModelPricing pricing) {
        return calculator.normalize(pricing == null ? BigDecimal.ZERO : pricing.getFixedCredits());
    }

    /**
     * 数据库未初始化费率时的保守默认值，避免计费链路因空配置中断。
     */
    private AiModelPricing buildDefaultPricing(String provider, String model, String requestType) {
        AiModelPricing pricing = new AiModelPricing();
        pricing.setProvider(provider);
        pricing.setModel(model);
        pricing.setRequestType(requestType);
        pricing.setEnabled(1);
        pricing.setCreateTime(LocalDateTime.now());
        pricing.setUpdateTime(LocalDateTime.now());
        pricing.setIsDelete(0);
        if (AiRequestTypeEnum.IMAGE.getValue().equals(requestType)) {
            pricing.setPromptTokenPricePer1k(BigDecimal.ZERO);
            pricing.setCompletionTokenPricePer1k(BigDecimal.ZERO);
            pricing.setFixedCredits(new BigDecimal("5.0000"));
            pricing.setReserveCredits(new BigDecimal("5.0000"));
            return pricing;
        }
        pricing.setPromptTokenPricePer1k(new BigDecimal("0.0020"));
        pricing.setCompletionTokenPricePer1k(new BigDecimal("0.0060"));
        pricing.setFixedCredits(BigDecimal.ZERO);
        pricing.setReserveCredits(new BigDecimal("1.0000"));
        return pricing;
    }
}
