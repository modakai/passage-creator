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
 * 消息通知模板实体。
 *
 * @author Sakura
 */
@Data
@Table("sys_notification_template")
public class NotificationTemplate implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
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
     * 是否启用：1 启用，0 停用。
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
