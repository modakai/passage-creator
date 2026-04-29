package com.sakura.passage_creator.observability.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 运维观测事件实体。
 *
 * @author Sakura
 */
@Data
@Table("sys_observability_event")
public class ObservabilityEvent implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 事件类型。
     */
    private String eventType;

    /**
     * 事件级别。
     */
    private String eventLevel;

    /**
     * 事件标题。
     */
    private String title;

    /**
     * 事件主体，例如 IP、账号或接口路径。
     */
    private String subject;

    /**
     * 请求路径。
     */
    private String requestPath;

    /**
     * HTTP 方法。
     */
    private String httpMethod;

    /**
     * HTTP 状态码。
     */
    private Integer statusCode;

    /**
     * 耗时，单位毫秒。
     */
    private Long durationMillis;

    /**
     * 用户 id。
     */
    private Long userId;

    /**
     * 账号标识。
     */
    private String accountIdentifier;

    /**
     * IP 地址。
     */
    private String ipAddress;

    /**
     * 异常摘要。
     */
    private String exceptionSummary;

    /**
     * 事件详情。
     */
    private String detail;

    /**
     * 关联审计日志 id。
     */
    private Long auditLogId;

    /**
     * 关联通知 id。
     */
    private Long notificationId;

    /**
     * 事件发生时间。
     */
    private Date eventTime;

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
