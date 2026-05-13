package com.sakura.passage_creator.creation.workflow.repository;

import com.mybatisflex.core.BaseMapper;
import com.sakura.passage_creator.creation.workflow.WorkflowHumanTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * Workflow 人工任务 Mapper。
 */
@Mapper
public interface WorkflowHumanTaskMapper extends BaseMapper<WorkflowHumanTask> {
}
