package com.sakura.passage_creator.billing.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.billing.model.dto.AiModelPricingQueryRequest;
import com.sakura.passage_creator.billing.model.dto.AiModelPricingSaveRequest;
import com.sakura.passage_creator.billing.model.entity.AiModelPricing;
import com.sakura.passage_creator.billing.model.enums.AiRequestTypeEnum;
import com.sakura.passage_creator.billing.model.vo.AiModelPricingVO;
import com.sakura.passage_creator.billing.repository.AiModelPricingMapper;
import com.sakura.passage_creator.billing.service.AiModelPricingService;
import com.sakura.passage_creator.billing.service.AiTokenCostCalculator;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    public QueryWrapper getQueryWrapper(AiModelPricingQueryRequest request) {
        AiModelPricingQueryRequest safeRequest = request == null ? new AiModelPricingQueryRequest() : request;
        QueryWrapper wrapper = QueryWrapper.create();
        wrapper.where(AI_MODEL_PRICING.PROVIDER.like(safeRequest.getProvider(),
                StringUtils.isNotBlank(safeRequest.getProvider())));
        wrapper.and(AI_MODEL_PRICING.MODEL.like(safeRequest.getModel(), StringUtils.isNotBlank(safeRequest.getModel())));
        wrapper.and(AI_MODEL_PRICING.REQUEST_TYPE.eq(safeRequest.getRequestType(),
                StringUtils.isNotBlank(safeRequest.getRequestType())));
        wrapper.and(AI_MODEL_PRICING.ENABLED.eq(safeRequest.getEnabled(), safeRequest.getEnabled() != null));
        wrapper.orderBy(AI_MODEL_PRICING.UPDATE_TIME, false);
        wrapper.orderBy(AI_MODEL_PRICING.ID, false);
        return wrapper;
    }

    @Override
    public AiModelPricing savePricing(AiModelPricingSaveRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!AiRequestTypeEnum.TEXT.getValue().equals(request.getRequestType())
                && !AiRequestTypeEnum.IMAGE.getValue().equals(request.getRequestType()), ErrorCode.PARAMS_ERROR,
                "请求类型只支持 TEXT 或 IMAGE");
        ThrowUtils.throwIf(request.getEnabled() == null || (request.getEnabled() != 0 && request.getEnabled() != 1),
                ErrorCode.PARAMS_ERROR, "启用状态只支持 0 或 1");

        AiModelPricing pricing = request.getId() == null ? new AiModelPricing() : this.getById(request.getId());
        ThrowUtils.throwIf(pricing == null, ErrorCode.NOT_FOUND_ERROR, "模型费率配置不存在");
        boolean creating = request.getId() == null;
        pricing.setProvider(StringUtils.trim(request.getProvider()));
        pricing.setModel(StringUtils.trim(request.getModel()));
        pricing.setRequestType(request.getRequestType());
        pricing.setPromptTokenPricePer1k(calculator.normalize(request.getPromptTokenPricePer1k()));
        pricing.setCompletionTokenPricePer1k(calculator.normalize(request.getCompletionTokenPricePer1k()));
        pricing.setFixedCredits(calculator.normalize(request.getFixedCredits()));
        pricing.setReserveCredits(calculator.normalize(request.getReserveCredits()));
        pricing.setEnabled(request.getEnabled());
        pricing.setUpdateTime(LocalDateTime.now());
        if (creating) {
            pricing.setCreateTime(LocalDateTime.now());
            pricing.setIsDelete(0);
            this.save(pricing);
            return pricing;
        }
        this.updateById(pricing);
        return pricing;
    }

    @Override
    public List<AiModelPricingVO> getPricingVO(List<AiModelPricing> records) {
        return records.stream().map(this::toVO).toList();
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

    private AiModelPricingVO toVO(AiModelPricing pricing) {
        AiModelPricingVO vo = new AiModelPricingVO();
        vo.setId(pricing.getId());
        vo.setProvider(pricing.getProvider());
        vo.setModel(pricing.getModel());
        vo.setRequestType(pricing.getRequestType());
        vo.setPromptTokenPricePer1k(calculator.normalize(pricing.getPromptTokenPricePer1k()));
        vo.setCompletionTokenPricePer1k(calculator.normalize(pricing.getCompletionTokenPricePer1k()));
        vo.setFixedCredits(calculator.normalize(pricing.getFixedCredits()));
        vo.setReserveCredits(calculator.normalize(pricing.getReserveCredits()));
        vo.setEnabled(pricing.getEnabled());
        vo.setCreateTime(pricing.getCreateTime());
        vo.setUpdateTime(pricing.getUpdateTime());
        return vo;
    }
}
