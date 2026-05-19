package com.sakura.passage_creator.rednote.agent.billing;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.sakura.passage_creator.billing.api.AiBillingService;
import org.springframework.stereotype.Component;

/**
 * Rednote 计费拦截器工厂，为每个 Agent 创建带独立阶段信息的 ModelInterceptor。
 */
@Component
public class RednoteBillingModelInterceptorFactory {

    private static final String DASHSCOPE_PROVIDER = "DASHSCOPE";

    private final AiBillingService aiBillingService;

    public RednoteBillingModelInterceptorFactory(AiBillingService aiBillingService) {
        this.aiBillingService = aiBillingService;
    }

    /**
     * 创建 DashScope 文本模型计费拦截器。
     */
    public ModelInterceptor createDashScopeTextInterceptor(String agentName, String phase, String model) {
        return new RednoteBillingModelInterceptor(aiBillingService, agentName, phase, DASHSCOPE_PROVIDER, model);
    }
}
