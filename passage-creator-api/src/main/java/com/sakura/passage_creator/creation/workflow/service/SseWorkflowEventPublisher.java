package com.sakura.passage_creator.creation.workflow.service;

import cn.hutool.json.JSONUtil;
import com.sakura.passage_creator.article.model.dto.SseMessage;
import com.sakura.passage_creator.article.model.enums.SseMessageTypeEnum;
import com.sakura.passage_creator.creation.workflow.WorkflowEvent;
import com.sakura.passage_creator.creation.workflow.WorkflowEventPublisher;
import com.sakura.passage_creator.creation.workflow.enums.CreationTypeEnum;
import com.sakura.passage_creator.creation.workflow.enums.WorkflowEventTypeEnum;
import com.sakura.passage_creator.article.workflow.ArticleWorkflowNodeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 基于 SSE 的 workflow 事件发布器。
 */
@Component
@Slf4j
public class SseWorkflowEventPublisher implements WorkflowEventPublisher {

    private final WorkflowSseEmitterManager sseEmitterManager;

    public SseWorkflowEventPublisher(WorkflowSseEmitterManager sseEmitterManager) {
        this.sseEmitterManager = sseEmitterManager;
    }

    @Override
    public void publish(WorkflowEvent event) {
        try {
            sseEmitterManager.send(event.getTaskId(), JSONUtil.toJsonStr(event));
            sendArticleCompatibilityEvent(event);
            completeIfTerminalEvent(event);
        }
        catch (RuntimeException e) {
            // SSE 推送失败不能反向破坏 workflow 状态，前端可通过进度接口恢复快照。
            log.warn("workflow SSE 事件推送失败, taskId={}, type={}", event.getTaskId(), event.getType(), e);
        }
    }

    /**
     * 迁移期兼容旧文章 SSE 事件，避免前端立即适配通用 workflow payload。
     */
    private void sendArticleCompatibilityEvent(WorkflowEvent event) {
        if (!CreationTypeEnum.ARTICLE.getValue().equals(event.getBizType())) {
            return;
        }
        sendPhaseChangedIfNeeded(event);
        if (WorkflowEventTypeEnum.NODE_RESULT.getValue().equals(event.getType())) {
            sendArticleNodeResult(event);
        }
        if (WorkflowEventTypeEnum.NODE_WAITING_USER.getValue().equals(event.getType())) {
            sendArticleWaitingUser(event);
        }
        if (WorkflowEventTypeEnum.WORKFLOW_COMPLETED.getValue().equals(event.getType())) {
            Object fullContent = event.getPayload().get("fullContent");
            if (fullContent != null) {
                send(event.getTaskId(), SseMessageTypeEnum.ALL_COMPLETE, fullContent);
            }
        }
        if (WorkflowEventTypeEnum.WORKFLOW_FAILED.getValue().equals(event.getType())) {
            send(event.getTaskId(), SseMessageTypeEnum.ERROR, event.getPayload().get("error"));
        }
    }

    /**
     * 旧前端通过 PHASE_CHANGED 驱动步骤条，节点开始时补发阶段变化。
     */
    private void sendPhaseChangedIfNeeded(WorkflowEvent event) {
        if (!WorkflowEventTypeEnum.NODE_STARTED.getValue().equals(event.getType())) {
            return;
        }
        if (ArticleWorkflowNodeType.TITLE_GENERATING.getValue().equals(event.getNodeType())
                || ArticleWorkflowNodeType.OUTLINE_GENERATING.getValue().equals(event.getNodeType())
                || ArticleWorkflowNodeType.CONTENT_GENERATING.getValue().equals(event.getNodeType())
                || ArticleWorkflowNodeType.IMAGE_ANALYZING.getValue().equals(event.getNodeType())
                || ArticleWorkflowNodeType.IMAGE_GENERATING.getValue().equals(event.getNodeType())
                || ArticleWorkflowNodeType.CONTENT_MERGING.getValue().equals(event.getNodeType())) {
            send(event.getTaskId(), SseMessageTypeEnum.PHASE_CHANGED, event.getNodeType());
        }
    }

    /**
     * 将通用节点结果映射回文章旧事件。
     */
    private void sendArticleNodeResult(WorkflowEvent event) {
        Map<String, Object> payload = event.getPayload();
        if (ArticleWorkflowNodeType.TITLE_GENERATING.getValue().equals(event.getNodeType())) {
            send(event.getTaskId(), SseMessageTypeEnum.TITLES_GENERATED, payload.get("titleOptions"));
        }
        else if (ArticleWorkflowNodeType.OUTLINE_GENERATING.getValue().equals(event.getNodeType())) {
            send(event.getTaskId(), SseMessageTypeEnum.OUTLINE_GENERATED, payload.get("outline"));
        }
        else if (ArticleWorkflowNodeType.IMAGE_ANALYZING.getValue().equals(event.getNodeType())) {
            send(event.getTaskId(), SseMessageTypeEnum.IMAGE_ANALYZED, payload.get("imageRequirements"));
        }
        else if (ArticleWorkflowNodeType.IMAGE_GENERATING.getValue().equals(event.getNodeType())) {
            send(event.getTaskId(), SseMessageTypeEnum.IMAGE_GENERATED, payload.get("images"));
        }
        else if (ArticleWorkflowNodeType.CONTENT_MERGING.getValue().equals(event.getNodeType())) {
            send(event.getTaskId(), SseMessageTypeEnum.MERGE_COMPLETE, payload.get("fullContent"));
        }
    }

    /**
     * 将重连补发的人工等待态转换成旧前端能直接消费的文章事件。
     */
    @SuppressWarnings("unchecked")
    private void sendArticleWaitingUser(WorkflowEvent event) {
        Object snapshot = event.getPayload().get("inputSnapshot");
        if (!(snapshot instanceof Map<?, ?> inputSnapshot)) {
            return;
        }
        if (ArticleWorkflowNodeType.TITLE_CONFIRM.getValue().equals(event.getNodeType())) {
            send(event.getTaskId(), SseMessageTypeEnum.TITLES_GENERATED, inputSnapshot.get("titleOptions"));
        }
        else if (ArticleWorkflowNodeType.OUTLINE_CONFIRM.getValue().equals(event.getNodeType())) {
            send(event.getTaskId(), SseMessageTypeEnum.OUTLINE_GENERATED, inputSnapshot.get("outline"));
        }
    }

    private void send(String taskId, SseMessageTypeEnum type, Object payload) {
        if (payload != null) {
            sseEmitterManager.send(taskId, JSONUtil.toJsonStr(SseMessage.of(type, payload)));
        }
    }

    /**
     * workflow 进入终态后主动关闭 SSE，避免连接和 emitterMap 条目滞留到超时。
     */
    private void completeIfTerminalEvent(WorkflowEvent event) {
        if (WorkflowEventTypeEnum.WORKFLOW_COMPLETED.getValue().equals(event.getType())
                || WorkflowEventTypeEnum.WORKFLOW_FAILED.getValue().equals(event.getType())
                || WorkflowEventTypeEnum.WORKFLOW_EXPIRED.getValue().equals(event.getType())) {
            sseEmitterManager.complete(event.getTaskId());
        }
    }
}
