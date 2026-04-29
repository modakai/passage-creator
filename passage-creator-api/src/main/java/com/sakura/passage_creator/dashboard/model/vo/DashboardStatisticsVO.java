package com.sakura.passage_creator.dashboard.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 管理端 Dashboard 统计响应。
 */
@Data
public class DashboardStatisticsVO implements Serializable {

    /**
     * 顶部概览指标。
     */
    private DashboardSummaryVO summary;

    /**
     * 成功登录趋势。
     */
    private List<DashboardLoginTrendVO> loginTrend = new ArrayList<>();

    /**
     * 最近操作日志。
     */
    private List<DashboardRecentOperationVO> recentOperations = new ArrayList<>();

    /**
     * 后端采样时间。
     */
    private Date sampleTime;

    private static final long serialVersionUID = 1L;
}
