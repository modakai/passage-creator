package com.sakura.passage_creator.article.workflow;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.dto.ArticleConfirmOutlineRequest;
import com.sakura.passage_creator.article.model.dto.ArticleConfirmTitleRequest;
import com.sakura.passage_creator.article.model.dto.ArticleCreateRequest;
import com.sakura.passage_creator.article.model.entity.Article;
import com.sakura.passage_creator.article.model.enums.ArticlePhaseEnum;
import com.sakura.passage_creator.article.model.enums.ArticleStatusEnum;
import com.sakura.passage_creator.article.service.ArticleService;
import com.sakura.passage_creator.creation.workflow.WorkflowContext;
import com.sakura.passage_creator.creation.workflow.WorkflowEvent;
import com.sakura.passage_creator.creation.workflow.WorkflowEventPublisher;
import com.sakura.passage_creator.creation.workflow.WorkflowHumanTask;
import com.sakura.passage_creator.creation.workflow.WorkflowHumanTaskCompleteCommand;
import com.sakura.passage_creator.creation.workflow.WorkflowHumanTaskCreateCommand;
import com.sakura.passage_creator.creation.workflow.WorkflowTask;
import com.sakura.passage_creator.creation.workflow.config.CreationWorkflowProperties;
import com.sakura.passage_creator.creation.workflow.enums.CreationTypeEnum;
import com.sakura.passage_creator.creation.workflow.enums.WorkflowEventTypeEnum;
import com.sakura.passage_creator.creation.workflow.enums.WorkflowStatusEnum;
import com.sakura.passage_creator.creation.workflow.service.WorkflowHumanTaskService;
import com.sakura.passage_creator.creation.workflow.service.WorkflowTaskStore;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 文章 workflow 门面，使用 Spring AI Alibaba StateGraph 承接文章创作编排。
 */
@Service
@Slf4j
public class ArticleWorkflowFacade {

    private final ArticleService articleService;
    private final ArticleWorkflowAdapter articleWorkflowAdapter;
    private final ArticleWorkflowGraphFactory graphFactory;
    private final WorkflowTaskStore workflowTaskStore;
    private final WorkflowHumanTaskService humanTaskService;
    private final WorkflowEventPublisher eventPublisher;
    private final Executor articleExecutor;
    private final CreationWorkflowProperties workflowProperties;

    public ArticleWorkflowFacade(ArticleService articleService,
            ArticleWorkflowAdapter articleWorkflowAdapter,
            ArticleWorkflowGraphFactory graphFactory,
            WorkflowTaskStore workflowTaskStore,
            WorkflowHumanTaskService humanTaskService,
            WorkflowEventPublisher eventPublisher,
            @Qualifier("articleExecutor") Executor articleExecutor,
            CreationWorkflowProperties workflowProperties) {
        this.articleService = articleService;
        this.articleWorkflowAdapter = articleWorkflowAdapter;
        this.graphFactory = graphFactory;
        this.workflowTaskStore = workflowTaskStore;
        this.humanTaskService = humanTaskService;
        this.eventPublisher = eventPublisher;
        this.articleExecutor = articleExecutor;
        this.workflowProperties = workflowProperties;
    }

    /**
     * 创建文章 workflow 并异步启动 StateGraph。
     */
    public String createArticleWorkflow(ArticleCreateRequest request, LoginUserInfo loginUser) {
        String taskId = articleService.createArticle(request.getTopic(), request.getEnabledImageMethods(), loginUser);
        Article article = articleWorkflowAdapter.getArticleByTaskId(taskId);
        // context_json 是业务侧快照；真正的执行位置由 Spring AI Alibaba checkpoint 维护。
        WorkflowContext context = WorkflowContext.fromMap(Map.of(
                "taskId", taskId,
                "topic", request.getTopic(),
                "enabledImageMethods", request.getEnabledImageMethods() == null ? List.of() : request.getEnabledImageMethods()
        ));
        workflowTaskStore.create(WorkflowTask.builder()
                .taskId(taskId)
                .bizType(CreationTypeEnum.ARTICLE.getValue())
                .bizId(article.getId())
                .userId(loginUser.userId())
                .status(WorkflowStatusEnum.PENDING.getValue())
                .currentNode(ArticleWorkflowNodeType.TITLE_GENERATING.getValue())
                .contextJson(context.toJson())
                .build());
        publish(taskId, WorkflowEventTypeEnum.WORKFLOW_STARTED.getValue(), null, context.getValues());
        // 创建接口只返回 taskId，耗时的 Agent 调用统一在线程池里推进。
        submitRun(taskId, false);
        return taskId;
    }

