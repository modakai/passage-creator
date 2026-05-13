package com.sakura.passage_creator.creation.workflow.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.sakura.passage_creator.creation.workflow.WorkflowTask;
import com.sakura.passage_creator.creation.workflow.repository.WorkflowTaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.sakura.passage_creator.creation.workflow.table.WorkflowTaskTableDef.WORKFLOW_TASK;

/**
 * 基于 MyBatis-Flex 的 workflow 任务存储。
 */
@Service
public class MybatisWorkflowTaskStore implements WorkflowTaskStore {

    private final WorkflowTaskMapper workflowTaskMapper;

    public MybatisWorkflowTaskStore(WorkflowTaskMapper workflowTaskMapper) {
        this.workflowTaskMapper = workflowTaskMapper;
    }

    @Override
    public WorkflowTask create(WorkflowTask task) {
        LocalDateTime now = LocalDateTime.now();
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setIsDelete(0);
        workflowTaskMapper.insert(task);
        return task;
    }

    @Override
    public WorkflowTask update(WorkflowTask task) {
        task.setUpdateTime(LocalDateTime.now());
        workflowTaskMapper.update(task);
        return task;
    }

    @Override
    public Optional<WorkflowTask> findByTaskId(String taskId) {
        return Optional.ofNullable(workflowTaskMapper.selectOneByQuery(QueryWrapper.create()
                .where(WORKFLOW_TASK.TASK_ID.eq(taskId))));
    }

    @Override
    public WorkflowTask getRequired(String taskId) {
        return findByTaskId(taskId).orElseThrow(() -> new IllegalArgumentException("Workflow 任务不存在: " + taskId));
    }
}
