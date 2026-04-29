package com.sakura.passage_creator.audit.model.excel;

import com.sakura.passage_creator.audit.model.vo.AuditLogVO;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.fesod.sheet.annotation.write.style.ColumnWidth;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审计日志 Excel 导出行。
 *
 * @author Sakura
 */
@Data
public class AuditLogExcelRow {

    /**
     * 审计时间导出格式。
     */
    private static final String AUDIT_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 主键 id。
     */
    @ExcelProperty("ID")
    @ColumnWidth(22)
    private String id;

    /**
     * 日志类型。
     */
    @ExcelProperty("日志类型")
    private String logType;

    /**
     * 用户 id。
     */
    @ExcelProperty("用户ID")
    @ColumnWidth(22)
    private String userId;

    /**
     * 账号标识。
     */
    @ExcelProperty("账号标识")
    private String accountIdentifier;

    /**
     * IP 地址。
     */
    @ExcelProperty("IP地址")
    private String ipAddress;

    /**
     * 请求路径。
     */
    @ExcelProperty("请求路径")
    private String requestPath;

    /**
     * HTTP 方法。
     */
    @ExcelProperty("请求方法")
    private String httpMethod;

    /**
     * 操作描述。
     */
    @ExcelProperty("操作描述")
    private String operationDescription;

    /**
     * 业务模块。
     */
    @ExcelProperty("业务模块")
    private String businessModule;

    /**
     * 操作类型。
     */
    @ExcelProperty("操作类型")
    private String operationType;

    /**
     * 执行结果。
     */
    @ExcelProperty("执行结果")
    private String result;

    /**
     * 状态码。
     */
    @ExcelProperty("状态码")
    private Integer statusCode;

    /**
     * 耗时，单位毫秒。
     */
    @ExcelProperty("耗时毫秒")
    private Long costMillis;

    /**
     * 失败原因。
     */
    @ExcelProperty("失败原因")
    private String failureReason;

    /**
     * 追踪 ID。
     */
    @ExcelProperty("追踪ID")
    private String traceId;

    /**
     * 审计时间。
     */
    @ExcelProperty("审计时间")
    @ColumnWidth(22)
    private String auditTime;

    /**
     * 转换审计日志导出行列表。
     *
     * @param logs 审计日志视图列表
     * @return Excel 导出行列表
     */
    public static List<AuditLogExcelRow> fromList(List<AuditLogVO> logs) {
        return logs.stream().map(AuditLogExcelRow::from).collect(Collectors.toList());
    }

    /**
     * 转换单条审计日志导出行。
     *
     * @param log 审计日志视图
     * @return Excel 导出行
     */
    private static AuditLogExcelRow from(AuditLogVO log) {
        AuditLogExcelRow row = new AuditLogExcelRow();
        row.setId(formatLong(log.getId()));
        row.setLogType(log.getLogType());
        row.setUserId(formatLong(log.getUserId()));
        row.setAccountIdentifier(log.getAccountIdentifier());
        row.setIpAddress(log.getIpAddress());
        row.setRequestPath(log.getRequestPath());
        row.setHttpMethod(log.getHttpMethod());
        row.setOperationDescription(log.getOperationDescription());
        row.setBusinessModule(log.getBusinessModule());
        row.setOperationType(log.getOperationType());
        row.setResult(log.getResult());
        row.setStatusCode(log.getStatusCode());
        row.setCostMillis(log.getCostMillis());
        row.setFailureReason(log.getFailureReason());
        row.setTraceId(log.getTraceId());
        row.setAuditTime(log.getAuditTime() == null ? null : new SimpleDateFormat(AUDIT_TIME_PATTERN).format(log.getAuditTime()));
        return row;
    }

    /**
     * 将长整型转为文本，避免 Excel 对大数字使用科学计数法展示。
     */
    private static String formatLong(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
