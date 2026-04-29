package com.sakura.passage_creator.audit.model.vo;

import com.sakura.passage_creator.audit.model.entity.AuditLog;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.util.Date;

/**
 * 审计日志视图对象。
 *
 * @author Sakura
 */
@Data
@AutoMapper(target = AuditLog.class)
public class AuditLogVO {

    /**
     * 主键 id。
     */
    private Long id;

    /**
     * 日志类型。
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
     * 执行结果。
     */
    private String result;

    /**
     * 状态码。
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
}
