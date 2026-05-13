package com.sakura.passage_creator.creation.workflow.service;

import cn.hutool.json.JSONUtil;
import com.sakura.passage_creator.creation.workflow.WorkflowHumanTask;
import com.sakura.passage_creator.creation.workflow.WorkflowHumanTaskCompleteCommand;
import com.sakura.passage_creator.creation.workflow.WorkflowHumanTaskCreateCommand;
import com.sakura.passage_creator.creation.workflow.enums.HumanTaskStatusEnum;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Human-in-the-Loop 人工任务服务。
 */
@Service
public class WorkflowHumanTaskService {

    private final HumanTaskStore humanTaskStore;

    public WorkflowHumanTaskService(HumanTaskStore humanTaskStore) {
        this.humanTaskStore = humanTaskStore;
    }

    /**
     * 创建等待用户处理的人工任务。
     */
    public WorkflowHumanTask createWaitingTask(WorkflowHumanTaskCreateCommand command) {
        WorkflowHumanTask task = WorkflowHumanTask.builder()
                .taskId(command.getTaskId())
                .bizType(command.getBizType())
                .nodeType(command.getNodeType())
                .status(HumanTaskStatusEnum.WAITING.getValue())
                .assigneeUserId(command.getAssigneeUserId())
                .inputSnapshotJson(command.getInputSnapshotJson())
                .formSchemaJson(command.getFormSchemaJson())
                .version(1)
                .build();
        return humanTaskStore.create(task);
    }

    /**
     * 如果当前节点没有等待中的人工任务，则创建一个。
     */
    public WorkflowHumanTask createWaitingTaskIfAbsent(WorkflowHumanTaskCreateCommand command) {
        return getLatestWaitingTask(command.getTaskId(), command.getNodeType())
                .orElseGet(() -> createWaitingTask(command));
    }

    /**
     * 完成人工任务，并执行所有服务端校验。
     */
    public WorkflowHumanTask completeTask(WorkflowHumanTaskCompleteCommand command) {
        WorkflowHumanTask task = humanTaskStore.findLatest(command.getTaskId(), command.getNodeType())
                .orElseThrow(() -> new IllegalArgumentException("人工任务不存在"));
        if (!HumanTaskStatusEnum.WAITING.getValue().equals(task.getStatus())) {
            throw new IllegalStateException("人工任务不处于等待状态");
        }
        if (!task.getAssigneeUserId().equals(command.getUserId())) {
            throw new IllegalArgumentException("无权完成人工任务");
        }
        if (!task.getVersion().equals(command.getVersion())) {
            throw new IllegalStateException("人工任务版本已过期");
        }
        task.setResultJson(JSONUtil.toJsonStr(command.getResult()));
        task.setStatus(HumanTaskStatusEnum.COMPLETED.getValue());
        task.setVersion(task.getVersion() + 1);
        task.setCompletedTime(LocalDateTime.now());
        return humanTaskStore.update(task);
    }

    /**
     * 查询最新等待中的人工任务。
     */
    public Optional<WorkflowHumanTask> getLatestWaitingTask(String taskId, String nodeType) {
        return humanTaskStore.findLatestWaiting(taskId, nodeType);
    }
}
