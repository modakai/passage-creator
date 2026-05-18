package com.sakura.passage_creator.creation.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用创作 workflow SSE 消息体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSseMessage<T> {

    /**
     * 消息类型，例如 PROGRESS。
     */
    private String type;

    /**
     * 消息描述，用于前端提示。
     */
    private String message;

    /**
     * 消息数据。
     */
    private T data;

    /**
     * 创建通用 SSE 消息。
     */
    public static <T> WorkflowSseMessage<T> of(String type, String message, T data) {
        return new WorkflowSseMessage<>(type, message, data);
    }
}
