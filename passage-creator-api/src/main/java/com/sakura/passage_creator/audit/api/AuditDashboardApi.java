package com.sakura.passage_creator.audit.api;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 审计模块供 Dashboard 使用的窄口径查询 API。
 */
public interface AuditDashboardApi {

    /**
     * 查询成功登录趋势。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param bucketCount 桶数量
     * @param zoneId 业务时区
     * @return 登录趋势桶
     */
    List<DashboardLoginTrendBucket> listSuccessfulLoginTrend(Date startTime, Date endTime, int bucketCount, ZoneId zoneId);

    /**
     * 查询最近管理员操作日志。
     *
     * @param limit 最大返回数量
     * @return 最近操作日志
     */
    List<DashboardRecentOperation> listRecentOperations(int limit);

    /**
     * 统计管理员操作日志数量。
     *
     * @return 管理员操作日志数量
     */
    long countAdminOperationLogs();
}
