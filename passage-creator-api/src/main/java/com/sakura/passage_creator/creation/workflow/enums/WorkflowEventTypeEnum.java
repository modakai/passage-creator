package com.sakura.passage_creator.creation.workflow.enums;

import lombok.Getter;

/**
 * 通用 workflow 事件类型。
 */
@Getter
public enum WorkflowEventTypeEnum {

    WORKFLOW_STARTED("WORKFLOW_STARTED"),
    NODE_STARTED("NODE_STARTED"),
    NODE_RESULT("NODE_RESULT"),
    NODE_WAITING_USER("NODE_WAITING_USER"),
    NODE_FAILED("NODE_FAILED"),
    NODE_RETRYING("NODE_RETRYING"),
    WORKFLOW_COMPLETED("WORKFLOW_COMPLETED"),
    WORKFLOW_FAILED("WORKFLOW_FAILED"),
    WORKFLOW_EXPIRED("WORKFLOW_EXPIRED");

    private final String value;

    WorkflowEventTypeEnum(String value) {
        this.value = value;
    }
}
