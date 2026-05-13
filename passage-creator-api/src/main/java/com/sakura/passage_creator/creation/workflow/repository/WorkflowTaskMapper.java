package com.sakura.passage_creator.creation.workflow.repository;

import com.mybatisflex.core.BaseMapper;
import com.sakura.passage_creator.creation.workflow.WorkflowTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * Workflow 任务 Mapper。
 */
@Mapper
public interface WorkflowTaskMapper extends BaseMapper<WorkflowTask> {
}
