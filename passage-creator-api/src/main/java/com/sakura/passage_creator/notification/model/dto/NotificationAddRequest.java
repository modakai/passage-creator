package com.sakura.passage_creator.notification.model.dto;

import com.sakura.passage_creator.notification.model.entity.Notification;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 新增通知公告请求。
 *
 * @author Sakura
 */
@Data
@AutoMapper(target = Notification.class, reverseConvertGenerate = false)
public class NotificationAddRequest implements Serializable {

    /**
     * 通知类型：message/announcement。
     */
    @NotBlank(message = "通知类型不能为空")
    private String type;

    /**
     * 通知标题。
     */
    @NotBlank(message = "通知标题不能为空")
    private String title;

    /**
     * 通知摘要。
     */
    private String summary;

    /**
     * 通知正文。
     */
    @NotBlank(message = "通知内容不能为空")
    private String content;

    /**
     * 通知级别。
     */
    private String level;

    /**
     * 接收端范围：admin/app/all。
     */
    @NotBlank(message = "接收端范围不能为空")
    private String receiverType;

    /**
     * 目标范围：all/role/user。
     */
    @NotBlank(message = "目标范围不能为空")
    private String targetType;

    /**
     * 目标角色编码。
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
     * 是否弹窗展示。
     */
    private Integer popup;

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

    private static final long serialVersionUID = 1L;
}
