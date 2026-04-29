package com.sakura.passage_creator.notification.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统通知与公告实体。
 *
 * @author Sakura
 */
@Data
@Table("sys_notification")
public class Notification implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 通知类型：message/announcement。
     */
    private String type;

    /**
     * 通知标题。
     */
    private String title;

    /**
     * 通知摘要。
     */
    private String summary;

    /**
     * 通知正文。
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
     * 接收端范围：admin/app/all。
     */
    private String receiverType;

    /**
     * 目标范围：all/role/user。
     */
    private String targetType;

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

    /**
     * 发布时间。
     */
    private Date publishTime;

    /**
     * 发布人 id。
     */
    private Long publisherId;

    /**
     * 创建人 id。
     */
    private Long createUserId;

    /**
     * 更新人 id。
     */
    private Long updateUserId;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;

    /**
     * 逻辑删除标记。
     */
    @Column(isLogicDelete = true)
    private Integer isDelete;

    /**
     * 序列化版本号。
     */
    @Column(ignore = true)
    private static final long serialVersionUID = 1L;
}
