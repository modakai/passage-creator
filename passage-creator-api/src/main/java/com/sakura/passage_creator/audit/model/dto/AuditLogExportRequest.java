package com.sakura.passage_creator.audit.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审计日志导出请求。
 *
 * @author Sakura
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogExportRequest extends AuditLogQueryRequest {

    /**
     * 最大导出条数。
     */
    private Integer exportLimit = 5000;
}
