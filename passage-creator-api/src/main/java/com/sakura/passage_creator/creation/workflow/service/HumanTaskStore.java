package com.sakura.passage_creator.creation.workflow.service;

import com.sakura.passage_creator.creation.workflow.WorkflowHumanTask;

import java.util.Optional;

/**
 * 人工任务存储接口。
 */
public interface HumanTaskStore {

    WorkflowHumanTask create(WorkflowHumanTask task);

    WorkflowHumanTask update(WorkflowHumanTask task);

    Optional<WorkflowHumanTask> findLatest(String taskId, String nodeType);

    Optional<WorkflowHumanTask> findLatestWaiting(String taskId, String nodeType);
}
