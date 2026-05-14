package com.sakura.passage_creator.creation.workflow.enums;

import lombok.Getter;

/**
 * Workflow 任务状态。
 */
@Getter
public enum WorkflowStatusEnum {

    /**
     * 任务已创建，等待执行。
     */
    PENDING("PENDING"),

    /**
     * 自动节点执行中。
     */
    PROCESSING("PROCESSING"),

    /**
     * 流程暂停，等待用户完成 Human-in-the-Loop 任务。
     */
    WAITING_USER("WAITING_USER"),

    /**
     * 流程已完成。
     */
    COMPLETED("COMPLETED"),

    /**
     * 流程失败。
     */
    FAILED("FAILED"),

    /**
     * 流程等待用户过久，checkpoint 已过期。
     */
    EXPIRED("EXPIRED");

    private final String value;

    WorkflowStatusEnum(String value) {
        this.value = value;
    }
}
