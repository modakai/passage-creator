package com.sakura.passage_creator.creation.workflow.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.sakura.passage_creator.creation.workflow.WorkflowHumanTask;
import com.sakura.passage_creator.creation.workflow.enums.HumanTaskStatusEnum;
import com.sakura.passage_creator.creation.workflow.repository.WorkflowHumanTaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.sakura.passage_creator.creation.workflow.table.WorkflowHumanTaskTableDef.WORKFLOW_HUMAN_TASK;

/**
 * 基于 MyBatis-Flex 的人工任务存储。
 */
@Service
public class MybatisHumanTaskStore implements HumanTaskStore {

    private final WorkflowHumanTaskMapper workflowHumanTaskMapper;

    public MybatisHumanTaskStore(WorkflowHumanTaskMapper workflowHumanTaskMapper) {
        this.workflowHumanTaskMapper = workflowHumanTaskMapper;
    }

    @Override
    public WorkflowHumanTask create(WorkflowHumanTask task) {
        LocalDateTime now = LocalDateTime.now();
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setIsDelete(0);
        workflowHumanTaskMapper.insert(task);
        return task;
    }

    @Override
    public WorkflowHumanTask update(WorkflowHumanTask task) {
        task.setUpdateTime(LocalDateTime.now());
        workflowHumanTaskMapper.update(task);
        return task;
    }

    @Override
    public Optional<WorkflowHumanTask> findLatest(String taskId, String nodeType) {
        return Optional.ofNullable(workflowHumanTaskMapper.selectOneByQuery(QueryWrapper.create()
                .where(WORKFLOW_HUMAN_TASK.TASK_ID.eq(taskId))
                .and(WORKFLOW_HUMAN_TASK.NODE_TYPE.eq(nodeType))
                .orderBy(WORKFLOW_HUMAN_TASK.CREATE_TIME.desc())
                .limit(1)));
    }

    @Override
    public Optional<WorkflowHumanTask> findLatestWaiting(String taskId, String nodeType) {
        return Optional.ofNullable(workflowHumanTaskMapper.selectOneByQuery(QueryWrapper.create()
                .where(WORKFLOW_HUMAN_TASK.TASK_ID.eq(taskId))
                .and(WORKFLOW_HUMAN_TASK.NODE_TYPE.eq(nodeType))
                .and(WORKFLOW_HUMAN_TASK.STATUS.eq(HumanTaskStatusEnum.WAITING.getValue()))
                .orderBy(WORKFLOW_HUMAN_TASK.CREATE_TIME.desc())
                .limit(1)));
    }
}
