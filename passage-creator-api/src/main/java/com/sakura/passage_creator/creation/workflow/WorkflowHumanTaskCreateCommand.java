package com.sakura.passage_creator.creation.workflow;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建人工任务命令。
 */
@Data
@Builder
public class WorkflowHumanTaskCreateCommand {
    private String taskId;
    private String bizType;
    private String nodeType;
    private Long assigneeUserId;
    private String inputSnapshotJson;
    private String formSchemaJson;
    /**
     * 人工任务过期时间，超过后不允许继续恢复旧 checkpoint。
     */
    private LocalDateTime expireTime;
}
