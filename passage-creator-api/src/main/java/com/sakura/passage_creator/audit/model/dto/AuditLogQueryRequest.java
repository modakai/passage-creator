package com.sakura.passage_creator.audit.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 审计日志查询请求。
 *
 * @author Sakura
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogQueryRequest extends PageRequest {

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
     * 请求路径。
     */
    private String requestPath;

    /**
     * HTTP 方法。
     */
    private String httpMethod;

    /**
     * 执行结果。
     */
    private String result;

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
     * 审计开始时间。
     */
    private Date auditStartTime;

    /**
     * 审计结束时间。
     */
    private Date auditEndTime;
}
