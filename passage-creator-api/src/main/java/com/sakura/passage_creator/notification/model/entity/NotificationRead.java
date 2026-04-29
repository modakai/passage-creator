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
 * 通知已读与关闭状态实体。
 *
 * @author Sakura
 */
@Data
@Table("sys_notification_read")
public class NotificationRead implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 通知 id。
     */
    private Long notificationId;

    /**
     * 用户端类型：admin/app。
     */
    private String receiverType;

    /**
     * 用户 id。
     */
    private Long userId;

    /**
     * 已读时间。
     */
    private Date readTime;

    /**
     * 关闭时间。
     */
    private Date closeTime;

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
