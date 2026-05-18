package com.sakura.passage_creator.rednote.agent.hook;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.sakura.passage_creator.rednote.agent.tool.image.RednoteCoverImagePromptResponseSupport;
import com.sakura.passage_creator.rednote.model.enums.RednotePhaseEnum;
import com.sakura.passage_creator.rednote.model.enums.RednoteStatusEnum;
import com.sakura.passage_creator.rednote.service.RednoteNotePersistenceService;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 封面图提示词 Agent Hook，负责持久化 cover_title 和 cover_prompt。
 */
@Component
@HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
public class RednoteCoverImagePromptAgentHook extends AgentHook {

    private final RednoteNotePersistenceService rednoteNotePersistenceService;

    public RednoteCoverImagePromptAgentHook(RednoteNotePersistenceService rednoteNotePersistenceService) {
        this.rednoteNotePersistenceService = rednoteNotePersistenceService;
    }

    @Override
    public String getName() {
        return "rednote_cover_image_prompt_agent_persistence";
    }

    /**
     * 封面提示词开始前，只推进阶段，不覆盖普通图提示词分支字段。
     */
    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        String taskId = requiredTaskId(state);
        ensureUpdated(rednoteNotePersistenceService.markPhase(taskId,
                RednoteStatusEnum.PROCESSING.getValue(),
                RednotePhaseEnum.IMAGE_PROMPT_GENERATING.getValue()));
        return CompletableFuture.completedFuture(Map.of());
    }

    /**
     * 封面提示词完成后，保存封面标题和封面图提示词。
     */
    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        String taskId = requiredTaskId(state);
        String subject = stringValue(state, RednoteWorkflowState.KEY_SUBJECT);
        String bodyContent = stringValue(state, RednoteWorkflowState.KEY_BODY_CONTENT);
        List<String> tags = listValue(state, RednoteWorkflowState.KEY_TAGS);

        RednoteWorkflowState.CoverImagePromptResponse promptResponse =
                resolveCoverImagePromptResponse(state, subject, bodyContent, tags);
        ensureUpdated(rednoteNotePersistenceService.saveCoverImagePrompt(taskId,
                promptResponse.getTitle(),
                promptResponse.getCoverPrompt(),
                RednoteStatusEnum.PROCESSING.getValue(),
                RednotePhaseEnum.IMAGE_GENERATING.getValue()));

        return CompletableFuture.completedFuture(RednoteCoverImagePromptResponseSupport.toStateUpdates(promptResponse));
    }

    /**
     * 优先读取 outputKey 结构化结果，失败时读取最后一条 AssistantMessage。
     */
    private RednoteWorkflowState.CoverImagePromptResponse resolveCoverImagePromptResponse(OverAllState state,
                                                                                         String subject,
                                                                                         String bodyContent,
                                                                                         List<String> tags) {
        Object outputValue = state.value(RednoteWorkflowState.KEY_COVER_IMAGE_PROMPT_RESPONSE).orElse(null);
        if (outputValue instanceof RednoteWorkflowState.CoverImagePromptResponse promptResponse) {
            RednoteCoverImagePromptResponseSupport.normalize(promptResponse, subject, bodyContent, tags);
            return promptResponse;
        }
        String outputText = assistantText(outputValue);
        if (StringUtils.isNotBlank(outputText)) {
            return RednoteCoverImagePromptResponseSupport.parseAndNormalize(outputText, subject, bodyContent, tags);
        }
        if (outputValue != null) {
            return RednoteCoverImagePromptResponseSupport.parseAndNormalize(JSONUtil.toJsonStr(outputValue),
                    subject, bodyContent, tags);
        }
        return RednoteCoverImagePromptResponseSupport.parseAndNormalize(latestAssistantText(state),
                subject, bodyContent, tags);
    }

    /**
     * 从 Agent state 中读取必填 taskId。
     */
    private String requiredTaskId(OverAllState state) {
        String taskId = stringValue(state, RednoteWorkflowState.KEY_TASK_ID);
        if (StringUtils.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "小红书任务 id 不能为空");
        }
        return taskId;
    }

    /**
     * 从 Agent state 中读取字符串字段。
     */
    private String stringValue(OverAllState state, String key) {
        Object value = state.value(key).orElse(null);
        return value == null ? null : value.toString();
    }

    /**
     * 从 Agent state 中读取字符串列表字段。
     */
    private List<String> listValue(OverAllState state, String key) {
        Object value = state.value(key).orElse(null);
        if (value instanceof Iterable<?> iterable) {
            List<String> values = new ArrayList<>();
            for (Object item : iterable) {
                if (item != null && StringUtils.isNotBlank(item.toString())) {
                    values.add(item.toString().trim());
                }
            }
            return values;
        }
        if (value == null || StringUtils.isBlank(value.toString())) {
            return List.of();
        }
        String text = value.toString().trim();
        if (JSONUtil.isTypeJSONArray(text)) {
            List<String> values = new ArrayList<>();
            for (Object item : JSONUtil.parseArray(text)) {
                if (item != null && StringUtils.isNotBlank(item.toString())) {
                    values.add(item.toString().trim());
                }
            }
            return values;
        }
        return List.of(text);
    }

    /**
     * 获取最后一条 AssistantMessage 文本作为 Agent 最终输出。
     */
    private String latestAssistantText(OverAllState state) {
        Object messagesValue = state.value("messages")
                .orElseThrow(() -> new IllegalStateException("封面图片提示词 Agent state 缺少 messages"));
        if (!(messagesValue instanceof List<?> messages)) {
            throw new IllegalStateException("封面图片提示词 Agent messages 类型不正确");
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            String assistantText = assistantText(messages.get(index));
            if (StringUtils.isNotBlank(assistantText)) {
                return assistantText;
            }
        }
        throw new IllegalStateException("封面图片提示词 Agent 未返回 AssistantMessage");
    }

    /**
     * 兼容 AssistantMessage 以及旧 checkpoint 反序列化后的 Map 文本结构。
     */
    private String assistantText(Object value) {
        if (value instanceof AssistantMessage assistantMessage) {
            return assistantMessage.getText();
        }
        if (value instanceof Message message) {
            return message.getText();
        }
        if (value instanceof Map<?, ?> map) {
            Object text = map.get("text");
            if (text == null) {
                text = map.get("content");
            }
            return text == null ? null : text.toString();
        }
        return value instanceof String text ? text : null;
    }

    /**
     * 确认数据库更新成功，否则说明 taskId 不存在或被逻辑删除。
     */
    private void ensureUpdated(boolean updated) {
        if (!updated) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "小红书任务不存在或状态更新失败");
        }
    }
}
