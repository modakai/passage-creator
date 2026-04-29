package com.sakura.passage_creator.audit.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.audit.enums.AuditLogResultEnum;
import com.sakura.passage_creator.audit.enums.AuditLogTypeEnum;
import com.sakura.passage_creator.audit.model.dto.AuditLogCreateRequest;
import com.sakura.passage_creator.audit.model.dto.AuditLogExportRequest;
import com.sakura.passage_creator.audit.model.dto.AuditLogQueryRequest;
import com.sakura.passage_creator.audit.model.entity.AuditLog;
import com.sakura.passage_creator.audit.model.vo.AuditLogVO;
import com.sakura.passage_creator.audit.repository.AuditLogMapper;
import com.sakura.passage_creator.audit.service.AuditLogService;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.constant.CommonConstant;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import io.github.linpeilie.Converter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.sakura.passage_creator.audit.model.entity.table.AuditLogTableDef.AUDIT_LOG;

/**
 * 审计日志服务实现。
 *
 * @author Sakura
 */
@Service
@Slf4j
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {

    /**
     * 默认最大导出数量。
     */
    private static final int DEFAULT_EXPORT_LIMIT = 5000;

    /**
     * 审计日志 Mapper。
     */
    private final AuditLogMapper auditLogMapper;

    /**
     * 摘要脱敏工具。
     */
    private final AuditSanitizer auditSanitizer;

    /**
     * MapStruct Plus 转换器，用于替代反射式 BeanUtils 属性复制。
     */
    private final Converter converter;

    /**
     * 审计写入执行器，使用守护线程避免阻塞应用退出。
     */
    private final Executor auditLogExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "audit-log-writer");
        thread.setDaemon(true);
        return thread;
    });

    public AuditLogServiceImpl(AuditLogMapper auditLogMapper, Converter converter) {
        this.auditLogMapper = auditLogMapper;
        this.auditSanitizer = new AuditSanitizer();
        this.converter = converter;
    }

    @Override
    public void submitLoginLog(AuditLogCreateRequest request, boolean success, String failureReason, long costMillis) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        request.setLogType(AuditLogTypeEnum.LOGIN.getValue());
        request.setOperationType("login");
        request.setResult(success ? AuditLogResultEnum.SUCCESS.getValue() : AuditLogResultEnum.FAILURE.getValue());
        request.setFailureReason(failureReason);
        request.setCostMillis(costMillis);
        saveAuditLogSafely(request);
    }

    @Override
    public void submitOperationLog(AuditLogCreateRequest request, boolean success, Throwable throwable, long costMillis) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        request.setLogType(AuditLogTypeEnum.ADMIN_OPERATION.getValue());
        request.setResult(success ? AuditLogResultEnum.SUCCESS.getValue() : AuditLogResultEnum.FAILURE.getValue());
        request.setCostMillis(costMillis);
        if (throwable != null) {
            request.setExceptionSummary(buildExceptionSummary(throwable));
        }
        saveAuditLogSafely(request);
    }

    @Override
    public QueryWrapper getQueryWrapper(AuditLogQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(AUDIT_LOG.LOG_TYPE.eq(request.getLogType(), StringUtils.isNotBlank(request.getLogType())));
        queryWrapper.and(AUDIT_LOG.USER_ID.eq(request.getUserId(), request.getUserId() != null));
        queryWrapper.and(AUDIT_LOG.ACCOUNT_IDENTIFIER.like(request.getAccountIdentifier(),
                StringUtils.isNotBlank(request.getAccountIdentifier())));
        queryWrapper.and(AUDIT_LOG.IP_ADDRESS.eq(request.getIpAddress(), StringUtils.isNotBlank(request.getIpAddress())));
        queryWrapper.and(AUDIT_LOG.REQUEST_PATH.like(request.getRequestPath(),
                StringUtils.isNotBlank(request.getRequestPath())));
        queryWrapper.and(AUDIT_LOG.HTTP_METHOD.eq(request.getHttpMethod(), StringUtils.isNotBlank(request.getHttpMethod())));
        queryWrapper.and(AUDIT_LOG.RESULT.eq(request.getResult(), StringUtils.isNotBlank(request.getResult())));
        queryWrapper.and(AUDIT_LOG.OPERATION_DESCRIPTION.like(request.getOperationDescription(),
                StringUtils.isNotBlank(request.getOperationDescription())));
        queryWrapper.and(AUDIT_LOG.BUSINESS_MODULE.eq(request.getBusinessModule(),
                StringUtils.isNotBlank(request.getBusinessModule())));
        queryWrapper.and(AUDIT_LOG.OPERATION_TYPE.eq(request.getOperationType(),
                StringUtils.isNotBlank(request.getOperationType())));
        queryWrapper.and(AUDIT_LOG.AUDIT_TIME.ge(request.getAuditStartTime(), request.getAuditStartTime() != null));
        queryWrapper.and(AUDIT_LOG.AUDIT_TIME.le(request.getAuditEndTime(), request.getAuditEndTime() != null));
        QueryColumn sortColumn = resolveSortColumn(request.getSortField());
        if (sortColumn != null) {
            queryWrapper.orderBy(sortColumn, CommonConstant.SORT_ORDER_ASC.equals(request.getSortOrder()));
        } else {
            queryWrapper.orderBy(AUDIT_LOG.AUDIT_TIME, false).orderBy(AUDIT_LOG.ID, false);
        }
        return queryWrapper;
    }

    /**
     * 将客户端排序字段转换为审计日志表 APT 字段。
     */
    private QueryColumn resolveSortColumn(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return null;
        }
        return switch (sortField) {
            case "id" -> AUDIT_LOG.ID;
            case "log_type" -> AUDIT_LOG.LOG_TYPE;
            case "user_id" -> AUDIT_LOG.USER_ID;
            case "account_identifier" -> AUDIT_LOG.ACCOUNT_IDENTIFIER;
            case "ip_address" -> AUDIT_LOG.IP_ADDRESS;
            case "request_path" -> AUDIT_LOG.REQUEST_PATH;
            case "http_method" -> AUDIT_LOG.HTTP_METHOD;
            case "result" -> AUDIT_LOG.RESULT;
            case "operation_description" -> AUDIT_LOG.OPERATION_DESCRIPTION;
            case "business_module" -> AUDIT_LOG.BUSINESS_MODULE;
            case "operation_type" -> AUDIT_LOG.OPERATION_TYPE;
            case "audit_time" -> AUDIT_LOG.AUDIT_TIME;
            case "cost_millis" -> AUDIT_LOG.COST_MILLIS;
            case "create_time" -> AUDIT_LOG.CREATE_TIME;
            case "update_time" -> AUDIT_LOG.UPDATE_TIME;
            default -> null;
        };
    }

    @Override
    public AuditLogVO getAuditLogVO(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return converter.convert(auditLog, AuditLogVO.class);
    }

    @Override
    public List<AuditLogVO> getAuditLogVO(List<AuditLog> auditLogs) {
        if (CollUtil.isEmpty(auditLogs)) {
            return new ArrayList<>();
        }
        return auditLogs.stream().map(this::getAuditLogVO).collect(Collectors.toList());
    }

    @Override
    public List<AuditLogVO> listExportLogs(AuditLogExportRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        int limit = request.getExportLimit() == null ? DEFAULT_EXPORT_LIMIT : request.getExportLimit();
        if (limit <= 0 || limit > DEFAULT_EXPORT_LIMIT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "audit.export.limit.invalid");
        }
        QueryWrapper queryWrapper = getQueryWrapper(request).limit(limit);
        return getAuditLogVO(this.list(queryWrapper));
    }

    /**
     * 安全保存审计日志，失败时不影响主业务。
     */
    private void saveAuditLogSafely(AuditLogCreateRequest request) {
        auditLogExecutor.execute(() -> insertAuditLogSafely(request));
    }

    /**
     * 执行审计日志落库并吞掉审计自身异常。
     */
    private void insertAuditLogSafely(AuditLogCreateRequest request) {
        try {
            AuditLog auditLog = buildAuditLog(request);
            if (auditLogMapper != null) {
                auditLogMapper.insertSelective(auditLog);
            } else {
                this.save(auditLog);
            }
        } catch (Exception e) {
            log.error("save audit log failed", e);
        }
    }

    /**
     * 构造审计日志实体。
     */
    private AuditLog buildAuditLog(AuditLogCreateRequest request) {
        AuditLog auditLog = converter.convert(request, AuditLog.class);
        auditLog.setRequestSummary(auditSanitizer.sanitize(request.getRequestSummary()));
        auditLog.setResponseSummary(auditSanitizer.sanitize(request.getResponseSummary()));
        auditLog.setExceptionSummary(auditSanitizer.sanitize(request.getExceptionSummary()));
        if (auditLog.getAuditTime() == null) {
            auditLog.setAuditTime(new Date());
        }
        return auditLog;
    }

    /**
     * 构造异常摘要。
     */
    private String buildExceptionSummary(Throwable throwable) {
        String message = throwable.getMessage();
        if (StringUtils.isBlank(message)) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getClass().getSimpleName() + ": " + message;
    }
}
