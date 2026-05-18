package com.sakura.passage_creator.rednote.workflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.sakura.passage_creator.rednote.service.RednoteNotePersistenceService;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Rednote workflow 完成节点，统一在普通图和封面图分支都结束后标记完成。
 */
@Component
public class RednoteWorkflowCompleteNodeHandler implements NodeAction {

    private final RednoteNotePersistenceService rednoteNotePersistenceService;

    public RednoteWorkflowCompleteNodeHandler(RednoteNotePersistenceService rednoteNotePersistenceService) {
        this.rednoteNotePersistenceService = rednoteNotePersistenceService;
    }

    /**
     * 两个图片生成分支汇聚后再结束流程，避免任一分支提前把任务置为完成。
     */
    @Override
    public Map<String, Object> apply(OverAllState stateSnapshot) {
        String taskId = requiredString(stateSnapshot, RednoteWorkflowState.KEY_TASK_ID);
        rednoteNotePersistenceService.markCompleted(taskId);
        return Map.of();
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
}
