package com.sakura.passage_creator.notification.model.vo;

import com.sakura.passage_creator.notification.model.entity.Notification;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 通知公告返回对象。
 *
 * @author Sakura
 */
@Data
@AutoMapper(target = Notification.class)
public class NotificationVO implements Serializable {

    /**
     * 通知 id。
     */
    private Long id;

    /**
     * 通知类型。
     */
    private String type;

    /**
     * 标题。
     */
    private String title;

    /**
     * 摘要。
     */
    private String summary;

    /**
     * 内容。
     */
    private String content;

    /**
     * 通知级别。
     */
    private String level;

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
     * 目标角色。
     */
    private List<String> targetRoles;

    /**
     * 目标用户 id。
     */
    private List<Long> targetUserIds;

    /**
     * 是否置顶。
     */
    private Integer pinned;

    /**
     * 是否弹窗。
     */
    private Integer popup;

    /**
     * 是否已读。
     */
    private Boolean read;

    /**
     * 跳转链接。
     */
    private String linkUrl;

    /**
     * 生效时间。
     */
    private Date effectiveTime;

    /**
     * 失效时间。
     */
    private Date expireTime;

    /**
     * 发布时间。
     */
    private Date publishTime;

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
