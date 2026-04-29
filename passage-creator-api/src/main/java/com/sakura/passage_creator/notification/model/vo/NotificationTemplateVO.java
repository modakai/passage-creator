package com.sakura.passage_creator.notification.model.vo;

import com.sakura.passage_creator.notification.model.entity.NotificationTemplate;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息通知模板返回对象。
 *
 * @author Sakura
 */
@Data
@AutoMapper(target = NotificationTemplate.class)
public class NotificationTemplateVO implements Serializable {

    /**
     * 模板 id。
     */
    private Long id;

    /**
     * 模板编码。
     */
    private String templateCode;

    /**
     * 事件类型。
     */
    private String eventType;

    /**
     * 标题模板。
     */
    private String titleTemplate;

    /**
     * 内容模板。
     */
    private String contentTemplate;

    /**
     * 变量定义 JSON。
     */
    private String variableSchema;

    /**
     * 默认接收端范围。
     */
    private String receiverType;

    /**
     * 是否启用。
     */
    private Integer enabled;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