    /**
     * 完成标题确认人工任务，并从标题确认节点继续执行。
     */
    public boolean confirmTitle(ArticleConfirmTitleRequest request, LoginUserInfo loginUser) {
        WorkflowHumanTask task = humanTaskService.getLatestWaitingTask(
                        request.getTaskId(), ArticleWorkflowNodeType.TITLE_CONFIRM.getValue())
                .orElseThrow(() -> new IllegalStateException("当前没有等待确认的标题任务"));
        Map<String, Object> result = new HashMap<>();
        result.put("selectedMainTitle", request.getSelectedMainTitle());
        result.put("selectedSubTitle", request.getSelectedSubTitle());
        result.put("userDescription", request.getUserDescription() == null ? "" : request.getUserDescription());
        ensureHumanTaskRecoverable(request.getTaskId(), loginUser, task);
        // 人工输入先写入 Graph checkpoint，resume 后 TITLE_CONFIRM 节点才能从状态中读到选择结果。
        updateCheckpointAndContext(request.getTaskId(), result);
        completeHumanTask(request.getTaskId(), ArticleWorkflowNodeType.TITLE_CONFIRM.getValue(), loginUser, task, result);
        submitRun(request.getTaskId(), true);
        return true;
    }

    /**
     * 完成大纲确认人工任务，并从大纲确认节点继续执行。
     */
    public boolean confirmOutline(ArticleConfirmOutlineRequest request, LoginUserInfo loginUser) {
        WorkflowHumanTask task = humanTaskService.getLatestWaitingTask(
                        request.getTaskId(), ArticleWorkflowNodeType.OUTLINE_CONFIRM.getValue())
                .orElseThrow(() -> new IllegalStateException("当前没有等待确认的大纲任务"));
        ArticleState.OutlineResult outline = request.getOutline();
        Map<String, Object> result = Map.of("confirmedOutline", outline);
        ensureHumanTaskRecoverable(request.getTaskId(), loginUser, task);
        // 大纲确认同样写入 checkpoint，确保后续正文节点使用用户最终确认的大纲。
        updateCheckpointAndContext(request.getTaskId(), result);
        completeHumanTask(request.getTaskId(), ArticleWorkflowNodeType.OUTLINE_CONFIRM.getValue(), loginUser, task, result);
        submitRun(request.getTaskId(), true);
        return true;
    }

    /**
     * 重试失败 workflow，优先沿用 Graph checkpoint 恢复。
     */
    public void retryFailedNode(String taskId) {
        submitRun(taskId, true);
    }

    /**
     * SSE 重连时补发可恢复的人工任务；若 checkpoint 或人工任务已过期，则显式发布过期事件。
     */
    public void publishPendingHumanTaskIfPresent(String taskId) {
        findPendingHumanTask(taskId).ifPresent(task -> {
            if (humanTaskService.isExpired(task) || !graphFactory.hasCheckpoint(taskId)) {
                expireWorkflow(taskId, task, "Workflow checkpoint 已过期，请重新生成");
                return;
            }
            publishWaitingHumanTask(taskId, task, WorkflowContext.fromJson(task.getInputSnapshotJson()).getValues());
        });
    }

    private void completeHumanTask(String taskId, String nodeType, LoginUserInfo loginUser,
            WorkflowHumanTask task, Map<String, Object> result) {
        humanTaskService.completeTask(WorkflowHumanTaskCompleteCommand.builder()
                .taskId(taskId)
                .nodeType(nodeType)
                .userId(loginUser.userId())
                .version(task.getVersion())
                .result(result)
                .build());
    }

    private void submitRun(String taskId, boolean resume) {
        articleExecutor.execute(() -> {
            try {
                // Spring @Async 无法直接包住 private 方法，这里显式交给文章线程池执行。
                runGraph(taskId, resume);
            }
            catch (RuntimeException e) {
                log.error("文章 workflow 执行失败, taskId={}", taskId, e);
            }
        });
    }

