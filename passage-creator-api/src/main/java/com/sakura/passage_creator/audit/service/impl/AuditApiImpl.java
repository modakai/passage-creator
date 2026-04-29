package com.sakura.passage_creator.audit.service.impl;

import com.sakura.passage_creator.audit.api.AuditApi;
import com.sakura.passage_creator.audit.api.AuditDashboardApi;
import com.sakura.passage_creator.audit.api.DashboardLoginTrendBucket;
import com.sakura.passage_creator.audit.api.DashboardRecentOperation;
import com.sakura.passage_creator.audit.api.LoginAuditCommand;
import com.sakura.passage_creator.audit.api.LoginAuditSubmittedEvent;
import com.sakura.passage_creator.audit.enums.AuditLogResultEnum;
import com.sakura.passage_creator.audit.enums.AuditLogTypeEnum;
import com.sakura.passage_creator.audit.model.dto.AuditLogCreateRequest;
import com.sakura.passage_creator.audit.model.entity.AuditLog;
import com.sakura.passage_creator.audit.service.AuditLogService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sakura.passage_creator.audit.model.entity.table.AuditLogTableDef.AUDIT_LOG;

/**
 * 审计模块对外 API 实现，隔离调用方和审计内部模型。
 *
 * @author Sakura
 */
@Component
public class AuditApiImpl implements AuditApi, AuditDashboardApi {

    /**
     * Dashboard 趋势标签格式。
     */
    private static final DateTimeFormatter TREND_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    /**
     * Dashboard 最近操作日志最大返回数量。
     */
    private static final int MAX_RECENT_OPERATION_LIMIT = 20;

    /**
     * 审计日志服务。
     */
    private final AuditLogService auditLogService;

    /**
     * Spring 事件发布器。
     */
    private final ApplicationEventPublisher eventPublisher;

    public AuditApiImpl(AuditLogService auditLogService, ApplicationEventPublisher eventPublisher) {
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void submitLoginLog(LoginAuditCommand command) {
        if (command == null) {
            return;
        }
        AuditLogCreateRequest request = new AuditLogCreateRequest();
        request.setUserId(command.userId());
        request.setAccountIdentifier(command.accountIdentifier());
        request.setIpAddress(command.ipAddress());
        request.setClientInfo(command.clientInfo());
        auditLogService.submitLoginLog(request, command.success(), command.failureReason(), command.costMillis());
        eventPublisher.publishEvent(new LoginAuditSubmittedEvent(command));
    }

    @Override
    public List<DashboardLoginTrendBucket> listSuccessfulLoginTrend(Date startTime, Date endTime, int bucketCount,
            ZoneId zoneId) {
        ZoneId effectiveZoneId = zoneId == null ? ZoneId.systemDefault() : zoneId;
        LocalDate startDate = startTime.toInstant().atZone(effectiveZoneId).toLocalDate();
        int safeBucketCount = Math.max(1, bucketCount);
        Date effectiveEndTime = endTime == null
                ? Date.from(startDate.plusDays(safeBucketCount).atStartOfDay(effectiveZoneId).toInstant())
                : endTime;
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(AUDIT_LOG.LOG_TYPE.eq(AuditLogTypeEnum.LOGIN.getValue()))
                .and(AUDIT_LOG.RESULT.eq(AuditLogResultEnum.SUCCESS.getValue()))
                .and(AUDIT_LOG.AUDIT_TIME.ge(startTime))
                .and(AUDIT_LOG.AUDIT_TIME.lt(effectiveEndTime));
        Map<LocalDate, Long> loginCountMap = auditLogService.list(queryWrapper).stream()
                .collect(Collectors.groupingBy(
                        log -> log.getAuditTime().toInstant().atZone(effectiveZoneId).toLocalDate(),
                        Collectors.counting()
                ));
        return startDate.datesUntil(startDate.plusDays(safeBucketCount))
                .map(date -> buildLoginTrendBucket(date, effectiveZoneId, loginCountMap.getOrDefault(date, 0L)))
                .toList();
    }

    @Override
    public List<DashboardRecentOperation> listRecentOperations(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_RECENT_OPERATION_LIMIT));
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(AUDIT_LOG.LOG_TYPE.eq(AuditLogTypeEnum.ADMIN_OPERATION.getValue()))
                .orderBy(AUDIT_LOG.AUDIT_TIME, false)
                .orderBy(AUDIT_LOG.ID, false)
                .limit(safeLimit);
        return auditLogService.list(queryWrapper).stream()
                .map(this::toDashboardRecentOperation)
                .toList();
    }

    @Override
    public long countAdminOperationLogs() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(AUDIT_LOG.LOG_TYPE.eq(AuditLogTypeEnum.ADMIN_OPERATION.getValue()));
        return auditLogService.count(queryWrapper);
    }

    /**
     * 构造单日成功登录趋势桶。
     */
    private DashboardLoginTrendBucket buildLoginTrendBucket(LocalDate date, ZoneId zoneId, long loginCount) {
        Date bucketStart = Date.from(date.atStartOfDay(zoneId).toInstant());
        Date bucketEnd = Date.from(date.plusDays(1).atStartOfDay(zoneId).toInstant());
        return new DashboardLoginTrendBucket(date.format(TREND_LABEL_FORMATTER), bucketStart, bucketEnd, loginCount);
    }

    /**
     * 转换 Dashboard 最近操作日志，并确保动作摘要使用审计模块已脱敏字段。
     */
    private DashboardRecentOperation toDashboardRecentOperation(AuditLog auditLog) {
        String action = auditLog.getOperationDescription();
        return new DashboardRecentOperation(
                auditLog.getId(),
                auditLog.getAccountIdentifier(),
                action,
                auditLog.getBusinessModule(),
                auditLog.getOperationType(),
                auditLog.getResult(),
                auditLog.getIpAddress(),
                auditLog.getAuditTime()
        );
    }
}
