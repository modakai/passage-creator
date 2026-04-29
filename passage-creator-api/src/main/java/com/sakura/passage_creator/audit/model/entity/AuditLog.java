package com.sakura.passage_creator.audit.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审计日志实体。
 *
 * @author Sakura
 */
@Data
@Table("sys_audit_log")
public class AuditLog implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 日志类型：login/admin_operation。
     */
    private String logType;

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
     * 客户端信息。
     */
    private String clientInfo;

    /**
     * 请求路径。
     */
    private String requestPath;

    /**
     * HTTP 方法。
     */
    private String httpMethod;

    /**
     * 操作描述。
     */
    private String operationDescription;

    /**
     * 业务模块。
     */
    private String businessModule;

    /**
     * 操作类型。
     */
    private String operationType;

    /**
     * 耗时，单位毫秒。
     */
    private Long costMillis;

    /**
     * 执行结果：success/failure。
     */
    private String result;

    /**
     * HTTP 或业务状态码。
     */
    private Integer statusCode;

    /**
     * 失败原因。
     */
    private String failureReason;

    /**
     * 异常摘要。
     */
    private String exceptionSummary;

    /**
     * 请求摘要。
     */
    private String requestSummary;

    /**
     * 响应摘要。
     */
    private String responseSummary;

    /**
     * 追踪 ID。
     */
    private String traceId;

    /**
     * 审计时间。
     */
    private Date auditTime;

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
