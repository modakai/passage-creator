package com.sakura.passage_creator.dashboard.service;

import com.sakura.passage_creator.dashboard.model.vo.DashboardStatisticsVO;

import java.time.Instant;

/**
 * Dashboard 统计聚合服务。
 */
public interface DashboardStatisticsService {

    /**
     * 查询当前 Dashboard 统计。
     *
     * @return Dashboard 统计响应
     */
    DashboardStatisticsVO getStatistics();

    /**
     * 按指定采样时间查询 Dashboard 统计，便于测试固定时间口径。
     *
     * @param sampleInstant 采样时间
     * @return Dashboard 统计响应
     */
    DashboardStatisticsVO getStatistics(Instant sampleInstant);
}
