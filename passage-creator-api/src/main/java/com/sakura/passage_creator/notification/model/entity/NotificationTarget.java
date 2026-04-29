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
 * 通知投放目标实体。
 *
 * @author Sakura
 */
@Data
@Table("sys_notification_target")
public class NotificationTarget implements Serializable {

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
     * 目标类型：role/user。
     */
    private String targetType;

    /**
     * 目标值，角色编码或用户 id。
     */
    private String targetValue;

    /**
     * 创建时间。
     */
    private Date createTime;

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
