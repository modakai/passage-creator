package com.sakura.passage_creator.notification.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 消息通知模板分页查询请求。
 *
 * @author Sakura
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class NotificationTemplateQueryRequest extends PageRequest implements Serializable {

    /**
     * 模板编码。
     */
    private String templateCode;

    /**
     * 事件类型。
     */
    private String eventType;

    /**
     * 是否启用。
     */
    private Integer enabled;

    private static final long serialVersionUID = 1L;
}
