package com.sakura.passage_creator.audit.api;

import java.util.Date;

/**
 * Dashboard 登录趋势桶。
 *
 * @param label 展示标签
 * @param startTime 桶开始时间
 * @param endTime 桶结束时间
 * @param loginCount 成功登录次数
 */
public record DashboardLoginTrendBucket(String label, Date startTime, Date endTime, long loginCount) {
}
