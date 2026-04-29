package com.sakura.passage_creator.notification.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 自动模板消息发送请求。
 *
 * @author Sakura
 */
@Data
public class NotificationAutoSendRequest implements Serializable {

    /**
     * 系统事件类型。
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /**
     * 接收端范围：admin/app。
     */
    @NotBlank(message = "接收端不能为空")
    private String receiverType;

    /**
     * 目标用户 id。
     */
    @NotNull(message = "目标用户 id 不能为空")
    private Long targetUserId;

    /**
     * 模板变量。
     */
    private Map<String, Object> variables;

    private static final long serialVersionUID = 1L;
}
