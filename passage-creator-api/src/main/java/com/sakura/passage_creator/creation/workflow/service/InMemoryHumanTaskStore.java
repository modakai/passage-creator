package com.sakura.passage_creator.creation.workflow.service;

import com.sakura.passage_creator.creation.workflow.WorkflowHumanTask;
import com.sakura.passage_creator.creation.workflow.enums.HumanTaskStatusEnum;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存人工任务存储，主要用于单元测试。
 */
public class InMemoryHumanTaskStore implements HumanTaskStore {

    private final List<WorkflowHumanTask> tasks = new CopyOnWriteArrayList<>();

    @Override
    public WorkflowHumanTask create(WorkflowHumanTask task) {
        LocalDateTime now = LocalDateTime.now();
        task.setId((long) tasks.size() + 1);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setIsDelete(0);
        tasks.add(task);
        return task;
    }

    @Override
    public WorkflowHumanTask update(WorkflowHumanTask task) {
        task.setUpdateTime(LocalDateTime.now());
        return task;
    }

    @Override
    public Optional<WorkflowHumanTask> findLatest(String taskId, String nodeType) {
        return tasks.stream()
                .filter(task -> taskId.equals(task.getTaskId()) && nodeType.equals(task.getNodeType()))
                .max(Comparator.comparing(WorkflowHumanTask::getCreateTime));
    }

    @Override
    public Optional<WorkflowHumanTask> findLatestWaiting(String taskId, String nodeType) {
        return tasks.stream()
                .filter(task -> taskId.equals(task.getTaskId()) && nodeType.equals(task.getNodeType()))
                .filter(task -> HumanTaskStatusEnum.WAITING.getValue().equals(task.getStatus()))
                .max(Comparator.comparing(WorkflowHumanTask::getCreateTime));
    }
}
