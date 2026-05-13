package com.sakura.passage_creator.creation.workflow.service;

import com.sakura.passage_creator.creation.workflow.WorkflowTask;

import java.util.Optional;

/**
 * Workflow 任务存储接口。
 */
public interface WorkflowTaskStore {

    WorkflowTask create(WorkflowTask task);

    WorkflowTask update(WorkflowTask task);

    Optional<WorkflowTask> findByTaskId(String taskId);

    WorkflowTask getRequired(String taskId);
}
