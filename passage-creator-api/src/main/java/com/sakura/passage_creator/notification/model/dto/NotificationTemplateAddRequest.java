package com.sakura.passage_creator.notification.model.dto;

import com.sakura.passage_creator.notification.model.entity.NotificationTemplate;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增消息通知模板请求。
 *
 * @author Sakura
 */
@Data
@AutoMapper(target = NotificationTemplate.class, reverseConvertGenerate = false)
public class NotificationTemplateAddRequest implements Serializable {

    /**
     * 模板编码。
     */
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    /**
     * 系统事件类型。
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /**
     * 标题模板。
     */
    @NotBlank(message = "标题模板不能为空")
    private String titleTemplate;

    /**
     * 内容模板。
     */
    @NotBlank(message = "内容模板不能为空")
    private String contentTemplate;

    /**
     * 变量定义 JSON。
     */
    private String variableSchema;

    /**
     * 默认接收端范围。
     */
    @NotBlank(message = "接收端范围不能为空")
    private String receiverType;

    /**
     * 是否启用。
     */
    private Integer enabled;

    /**
     * 备注。
     */
    private String remark;

    private static final long serialVersionUID = 1L;
}
