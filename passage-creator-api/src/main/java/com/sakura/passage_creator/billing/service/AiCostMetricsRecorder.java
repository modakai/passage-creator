package com.sakura.passage_creator.billing.service;

import com.sakura.passage_creator.billing.api.AiBillingReservation;
import com.sakura.passage_creator.billing.api.AiTokenUsageSnapshot;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * AI 成本 Micrometer 指标记录器，用于向 Prometheus 暴露实时成本、Token 和耗时指标。
 */
@Component
public class AiCostMetricsRecorder {

    /**
     * 未知维度统一占位，避免空 tag 破坏聚合查询。
     */
    private static final String UNKNOWN_TAG_VALUE = "unknown";

    /**
     * Micrometer 指标注册表。
     */
    private final MeterRegistry meterRegistry;

    public AiCostMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录一次 AI 调用的实时指标；用户、任务和错误详情保留在数据库明细中，不进入 Prometheus tag。
     */
    public void recordUsage(AiBillingReservation reservation, AiTokenUsageSnapshot usage, BigDecimal creditCost,
            Integer latencyMs, boolean responseOk) {
        if (reservation == null) {
            return;
        }
        Tags commonTags = Tags.of(
                "provider", safeTagValue(reservation.provider()),
                "model", safeTagValue(reservation.model()),
                "request_type", safeTagValue(reservation.requestType()),
                "phase", safeTagValue(reservation.phase()),
                "agent_name", safeTagValue(reservation.agentName()),
                "status", responseOk ? "success" : "failure");

        Counter.builder("ai.cost.calls")
                .description("AI model call count grouped by provider, model and phase")
                .tags(commonTags)
                .register(meterRegistry)
                .increment();
        Counter.builder("ai.cost.credits")
                .description("AI credit cost total grouped by provider, model and phase")
                .tags(commonTags)
                .register(meterRegistry)
                .increment(safeAmount(creditCost));
        recordToken("prompt", usage == null ? null : usage.promptTokens(), commonTags);
        recordToken("completion", usage == null ? null : usage.completionTokens(), commonTags);
        recordToken("total", usage == null ? null : usage.totalTokens(), commonTags);
        recordLatency(latencyMs, commonTags);
    }

    /**
     * 记录指定类型 Token 数；负数会归零，避免异常上报污染累计值。
     */
    private void recordToken(String tokenType, Long tokenCount, Tags commonTags) {
        Counter.builder("ai.cost.tokens")
                .description("AI token total grouped by token type, provider, model and phase")
                .tags(commonTags.and("token_type", tokenType))
                .register(meterRegistry)
                .increment(safeToken(tokenCount));
    }

    /**
     * 记录调用耗时；缺失或负数耗时按 0 毫秒处理，保证指标可聚合。
     */
    private void recordLatency(Integer latencyMs, Tags commonTags) {
        Timer.builder("ai.cost.latency")
                .description("AI model call latency grouped by provider, model and phase")
                .tags(commonTags)
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0, latencyMs == null ? 0 : latencyMs)));
    }

    /**
     * 清洗 tag 值，禁止空值进入指标维度。
     */
    private String safeTagValue(String value) {
        return StringUtils.defaultIfBlank(value, UNKNOWN_TAG_VALUE);
    }

    /**
     * 清洗积分成本，Counter 只接受非负数。
     */
    private double safeAmount(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return 0.0D;
        }
        return value.doubleValue();
    }

    /**
     * 清洗 Token 数，Counter 只接受非负数。
     */
    private double safeToken(Long value) {
        return value == null ? 0.0D : Math.max(0L, value);
    }
}