    private void runGraph(String taskId, boolean resume) {
        WorkflowTask task = workflowTaskStore.getRequired(taskId);
        if (WorkflowStatusEnum.EXPIRED.getValue().equals(task.getStatus())) {
            // 已过期任务不能被后台重试重新拉起，必须由用户显式重新生成。
            return;
        }
        markTask(task, WorkflowStatusEnum.PROCESSING.getValue(), task.getCurrentNode(), null, null);
        try {
            CompiledGraph graph = graphFactory.compile(new ArticleWorkflowLifecycleListener(taskId));
            RunnableConfig config = resume
                    // resume() 告诉框架从 threadId 对应的 checkpoint 继续，而不是从 START 重新开始。
                    ? RunnableConfig.builder().threadId(taskId).resume().build()
                    : RunnableConfig.builder().threadId(taskId).build();
            // 恢复执行时状态已经在 checkpoint 中，传空输入可以避免覆盖 checkpoint 里的人工结果。
            Map<String, Object> input = resume ? Map.of() : WorkflowContext.fromJson(task.getContextJson()).getValues();
            NodeOutput output = graph.invokeAndGetOutput(input, config)
                    .orElseThrow(() -> new IllegalStateException("文章 workflow 没有返回执行结果"));
            handleGraphOutput(taskId, output);
        }
        catch (Exception e) {
            failTask(taskId, e);
            throw new IllegalStateException("文章 workflow 执行失败", e);
        }
    }

    private void handleGraphOutput(String taskId, NodeOutput output) {
        Map<String, Object> state = new HashMap<>(output.state().data());
        // invokeAndGetOutput 返回中断前最后一个节点；标题生成后转为业务人工任务。
        if (ArticleWorkflowNodeType.TITLE_GENERATING.getValue().equals(output.node())) {
            waitHuman(taskId, ArticleWorkflowNodeType.TITLE_CONFIRM.getValue(), state,
                    ArticleWorkflowForms.titleConfirmSchema());
            return;
        }
        // 大纲生成后的中断同理，等待用户编辑或确认结构化大纲。
        if (ArticleWorkflowNodeType.OUTLINE_GENERATING.getValue().equals(output.node())) {
            waitHuman(taskId, ArticleWorkflowNodeType.OUTLINE_CONFIRM.getValue(), state,
                    ArticleWorkflowForms.outlineConfirmSchema());
            return;
        }
        if (output.isEND()) {
            WorkflowTask task = workflowTaskStore.getRequired(taskId);
            markTask(task, WorkflowStatusEnum.COMPLETED.getValue(), output.node(), WorkflowContext.fromMap(state).toJson(), null);
            publish(taskId, WorkflowEventTypeEnum.WORKFLOW_COMPLETED.getValue(), output.node(), state);
        }
    }

    private void waitHuman(String taskId, String nodeType, Map<String, Object> state, String formSchema) {
        WorkflowTask task = workflowTaskStore.getRequired(taskId);
        // WAITING_USER 是业务可见状态；Graph checkpoint 只负责技术恢复，不能替代表单任务。
        markTask(task, WorkflowStatusEnum.WAITING_USER.getValue(), nodeType, WorkflowContext.fromMap(state).toJson(), null);
        WorkflowHumanTask humanTask = humanTaskService.createWaitingTaskIfAbsent(WorkflowHumanTaskCreateCommand.builder()
                .taskId(taskId)
                .bizType(CreationTypeEnum.ARTICLE.getValue())
                .nodeType(nodeType)
                .assigneeUserId(task.getUserId())
                .inputSnapshotJson(WorkflowContext.fromMap(state).toJson())
                .formSchemaJson(formSchema)
                .expireTime(LocalDateTime.now().plus(checkpointTtl()))
                .build());
        publishWaitingHumanTask(taskId, humanTask, state);
    }

    private void updateCheckpointAndContext(String taskId, Map<String, Object> updates) {
        try {
            // updateState 只更新 threadId 对应 checkpoint，不会推进节点；真正推进发生在后续 resume。
            graphFactory.compile().updateState(RunnableConfig.builder().threadId(taskId).build(), updates);
        }
        catch (Exception e) {
            throw new IllegalStateException("更新文章 workflow checkpoint 失败", e);
        }
        WorkflowTask task = workflowTaskStore.getRequired(taskId);
        WorkflowContext context = WorkflowContext.fromJson(task.getContextJson());
        // 同步一份业务快照，供 SSE 重连、排查问题和未来非内存 checkpoint 恢复使用。
        context.putAll(updates);
        markTask(task, WorkflowStatusEnum.PROCESSING.getValue(), task.getCurrentNode(), context.toJson(), null);
    }

    private void ensureHumanTaskRecoverable(String taskId, LoginUserInfo loginUser, WorkflowHumanTask task) {
        if (!task.getAssigneeUserId().equals(loginUser.userId())) {
            throw new IllegalArgumentException("无权完成人工任务");
        }
        if (humanTaskService.isExpired(task)) {
            expireWorkflow(taskId, task, "人工确认任务已过期，请重新生成");
            throw new IllegalStateException("人工确认任务已过期，请重新生成");
        }
        if (!graphFactory.hasCheckpoint(taskId)) {
            expireWorkflow(taskId, task, "Workflow checkpoint 已过期，请重新生成");
            throw new IllegalStateException("Workflow checkpoint 已过期，请重新生成");
        }
    }

