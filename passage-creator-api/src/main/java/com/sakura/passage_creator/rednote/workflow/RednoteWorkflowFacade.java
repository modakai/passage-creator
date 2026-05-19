package com.sakura.passage_creator.rednote.workflow;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.sakura.passage_creator.creation.workflow.WorkflowContext;
import com.sakura.passage_creator.creation.workflow.WorkflowEvent;
import com.sakura.passage_creator.creation.workflow.WorkflowEventPublisher;
import com.sakura.passage_creator.creation.workflow.WorkflowTask;
import com.sakura.passage_creator.creation.workflow.enums.CreationTypeEnum;
import com.sakura.passage_creator.creation.workflow.enums.WorkflowEventTypeEnum;
import com.sakura.passage_creator.creation.workflow.enums.WorkflowStatusEnum;
import com.sakura.passage_creator.creation.workflow.service.WorkflowTaskStore;
import com.sakura.passage_creator.rednote.constant.UniversalConstant;
import com.sakura.passage_creator.rednote.model.dto.RednoteCreateRequest;
import com.sakura.passage_creator.rednote.model.entity.RednoteNote;
import com.sakura.passage_creator.rednote.service.RednoteNotePersistenceService;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 小红书 workflow 门面，负责创建业务任务、启动 StateGraph、同步 workflow_task 和 SSE。
 */
@Service
@Slf4j
public class RednoteWorkflowFacade {

    private final RednoteNotePersistenceService rednoteNoteService;
    private final RednoteWorkflowGraphFactory graphFactory;
    private final WorkflowTaskStore workflowTaskStore;
    private final WorkflowEventPublisher eventPublisher;
    private final Executor rednoteExecutor;

    public RednoteWorkflowFacade(RednoteNotePersistenceService rednoteNoteService,
            RednoteWorkflowGraphFactory graphFactory,
            WorkflowTaskStore workflowTaskStore,
            WorkflowEventPublisher eventPublisher,
            @Qualifier("rednoteExecutor") Executor rednoteExecutor) {
        this.rednoteNoteService = rednoteNoteService;
        this.graphFactory = graphFactory;
        this.workflowTaskStore = workflowTaskStore;
        this.eventPublisher = eventPublisher;
        // Rednote workflow 使用独立线程池，避免和文章任务互相挤占队列。
        this.rednoteExecutor = rednoteExecutor;
    }

    /**
     * 创建小红书 workflow 并异步启动全自动生成流程。
     */
    public String createRednoteWorkflow(RednoteCreateRequest request, LoginUserInfo loginUser) {
        String taskId = rednoteNoteService.createRednote(request.getContent(), loginUser);
        RednoteNote note = rednoteNoteService.getOwnedRednoteByTaskId(taskId, loginUser);
        WorkflowContext context = WorkflowContext.fromMap(Map.of(
                "taskId", taskId,
                "userId", loginUser.userId(),
                "content", request.getContent()
        ));
        workflowTaskStore.create(WorkflowTask.builder()
                .taskId(taskId)
                .bizType(CreationTypeEnum.REDNOTE.getValue())
                .bizId(note.getId())
                .userId(loginUser.userId())
                .status(WorkflowStatusEnum.PENDING.getValue())
                .currentNode(UniversalConstant.SEARCH_AGENT_NAME)
                .contextJson(context.toJson())
                .build());
        publish(taskId, WorkflowEventTypeEnum.WORKFLOW_STARTED.getValue(), null, context.getValues());
        submitRun(taskId, false);
        return taskId;
    }

    /**
     * 失败任务重新生成：沿用业务 taskId，但重置业务状态和 workflow 快照后从 START 重新执行。
     */
    public boolean retryFailedNode(String taskId, LoginUserInfo loginUser) {
        RednoteNote note = rednoteNoteService.getOwnedRednoteByTaskId(taskId, loginUser);
        rednoteNoteService.resetForRetry(taskId);
        WorkflowContext context = WorkflowContext.fromMap(Map.of(
                "taskId", taskId,
                "userId", loginUser.userId(),
                "content", note.getContent()
        ));
        WorkflowTask task = workflowTaskStore.getRequired(taskId);
        markTask(task, WorkflowStatusEnum.PENDING.getValue(), UniversalConstant.SEARCH_AGENT_NAME, context.toJson(), null);
        publish(taskId, WorkflowEventTypeEnum.NODE_RETRYING.getValue(), UniversalConstant.SEARCH_AGENT_NAME,
                Map.of("reason", "用户重新生成失败任务"));
        submitRun(taskId, false);
        return true;
    }

    /**
     * SSE 重连时补发当前 rednote 业务快照。
     */
    public void publishSnapshot(String taskId, LoginUserInfo loginUser) {
        RednoteNote note = rednoteNoteService.getOwnedRednoteByTaskId(taskId, loginUser);
        publish(taskId, WorkflowEventTypeEnum.NODE_RESULT.getValue(), note.getPhase(),
                Map.of("snapshot", rednoteNoteService.getRednoteVO(note)));
    }

