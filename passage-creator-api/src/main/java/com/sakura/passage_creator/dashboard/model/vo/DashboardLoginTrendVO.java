package com.sakura.passage_creator.dashboard.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Dashboard 登录趋势点。
 */
@Data
public class DashboardLoginTrendVO implements Serializable {

    /**
     * 前端图表展示标签。
     */
    private String label;

    /**
     * 趋势桶开始时间。
     */
    private Date startTime;

    /**
     * 趋势桶结束时间。
     */
    private Date endTime;

    /**
     * 成功登录次数。
     */
    private Long loginCount;

    private static final long serialVersionUID = 1L;
}
