package com.sakura.passage_creator.dashboard.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Dashboard 顶部概览指标。
 */
@Data
public class DashboardSummaryVO implements Serializable {

    /**
     * 用户总数，统计未逻辑删除的全部用户。
     */
    private Long userTotalCount;

    /**
     * 今日新增用户数，按服务器业务时区自然日统计。
     */
    private Long todayNewUserCount;

    /**
     * 后台管理员可见通知数量。
     */
    private Long notificationCount;

    /**
     * 管理员操作日志总数。
     */
    private Long operationLogCount;

    private static final long serialVersionUID = 1L;
}