    private void publishWaitingHumanTask(String taskId, WorkflowHumanTask humanTask, Map<String, Object> state) {
        // SSE payload 直接带表单 schema、输入快照和过期时间，前端刷新后也能重建确认界面。
        Map<String, Object> payload = new HashMap<>();
        payload.put("humanTaskId", humanTask.getId());
        payload.put("formSchema", JSONUtil.parseObj(humanTask.getFormSchemaJson()));
        payload.put("inputSnapshot", state);
        payload.put("version", humanTask.getVersion());
        payload.put("expireTime", humanTask.getExpireTime());
        publish(taskId, WorkflowEventTypeEnum.NODE_WAITING_USER.getValue(), humanTask.getNodeType(), payload);
    }

    private void expireWorkflow(String taskId, WorkflowHumanTask humanTask, String reason) {
        WorkflowTask workflowTask = workflowTaskStore.getRequired(taskId);
        if (WorkflowStatusEnum.EXPIRED.getValue().equals(workflowTask.getStatus())) {
            return;
        }
        humanTaskService.expireTask(humanTask);
        markTask(workflowTask, WorkflowStatusEnum.EXPIRED.getValue(), humanTask.getNodeType(),
                workflowTask.getContextJson(), reason);
        articleService.updateStatus(ArticleStatusEnum.FAILED, taskId);
        articleService.updatePhase(ArticlePhaseEnum.EXPIRED, taskId);
        publish(taskId, WorkflowEventTypeEnum.WORKFLOW_EXPIRED.getValue(), humanTask.getNodeType(),
                Map.of("reason", reason, "nodeType", humanTask.getNodeType()));
    }

    private java.util.Optional<WorkflowHumanTask> findPendingHumanTask(String taskId) {
        java.util.Optional<WorkflowHumanTask> titleTask = humanTaskService.getLatestWaitingTask(
                taskId, ArticleWorkflowNodeType.TITLE_CONFIRM.getValue());
        if (titleTask.isPresent()) {
            return titleTask;
        }
        return humanTaskService.getLatestWaitingTask(taskId, ArticleWorkflowNodeType.OUTLINE_CONFIRM.getValue());
    }

    private Duration checkpointTtl() {
        Duration ttl = workflowProperties.getCheckpointTtl();
        return ttl == null || ttl.isNegative() || ttl.isZero() ? Duration.ofDays(7) : ttl;
    }

    private void failTask(String taskId, Exception e) {
        WorkflowTask task = workflowTaskStore.getRequired(taskId);
        markTask(task, WorkflowStatusEnum.FAILED.getValue(), task.getCurrentNode(), task.getContextJson(), e.getMessage());
        articleService.updateStatus(ArticleStatusEnum.FAILED, taskId);
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
                .bizType(CreationTypeEnum.ARTICLE.getValue())
                .nodeType(nodeType)
                .payload(payload == null ? Map.of() : payload)
                .eventTime(LocalDateTime.now())
                .build());
    }

    /**
     * Graph 生命周期监听器，用于把 Spring AI Alibaba 节点事件同步到现有 SSE 事件。
     */
    private class ArticleWorkflowLifecycleListener implements GraphLifecycleListener {

        private final String taskId;

        private ArticleWorkflowLifecycleListener(String taskId) {
            this.taskId = taskId;
        }

        @Override
        public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long timestamp) {
            WorkflowTask task = workflowTaskStore.getRequired(taskId);
            // before 事件用于让前端尽早看到当前阶段已经开始。
            markTask(task, WorkflowStatusEnum.PROCESSING.getValue(), nodeId, WorkflowContext.fromMap(state).toJson(), null);
            publish(taskId, WorkflowEventTypeEnum.NODE_STARTED.getValue(), nodeId, state);
        }

        @Override
        public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long timestamp) {
            WorkflowTask task = workflowTaskStore.getRequired(taskId);
            // after 事件保存节点输出快照，兼容旧文章 SSE 的标题/大纲/配图结果推送。
            markTask(task, WorkflowStatusEnum.PROCESSING.getValue(), nodeId, WorkflowContext.fromMap(state).toJson(), null);
            publish(taskId, WorkflowEventTypeEnum.NODE_RESULT.getValue(), nodeId, state);
        }
    }
}
