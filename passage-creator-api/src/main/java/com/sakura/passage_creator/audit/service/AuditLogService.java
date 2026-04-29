package com.sakura.passage_creator.audit.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.audit.model.dto.AuditLogCreateRequest;
import com.sakura.passage_creator.audit.model.dto.AuditLogExportRequest;
import com.sakura.passage_creator.audit.model.dto.AuditLogQueryRequest;
import com.sakura.passage_creator.audit.model.entity.AuditLog;
import com.sakura.passage_creator.audit.model.vo.AuditLogVO;

import java.util.List;

/**
 * 审计日志服务。
 *
 * @author Sakura
 */
public interface AuditLogService extends IService<AuditLog> {

    /**
     * 提交登录审计日志。
     *
     * @param request 审计创建请求
     * @param success 是否成功
     * @param failureReason 失败原因
     * @param costMillis 耗时
     */
    void submitLoginLog(AuditLogCreateRequest request, boolean success, String failureReason, long costMillis);

    /**
     * 提交管理员操作审计日志。
     *
     * @param request 审计创建请求
     * @param success 是否成功
     * @param throwable 异常
     * @param costMillis 耗时
     */
    void submitOperationLog(AuditLogCreateRequest request, boolean success, Throwable throwable, long costMillis);

    /**
     * 构造查询条件。
     *
     * @param request 查询请求
     * @return 查询包装器
     */
    QueryWrapper getQueryWrapper(AuditLogQueryRequest request);

    /**
     * 转换审计日志视图。
     *
     * @param auditLog 审计日志实体
     * @return 视图对象
     */
    AuditLogVO getAuditLogVO(AuditLog auditLog);

    /**
     * 转换审计日志视图列表。
     *
     * @param auditLogs 审计日志实体列表
     * @return 视图对象列表
     */
    List<AuditLogVO> getAuditLogVO(List<AuditLog> auditLogs);

    /**
     * 查询可导出的审计日志。
     *
     * @param request 导出请求
     * @return 审计日志列表
     */
    List<AuditLogVO> listExportLogs(AuditLogExportRequest request);
}
