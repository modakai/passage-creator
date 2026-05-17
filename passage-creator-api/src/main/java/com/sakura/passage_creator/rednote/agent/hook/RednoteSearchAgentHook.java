package com.sakura.passage_creator.rednote.agent.hook;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.mybatisflex.core.query.QueryWrapper;
import com.sakura.passage_creator.rednote.agent.tool.search.RednoteSearchResponseSupport;
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
 * SearchAgent 生命周期 Hook，负责在 Agent 前后同步 rednote 业务状态。
 */
@Component
@HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
public class RednoteSearchAgentHook extends AgentHook {

    private final RednoteNoteMapper rednoteNoteMapper;

    public RednoteSearchAgentHook(RednoteNoteMapper rednoteNoteMapper) {
        this.rednoteNoteMapper = rednoteNoteMapper;
    }

    @Override
    public String getName() {
        return "rednote_search_agent_persistence";
    }

    /**
     * SearchAgent 开始前，把业务任务推进到搜索阶段。
     */
    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        String taskId = stringValue(state, RednoteWorkflowState.KEY_TASK_ID);
        RednoteNote note = getNoteByTaskId(taskId);
        note.setStatus(RednoteStatusEnum.PROCESSING.getValue());
        note.setPhase(RednotePhaseEnum.SEARCH_AGENT.getValue());
        note.setErrorMessage(null);
        rednoteNoteMapper.update(note);
        return CompletableFuture.completedFuture(Map.of());
    }

    /**
     * SearchAgent 完成后，解析最终 RednoteBrief，落库并把扁平字段返回给 Graph state。
     */
    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        String taskId = stringValue(state, RednoteWorkflowState.KEY_TASK_ID);
        String content = stringValue(state, RednoteWorkflowState.KEY_CONTENT);

        RednoteNote note = getNoteByTaskId(taskId);

        RednoteWorkflowState.SearchResponse searchResponse = RednoteSearchResponseSupport.parseAndNormalize(latestAssistantText(state), content);

        note.setSubject(searchResponse.getSubject());
        note.setContext(searchResponse.getContext());
        note.setContentLength(searchResponse.getContentLength());
        note.setTargetWordCount(searchResponse.getTargetWordCount());
        note.setKeywords(JSONUtil.toJsonStr(searchResponse.getKeywords()));
        note.setTagCount(searchResponse.getTagCount());
        note.setImageCount(searchResponse.getImageCount());
        note.setSearchResults(JSONUtil.toJsonStr(searchResponse.getSearchResults()));
        note.setStatus(RednoteStatusEnum.PROCESSING.getValue());
        note.setPhase(RednotePhaseEnum.COPY_GENERATING.getValue());
        note.setErrorMessage(null);
        rednoteNoteMapper.update(note);

        return CompletableFuture.completedFuture(RednoteSearchResponseSupport.toStateUpdates(searchResponse));
    }

    /**
     * 从 Agent state 中读取字符串字段。
     */
    private String stringValue(OverAllState state, String key) {
        Object value = state.value(key).orElse(null);
        return value == null ? null : value.toString();
    }

    /**
     * 获取最后一条 AssistantMessage 文本作为 Agent 最终输出。
     */
    private String latestAssistantText(OverAllState state) {
        Object messagesValue = state.value("messages")
                .orElseThrow(() -> new IllegalStateException("SearchAgent state 缺少 messages"));
        if (!(messagesValue instanceof List<?> messages)) {
            throw new IllegalStateException("SearchAgent messages 类型不正确");
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            Message message = (Message) messages.get(index);
            if (message instanceof AssistantMessage assistantMessage && StringUtils.isNotBlank(assistantMessage.getText())) {
                return assistantMessage.getText();
            }
        }
        throw new IllegalStateException("SearchAgent 未返回 AssistantMessage");
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
