package com.sakura.passage_creator.rednote.workflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.sakura.passage_creator.rednote.model.enums.RednotePhaseEnum;
import com.sakura.passage_creator.rednote.model.enums.RednoteStatusEnum;
import com.sakura.passage_creator.rednote.service.RednoteImageGenerationService;
import com.sakura.passage_creator.rednote.service.RednoteNotePersistenceService;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 封面图生成节点，基于封面提示词生成单张封面图。
 */
@Component
public class RednoteCoverImageGenerateNodeHandler implements NodeAction {

    private final RednoteImageGenerationService rednoteImageGenerationService;

    private final RednoteNotePersistenceService rednoteNotePersistenceService;

    public RednoteCoverImageGenerateNodeHandler(RednoteImageGenerationService rednoteImageGenerationService,
                                                RednoteNotePersistenceService rednoteNotePersistenceService) {
        this.rednoteImageGenerationService = rednoteImageGenerationService;
        this.rednoteNotePersistenceService = rednoteNotePersistenceService;
    }

    /**
     * 封面图分支独立执行，和普通配图分支在 Graph 层并行。
     */
    @Override
    public Map<String, Object> apply(OverAllState stateSnapshot) {
        String taskId = requiredString(stateSnapshot, RednoteWorkflowState.KEY_TASK_ID);
        Long userId = requiredLong(stateSnapshot, RednoteWorkflowState.KEY_USER_ID);
        String coverPrompt = requiredString(stateSnapshot, RednoteWorkflowState.KEY_COVER_PROMPT);
        rednoteNotePersistenceService.markPhase(taskId,
                RednoteStatusEnum.PROCESSING.getValue(),
                RednotePhaseEnum.IMAGE_GENERATING.getValue());
        RednoteWorkflowState.RednoteImageResult coverImageResult =
                rednoteImageGenerationService.generate(taskId, userId, coverPrompt, 0, "COVER");
        rednoteNotePersistenceService.saveCoverImage(taskId,
                coverImageResult.getUrl(),
                RednoteStatusEnum.PROCESSING.getValue(),
                RednotePhaseEnum.IMAGE_GENERATING.getValue());

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(RednoteWorkflowState.KEY_COVER_IMAGE_RESULT, coverImageResult);
        if (StringUtils.isNotBlank(coverImageResult.getUrl())) {
            updates.put(RednoteWorkflowState.KEY_COVER_IMAGE, coverImageResult.getUrl());
        }
        return updates;
    }

    /**
     * 读取必填字符串状态。
     */
    private String requiredString(OverAllState stateSnapshot, String key) {
        Object value = stateSnapshot.value(key).orElse(null);
        if (value == null || StringUtils.isBlank(value.toString())) {
            throw new IllegalStateException("缺少 rednote workflow 状态：" + key);
        }
        return value.toString();
    }

    /**
     * 读取必填 Long 状态。
     */
    private Long requiredLong(OverAllState stateSnapshot, String key) {
        Object value = stateSnapshot.value(key).orElse(null);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null && StringUtils.isNotBlank(value.toString())) {
            return Long.parseLong(value.toString());
        }
        throw new IllegalStateException("缺少 rednote workflow 状态：" + key);
    }
}
