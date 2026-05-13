package com.sakura.passage_creator.creation.workflow;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通用 workflow 事件。
 */
@Data
@Builder
public class WorkflowEvent {
    private String type;
    private String taskId;
    private String bizType;
    private String nodeType;
    private Map<String, Object> payload;
    private LocalDateTime eventTime;
}
