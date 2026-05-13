package com.sakura.passage_creator.creation.workflow;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 完成人工任务命令。
 */
@Data
@Builder
public class WorkflowHumanTaskCompleteCommand {
    private String taskId;
    private String nodeType;
    private Long userId;
    private Integer version;
    private Map<String, Object> result;
}
