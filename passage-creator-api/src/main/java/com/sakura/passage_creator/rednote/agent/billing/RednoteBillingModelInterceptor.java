package com.sakura.passage_creator.rednote.agent.billing;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.sakura.passage_creator.billing.api.AiBillingReservation;
import com.sakura.passage_creator.billing.api.AiBillingService;
import com.sakura.passage_creator.billing.api.AiChatBillingSupport;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import org.apache.commons.lang3.StringUtils;

/**
 * Rednote 文本模型计费拦截器，包裹 ReactAgent 的每次模型调用并写入积分账本。
 */
public class RednoteBillingModelInterceptor extends ModelInterceptor {

    private static final String MISSING_CHAT_RESPONSE_MESSAGE = "模型响应缺少 ChatResponse，释放 rednote 文本预扣积分";

    private final AiBillingService aiBillingService;

    private final String agentName;

    private final String phase;

    private final String provider;

    private final String model;

    public RednoteBillingModelInterceptor(AiBillingService aiBillingService, String agentName, String phase,
            String provider, String model) {
        this.aiBillingService = aiBillingService;
        this.agentName = agentName;
        this.phase = phase;
        this.provider = provider;
        this.model = model;
    }

    /**
     * 拦截模型调用：调用前预扣，调用后按真实 token 结算，异常或无响应时释放预扣。
     */
    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        Long userId = requiredLong(request, RednoteWorkflowState.KEY_USER_ID);
        String taskId = requiredString(request, RednoteWorkflowState.KEY_TASK_ID);
        long startMillis = System.currentTimeMillis();
        AiBillingReservation reservation = aiBillingService.reserveTextCall(userId, taskId, agentName, phase,
                provider, model);
        try {
            ModelResponse response = handler.call(request);
            if (response == null || response.getChatResponse() == null) {
                aiBillingService.releaseReservation(reservation, resolveLatency(startMillis),
                        MISSING_CHAT_RESPONSE_MESSAGE);
                return response;
            }
            aiBillingService.completeTextCall(reservation, AiChatBillingSupport.usageOf(response.getChatResponse()),
                    resolveLatency(startMillis), true, null);
            return response;
        } catch (RuntimeException e) {
            aiBillingService.releaseReservation(reservation, resolveLatency(startMillis), e.getMessage());
            throw e;
        }
    }

    /**
     * 返回唯一拦截器名称，避免多个 Agent 注册时被框架去重。
     */
    @Override
    public String getName() {
        return "rednote-billing-" + agentName;
    }

    /**
     * 从模型请求上下文读取必填字符串。
     */
    private String requiredString(ModelRequest request, String key) {
        Object value = request.getContext() == null ? null : request.getContext().get(key);
        if (value == null || StringUtils.isBlank(value.toString())) {
            throw new IllegalStateException("缺少 rednote 计费上下文：" + key);
        }
        return value.toString();
    }

    /**
     * 从模型请求上下文读取必填用户 ID。
     */
    private Long requiredLong(ModelRequest request, String key) {
        Object value = request.getContext() == null ? null : request.getContext().get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null && StringUtils.isNotBlank(value.toString())) {
            return Long.parseLong(value.toString());
        }
        throw new IllegalStateException("缺少 rednote 计费上下文：" + key);
    }

    /**
     * 计算模型调用耗时，避免超过 Integer 上限。
     */
    private Integer resolveLatency(long startMillis) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis));
    }
}
