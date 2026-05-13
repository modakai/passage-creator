package com.sakura.passage_creator.creation.workflow;

/**
 * Workflow 事件发布器。
 */
public interface WorkflowEventPublisher {

    /**
     * 发布 workflow 事件。
     */
    void publish(WorkflowEvent event);
}
