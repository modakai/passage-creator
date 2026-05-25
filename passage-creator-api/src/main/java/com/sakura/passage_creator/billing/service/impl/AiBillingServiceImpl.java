package com.sakura.passage_creator.billing.service.impl;

import com.sakura.passage_creator.billing.api.AiBillingReservation;
import com.sakura.passage_creator.billing.api.AiBillingService;
import com.sakura.passage_creator.billing.api.AiTokenUsageSnapshot;
import com.sakura.passage_creator.billing.model.dto.RecordAiUsageCommand;
import com.sakura.passage_creator.billing.model.entity.AiModelPricing;
import com.sakura.passage_creator.billing.model.entity.CreditTransaction;
import com.sakura.passage_creator.billing.model.enums.AiRequestTypeEnum;
import com.sakura.passage_creator.billing.service.AiCostMetricsRecorder;
import com.sakura.passage_creator.billing.service.AiModelPricingService;
import com.sakura.passage_creator.billing.service.AiTokenCostCalculator;
import com.sakura.passage_creator.billing.service.AiUsageRecordService;
import com.sakura.passage_creator.billing.service.CreditAccountService;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * AI 计费门面实现，统一封装预扣、用量记录和结算。
 */
@Service
public class AiBillingServiceImpl implements AiBillingService {

    private static final String AI_CALL_BIZ_TYPE = "AI_CALL";

    private final AiModelPricingService pricingService;

    private final CreditAccountService creditAccountService;

    private final AiUsageRecordService usageRecordService;

    private final AiCostMetricsRecorder costMetricsRecorder;

    private final AiTokenCostCalculator amountNormalizer = new AiTokenCostCalculator();

    public AiBillingServiceImpl(AiModelPricingService pricingService, CreditAccountService creditAccountService,
                                AiUsageRecordService usageRecordService, AiCostMetricsRecorder costMetricsRecorder) {
        this.pricingService = pricingService;
        this.creditAccountService = creditAccountService;
        this.usageRecordService = usageRecordService;
        this.costMetricsRecorder = costMetricsRecorder;
    }

    @Override
    public AiBillingReservation reserveTextCall(Long userId, String taskId, String agentName, String phase,
                                                String provider, String model) {
        return reserve(userId, taskId, agentName, phase, provider, model, AiRequestTypeEnum.TEXT.getValue());
    }

    @Override
    public AiBillingReservation reserveImageCall(Long userId, String taskId, String agentName, String phase,
                                                 String provider, String model) {
        return reserve(userId, taskId, agentName, phase, provider, model, AiRequestTypeEnum.IMAGE.getValue());
    }

    @Override
    public void completeTextCall(AiBillingReservation reservation, AiTokenUsageSnapshot usage, Integer latencyMs,
                                 boolean responseOk, String errorMessage) {
        if (reservation == null) {
            return;
        }
        AiModelPricing pricing = pricingService.resolvePricing(reservation.provider(), reservation.model(),
                reservation.requestType());
        BigDecimal cost = pricingService.calculateTextCost(pricing,
                usage == null ? null : usage.promptTokens(),
                usage == null ? null : usage.completionTokens());
        recordUsage(reservation, usage, cost, latencyMs, responseOk, errorMessage);
        creditAccountService.settleReserved(reservation.transactionId(), cost, "AI 文本模型真实用量结算");
    }

    @Override
    public void completeImageCall(AiBillingReservation reservation, Integer latencyMs, boolean responseOk,
                                  String errorMessage) {
        if (reservation == null) {
            return;
        }
        AiModelPricing pricing = pricingService.resolvePricing(reservation.provider(), reservation.model(),
                reservation.requestType());
        BigDecimal cost = pricingService.resolveFixedCost(pricing);
        recordUsage(reservation, new AiTokenUsageSnapshot(0L, 0L, 0L), cost, latencyMs, responseOk, errorMessage);
        creditAccountService.settleReserved(reservation.transactionId(), cost, "AI 图片模型固定成本结算");
    }

    @Override
    public void releaseReservation(AiBillingReservation reservation, Integer latencyMs, String errorMessage) {
        if (reservation == null) {
            return;
        }
        recordUsage(reservation, new AiTokenUsageSnapshot(0L, 0L, 0L), BigDecimal.ZERO, latencyMs, false, errorMessage);
        creditAccountService.releaseReserved(reservation.transactionId(), errorMessage);
    }

    private AiBillingReservation reserve(Long userId, String taskId, String agentName, String phase, String provider,
                                         String model, String requestType) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "AI 调用缺少用户 id，无法计费");
        AiModelPricing pricing = pricingService.resolvePricing(provider, model, requestType);
        BigDecimal reserveCredits = amountNormalizer.normalize(pricing.getReserveCredits());
        CreditTransaction transaction = creditAccountService.reserveCredits(userId, reserveCredits, AI_CALL_BIZ_TYPE,
                buildBizId(taskId, agentName, phase), "AI 调用前预扣积分");
        return new AiBillingReservation(transaction.getId(), userId, taskId, agentName, phase, provider, model,
                requestType, reserveCredits);
    }

    private void recordUsage(AiBillingReservation reservation, AiTokenUsageSnapshot usage, BigDecimal cost,
                             Integer latencyMs, boolean responseOk, String errorMessage) {
        RecordAiUsageCommand command = new RecordAiUsageCommand();
        command.setUserId(reservation.userId());
        command.setTaskId(reservation.taskId());
        command.setAgentName(reservation.agentName());
        command.setPhase(reservation.phase());
        command.setProvider(reservation.provider());
        command.setModel(reservation.model());
        command.setRequestType(reservation.requestType());
        command.setPromptTokens(usage == null ? 0L : usage.promptTokens());
        command.setCompletionTokens(usage == null ? 0L : usage.completionTokens());
        command.setTotalTokens(usage == null ? 0L : usage.totalTokens());
        command.setCreditCost(cost);
        command.setLatencyMs(latencyMs);
        command.setResponseOk(responseOk);
        command.setErrorMessage(errorMessage);
        usageRecordService.recordUsage(command);
        costMetricsRecorder.recordUsage(reservation, usage, cost, latencyMs, responseOk);
    }

    private String buildBizId(String taskId, String agentName, String phase) {
        return "%s:%s:%s:%d".formatted(taskId, agentName, phase, System.nanoTime());
    }
}