    private void submitRun(String taskId, boolean resume) {
        rednoteExecutor.execute(() -> {
            try {
                // Spring @Async 无法直接包住 private 方法，这里显式交给 rednote 任务线程池执行。
                runGraph(taskId, resume);
            } catch (RuntimeException e) {
                log.error("小红书 workflow 执行失败, taskId={}", taskId, e);
            }
        });
    }

    private void runGraph(String taskId, boolean resume) {
        WorkflowTask task = workflowTaskStore.getRequired(taskId);
        markTask(task, WorkflowStatusEnum.PROCESSING.getValue(), task.getCurrentNode(), null, null);
        try {
            if (!resume) {
                // Spring AI Alibaba 会按 threadId 自动合并 checkpoint；全新执行必须先清掉旧快照。
                graphFactory.clearCheckpoint(taskId);
            }
            CompiledGraph graph = graphFactory.compile(new RednoteWorkflowLifecycleListener(taskId));
            RunnableConfig config = resume
                    // Rednote 计费需要同步 ChatResponse 才能读取 token usage，禁用 Agent 默认流式模型调用。
                    ? RunnableConfig.builder().threadId(taskId).resume().addMetadata("_stream_", false).build()
                    : RunnableConfig.builder().threadId(taskId).addMetadata("_stream_", false).build();
            Map<String, Object> input = resume ? Map.of() : WorkflowContext.fromJson(task.getContextJson()).getValues();
            NodeOutput output = graph.invokeAndGetOutput(input, config)
                    .orElseThrow(() -> new IllegalStateException("小红书 workflow 没有返回执行结果"));
            handleGraphOutput(taskId, output);
        } catch (Exception e) {
            failTask(taskId, e);
            throw new IllegalStateException("小红书 workflow 执行失败", e);
        }
    }

    private void handleGraphOutput(String taskId, NodeOutput output) {
        if (output.isEND()) {
            Map<String, Object> state = output.state().data();
            WorkflowTask task = workflowTaskStore.getRequired(taskId);
            markTask(task, WorkflowStatusEnum.COMPLETED.getValue(), output.node(), WorkflowContext.fromMap(state).toJson(), null);
            // Rednote 是全自动流程，完成后不保留可恢复 checkpoint，避免后续同 taskId 误读旧状态。
            graphFactory.clearCheckpoint(taskId);
            publish(taskId, WorkflowEventTypeEnum.WORKFLOW_COMPLETED.getValue(), output.node(), state);
        }
    }

    private void failTask(String taskId, Exception e) {
        WorkflowTask task = workflowTaskStore.getRequired(taskId);
        markTask(task, WorkflowStatusEnum.FAILED.getValue(), task.getCurrentNode(), task.getContextJson(), e.getMessage());
        rednoteNoteService.markFailed(taskId, e.getMessage());
        publish(taskId, WorkflowEventTypeEnum.WORKFLOW_FAILED.getValue(), task.getCurrentNode(),
                Map.of("error", e.getMessage()));
    }

    private void markTask(WorkflowTask task, String status, String currentNode, String contextJson, String errorMessage) {
        task.setStatus(status);
        task.setCurrentNode(currentNode);
        if (contextJson != null) {
            task.setContextJson(contextJson);
        }
        task.setErrorMessage(errorMessage);
        workflowTaskStore.update(task);
    }

    private void publish(String taskId, String type, String nodeType, Map<String, Object> payload) {
        eventPublisher.publish(WorkflowEvent.builder()
                .type(type)
                .taskId(taskId)
                .bizType(CreationTypeEnum.REDNOTE.getValue())
                .nodeType(nodeType)
                .payload(payload == null ? Map.of() : payload)
                .eventTime(LocalDateTime.now())
                .build());
    }

    /**
     * Graph 生命周期监听器，用于把 Spring AI Alibaba 节点事件同步到 workflow_task 和 SSE。
     */
    private class RednoteWorkflowLifecycleListener implements GraphLifecycleListener {

        private final String taskId;

        private RednoteWorkflowLifecycleListener(String taskId) {
            this.taskId = taskId;
        }

        @Override
        public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long timestamp) {
            WorkflowTask task = workflowTaskStore.getRequired(taskId);
            markTask(task, WorkflowStatusEnum.PROCESSING.getValue(), nodeId, WorkflowContext.fromMap(state).toJson(), null);
            publish(taskId, WorkflowEventTypeEnum.NODE_STARTED.getValue(), nodeId, state);
        }

        @Override
        public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long timestamp) {
            WorkflowTask task = workflowTaskStore.getRequired(taskId);
            markTask(task, WorkflowStatusEnum.PROCESSING.getValue(), nodeId, WorkflowContext.fromMap(state).toJson(), null);
            publish(taskId, WorkflowEventTypeEnum.NODE_RESULT.getValue(), nodeId, state);
        }
    }
}
