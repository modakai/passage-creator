package com.sakura.passage_creator.rednote.workflow;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.sakura.passage_creator.rednote.model.entity.RednoteNote;
import com.sakura.passage_creator.rednote.model.enums.RednotePhaseEnum;
import com.sakura.passage_creator.rednote.model.enums.RednoteStatusEnum;
import com.sakura.passage_creator.rednote.service.RednoteNotePersistenceService;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片提示词汇总节点，确认普通配图提示词和封面提示词都已经写入 Graph state。
 */
@Component
public class RednoteImagePromptCompleteNodeHandler implements NodeAction {

    private final RednoteNotePersistenceService rednoteNotePersistenceService;

    public RednoteImagePromptCompleteNodeHandler(RednoteNotePersistenceService rednoteNotePersistenceService) {
        this.rednoteNotePersistenceService = rednoteNotePersistenceService;
    }

    /**
     * 两个提示词分支都完成后才允许进入图片生成并行节点。
     */
    @Override
    public Map<String, Object> apply(OverAllState stateSnapshot) {
        String taskId = requiredString(stateSnapshot, RednoteWorkflowState.KEY_TASK_ID);
        Map<String, Object> updates = resolvePromptState(taskId, stateSnapshot);
        rednoteNotePersistenceService.markPhase(taskId,
                RednoteStatusEnum.PROCESSING.getValue(),
                RednotePhaseEnum.IMAGE_GENERATING.getValue());
        return updates;
    }

    /**
     * 读取必填字符串状态。
     */
    private String requiredString(OverAllState stateSnapshot, String key) {
        Object value = requiredState(stateSnapshot, key, "缺少 rednote workflow 状态：" + key);
        return value.toString();
    }

    /**
     * 校验必填状态，避免图片生成节点拿不到前置节点输出。
     */
    private Object requiredState(OverAllState stateSnapshot, String key, String message) {
        Object value = stateSnapshot.value(key).orElse(null);
        if (value == null || StringUtils.isBlank(value.toString())) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    /**
     * 提示词 Agent Hook 已经落库，Graph 并行合并缺字段时从 rednote_note 兜底恢复。
     */
    private Map<String, Object> resolvePromptState(String taskId, OverAllState stateSnapshot) {
        Map<String, Object> updates = new LinkedHashMap<>();
        Object imagePrompts = stateSnapshot.value(RednoteWorkflowState.KEY_IMAGE_PROMPTS).orElse(null);
        Object coverPrompt = stateSnapshot.value(RednoteWorkflowState.KEY_COVER_PROMPT).orElse(null);

        if (isBlankState(imagePrompts) || isBlankState(coverPrompt)) {
            RednoteNote note = rednoteNotePersistenceService.getRednoteByTaskId(taskId);
            if (isBlankState(imagePrompts) && StringUtils.isNotBlank(note.getImagePrompts())) {
                List<RednoteWorkflowState.ImagePromptItem> promptItems =
                        JSONUtil.toList(note.getImagePrompts(), RednoteWorkflowState.ImagePromptItem.class);
                updates.put(RednoteWorkflowState.KEY_IMAGE_PROMPTS, promptItems);
                imagePrompts = promptItems;
            }
            if (isBlankState(coverPrompt) && StringUtils.isNotBlank(note.getCoverPrompt())) {
                updates.put(RednoteWorkflowState.KEY_COVER_PROMPT, note.getCoverPrompt());
                coverPrompt = note.getCoverPrompt();
            }
            if (StringUtils.isNotBlank(note.getCoverTitle())) {
                updates.put(RednoteWorkflowState.KEY_COVER_TITLE, note.getCoverTitle());
            }
        }

        if (isBlankState(imagePrompts)) {
            throw new IllegalStateException("缺少普通配图提示词");
        }
        if (isBlankState(coverPrompt)) {
            throw new IllegalStateException("缺少封面图提示词");
        }
        return updates;
    }

    /**
     * 判断 Graph state 中的值是否为空，兼容集合和字符串。
     */
    private boolean isBlankState(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Iterable<?> iterable) {
            return !iterable.iterator().hasNext();
        }
        return StringUtils.isBlank(value.toString());
    }
}
