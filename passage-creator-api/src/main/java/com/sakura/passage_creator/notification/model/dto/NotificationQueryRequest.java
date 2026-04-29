package com.sakura.passage_creator.notification.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知公告分页查询请求。
 *
 * @author Sakura
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class NotificationQueryRequest extends PageRequest implements Serializable {

    /**
     * 通知 id。
     */
    private Long id;

    /**
     * 通知类型。
     */
    private String type;

    /**
     * 标题关键字。
     */
    private String title;

    /**
     * 发布状态。
     */
    private String status;

    /**
     * 接收端范围。
     */
    private String receiverType;

    /**
     * 目标范围。
     */
    private String targetType;

    /**
     * 发布时间开始。
     */
    private Date publishStartTime;

    /**
     * 发布时间结束。
     */
    private Date publishEndTime;

    private static final long serialVersionUID = 1L;
}
