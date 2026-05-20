package com.sakura.passage_creator.rednote.agent.billing;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.sakura.passage_creator.prompt.api.PromptTemplateRenderResult;
import com.sakura.passage_creator.prompt.api.PromptUsageLogService;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Rednote Prompt 使用日志拦截器，记录 Agent 实际绑定的系统 Prompt 和用户指令版本。
 */
public class RednotePromptUsageModelInterceptor extends ModelInterceptor {

    private final PromptUsageLogService promptUsageLogService;

    private final String agentName;

    private final List<PromptTemplateRenderResult> promptResults;

    public RednotePromptUsageModelInterceptor(PromptUsageLogService promptUsageLogService, String agentName,
            List<PromptTemplateRenderResult> promptResults) {
        this.promptUsageLogService = promptUsageLogService;
        this.agentName = agentName;
        this.promptResults = promptResults;
    }

    /**
     * 包裹模型调用并在成功或失败时记录 Prompt 模板版本快照。
     */
    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        Long userId = requiredLong(request, RednoteWorkflowState.KEY_USER_ID);
        String taskId = requiredString(request, RednoteWorkflowState.KEY_TASK_ID);
        long startMillis = System.currentTimeMillis();
        try {
            ModelResponse response = handler.call(request);
            boolean responseOk = response != null && response.getChatResponse() != null;
            recordUsage(taskId, userId, responseOk, responseOk ? null : "模型响应缺少 ChatResponse", startMillis);
            return response;
        } catch (RuntimeException e) {
            recordUsage(taskId, userId, false, e.getMessage(), startMillis);
            throw e;
        }
    }

    /**
     * 返回唯一拦截器名称，避免多个 Agent 注册时被框架去重。
     */
    @Override
    public String getName() {
        return "rednote-prompt-usage-" + agentName;
    }

    /**
     * 记录当前 Agent 绑定的所有 Prompt 模板使用情况。
     */
    private void recordUsage(String taskId, Long userId, boolean responseOk, String errorMessage, long startMillis) {
        Integer latencyMs = Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startMillis));
        for (PromptTemplateRenderResult result : promptResults) {
            promptUsageLogService.recordUsage(result, agentName, taskId, userId, responseOk, errorMessage, latencyMs);
        }
    }

    /**
     * 从模型请求上下文读取必填字符串。
     */
    private String requiredString(ModelRequest request, String key) {
        Object value = request.getContext() == null ? null : request.getContext().get(key);
        if (value == null || StringUtils.isBlank(value.toString())) {
            throw new IllegalStateException("缺少 rednote Prompt 日志上下文：" + key);
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
        throw new IllegalStateException("缺少 rednote Prompt 日志上下文：" + key);
    }
}
