package com.sakura.passage_creator.dashboard.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.sakura.passage_creator.audit.api.AuditDashboardApi;
import com.sakura.passage_creator.audit.api.DashboardLoginTrendBucket;
import com.sakura.passage_creator.audit.api.DashboardRecentOperation;
import com.sakura.passage_creator.dashboard.model.vo.DashboardLoginTrendVO;
import com.sakura.passage_creator.dashboard.model.vo.DashboardRecentOperationVO;
import com.sakura.passage_creator.dashboard.model.vo.DashboardStatisticsVO;
import com.sakura.passage_creator.dashboard.model.vo.DashboardSummaryVO;
import com.sakura.passage_creator.dashboard.service.DashboardStatisticsService;
import com.sakura.passage_creator.notification.api.NotificationDashboardApi;
import com.sakura.passage_creator.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static com.sakura.passage_creator.user.model.entity.table.UserTableDef.USER;

/**
 * Dashboard 统计聚合服务实现。
 */
@Service
public class DashboardStatisticsServiceImpl implements DashboardStatisticsService {

    /**
     * 登录趋势默认展示最近 7 天。
     */
    private static final int LOGIN_TREND_DAYS = 7;

    /**
     * 最近操作日志默认展示 8 条。
     */
    private static final int RECENT_OPERATION_LIMIT = 8;

    /**
     * Dashboard 最近操作摘要兜底脱敏字段。
     */
    private static final List<String> SENSITIVE_KEYS = List.of(
            "password", "token", "authorization", "captcha", "secret", "key");

    private final UserService userService;

    private final NotificationDashboardApi notificationDashboardApi;

    private final AuditDashboardApi auditDashboardApi;

    private final ZoneId zoneId;

    @Autowired
    public DashboardStatisticsServiceImpl(UserService userService,
            NotificationDashboardApi notificationDashboardApi,
            AuditDashboardApi auditDashboardApi) {
        this(userService, notificationDashboardApi, auditDashboardApi, ZoneId.systemDefault());
    }

    DashboardStatisticsServiceImpl(UserService userService,
            NotificationDashboardApi notificationDashboardApi,
            AuditDashboardApi auditDashboardApi,
            ZoneId zoneId) {
        this.userService = userService;
        this.notificationDashboardApi = notificationDashboardApi;
        this.auditDashboardApi = auditDashboardApi;
        this.zoneId = zoneId;
    }

    @Override
    public DashboardStatisticsVO getStatistics() {
        return getStatistics(Instant.now());
    }

    @Override
    public DashboardStatisticsVO getStatistics(Instant sampleInstant) {
        Instant effectiveSampleInstant = sampleInstant == null ? Instant.now() : sampleInstant;
        LocalDate today = LocalDate.ofInstant(effectiveSampleInstant, zoneId);
        Date todayStart = Date.from(today.atStartOfDay(zoneId).toInstant());
        Date tomorrowStart = Date.from(today.plusDays(1).atStartOfDay(zoneId).toInstant());
        Date trendStart = Date.from(today.minusDays(LOGIN_TREND_DAYS - 1L).atStartOfDay(zoneId).toInstant());

        DashboardStatisticsVO statistics = new DashboardStatisticsVO();
        statistics.setSummary(buildSummary(todayStart, tomorrowStart));
        statistics.setLoginTrend(buildLoginTrend(trendStart, tomorrowStart));
        statistics.setRecentOperations(buildRecentOperations());
        statistics.setSampleTime(Date.from(effectiveSampleInstant));
        return statistics;
    }

    /**
     * 构建概览指标；所有统计口径由后端统一决定，前端不得自行推导。
     */
    private DashboardSummaryVO buildSummary(Date todayStart, Date tomorrowStart) {
        DashboardSummaryVO summary = new DashboardSummaryVO();
        summary.setUserTotalCount(userService.count());
        summary.setTodayNewUserCount(countTodayNewUsers(todayStart, tomorrowStart));
        // 通知数量采用后台管理员当前可见的已发布通知范围，避免把草稿当作待处理消息展示。
        summary.setNotificationCount(notificationDashboardApi.countAdminVisibleNotifications());
        // 操作日志数来自审计模块，替代无本地持久化来源的 OSS 文件上传量。
        summary.setOperationLogCount(auditDashboardApi.countAdminOperationLogs());
        return summary;
    }

    /**
     * 今日新增用户按服务器业务时区自然日 [start, nextStart) 统计。
     */
    private long countTodayNewUsers(Date todayStart, Date tomorrowStart) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(USER.CREATE_TIME.ge(todayStart))
                .and(USER.CREATE_TIME.lt(tomorrowStart));
        return userService.count(queryWrapper);
    }

    /**
     * 登录趋势只统计成功登录，失败登录继续归入安全观测场景。
     */
    private List<DashboardLoginTrendVO> buildLoginTrend(Date trendStart, Date trendEnd) {
        return auditDashboardApi.listSuccessfulLoginTrend(trendStart, trendEnd, LOGIN_TREND_DAYS, zoneId).stream()
                .map(this::toLoginTrendVO)
                .toList();
    }

    /**
     * 最近操作日志只展示脱敏后的动作摘要。
     */
    private List<DashboardRecentOperationVO> buildRecentOperations() {
        return auditDashboardApi.listRecentOperations(RECENT_OPERATION_LIMIT).stream()
                .map(this::toRecentOperationVO)
                .toList();
    }

    private DashboardLoginTrendVO toLoginTrendVO(DashboardLoginTrendBucket bucket) {
        DashboardLoginTrendVO vo = new DashboardLoginTrendVO();
        vo.setLabel(bucket.label());
        vo.setStartTime(bucket.startTime());
        vo.setEndTime(bucket.endTime());
        vo.setLoginCount(bucket.loginCount());
        return vo;
    }

    private DashboardRecentOperationVO toRecentOperationVO(DashboardRecentOperation operation) {
        DashboardRecentOperationVO vo = new DashboardRecentOperationVO();
        vo.setId(operation.id());
        vo.setOperator(operation.operator());
        vo.setAction(sanitizeAction(operation.action()));
        vo.setModule(operation.module());
        vo.setOperationType(operation.operationType());
        vo.setResult(operation.result());
        vo.setIpAddress(operation.ipAddress());
        vo.setOperationTime(operation.operationTime());
        return vo;
    }

    /**
     * 最近操作摘要兜底脱敏，防止历史审计数据或调用方传入的摘要包含敏感值。
     */
    private String sanitizeAction(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String sanitized = raw;
        for (String key : SENSITIVE_KEYS) {
            sanitized = Pattern.compile("(?i)(\\b" + Pattern.quote(key) + "\\s*=\\s*)([^,\\s&}]+)")
                    .matcher(sanitized)
                    .replaceAll("$1***");
            sanitized = Pattern.compile("(?i)(\"" + Pattern.quote(key) + "\"\\s*:\\s*\")([^\"]*)(\")")
                    .matcher(sanitized)
                    .replaceAll("$1***$3");
        }
        return sanitized;
    }
}
