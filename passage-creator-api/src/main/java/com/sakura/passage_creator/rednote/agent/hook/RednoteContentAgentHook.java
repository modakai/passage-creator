package com.sakura.passage_creator.rednote.agent.hook;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.mybatisflex.core.query.QueryWrapper;
import com.sakura.passage_creator.rednote.agent.tool.content.RednoteContentResponseSupport;
import com.sakura.passage_creator.rednote.model.entity.RednoteNote;
import com.sakura.passage_creator.rednote.model.enums.RednotePhaseEnum;
import com.sakura.passage_creator.rednote.model.enums.RednoteStatusEnum;
import com.sakura.passage_creator.rednote.repository.RednoteNoteMapper;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.sakura.passage_creator.rednote.model.entity.table.RednoteNoteTableDef.REDNOTE_NOTE;

/**
 * ContentAgent 生命周期 Hook，负责把正文和标签写入 rednote_note。
 */
@Component
@HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
public class RednoteContentAgentHook extends AgentHook {

    private final RednoteNoteMapper rednoteNoteMapper;

    public RednoteContentAgentHook(RednoteNoteMapper rednoteNoteMapper) {
        this.rednoteNoteMapper = rednoteNoteMapper;
    }

    @Override
    public String getName() {
        return "rednote_content_agent_persistence";
    }

    /**
     * ContentAgent 开始前，把业务任务推进到文案生成阶段。
     */
    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        String taskId = stringValue(state, RednoteWorkflowState.KEY_TASK_ID);
        RednoteNote note = getNoteByTaskId(taskId);
        note.setStatus(RednoteStatusEnum.PROCESSING.getValue());
        note.setPhase(RednotePhaseEnum.COPY_GENERATING.getValue());
        note.setErrorMessage(null);
        rednoteNoteMapper.update(note);
        return CompletableFuture.completedFuture(Map.of());
    }

    /**
     * ContentAgent 完成后，只保存正文主体和标签。
     */
    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        String taskId = stringValue(state, RednoteWorkflowState.KEY_TASK_ID);
        Integer tagCount = integerValue(state, RednoteWorkflowState.KEY_TAG_COUNT);

        RednoteWorkflowState.ContentResponse contentResponse = resolveContentResponse(state, tagCount);
        RednoteNote note = getNoteByTaskId(taskId);
        note.setBodyContent(contentResponse.getBodyContent());
        note.setTags(JSONUtil.toJsonStr(contentResponse.getTags()));
        note.setStatus(RednoteStatusEnum.PROCESSING.getValue());
        note.setPhase(RednotePhaseEnum.IMAGE_PROMPT_GENERATING.getValue());
        note.setErrorMessage(null);
        rednoteNoteMapper.update(note);

        return CompletableFuture.completedFuture(RednoteContentResponseSupport.toStateUpdates(contentResponse));
    }

    /**
     * 优先读取 outputKey 中的结构化结果，失败时读取最后一条 AssistantMessage。
     */
    private RednoteWorkflowState.ContentResponse resolveContentResponse(OverAllState state, Integer tagCount) {
        Object outputValue = state.value(RednoteWorkflowState.KEY_COPYWRITING).orElse(null);
        if (outputValue instanceof RednoteWorkflowState.ContentResponse contentResponse) {
            RednoteContentResponseSupport.normalizeContent(contentResponse, tagCount);
            return contentResponse;
        }
        String outputText = assistantText(outputValue);
        if (StringUtils.isNotBlank(outputText)) {
            return RednoteContentResponseSupport.parseAndNormalize(outputText, tagCount);
        }
        if (outputValue != null) {
            return RednoteContentResponseSupport.parseAndNormalize(JSONUtil.toJsonStr(outputValue), tagCount);
        }
        return RednoteContentResponseSupport.parseAndNormalize(latestAssistantText(state), tagCount);
    }

    /**
     * 从 Agent state 中读取字符串字段。
     */
    private String stringValue(OverAllState state, String key) {
        Object value = state.value(key).orElse(null);
        return value == null ? null : value.toString();
    }

    /**
     * 从 Agent state 中读取整数字段。
     */
    private Integer integerValue(OverAllState state, String key) {
        Object value = state.value(key).orElse(null);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || StringUtils.isBlank(value.toString())) {
            return null;
        }
        return Integer.parseInt(value.toString());
    }

    /**
     * 获取最后一条 AssistantMessage 文本作为 Agent 最终输出。
     */
    private String latestAssistantText(OverAllState state) {
        Object messagesValue = state.value("messages")
                .orElseThrow(() -> new IllegalStateException("ContentAgent state 缺少 messages"));
        if (!(messagesValue instanceof List<?> messages)) {
            throw new IllegalStateException("ContentAgent messages 类型不正确");
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            String assistantText = assistantText(messages.get(index));
            if (StringUtils.isNotBlank(assistantText)) {
                return assistantText;
            }
        }
        throw new IllegalStateException("ContentAgent 未返回 AssistantMessage");
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
     * 根据 taskId 获取小红书任务，不存在时抛出业务异常。
     */
    private RednoteNote getNoteByTaskId(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "小红书任务 id 不能为空");
        }
        RednoteNote note = rednoteNoteMapper.selectOneByQuery(QueryWrapper.create()
                .where(REDNOTE_NOTE.TASK_ID.eq(taskId)));
        if (note == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "小红书任务不存在");
        }
        return note;
    }
}
